package project.smartMessage;

import project.exceptions.PheromoneExpiredException;

public abstract class Pheromone {
	
	private int intensity;
	private boolean EVAPORATED = false;
	
	private static final int MAX_INTENSITY = 1000;
	
	protected Pheromone() {
		this.intensity = MAX_INTENSITY;
	}
	
	/**
	 * Reduces the intensity of the Pheromone by 1.
	 * 
	 * @throws PheromoneExpiredException
	 */
	public void evaporate() throws PheromoneExpiredException {
		if(EVAPORATED) {
			throw new PheromoneExpiredException("The pheromone you want to evaporate has already expired and should have beed deleted");
		}
		intensity -= 1;
		if(intensity == 0) {
			EVAPORATED = true;
		}
	}
	
	public void resetIntensity() {
		this.intensity = MAX_INTENSITY;
	}
	
	/**
	 * Returns whether the Pheromone has expired
	 * 
	 * @return True if the Pheromone has 
	 */
	public boolean hasEvaporated() {
		return EVAPORATED;
	}
}