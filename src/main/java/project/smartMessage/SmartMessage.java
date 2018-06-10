package project.smartMessage;

import project.agents.AgvAgent;

public class SmartMessage {
	
	private AgvAgent executionContext;
	private ReproductiveBehaviour reproductiveBehaviour;
	private ComputationalBehaviour computationalBehaviour;
	private MigratoryBehaviour migratoryBehaviour;
	
	private final int MAX_HOPS = 5;
	private final int MAX_MIGRATE_ATTEMPTS = 50;
	
	SmartMessage(AgvAgent executionContext) {
		setExecutionContext(executionContext);
	}
	
	public void run() {
		// TODO
		computationalBehaviour.run(executionContext);
		reproductiveBehaviour.run(executionContext);
		migratoryBehaviour.run(executionContext);
	}
	
	ReproductiveBehaviour getReproductiveBehaviour() {
		return this.reproductiveBehaviour;
	}
	
	void setReproductionBehaviour(ReproductiveBehaviour reproductiveBehaviour) {
		this.reproductiveBehaviour = reproductiveBehaviour;
	}
	
	ComputationalBehaviour getComputationalBehaviour() {
		return this.computationalBehaviour;
	}
	
	void setComputationalBehaviour(ComputationalBehaviour computationalBehaviour) {
		this.computationalBehaviour = computationalBehaviour;
	}
	
	MigratoryBehaviour getMigratoryBehaviour() {
		return this.migratoryBehaviour;
	}
	
	void setMigratoryBehaviour(MigratoryBehaviour migratoryBehaviour) { 
		this.migratoryBehaviour = migratoryBehaviour;
	}
	
	AgvAgent getExecutionContext() {
		return this.executionContext;
	}
	
	void setExecutionContext(AgvAgent executionContext) {
		this.executionContext = executionContext;
	}

	int getMaxHops() {
		return this.MAX_HOPS;
	}
	
	int getMaxMigrateAttempts() {
		return this.MAX_MIGRATE_ATTEMPTS;
	}
}


















