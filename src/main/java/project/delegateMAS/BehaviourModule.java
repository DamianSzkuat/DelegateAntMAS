package project.delegateMAS;

import project.agents.PackageAgent;
import project.agents.ReservationProposal;

public abstract class BehaviourModule {

	private PackageAgent packageAgent;
	
	BehaviourModule(PackageAgent packageAgent) {
		setPackageAgent(packageAgent);
	}
	
	public abstract void findPotentialTransports();
	
	public abstract void reserveTransport(ReservationProposal reservation);
	
	public PackageAgent getPackageAgent() {
		return this.packageAgent;
	}
	
	private void setPackageAgent(PackageAgent packageAgent) {
		this.packageAgent = packageAgent;
	}
}
