package project.smartMessage;

import project.agents.AgvAgent;

public class ExplorationAntReproductiveBehaviour extends ReproductiveBehaviour{
		
	ExplorationAntReproductiveBehaviour(AgvAgent executionContext, AntAgent antAgent) {
		super(executionContext, antAgent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Executes the reproductive behaviour of the exploration ant.
	 */
	@Override
	void reproduce() {
		// TODO Auto-generated method stub
		if(getAntAgent().getState().canReproduce()) {
			if(this.getAntAgent().getState().getHopCounter() < this.getAntAgent().getMaxHops()) {
				getExecutionContext().deploySmartMessage(new AntAgent(getExecutionContext(),
						  getAntAgent().getPheromoneInfrastructure(),
						  getAntAgent().getState(),
						  getAntAgent().getSwarmMgmt()));
				getAntAgent().getState().prohibitReproduction();
			}
		}
	}

	@Override
	void run(AgvAgent executionContext) {
		// TODO Auto-generated method stub
		setExecutionContext(executionContext);
		reproduce();
	}

}
