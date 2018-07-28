package me.osm.gtfsmatcher.model;

import org.apache.commons.lang3.StringUtils;

public class GTFSTrip {

	private String[] stops;
	
	/*
	 * The problem is there is no any natural code or id in both,
	 * gtfs and osm for trip.
	 * 
	 * So this will always a better or worse guess
	 * */
	private OSMRelation route;

	private OSMObject matchedOSMTrip;

	private boolean exactMatch;

	public GTFSTrip(String[] stops) {
		this.stops = stops;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return StringUtils.join(stops, ';');
	}

	@Override
	public boolean equals(Object obj) {
		return toString().equals(obj);
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

}
