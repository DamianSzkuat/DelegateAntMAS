package project.smartMessage;

import com.google.common.base.Optional;

import project.agents.AgvAgent;
import project.agents.ReservationProposal;


public class ExplorationAntComputationalBehaviour extends ComputationalBehaviour{

	ExplorationAntComputationalBehaviour(AgvAgent executionContext, AntAgent antAgent) {
		super(executionContext, antAgent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * The SmartMessage will ask the AGV if it can deliver a given package
	 * in a given time span, and if so, how long will it take to do so.
	 * 
	 * If the AGV is capable and willing of delivering the given package to 
	 * its destination the SmartMessage will change its state from "Exploring"
	 * to "Returning". The 
	 */
	@Override
	void compute() {
		// TODO Auto-generated method stub
				
		if(getAntAgent().isTerminated()) {
			return;
		}
		
		// What we need to achieve: 
		// - Find out if the AGVAgent is willing and capable to transport our package
		// -- If so => Save the reservation proposal
		// -- If not => Do nothing
		AgvAgent agent = getExecutionContext().getAgentInCharge();	
		if (!agentAlreadyQuerriedForReservationProposal(agent)) {
						
			Optional<ReservationProposal> reservationProposal = agent.getReservationProposal(getAntAgent().getSwarmMgmt().getPackageAgent(), getAntAgent());
						
			if (reservationProposal.isPresent()) {
								
				getAntAgent().getState().addReservationProposal(reservationProposal.get());
			}
			getAntAgent().getState().addVisitedAGV(agent);
		}
		
		if (this.getAntAgent().getState().getHopCounter() >= this.getAntAgent().getMaxHops() ||
				this.getAntAgent().getState().getUnableToMigrateCounter() >= this.getAntAgent().getMaxMigrateAttempts()) {
			getAntAgent().getSwarmMgmt().infromSwarmOfTaskCompletion(getAntAgent(), getAntAgent().getState().getReservationProposals());
		}
	}

	private boolean agentAlreadyQuerriedForReservationProposal(AgvAgent agent) {
		for (AgvAgent agvAgent : getAntAgent().getState().getVisitedAGVs()) {
			if (agvAgent == agent) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	void run(AgvAgent executionContext) {
		setExecutionContext(executionContext);
		compute();
	}
}