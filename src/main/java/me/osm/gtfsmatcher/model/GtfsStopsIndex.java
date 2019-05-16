package me.osm.gtfsmatcher.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GtfsStopsIndex {

	private Map<String, GTFSStop > code2Stop = new HashMap<>();
	
	public GtfsStopsIndex(List<GTFSStop> stops) {
		for(GTFSStop stop : stops) {
			code2Stop.put(stop.getId(), stop); 
		}
	}

	public GTFSStop getById(String gtfsStopRef) {
		return code2Stop.get(gtfsStopRef);
	}

}
