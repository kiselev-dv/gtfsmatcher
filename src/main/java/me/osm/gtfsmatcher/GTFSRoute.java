package me.osm.gtfsmatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.osm.gtfsmatcher.model.GTFSTrip;
import me.osm.gtfsmatcher.model.OSMRelation;

public class GTFSRoute {

	private String id;
	private String name;
	private String longName;
	
	private OSMRelation matchedMaster;
	
	private List<GTFSTrip> trips = new ArrayList<>();

	public GTFSRoute(String id, String name, String longName) {
		this.id = id;
		this.name = name;
		this.longName = longName;
	}
	
	public String getName() {
		return name;
	}

	public void addTrip(String[] stops) {
		// Not very efficient, but I don't want to have additional Set with strings or hash representation
		GTFSTrip gtfsTrip = new GTFSTrip(stops);
		
		if(!trips.stream().anyMatch(t -> t.equals(gtfsTrip))) {
			trips.add(gtfsTrip);
		}
	}

	public void sortAndMergeTrips() {
		trips.sort((a, b) -> -Integer.compare(a.toString().length(), b.toString().length()));
		
		List<GTFSTrip> merged = new ArrayList<>();
		trips.forEach(trip -> {
			if (!merged.stream().anyMatch(t -> t.toString().contains(trip.toString()))) {
				merged.add(trip);
			}
		});
		
		trips = merged;
	}

}
