package project.interfaces;

import java.util.Map;

import javax.measure.Measure;
import javax.measure.quantity.Length;

import com.github.rinde.rinsim.core.model.comm.CommUser;

import project.agents.AgvAgent;
import project.smartMessage.SmartMessage;

public interface ExecutionContext {
	
	public void deploySmartMessage(SmartMessage s);
	
	public void migrateSmartMessage(SmartMessage s, CommUser destination);
				
	public Map<CommUser, Integer> getReachableNodes();
	
	public Measure<Double,Length> getDistanceToNode(CommUser reachableDestination, AgvAgent finalDestination);
	
	public AgvAgent getAgentInCharge();
}
