package project.smartMessage;

import project.agents.AgvAgent;

public abstract class ReproductiveBehaviour extends Behaviour{

	ReproductiveBehaviour(AgvAgent executionContext, AntAgent antAgent) {
		super(executionContext, antAgent);
	}

	abstract void reproduce();

}
