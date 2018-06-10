package project.agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.GeomHeuristics;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import project.exceptions.PheromoneExpiredException;
import project.interfaces.ExecutionContext;
import project.interfaces.PheromoneInfrastructure;
import project.messages.AckAgentMessage;
import project.messages.AgentMessage;
import project.messages.Messages;
import project.smartMessage.AntAgent;
import project.smartMessage.Pheromone;
import project.smartMessage.SmartMessage;

public class AgvAgent extends Vehicle implements ExecutionContext, PheromoneInfrastructure, CommUser{
	
	// Variables associated with the movement of the AGV
	private static final double SPEED = 1d;
	RoadModel roadModel;
	
	// Variables associated with the communication of the AGV
	Optional<CommDevice> commDevice;
	private final double reliability;
	static final double MIN_RELIABILITY = 0.9;
	static final double MAX_RELIABILITY = 1.0;
	private final double range;
	static final double MIN_RANGE = 5;
	static final double MAX_RANGE = 5.5;
	static final long LONELINESS_THRESHOLD = 10 * 1000;
	long lastReceiveTime;
	private final Map<CommUser, Integer> reachableAgents;
	static final Integer MAX_AVAILABILITY_SCORE = 20;
	private Set<AntAgent> antAgents;
	private Set<AntAgent> agentsToBeDeployed;
	Map<AntAgent, CommUser> antsToMigrate = new HashMap<>();
	
	// Variables associated with the planning of the AGV
	private AgvScheduler scheduler;
	private float bateryCharge;
	private Optional<PackageAgent> packageAgent;
	private long currentTime;

	// Various
	private final RandomGenerator rng;
	private Optional<Point> randomDestination;
	private Queue<Point> randomPath;
	public Unit<Duration> timeUnit;

	public AgvAgent(Point startPosition, int capacity, RandomGenerator r, RoadModel roadModel) {
		super(VehicleDTO.builder()
			      		.capacity(capacity)
			      		.startPosition(startPosition)
			      		.speed(SPEED)
			      		.build());
		packageAgent = Optional.absent();
		commDevice = Optional.absent();
		randomDestination = Optional.absent();
		scheduler = new AgvScheduler(this);
		rng = r;
		range = MIN_RANGE + rng.nextDouble() * (MAX_RANGE - MIN_RANGE); 
		reliability = MIN_RELIABILITY + rng.nextDouble() * (MAX_RELIABILITY - MIN_RELIABILITY);
		reachableAgents = new HashMap<>();
		this.roadModel = roadModel;
		antAgents = new HashSet<>();
		agentsToBeDeployed = new HashSet<>();
		randomDestination = Optional.absent();
		randomPath = new LinkedList<>();
	}

	public Map<CommUser, Integer> getReachableAgents() {
		return this.reachableAgents;
	}
	
	private void addToReachableAgents(CommUser commUser) {
		if( commUser instanceof AgvAgent ) {
			getReachableAgents().put(commUser, PackageAgent.MAX_AVAILABILITY_SCORE);
		}
	}
	
	private AgvScheduler getScheduler() {
		return this.scheduler;
	}
	
	long getCurrentTime() {
		return this.currentTime;
	}
	
	/**
	 * Performs one "tick" of evaporation on the Pheromones
	 * 
	 * @throws PheromoneExpiredException
	 */
	private void evaporateReservations() throws PheromoneExpiredException {
		getScheduler().evaporateReservations();
	}
	
	private Set<AntAgent> getAntAgents() {
		return this.antAgents;
	}
	
	private Set<AntAgent> getAntAgentsToBeDeployed() {
		return this.agentsToBeDeployed;
	}
	
	private void removeTerminatedAnts() {
				
		Iterator<AntAgent> iter1 = getAntAgents().iterator();
		while (iter1.hasNext()) {
			if(iter1.next().isTerminated()) {
				iter1.remove();
			}
		}
		
		Iterator<Entry<AntAgent, CommUser>> iter = this.antsToMigrate.entrySet().iterator();
		while (iter.hasNext()) {		
			Map.Entry<AntAgent, CommUser> entry = iter.next();
			if(entry.getKey().isTerminated()) {
				iter.remove();
			}
		}

	}
	
