package project.smartMessage;

import java.util.Map;

import javax.measure.Measure;
import javax.measure.quantity.Length;

import com.github.rinde.rinsim.core.model.comm.CommUser;

import project.agents.AgvAgent;

public class IntentionAntMigratoryBehaviour extends MigratoryBehaviour{

	IntentionAntMigratoryBehaviour(AgvAgent executionContext, AntAgent antAgent) {
		super(executionContext, antAgent);
	}

	/**
	 * The SmartMessage will migrate to the next ExecutionContext.
	 * 
	 * More concretely, if the SmartMessage is exploring it will move to a random AGV
	 * that is in range. If the SmartMessage is returning to the DelegateMAS it will 
	 * move the the AGV closest to the DelegateMAS position, or, if in range,
	 * move directly to the DelegateMAS.
	 */
	@Override
	void migrate() {		
		if(getExecutionContext().getAgentInCharge() !=
				(AgvAgent) getAntAgent().getState().getTransportReservation().get().getServiceProvider()) {
			
			
			Map<CommUser, Integer> reachableNodes = getExecutionContext().getReachableNodes();
			
			AgvAgent destination = (AgvAgent) getAntAgent().getState().getTransportReservation().get().getServiceProvider();
			
			//AgvAgent closesNodeToDestination = destination;
			// Closest node starts as the initial node
			CommUser closesNodeToDestination = getExecutionContext();
			// Initial Shortest distance if from initial node to goal
			Measure<Double,Length> shortestPath = getExecutionContext().getDistanceToNode(closesNodeToDestination, (AgvAgent) getAntAgent().getState().getTransportReservation().get().getServiceProvider());
			
			for(CommUser commUser : reachableNodes.keySet()){
				if (destination == (AgvAgent) commUser) {
					// Destination found, migrate
					getExecutionContext().migrateSmartMessage(getAntAgent(), commUser);
					return;
				}
				else {
					Measure<Double,Length> currentPath = getExecutionContext().getDistanceToNode(commUser, (AgvAgent) getAntAgent().getState().getTransportReservation().get().getServiceProvider());
					//TODO
					if(currentPath.compareTo(shortestPath) < 0) {
						shortestPath = currentPath;
						closesNodeToDestination = commUser;
					}
				}
			}
			getExecutionContext().migrateSmartMessage(getAntAgent(), closesNodeToDestination);
		}
	}

	@Override
	void run(AgvAgent executionContext) {
		setExecutionContext(executionContext);
		migrate();
	}

}
