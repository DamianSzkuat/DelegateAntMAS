package project.agents;

import project.smartMessage.Pheromone;

public abstract class AbstractReservation extends Pheromone {
	
	private PackageAgent client;
	private AgvAgent serviceProvider;
	
	private long expectedPickUpTime;
	private long expectedDeliveryTime;
	
	AbstractReservation(PackageAgent client, AgvAgent serviceProvider,
			long expectedPickUpTime, long expectedDeliveryTime){
		super();
		this.client = client;
		this.serviceProvider = serviceProvider;
		this.expectedPickUpTime = expectedPickUpTime;
		this.expectedDeliveryTime = expectedDeliveryTime;
	}
	
	/**
	 * Gets the PDP object that is the "Client" of this reservation.
	 * 
	 * @return PDPObjectImpl client
	 */
	public PackageAgent getClient() {
		return client;
	}
	
	/**
	 * Gets the PDP object that is the provider of the service of this reservation.
	 * 
	 * @return PDPObjectImpl serviceProvider
	 */
	public AgvAgent getServiceProvider() {
		return serviceProvider;
	}
	
	public long getExpectedPickUpTime() {
		return this.expectedPickUpTime;
	}
	
	public long getExpectedDeliveryTime() {
		return this.expectedDeliveryTime;
	}
	
	public boolean equals(AbstractReservation other) {
		
		if (this.getClient() != other.getClient()) {
			return false;
		}
		
		if (this.getServiceProvider() != other.getServiceProvider()) {
			return false;
		}
		
		if (this.getExpectedPickUpTime() != other.getExpectedPickUpTime()) {
			return false;
		}
		
		if (this.getExpectedDeliveryTime() != other.getExpectedDeliveryTime()) {
			return false;
		}
		
		return true;
	}
}
