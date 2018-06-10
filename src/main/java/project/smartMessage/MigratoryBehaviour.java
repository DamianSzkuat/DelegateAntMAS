package project.smartMessage;

import project.agents.AgvAgent;

public abstract class MigratoryBehaviour extends Behaviour{

	MigratoryBehaviour(AgvAgent executionContext, AntAgent antAgent) {
		super(executionContext, antAgent);
	}

	abstract void migrate();
}
