package project.delegateMAS;

import project.agents.PackageAgent;
import project.agents.ReservationProposal;

/**
 * The DelegateMAS class will manage SmartMessages and use them to 
 * fulfill an objective or task assigned by an agent
 * 
 * @author Damian Szkuat
 *
 */
public class DelegateMAS extends BehaviourModule{

	DelegateMAS(PackageAgent packageAgent) {
		super(packageAgent);
	}
	
	
	@Override
	public void findPotentialTransports() {
	}

	@Override
	public void reserveTransport(ReservationProposal reservation) {		
	}
}