	/********************************************************************/
	// Transport Negotiations                                           //
	/********************************************************************/
	
	public Optional<ReservationProposal> getReservationProposal(PackageAgent parcel, AntAgent antAgent){
		return getScheduler().getReservationProposal(parcel, antAgent);
	}
	
	long getTimeToReachGivenPoint(Point point) {

		Measure<Double, Velocity> maxSpeed = Measure.valueOf(getSpeed(), getRoadModel().getSpeedUnit());
		
		List<Point> path = new LinkedList<>(getRoadModel().getPathTo(this, point, this.timeUnit, maxSpeed,
																	 GeomHeuristics.euclidean()).getPath());
		
		Measure<Double,Length> distance = getRoadModel().getDistanceOfPath(path);	
		long time = (long) (distance.getValue()/AgvAgent.SPEED);
		return time;
	}
	
	long getTimeToMoveBetweenTwoPoints(Point start, Point end) {
		Measure<Double,Length> distance = getRoadModel().getDistanceOfPath(getRoadModel().getShortestPathTo(start, end));
		long time = (long) (distance.getValue()/AgvAgent.SPEED);
		return time;
	}
	
	/********************************************************************/
	// Vehicle                                                          //
	/********************************************************************/
	
	@Override
	public void afterTick(TimeLapse timeLapse) {
	}
	
