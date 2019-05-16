package me.osm.gtfsmatcher.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import me.osm.gtfsmatcher.util.SphericalMercator;

public class GTFSRoute {

	private String id;
	private String name;
	private String longName;

	private OSMRelation matchedMaster;

	private List<GTFSTrip> trips = new ArrayList<>();
	private List<OSMObject> possibleOSMTrips;
	private String match;
	
	private static final OSMObjectPTStopFilter objFilter = new OSMObjectPTStopFilter();

	public GTFSRoute(String id, String name, String longName) {
		this.id = id;
		this.name = name;
		this.longName = longName;
	}

	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}

	public String getLongName() {
		return longName;
	}

	public OSMRelation getMatchedMaster() {
		return matchedMaster;
	}

	public List<GTFSTrip> getTrips() {
		return trips;
	}

	public List<OSMObject> getPossibleOSMTrips() {
		return possibleOSMTrips;
	}

	public String getMatch() {
		return match;
	}

	public void addTrip(String[] stops, String tripId) {
		// Not very efficient, but I don't want to have additional Set with strings or
		// hash representation
		GTFSTrip gtfsTrip = new GTFSTrip(stops, tripId);

		GTFSTrip same = findSameTrip(gtfsTrip);
		if (same != null) {
			same.addId(tripId);
		}
		else {
			trips.add(gtfsTrip);
		}
		
	}

	private GTFSTrip findSameTrip(GTFSTrip gtfsTrip) {
		for(GTFSTrip t : trips) {
			if(t.equals(gtfsTrip)) {
				return t;
			}
		}
		return null;
	}

	public void sortAndMergeTrips() {
		// Bigger first
		trips.sort((a, b) -> -Integer.compare(a.toString().length(), b.toString().length()));
		
		List<GTFSTrip> merged = new ArrayList<>();
		for(GTFSTrip trip : trips) {
			GTFSTrip enclosing = findMatched(merged, trip);
			if(enclosing != null) {
				trip.setContainedBy(enclosing);
			}
			else {
				merged.add(trip);
			}
		}
	}

	private GTFSTrip findMatched(List<GTFSTrip> merged, GTFSTrip trip) {
		for(GTFSTrip t : merged) {
			if(t.toString().contains(trip.toString())) {
				return t;
			}
		}
		return null;
	}

	public void setTripCandidates(List<OSMObject> list) {
		this.possibleOSMTrips = list;
	}

	public void matchTrips(OSMData osmData, GtfsStopsIndex gtfsStopsAcc) {
		if (this.possibleOSMTrips != null && !this.possibleOSMTrips.isEmpty()) {
			
			System.out.println("Matching " + this.name + " " + this.longName + " " + this.trips.size() + 
					" trips with " + this.possibleOSMTrips.size() + " possible OSM routes");
			
			for(GTFSTrip gtfsTrip : this.trips) {
				
				int bubble = 0;
				OSMObject bestFitOSMTrip = null;
				boolean exact = false;
	
				String[] gtfsStops = gtfsTrip.getStops();
				for (OSMObject osmTrip : this.possibleOSMTrips) {

					String gtfsTripStopRefs = StringUtils.join(gtfsStops, " ");

					List<OSMObject> osmTripStops = getOSMTripStops(osmTrip, osmData);
					
					List<String> osmStopsRefs = osmTripStops.stream()
						.map(n -> n.getTags().get("ref")).collect(Collectors.toList());

					String osmTripStopsRefs = StringUtils.join(osmStopsRefs, " ");

					if(gtfsTripStopRefs.equals(osmTripStopsRefs)) {
						bestFitOSMTrip = osmTrip;
						exact = true;
						break;
					}
					else {
						int score = scoreTrip(gtfsTrip, osmTripStops, gtfsStopsAcc);
						if (score > bubble) {
							bubble = score;
							bestFitOSMTrip = osmTrip;
						}
					}
				}

				if (exact || bubble >= gtfsStops.length * 0.75) {
					gtfsTrip.setMatchedOSMTrip(bestFitOSMTrip);
					gtfsTrip.setExactMatch(gtfsStops.length == bubble);
	
//					List<String> osmStops = getOSMTripStops(bestFitOSMTrip, osmData).stream()
//							.map(n -> n.getTags().get("ref")).collect(Collectors.toList());
//	
//					System.out.println("Route " + bestFitOSMTrip.getTags().get("ref") + " matched trips:");
//					System.out.println("gtfs: " + StringUtils.join(gtfsStops, " "));
//					System.out.println("osm: " + StringUtils.join(osmStops, " "));
				}
			}
	
			for(GTFSTrip t : this.trips) {
				if(t.isExactMatch()) {
					this.possibleOSMTrips.remove(t.getMatchedOSMTrip());
				}
			}
	
			List<GTFSTrip> unmatched = this.trips.stream().filter(t -> (t.getMatchedOSMTrip() == null)).collect(Collectors.toList());
			boolean oneMatchExactly = this.trips.stream().anyMatch(t -> t.isExactMatch());
			// One left unmatched
			// Other was matched exactly
			// And only one possible osm route left
			if (unmatched.size() == 1 && oneMatchExactly && this.possibleOSMTrips.size() == 1) {
				unmatched.get(0).setMatchedOSMTrip(this.possibleOSMTrips.get(0));
				this.possibleOSMTrips.clear();
			}
	
			this.match = "none";
			if(this.trips.stream().allMatch(t -> t.isExactMatch())) {
				this.match = "full";
			}
			else if(this.trips.stream().allMatch(t -> t.getMatchedOSMTrip() != null)) {
				this.match = "all_trips";
			}
			else if(this.trips.stream().anyMatch(t -> t.getMatchedOSMTrip() != null)) {
				this.match = "some_trips";
			}
		}
	}

	public static int scoreTrip(GTFSTrip gtfsTrip, List<OSMObject> osmTripStops, GtfsStopsIndex gtfsStopsAcc) {
		List<String> gtfsStopIds = Arrays.asList(gtfsTrip.getStops());

		List<Integer> stopIndexes = new ArrayList<>();
		
		for(String gtfsStopId : gtfsStopIds) {
			GTFSStop gtfsStop = gtfsStopsAcc.getById(gtfsStopId);
		
			if (gtfsStop != null) {
				for(int i = 0; i < osmTripStops.size(); i++) {
					if (matchForTrip(gtfsStop, osmTripStops.get(i))) {
						stopIndexes.add(i);
					}
				}
			}
		}

		if (stopIndexes.size() > 1) {
			int matches = 0;
			for (int i = 1; i < stopIndexes.size(); i++) {
				int prevIndex = stopIndexes.get(i - 1);
				int currIndex = stopIndexes.get(i);

				if (currIndex > prevIndex) {
					matches++;
				}
			}
			return matches;
		}

		return stopIndexes.size();
	}

	private static boolean matchForTrip(GTFSStop gtfsStop, OSMObject osmStop) {
		
		double gx = SphericalMercator.lon2x(gtfsStop.getLon());
		double gy = SphericalMercator.lat2y(gtfsStop.getLat());
		
		double ox = SphericalMercator.lon2x(osmStop.getLon());
		double oy = SphericalMercator.lat2y(osmStop.getLat());
		
		double distance = Math.hypot(gx - ox, gy - oy);
		
		// Within 100 meters
		return distance < 100.0;
	}

	public static List<OSMObject> getOSMTripStops(OSMObject osmTrip, OSMData osmData) {
		List<OSMObject> stops = new ArrayList<>();

		if(osmTrip.listMembers() != null) {
			for(OSMRelationMember m : osmTrip.listMembers()) {
				if (isRelationMemberIsAStop(osmData, m)) {
					stops.add(getMember(osmData, m));
				}
			}
		}

		return stops;
	}

	private static boolean isRelationMemberIsAStop(OSMData osmData, OSMRelationMember m) {
		OSMObject obj = getMember(osmData, m);
		return objFilter.isStop(obj);
	}

	private static OSMObject getMember(OSMData osmData, OSMRelationMember m) {
		OSMObject obj = null;
		if ("node".equals(m.type)) {
			obj = osmData.getNodes().get(m.ref);
		}
		else if("way".equals(m.type)) {
			obj = osmData.getWays().get(m.ref);
		}
		return obj;
	}

}
