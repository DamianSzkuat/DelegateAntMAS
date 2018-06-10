package project.smartMessage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.github.rinde.rinsim.core.model.comm.CommUser;

import project.agents.AgvAgent;

public class ExplorationAntMigratoryBehaviour extends MigratoryBehaviour{

	ExplorationAntMigratoryBehaviour(AgvAgent executionContext, AntAgent antAgent) {
		super(executionContext, antAgent);
		// TODO Auto-generated constructor stub
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
		// See if we can hop again.	
				
		if(this.getAntAgent().getState().getHopCounter() < this.getAntAgent().getMaxHops() &&
				getExecutionContext().getReachableNodes().keySet().size() > 0) {
			
			// We want to chose the right Agent to migrate to
			// As we are exploring, there is no preferred agent 
			// since we don't know who can offer us services. 
			// Agents to migrate to are thus chosen at random.
			Map<CommUser, Integer> reachableNodes = getExecutionContext().getReachableNodes();
			Iterator<Entry<CommUser, Integer>> iter1 = reachableNodes.entrySet().iterator();
			
			Set<CommUser> reachableUnvisitedNodes = new HashSet<>();
						
			while(iter1.hasNext()) {
				CommUser commUser = iter1.next().getKey();
				if(!getAntAgent().getState().getVisitedAGVs().contains(commUser)) {
					reachableUnvisitedNodes.add(commUser);
				}
			}			
			if(reachableUnvisitedNodes.size()==0) {
				getAntAgent().getState().incrementUnableToMigrateCounter();
				return;
			}
						
			int size = reachableUnvisitedNodes.size();
			int item = new Random().nextInt(size);
			int i = 0;
			CommUser destination = null;
						
			Iterator<CommUser> iter2 = reachableUnvisitedNodes.iterator();			
			while(iter2.hasNext()) {
				if (i == item) {
			    	destination = iter2.next();
					break;
				}
			    i++;
			}
			getExecutionContext().migrateSmartMessage(getAntAgent(), destination);
		}
		getAntAgent().getState().incrementUnableToMigrateCounter();
	}

	@Override
	void run(AgvAgent executionContext) {
		setExecutionContext(executionContext);
		migrate();
	}
}
