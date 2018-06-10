package project.agents;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.common.base.Optional;

import project.exceptions.PheromoneExpiredException;
import project.smartMessage.AntAgent;

public class AgvScheduler {
	private ArrayList<TransportReservation> reservations;
	private Optional<TransportReservation> currentReservation;
	private AgvAgent agent;
	
	AgvScheduler(AgvAgent agent) {
		this.agent = agent;
		reservations = new ArrayList<TransportReservation>();
		currentReservation = Optional.absent();
	}
	
	/**
	 * The scheduler will look at the current AGV schedule and send back an approximated 
	 * ReserationRpoposal. 
	 * @param parcel
	 * @return
	 */
	public Optional<ReservationProposal> getReservationProposal(PackageAgent parcel, AntAgent antAgent) {
		//TODO batteries
			
		// We are asked for a reservation proposal.
		
		// 1) We look if the given parcel already has a TransportReservation in our schedule
		Optional<ReservationProposal> result = lookForExistingReservation(parcel, antAgent);

		
		if(result.isPresent()) {
			result.get().setAntAgent(antAgent);
			return result;
		}
		
		// 2) Calculate a new ReservationProposal based on the current schedule. 
		long expectedTimeToFinishPreviousTasks = getAgent().getCurrentTime();
		if (getReservations().size() > 0) {
			expectedTimeToFinishPreviousTasks = getLast(getReservations()).getExpectedDeliveryTime();
		}
		long expectedPickUpTime = calculateExpectedPickUpTime(parcel, expectedTimeToFinishPreviousTasks);
		long expectedDeliveryTime = calculateExpectedDeliveryTime(parcel, expectedPickUpTime);
		

		expectedPickUpTime += 500000;
		
		ReservationProposal proposal = new ReservationProposal(parcel, getAgent(), expectedPickUpTime, expectedDeliveryTime, antAgent);
		
		return Optional.of(proposal);
	}
	
	/**
	 * Evaporate each reservations once
	 * 
	 * @throws PheromoneExpiredException
	 */
	public void evaporateReservations() throws PheromoneExpiredException {
		
		// TODO might not work, so test 		
		Iterator<TransportReservation> iter = getReservations().iterator();
		while (iter.hasNext()) {
			TransportReservation reservation = iter.next();
			reservation.evaporate();
			if (reservation.hasEvaporated()) {
				iter.remove();
			}
		}
		//TODO reschedule?
	}
	
	/**
	 * Get the last element of the schedule.
	 * @param schedule
	 * @return
	 */
	private TransportReservation getLast(ArrayList<TransportReservation> schedule) {
		if (schedule.size() > 0) {
			return schedule.get(schedule.size()-1);
		} else {
			throw new IllegalArgumentException("The given schedule is empty");
		}
	}
	
	/**
	 * Given a parcel and the expected pick up time of that parcel, will calculate the 
	 * expected delivery time of that parcel.
	 * 
	 * @param parcel
	 * @param expectedPickUpTime
	 * @return
	 */
	private long calculateExpectedDeliveryTime(PackageAgent parcel, long expectedPickUpTime) {
		long pickUpTime = parcel.getPickupDuration();
		long deliveryTime = parcel.getDeliveryDuration();
		long travelTime = getAgent().getTimeToMoveBetweenTwoPoints(parcel.getPickupLocation(), parcel.getDeliveryLocation());
		return expectedPickUpTime + pickUpTime + deliveryTime + travelTime;
	}
	
	/**
	 * Given a parcel and the expected time to finish all current reservations,
	 * calculates the expected pick up time of the new parcel.
	 * 
	 * @param parcel
	 * @param expectedTimeToFinishPreviousTasks
	 * @return
	 */
	private long calculateExpectedPickUpTime(PackageAgent parcel, long expectedTimeToFinishPreviousTasks) {
		long time = getAgent().getTimeToReachGivenPoint(parcel.getPickupLocation());
		return time + expectedTimeToFinishPreviousTasks;
	}
	
	/**
	 * Used by ants to drop a pheromones, a.k.a. TransportReservations, at the schaduler.
	 * 
	 * @param reservation
	 */
	void dropPheromone(TransportReservation reservation) {
				
		// If reservation present, reset pheromone level
		for(TransportReservation res : getReservations()) {
			if(res.equals(reservation)) {
				//System.out.println("Dropping Pheromone");
				resetIntentisty(reservation);
				return;
			}
		}
		
		// See if the reservation can still be done
		long expectedTimeToFinishPreviousTasks = getAgent().getCurrentTime();
		if (getReservations().size() > 0) {
			expectedTimeToFinishPreviousTasks = getLast(getReservations()).getExpectedDeliveryTime();
		}
		long expectedPickUpTime = calculateExpectedPickUpTime(reservation.getClient(), expectedTimeToFinishPreviousTasks);
		long startTime = reservation.getExpectedPickUpTime();
		
		// If not possible, discard
		if (expectedPickUpTime > startTime+100000) {
			//System.out.println("Reservation cannot be honored...");
			//System.out.println("Expected PickUp Time: " + expectedPickUpTime);
			//System.out.println("Star Time of reservation: " + startTime);
			return;
		}
		
		// If possible, add to schedule
		addReservation(reservation);
	}
	
	private void resetIntentisty(TransportReservation reservation) {
		
		for (TransportReservation res : getReservations()) {
			if (res == reservation) {
				res.resetIntensity();
				return;
			}
		}
	}
	
	/**
	 * Given a parcel, looks if the is a TranspornReservation for that parcel in the schedule.
	 * 
	 * @param parcel
	 * @return
	 */
	private Optional<ReservationProposal> lookForExistingReservation(PackageAgent parcel, AntAgent antAgent) {
		Optional<ReservationProposal> result = Optional.absent();
		Iterator<TransportReservation> iter = getReservations().iterator();
		while (iter.hasNext()) {
			TransportReservation res = iter.next();
			if (res.getClient() == parcel) {
				result = Optional.of(new ReservationProposal(parcel, getAgent(),
						res.getExpectedPickUpTime(), res.getExpectedDeliveryTime(), antAgent));
			}
		}
		return result;
	}
	
	private void addReservation(TransportReservation reservation) {
		this.reservations.add(reservation);
	}
	
	private void removeReservation(TransportReservation reservation) {
		this.reservations.remove(reservation);
	}
	
	private void setReservations(ArrayList<TransportReservation> reservations) {
		this.reservations = reservations;
	}
	
	ArrayList<TransportReservation> getReservations() {
		return this.reservations;
	}
	
	/**
	 * Gets the first reservation from the list. This should only be called
	 * when the current reservation has been finished.
	 * @return
	 */
	Optional<TransportReservation> getNextReservation() {
		// Check if schedule is empty
		if(getReservations().size()==0) {
			return Optional.absent();
		}
		
		// Get first element
		TransportReservation reservation = getReservations().get(0);
		
		// Set the first element as the current reservation
		setCurrentReservation(reservation);
		
		// Remove the first element from the arrayList
		getReservations().remove(0);
		
		return getCurrentReservation();
	}
	
	/*
	 * This method will not poll a reservation from the queue but simply 
	 * return the current reservation being handled.
	 */
	private  Optional<TransportReservation> getCurrentReservation() {
		return this.currentReservation;
	}
	
	private void setCurrentReservation(TransportReservation reservation) {
		this.currentReservation = Optional.of(reservation);
	}
	
	private AgvAgent getAgent() {
		return this.agent;
	}
}
