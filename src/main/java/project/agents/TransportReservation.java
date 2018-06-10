package project.agents;

public class TransportReservation extends AbstractReservation{
	
	public TransportReservation(PackageAgent client, AgvAgent serviceProvider,
			long expectedPickUpTime, long expectedDeliveryTime){
		super(client, serviceProvider, expectedPickUpTime, expectedDeliveryTime);
	}
	
}
