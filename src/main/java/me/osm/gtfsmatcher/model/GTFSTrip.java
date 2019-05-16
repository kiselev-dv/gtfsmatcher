package me.osm.gtfsmatcher.model;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class GTFSTrip {

	private String[] stops;
	private Set<String> gtfsTripsIds = new HashSet<>();
	
	private String asString;
	
	private OSMObject matchedOSMTrip;

	private boolean exactMatch;
	private GTFSTrip containedBy;

	public GTFSTrip(String[] stops, String tripId) {
		this.stops = stops;
		this.gtfsTripsIds.add(tripId);
		this.asString = StringUtils.join(stops, ';');
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return this.asString;
	}

	@Override
	public boolean equals(Object obj) {
		return toString().equals(obj.toString());
	}

	public String[] getStops() {
		return stops;
	}

	public void setMatchedOSMTrip(OSMObject bestFitOSMTrip) {
		this.matchedOSMTrip = bestFitOSMTrip;
	}

	public void setExactMatch(boolean exact) {
		this.exactMatch = exact;
	}

	public OSMObject getMatchedOSMTrip() {
		return matchedOSMTrip;
	}

	public boolean isExactMatch() {
		return exactMatch;
	}

	public void addId(String tripId) {
		this.gtfsTripsIds.add(tripId);
	}

	public Set<String> getGtfsTripsIds() {
		return gtfsTripsIds;
	}

	public void setContainedBy(GTFSTrip enclosing) {
		this.containedBy = enclosing;
	}

}
