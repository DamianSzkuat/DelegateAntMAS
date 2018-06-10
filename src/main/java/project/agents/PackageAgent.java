package project.agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;

import java.util.Map.Entry;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.road.GraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.GeomHeuristics;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import project.delegateMAS.DelegateAntMAS;
import project.interfaces.ExecutionContext;
import project.messages.AckAgentMessage;
import project.messages.AgentMessage;
import project.messages.Messages;
import project.smartMessage.AntAgent;
import project.smartMessage.SmartMessage;

public class PackageAgent extends Parcel implements CommUser, TickListener, ExecutionContext{
	
	private Optional<Vehicle> currentTransport;
	private Optional<Point> dropOffPosition;
	
	private DelegateAntMAS delegateAntMAS;
	private boolean sendNewPotentialReservationRequest;
	
	Set<ParcelState> staticStates = new HashSet<>();
	Set<ParcelState> movingStates = new HashSet<>();
	
	Map<AntAgent, CommUser> antsToMigrate = new HashMap<>();
	private Set<AntAgent> antAgents;
	
	Optional<CommDevice> commDevice;
	private final double reliability;
	static final double MIN_RELIABILITY = 0.9;
	static final double MAX_RELIABILITY = 1.0;
	private final double range;
	private final RandomGenerator rng;
	static final double MIN_RANGE = 5.0;
	static final double MAX_RANGE = 5.5;
	static final int MAX_RESENDS = 3;
	long lastReceiveTime;
	long lastReservationRequestSendTime;
	static final Integer MAX_AVAILABILITY_SCORE = 20;
	private final Map<CommUser, Integer> reachableAgents;
	static final long LONELINESS_THRESHOLD = 100 * 1000;
	
	public PackageAgent(ParcelDTO parcelDto, RandomGenerator r) {
		super(parcelDto);
		
		currentTransport = Optional.absent();
		dropOffPosition = Optional.absent();
		
		delegateAntMAS = new DelegateAntMAS(r, this);
		sendNewPotentialReservationRequest = true;
		
		// TODO Auto-generated constructor stub
		initParcelStateSets();
		
		rng = r;
		range = MIN_RANGE + rng.nextDouble() * (MAX_RANGE - MIN_RANGE);
		reliability = MIN_RELIABILITY + rng.nextDouble() * (MAX_RELIABILITY - MIN_RELIABILITY);
		
		reachableAgents = new HashMap<>();
		antAgents = new HashSet<>();
	}
	
	public void notifyOfPickUp(Vehicle pickUpVehicle) {
		setCurrentTransport(pickUpVehicle);
	}
	
	public void notifyDropOff(Point dropLocation) {
		setDropOffPosition(dropLocation);
	}
	
	public void notifyDelivery(Point deliveryLocation) {
		setDropOffPosition(deliveryLocation);
	}
		
	private void initParcelStateSets() {
		// Adding static states a.k.a parcel is not moving
		staticStates.add(ParcelState.ANNOUNCED);
		staticStates.add(ParcelState.AVAILABLE);
		staticStates.add(ParcelState.PICKING_UP);
		staticStates.add(ParcelState.DELIVERED);
		movingStates.add(ParcelState.DELIVERING);
		
		// Adding moving states
		movingStates.add(ParcelState.IN_CARGO);
	}
	
	Optional<Vehicle> getCurrentTransport() {
		return this.currentTransport;
	}
	
	void setCurrentTransport(Vehicle pickUpVehicle) {
		currentTransport = Optional.of(pickUpVehicle);
	}
	
	Optional<Point> getDropOffPosition() {
		return this.getDropOffPosition();
	}
	
	void setDropOffPosition(Point dropLocation) {
		dropOffPosition = Optional.of(dropLocation);
	}
	
	public Map<CommUser, Integer> getReachableAgents() {
		return this.reachableAgents;
	}
	
	private void addToReachableAgents(CommUser commUser) {
		if( commUser instanceof AgvAgent ) {
			getReachableAgents().put(commUser, PackageAgent.MAX_AVAILABILITY_SCORE);
		}
	}
	
	public void notifyPotentialTransportsFound(Set<ReservationProposal> allReservationProposals) {
		
		if(allReservationProposals.size() == 0) {
			return;
		}
		
		Iterator<ReservationProposal> iter = allReservationProposals.iterator();
		ReservationProposal best = iter.next();
		while (iter.hasNext()) {
			ReservationProposal res = iter.next();
			if( best.getExpectedDeliveryTime() > res.getExpectedDeliveryTime() ) {
				best = res;
			}
		}
		
		getDelegateAntMAS().reserveTransport(best);
		
		this.sendNewPotentialReservationRequest = true;
	}
	
	private DelegateAntMAS getDelegateAntMAS() {
		return this.delegateAntMAS;
	}
	
	private Set<AntAgent> getAntAgents() {
		return this.antAgents;
	}
	
