package project.smartMessage;

import project.agents.AgvAgent;

public class IntentionAntReproductiveBehaviour extends ReproductiveBehaviour{

	IntentionAntReproductiveBehaviour(AgvAgent executionContext, AntAgent antAgent) {
		super(executionContext, antAgent);
	}

	/**
	 * Executes the reproductive behaviour of the exploration ant.
	 */
	@Override
	void reproduce() {
		// Intention ants do not reproduce themselves
	}

	@Override
	void run(AgvAgent executionContext) {
		setExecutionContext(executionContext);
		reproduce();
	}

}
