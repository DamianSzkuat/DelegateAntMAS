package project.smartMessage;

import project.agents.AgvAgent;

public abstract class ComputationalBehaviour extends Behaviour{

	ComputationalBehaviour(AgvAgent executionContext, AntAgent antAgent) {
		super(executionContext, antAgent);
	}

	abstract void compute(); 
	
}