	/********************************************************************/
	// TickListener                                                     //
	/********************************************************************/
	
	@Override
	public void tick(TimeLapse time) {
		
		if(this.getPDPModel().getParcelState(this) == ParcelState.DELIVERED) {
			return;
		}
		
		// Communication //
		commTick(time);
		
		intentionAntMigration();
	}

	private void commTick(TimeLapse time) {
		ParcelState parcelState = this.getPDPModel().getParcelState(this);
		if(parcelState != ParcelState.ANNOUNCED && parcelState != ParcelState.AVAILABLE) {
			return;
		}
		
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
			
				if(!receivedAgent.getState().isExplorationAnt()) {
				
					if (!getAntAgents().contains(receivedAgent) && !receivedAgent.isTerminated()) {
						receivedAgent.getState().incrementHopCounter();
						receivedAgent.getState().resetUnableToMigrateCounter();
						getAntAgents().add(receivedAgent);
						addToReachableAgents(message.getSender());
					}
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
					
		// when we have non-zero unread messages, we always broadcast "Hello" to everyone within range.
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
	
	private void intentionAntMigration() {
		Iterator<AntAgent> iter = getAntAgents().iterator();
		while(iter.hasNext()) {
			AntAgent ant = iter.next();			
			if(!ant.isTerminated()) {
				CommUser destination = getIntentionAntDestination(ant);
				migrateSmartMessage(ant, destination);
			}
		}
	}
	
	private CommUser getIntentionAntDestination(AntAgent ant) {
		
		Map<CommUser, Integer> reachableNodes = getReachableNodes();
		
		AgvAgent destination = (AgvAgent) ant.getState().getTransportReservation().get().getServiceProvider();
		
		CommUser closesNodeToDestination = this;
		Measure<Double,Length> shortestPath = getDistanceToNode(closesNodeToDestination, (AgvAgent) ant.getState().getTransportReservation().get().getServiceProvider());
		
		for(CommUser commUser : reachableNodes.keySet()){
			if (destination == (AgvAgent) commUser) {
				// Destination found, migrate
				return commUser;
			}
			else {
				Measure<Double,Length> currentPath = getDistanceToNode(commUser, (AgvAgent) ant.getState().getTransportReservation().get().getServiceProvider());
				//TODO
				if(currentPath.compareTo(shortestPath) < 0) {
					shortestPath = currentPath;
					closesNodeToDestination = commUser;
				}
			}
		}
		return closesNodeToDestination;
	}
	
	@Override
	public void afterTick(TimeLapse time) {
		if ((sendNewPotentialReservationRequest && !getReachableAgents().keySet().isEmpty())||
				time.getStartTime() - lastReservationRequestSendTime > LONELINESS_THRESHOLD) {
			sendNewPotentialReservationRequest = false;
			lastReservationRequestSendTime = time.getStartTime();
			getDelegateAntMAS().findPotentialTransports();
		}
	}
	
	/********************************************************************/
	// CommUser                                                         //
	/********************************************************************/
	
	@Override
	public Optional<Point> getPosition() {
		ParcelState parcelState = this.getPDPModel().getParcelState(this);
		
		if(staticStates.contains(parcelState)) {
			if (dropOffPosition.isPresent()) {
				return dropOffPosition;
			} else {
				return Optional.of(this.getPickupLocation());
			}
		}
		else if(movingStates.contains(parcelState)) {
			return Optional.of(this.getRoadModel().getPosition(currentTransport.get()));
		}
		else {
			if(currentTransport.isPresent()) {
				return Optional.of(this.getRoadModel().getPosition(currentTransport.get()));
			} else {
				return Optional.of(this.getDeliveryLocation());
			}
			//throw new IllegalArgumentException("The parcel state is invalid.");
		}
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
	// ExecutionContext                                                 //
	/********************************************************************/
	
	@Override
	public void deploySmartMessage(SmartMessage s) {
	}

	@Override
	public void migrateSmartMessage(SmartMessage s, CommUser destination) {
		this.antsToMigrate.put((AntAgent) s, destination);
	}

	@Override
	public Map<CommUser, Integer> getReachableNodes() {
		return null;
	}

	@Override
	public Measure<Double, Length> getDistanceToNode(CommUser reachableDestination, AgvAgent finalDestination) {

		Measure<Double, Velocity> maxSpeed = Measure.valueOf(finalDestination.getSpeed(), getRoadModel().getSpeedUnit());
				
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
		
		List<Point> path = new LinkedList<>(getRoadModel().getPathTo(from, to, finalDestination.timeUnit, maxSpeed,
																	 GeomHeuristics.euclidean()).getPath());
		
		Measure<Double,Length> distance = getRoadModel().getDistanceOfPath(path);
		
		return distance;
	}
	
	@Override
	public AgvAgent getAgentInCharge() {
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
}
