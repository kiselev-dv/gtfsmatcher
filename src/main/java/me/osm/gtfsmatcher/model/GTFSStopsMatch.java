package me.osm.gtfsmatcher.model;

import java.util.List;

public class GTFSStopsMatch {
	
	private List<GTFSStop> gtfs;
	private List<OSMObject> orphants;
	
	public List<GTFSStop> getGtfs() {
		return gtfs;
	}
	
	public void setGtfs(List<GTFSStop> gtfs) {
		this.gtfs = gtfs;
	}
	
	public List<OSMObject> getOrphants() {
		return orphants;
	}

	public void setOrphants(List<OSMObject> orphants) {
		this.orphants = orphants;
	}

}