	@Override
	protected void tickImpl(TimeLapse time) {
		currentTime = time.getStartTime();
		timeUnit = time.getTimeUnit();
						
		// Planning //
		planningTick();
		
		// Movement // 
		movementTick(time);

		// Communication //
		commTick(time);
		
		// "Deploy" the ant agents
		deployAnts();
		
		// Remove terminated ants
		removeTerminatedAnts();
				
		//Let the ants do their work
		Iterator<AntAgent> iter = getAntAgents().iterator();
		while(iter.hasNext()) {
			AntAgent ant = iter.next();			
			if(!ant.isTerminated()) {
				ant.run();
			}
		}
		
		// Evaporate the reservations a little
		try {
			evaporateReservations();
		} catch (PheromoneExpiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void planningTick() {
		
	}
	
	private void movementTick(TimeLapse time) {
		roadModel = getRoadModel();
		final PDPModel pdpModel = getPDPModel();
		
		// Is there time left to consume? 
		if (!time.hasTimeLeft()) {
			// Stop if not
			return;
		}
		
		// Is there a parcel on the AGV?
		if (!packageAgent.isPresent()) {
			// If not look for one 
			Optional<TransportReservation> reservation = getScheduler().getNextReservation();
			if(reservation.isPresent()) {
				packageAgent = Optional.of(reservation.get().getClient());
			} else {
				
				CollisionGraphRoadModelImpl model = (CollisionGraphRoadModelImpl) getRoadModel();
			   
				if (!randomDestination.isPresent()) {
				      nextRandomDestination(model);
				    }

				getRoadModel().moveTo(this, randomDestination.get(), time);


			    if (model.getPosition(this).equals(randomDestination.get())) {
			      nextRandomDestination(model);
			    }
			}
		}
		// Is there a parcel on the AGV?
		if (packageAgent.isPresent()) {
			// If yes, check if it is in the container.
			final boolean inCargo = pdpModel.containerContains(this, packageAgent.get());
			
			// Sanity check: if it is not in our cargo AND it is also not on the
		    // RoadModel, we cannot go to curr anymore.
			if (!inCargo && !roadModel.containsObject(packageAgent.get())) {
				packageAgent = Optional.absent();
			}
			else if (inCargo) {
				// If the parcel is in cargo, move to its destination
				roadModel.moveTo(this, packageAgent.get().getDeliveryLocation(), time);
				
				// If we arrived at the parcel delivery location
				if (roadModel.getPosition(this).equals(packageAgent.get().getDeliveryLocation())) {
					// Deliver the parcel
					pdpModel.deliver(this, packageAgent.get(), time);
					packageAgent.get().notifyDropOff(roadModel.getPosition(this));
				}
			} else {
				// The parcel is still available, go to pick it up
				roadModel.moveTo(this, packageAgent.get(), time);
				// If you are at the parcel position
				if (roadModel.equalPosition(this, packageAgent.get())) {
					// Pick up parcel
					packageAgent.get().notifyOfPickUp(this);
					pdpModel.pickup(this, packageAgent.get(), time);
				}
			}
		}
	}
	
	void nextRandomDestination(CollisionGraphRoadModelImpl model) {
		randomDestination = Optional.of(model.getRandomPosition(rng));
		
		Point end = randomDestination.get();
		
		Point start = model.getPosition(this);
		if (model.getConnection(this).isPresent()) {
			  start = model.getConnection(this).get().to();
		}
		
	    randomPath = new LinkedList<>(model.getShortestPathTo(start, end));
	  }
	
	private void commTick(TimeLapse time) {
				
		HashSet<CommUser> currentSenders = new HashSet<>();
				
		// Check if there are messages 
		if (commDevice.get().getUnreadCount() > 0) {
			currentSenders = handleUnreadMessages(time);
		}
		else if (commDevice.get().getReceivedCount() == 0) {
			// Send first message
			commDevice.get().broadcast(Messages.HELLO);
		}
		else if (time.getStartTime() - lastReceiveTime > LONELINESS_THRESHOLD) {
			// when we haven't received anything for a while, we become anxious :(
			commDevice.get().broadcast(Messages.HELLO);
		}
		
		// Reachable agent management
		manadgeReachableAgents(currentSenders);
		
		// Migrating smart messages 
		migrateSmartMessages();
	}
	
	private HashSet<CommUser> handleUnreadMessages(TimeLapse time) {
		// Update the time of the last received mesage
		lastReceiveTime = time.getStartTime();
		
		// Get the messages 
		final List<Message> messages = commDevice.get().getUnreadMessages();
		
		// Read the messages
		HashSet<CommUser> currentSenders = new HashSet<CommUser>();
		
		for (final Message message : messages) {
			
			currentSenders.add(message.getSender());
			
			if (message.getContents() instanceof AgentMessage) {
				// We received an AgentMessage, this message contains a SmartMessage inside
				// so we need to retrieve it.
				AntAgent receivedAgent = (AntAgent) ((AgentMessage) message.getContents()).retrieveSmartMessage();
				// Check if the agent has not been resent
				if (!getAntAgents().contains(receivedAgent) && !receivedAgent.isTerminated()) {
					receivedAgent.changeExecutionContext(this, this);
					receivedAgent.getState().incrementHopCounter();
					receivedAgent.getState().allowReproduction();
					receivedAgent.getState().resetUnableToMigrateCounter();
					receivedAgent.getState().setIdle(false, receivedAgent);				
					getAntAgents().add(receivedAgent);
					addToReachableAgents(message.getSender());
				}
				// Send back an acknowledgement that the agent has been received
				commDevice.get().send(new AckAgentMessage(receivedAgent), message.getSender());
			} 
			
			else if (message.getContents() instanceof AckAgentMessage) {
				// If we receive the acknowledgement that an agent has been received, we remove it from the Migrate queue
				AntAgent acknowledgedAgent = (AntAgent) ((AckAgentMessage) message.getContents()).retrieveSmartMessage();
				if(antsToMigrate.keySet().contains(acknowledgedAgent)) {
					antsToMigrate.remove(acknowledgedAgent);
				}
			}
			
			else if (message.getContents() == Messages.HELLO) {
				// We received a broadcast HELLO message
				// Send back a HI message
				commDevice.get().send(Messages.HI, message.getSender());
				addToReachableAgents(message.getSender());
			} 
			else if (message.getContents() == Messages.HI) {
				addToReachableAgents(message.getSender());
			}				
		}
		
		// when we have non-zero unread messages, we always broadcast "nice to
		// meet you" to everyone within range.
		commDevice.get().broadcast(Messages.HELLO);
		
		return currentSenders;
	}
	
	private void manadgeReachableAgents(HashSet<CommUser> currentSenders) {		
		Iterator<Entry<CommUser, Integer>> iter = getReachableAgents().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<CommUser, Integer> pair = iter.next();
			
			// If the Agent sent us something, set his availability score to maximum
			if (currentSenders.contains(pair.getKey())) {
				pair.setValue(MAX_AVAILABILITY_SCORE);
			}
			// If the agent did not send anything, lower his availability score by 1
			else {
				pair.setValue(pair.getValue() - 1);
				
				// If the availability score drops to 0, remove the agent from reachable agents.
				if (pair.getValue() <= 0) {
					iter.remove();
				}
			}	
		}
	}
	
	private void migrateSmartMessages() {
		// Migrating smart messages 
		Iterator<Map.Entry<AntAgent, CommUser>> entries = antsToMigrate.entrySet().iterator();
		while(entries.hasNext()){
			Map.Entry<AntAgent, CommUser> entry = entries.next();
			if (entry.getValue() instanceof AgvAgent && getReachableAgents().containsKey(entry.getValue())) {
				commDevice.get().send(new AgentMessage(entry.getKey()), entry.getValue());
			} 
			else {
				entry.getKey().notifyOfMigrationCancel();
				entries.remove();
			}
		}
	}
	
	private void deployAnts() {
		for (AntAgent ant : getAntAgentsToBeDeployed()) {
			if (!ant.isTerminated()) {
				getAntAgents().add(ant);
			}
		}
	}
	
	/********************************************************************/
	// Execution Context                                                //
	/********************************************************************/	
	
	@Override
	public void deploySmartMessage(SmartMessage s) {
		getAntAgentsToBeDeployed().add((AntAgent) s);
	}

	@Override
	public void migrateSmartMessage(SmartMessage s, CommUser destination) {
		this.antsToMigrate.put((AntAgent) s, destination);
	}

	@Override
	public Map<CommUser, Integer> getReachableNodes() {
		return this.reachableAgents;
	}
	
	@Override
	public AgvAgent getAgentInCharge() {
		return this;
	}
	
	@Override
	public Measure<Double,Length> getDistanceToNode(CommUser reachableDestination, AgvAgent finalDestination) {

		Measure<Double, Velocity> maxSpeed = Measure.valueOf(getSpeed(), getRoadModel().getSpeedUnit());
				
		GraphRoadModelImpl model = (GraphRoadModelImpl) getRoadModel();
		
		Optional<? extends Connection<?>> conn1 = model.getConnection((RoadUser) reachableDestination);
		
		Point from;
		if(conn1.isPresent()) {
			from = conn1.get().to();
		} else {
			from = reachableDestination.getPosition().get();
		}
		
		Optional<? extends Connection<?>> conn2 = model.getConnection(finalDestination);
		
		Point to;
		if(conn2.isPresent()) {
			to = conn2.get().to();
		} else {
			to = finalDestination.getPosition().get();
		}
		
		List<Point> path = new LinkedList<>(getRoadModel().getPathTo(from, to, this.timeUnit, maxSpeed,
																	 GeomHeuristics.euclidean()).getPath());
		Measure<Double,Length> distance = getRoadModel().getDistanceOfPath(path);
		
		return distance;
	}
	
	/********************************************************************/
	// CommUser                                                         //
	/********************************************************************/
	
	@Override
	public Optional<Point> getPosition() {
	    if (roadModel.containsObject(this)) {
	    	return Optional.of(roadModel.getPosition(this));
	    }
	    return Optional.absent();
	}

	@Override
	public void setCommDevice(CommDeviceBuilder builder) {
	    if (range >= 0) {
	    	builder.setMaxRange(range);
	    }
	    commDevice = Optional.of(builder
	    				.setReliability(reliability)
	    				.build());
	}

	/********************************************************************/
	// PheromoneInfrastructure                                          //
	/********************************************************************/
	
	@Override
	public void drop(Pheromone p) {
		TransportReservation reservation = (TransportReservation) p;
		getScheduler().dropPheromone(reservation);
	}

}











