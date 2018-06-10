package project.smartMessage;

import com.google.common.base.Optional;

import project.agents.AgvAgent;
import project.agents.ReservationProposal;
import project.agents.TransportReservation;
import project.delegateMAS.DelegateAntMAS;

public class AntAgent extends SmartMessage{
	
	AntState antState;
	AgvAgent pheromoneInfrastructure;
	DelegateAntMAS swarmMgmt;
	
	boolean isTerminated;
	
	/**
	 * Creates a new AntAgent. This Ant will act as an Exploration Ant until 
	 * it's bahaviour is transformed to act as a Intention Ant
	 */
	public AntAgent(AgvAgent executionContext, AgvAgent pheromoneInfrastructure, DelegateAntMAS swarmMgmt) {
		super(executionContext);
		this.isTerminated = false;
		setSwarmMgmt(swarmMgmt);
		getSwarmMgmt().addAntToSwarm(this);
		setPheromoneInfrastructure(pheromoneInfrastructure);
		setState(new AntState());
		getState().prohibitReproduction();
		setComputationalBehaviour(new ExplorationAntComputationalBehaviour(getExecutionContext(), this));
		setReproductionBehaviour(new ExplorationAntReproductiveBehaviour(getExecutionContext(), this));
		setMigratoryBehaviour(new ExplorationAntMigratoryBehaviour(getExecutionContext(), this));
		
	}
	
	AntAgent(AgvAgent executionContext, AgvAgent pheromoneInfrastructure, AntState state, DelegateAntMAS swarmMgmt){
		this(executionContext, pheromoneInfrastructure, swarmMgmt);				
		// ReservationProposals
		for (ReservationProposal proposal : state.getReservationProposals()) {
			this.getState().addReservationProposal(proposal);
		}
				
		// Visited Agents
		for (AgvAgent agent : state.getVisitedAGVs()) {
			this.getState().addVisitedAGV(agent);
		}
				
		// Hop counter
		state.getHopCounter();
		for(int hops = 0; hops<=state.getHopCounter(); hops++) {
			this.getState().incrementHopCounter();
		}
				
		// Forbid reproduction
		this.getState().prohibitReproduction();
		}
	
	@Override
	public void run() {
		if(!getState().isIdle() && !isTerminated()) {
			getComputationalBehaviour().run(getExecutionContext());
			getReproductiveBehaviour().run(getExecutionContext());
			getMigratoryBehaviour().run(getExecutionContext());
		}
		
	}
	
	/**
	 * Transforms the Ant into an Intention Ant.
	 */
	public void transformIntoIntentionAnt(TransportReservation transportReservation) {
		//TODO
		getState().changeToIntentionAnt();
		getState().setTransportReservation(Optional.of(transportReservation));
		setComputationalBehaviour(new IntentionAntComputationalBehaviour(getExecutionContext(), this));
		setReproductionBehaviour(new IntentionAntReproductiveBehaviour(getExecutionContext(), this));
		setMigratoryBehaviour(new IntentionAntMigratoryBehaviour(getExecutionContext(), this));
		getState().setIdle(false, this);
	}
	
	public void notifyOfMigrationCancel() {
		getState().setIdle(false, this);
	}
	
	public void changeExecutionContext(AgvAgent executionContext, AgvAgent pheromoneInfrastructure) {
		setExecutionContext(executionContext);
		setPheromoneInfrastructure(pheromoneInfrastructure);
	}
	
	AgvAgent getPheromoneInfrastructure() {
		return this.pheromoneInfrastructure;
	}
	
	private void setPheromoneInfrastructure(AgvAgent pheromoneInfrastructure) {
		this.pheromoneInfrastructure = pheromoneInfrastructure;
	}
	
	public AntState getState() {
		return this.antState;
	}
	
	void setState(AntState state) {
		this.antState = state;
	}
	
	DelegateAntMAS getSwarmMgmt() {
		return this.swarmMgmt;
	}
	
	private void setSwarmMgmt(DelegateAntMAS swarmMgmt) {
		this.swarmMgmt = swarmMgmt;
	}
	
	public boolean isTerminated() {
		return this.isTerminated;
	}
	
	public void terminate() {
		// Actually terminating ants would leads to 
		// so problems with threads where ant pass 
		// a check if they are not terminated but are acutally 
		// terminated on the next line of code resulting 
		// in nullpointer exceptions
		this.isTerminated = true;
	}
}
