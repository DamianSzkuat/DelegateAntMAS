package project.smartMessage;

import project.agents.AgvAgent;
import project.interfaces.ExecutionContext;

public abstract class Behaviour {

	private AgvAgent executionContext;
	private AntAgent antAgent;
	
	Behaviour(AgvAgent executionContext, AntAgent antAgent){
		setExecutionContext(executionContext);
		setAntAgent(antAgent);
	}
	
	abstract void run(AgvAgent executionContext);
	
	AgvAgent getExecutionContext() {
		return this.executionContext;
	}
	
	void setExecutionContext(AgvAgent executionContext) {
		if (executionContext instanceof ExecutionContext) {
			this.executionContext = executionContext;
		} else {
			throw new IllegalArgumentException("The provided obeject dos not implement the required ExecutionContext interface");
		}
	}
	
	AntAgent getAntAgent() {
		return this.antAgent;
	}
	
	void setAntAgent(AntAgent antAgent) {
		this.antAgent = antAgent;
	}
}
