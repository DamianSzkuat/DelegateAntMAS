package project.smartMessage;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Optional;

import project.agents.AgvAgent;
import project.agents.ReservationProposal;
import project.agents.TransportReservation;

public class State {

	private Set<ReservationProposal> reservationProposals;
	private Set<AgvAgent> visitedAgents;
	private int hopCounter;
	private int unableToMigrateCounter;
	
	Optional<TransportReservation> transportReservation;
	
	State() {
		reservationProposals = new HashSet<>();
		visitedAgents = new HashSet<>();
		hopCounter = 0;
		transportReservation = Optional.absent();
	}
	
	Set<ReservationProposal> getReservationProposals(){
		return this.reservationProposals;
	}
	
	void addReservationProposal(ReservationProposal reservationProposal) {
		this.reservationProposals.add(reservationProposal);
	}
	
	Set<AgvAgent> getVisitedAGVs() {
		return this.visitedAgents;
	}
	
	public void addVisitedAGV(AgvAgent agvAgent) {
		this.visitedAgents.add(agvAgent);
	}
	
	int getHopCounter() {
		return this.hopCounter;
	}
	
	public void incrementHopCounter() {
		this.hopCounter += 1;
	}
	
	void setHopCounter(int hopCounter) {
		this.hopCounter = hopCounter;
	}
	
	int getUnableToMigrateCounter() {
		return this.unableToMigrateCounter;
	}
	
	public void incrementUnableToMigrateCounter() {
		this.unableToMigrateCounter += 1;
	}
	
	public void resetUnableToMigrateCounter() {
		this.unableToMigrateCounter = 0;
	}
	
	public Optional<TransportReservation> getTransportReservation() {
		return this.transportReservation;
	}
	
	void setTransportReservation(Optional<TransportReservation> transportReservation) {
		this.transportReservation = transportReservation;
	}
}
