package project.smartMessage;

import project.agents.AgvAgent;

public class IntentionAntComputationalBehaviour extends ComputationalBehaviour{

	IntentionAntComputationalBehaviour(AgvAgent executionContext, AntAgent antAgent) {
		super(executionContext, antAgent);
	}

	@Override
	void compute() {		
		if(getExecutionContext().getAgentInCharge() == 
				(AgvAgent) getAntAgent().getState().getTransportReservation().get().getServiceProvider()) {
			getAntAgent().getPheromoneInfrastructure().drop(getAntAgent().getState().getTransportReservation().get());
			getAntAgent().getSwarmMgmt().infromSwarmOfPheromoneDrop(getAntAgent());
			getAntAgent().terminate();
		}
	}

	@Override
	void run(AgvAgent executionContext) {
		setExecutionContext(executionContext);
		compute();
	}
}
