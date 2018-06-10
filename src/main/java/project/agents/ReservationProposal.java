package project.agents;

import project.smartMessage.AntAgent;

public class ReservationProposal extends AbstractReservation{
		
	AntAgent antAgent;
	
	public ReservationProposal(PackageAgent client, AgvAgent serviceProvider,
			long expectedPickUpTime, long expectedDeliveryTime, AntAgent antAgent){
		super(client, serviceProvider, expectedPickUpTime, expectedDeliveryTime);
		this.antAgent = antAgent;
	}
	
	public AntAgent getAntAgent() {
		return this.antAgent;
	}
	
	public void setAntAgent(AntAgent ant) {
		this.antAgent = ant;
	}
}
