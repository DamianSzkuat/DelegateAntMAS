package project.delegateMAS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.comm.CommUser;

import project.agents.AgvAgent;
import project.agents.PackageAgent;
import project.agents.ReservationProposal;
import project.agents.TransportReservation;
import project.smartMessage.AntAgent;

public class DelegateAntMAS extends DelegateMAS{
	
	Map<AntAgent, Boolean> swarm;
	Set<ReservationProposal> allReservationProposals;
	final int MAX_REQUESTS = 10;
	int counter;
	
	//TEST 
	boolean readyForNewRequest;
	
	public DelegateAntMAS(RandomGenerator r, PackageAgent packageAgent) {
		super(packageAgent);
		swarm = new HashMap<>();
		allReservationProposals = new HashSet<>();
		counter = 0;
		readyForNewRequest = true;
	}
	
	/**
	 * Asks the Delegate Ant MAS to finds potential transport opportunities 
	 * for a parcel.
	 */
	@Override
	public void findPotentialTransports() {
				
		counter += 1;
		if(counter >= MAX_REQUESTS) {
			readyForNewRequest = true;
			counter = 0;
		}
		
		removeDeadAntsFromSwarm();
		
		if(readyForNewRequest || getSwarm().keySet().size() == 0) {
			readyForNewRequest = false;
			
			terminateSwarm();
			removeDeadAntsFromSwarm();
			sendOutInitialAntAgents();
		}
	}

	/**
	 * Asks the Delegate Ant MAS to reserve a previously found transport.
	 */
	@Override
	public void reserveTransport(ReservationProposal reservation) {
		
		if(!getSwarm().keySet().contains(reservation.getAntAgent())) {
			return;
		}		
		TransportReservation chosenRes = new TransportReservation(reservation.getClient(), reservation.getServiceProvider(),
				reservation.getExpectedPickUpTime(), reservation.getExpectedDeliveryTime());
		
		reservation.getAntAgent().transformIntoIntentionAnt(chosenRes);
		
		terminateIdleExplorationAntsAnts(reservation.getAntAgent());
		removeDeadAntsFromSwarm();
		resetReservationProposals();
	}
	
	private void terminateExplorationAnts() {
		Iterator<Entry<AntAgent, Boolean>> iter = getSwarm().entrySet().iterator();
		while (iter.hasNext()) {		
			Map.Entry<AntAgent, Boolean> entry = iter.next();
			if(!entry.getKey().getState().isExplorationAnt()) {
				entry.getKey().terminate();
			}
		}
	}
	
	private void terminateSwarm() {
		Iterator<Entry<AntAgent, Boolean>> iter = getSwarm().entrySet().iterator();
		while (iter.hasNext()) {		
			Map.Entry<AntAgent, Boolean> entry = iter.next();
			entry.getKey().terminate();
		}
	}
	
	/**
	 * Used by exploration ants to inform the DelegateAntMAS they have completed their task and 
	 * to send back their reservation proposals.
	 * 
	 * @param ant
	 * @param reservationProposals
	 */
	public void infromSwarmOfTaskCompletion(AntAgent ant, Set<ReservationProposal> reservationProposals) {
						
		if(!getSwarm().keySet().contains(ant)) {
			throw new IllegalArgumentException("The ant is reporting to a delegateAntMas that does not have the ant in his swarm.");
		}
		
		// Add the reservation proposals
		addToAllReservationProposals(reservationProposals);
		
		// Mark the ant as done with its work
		getSwarm().put(ant, true);
		
		ant.getState().setIdle(true, ant);
		
//		try {
//			swarmConsistencyCheck();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// Check if all ants in the swarm have completed their tasks
		if(checkIfSwarmCompletedAllTasks()) {
			// Inform the Package Agent the task of finding potential reservations is complete
			getPackageAgent().notifyPotentialTransportsFound(getAllReservationProposals());
		}
	}
	
	private void swarmConsistencyCheck() throws Exception {
		Iterator<Entry<AntAgent, Boolean>> iter = getSwarm().entrySet().iterator();
		while (iter.hasNext()) {		
			Map.Entry<AntAgent, Boolean> entry = iter.next();
			
			AntAgent agent = entry.getKey();
			boolean isDoneWithWork = entry.getValue();
			boolean isIdle = entry.getKey().getState().isIdle();
			
			if((isDoneWithWork && !isIdle)|| (!isDoneWithWork && isIdle)) {
				throw new Exception("Incosistent swarm state: " + agent + " isIdle?: " + isIdle + ", isDoneWithWork?: " + isDoneWithWork);
			}
		}
	}
	
	public void infromSwarmOfPheromoneDrop(AntAgent ant) {
		
		
		if(!getSwarm().keySet().contains(ant)) {
			throw new IllegalArgumentException("The ant is reporting to a delegateAntMas that does not have the ant in his swarm.");
		}
		// Mark the ant as done with its work
		getSwarm().put(ant, true);
		ant.getState().setIdle(true, ant);
		
		terminateSwarm();
		removeDeadAntsFromSwarm();
		this.swarm = new HashMap<>();
		readyForNewRequest = true;
	}
	
	/**
	 * Used to add newly created ant to the swarm.
	 * @param ant
	 */
	public void addAntToSwarm(AntAgent ant) {
		// The boolean value represent whether the ant has already reported 
		// back with reservation proposals
		getSwarm().put(ant, false);
	}
	
	private boolean checkIfSwarmCompletedAllTasks() {
		for (boolean b : getSwarm().values()) if (!b) return false;
		return true;
	}
	
	private void sendOutInitialAntAgents() {
		for (CommUser agent : getPackageAgent().getReachableAgents().keySet()) {
			// Create agent 
			AntAgent antAgent = new AntAgent((AgvAgent) agent, (AgvAgent) agent, this);
			// Add Agent to Swarm
			addAntToSwarm(antAgent);
			// Migrate agent to target AGV
			getPackageAgent().migrateSmartMessage(antAgent, agent);
		}
	}
	
	private void terminateIdleExplorationAntsAnts(AntAgent ant) {
		Iterator<Entry<AntAgent, Boolean>> iter = getSwarm().entrySet().iterator();
		while (iter.hasNext()) {		
			Map.Entry<AntAgent, Boolean> entry = iter.next();
			
			AntAgent agent = entry.getKey();
			boolean isDoneWithWork = entry.getValue();
			
			if(agent != ant && isDoneWithWork) {
				entry.getKey().terminate();
			}
		}
	}
	
	private void removeDeadAntsFromSwarm() {
		Iterator<Entry<AntAgent, Boolean>> iter = getSwarm().entrySet().iterator();
		while (iter.hasNext()) {		
			Map.Entry<AntAgent, Boolean> entry = iter.next();
			if(entry.getKey().isTerminated()) {
				iter.remove();
			}
		}
	}
	
	private Map<AntAgent, Boolean> getSwarm() {
		return this.swarm;
	}
	
	private Set<ReservationProposal> getAllReservationProposals(){
		return this.allReservationProposals;
	}

	private void addToAllReservationProposals(Set<ReservationProposal> reservationProposals) {
		getAllReservationProposals().addAll(reservationProposals);
	}
	
	private void resetReservationProposals() {
		this.allReservationProposals = new HashSet<>();
	}
	
	
	
	
	
	
	
	
	
	
	
	
}

