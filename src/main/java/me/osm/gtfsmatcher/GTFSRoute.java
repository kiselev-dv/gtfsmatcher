package me.osm.gtfsmatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import me.osm.gtfsmatcher.model.GTFSTrip;
import me.osm.gtfsmatcher.model.OSMObject;
import me.osm.gtfsmatcher.model.OSMRelation;

public class GTFSRoute {

	private String id;
	private String name;
	private String longName;

	private OSMRelation matchedMaster;

	private List<GTFSTrip> trips = new ArrayList<>();
	private List<OSMObject> possibleOSMTrips;
	private String match;

	public GTFSRoute(String id, String name, String longName) {
		this.id = id;
		this.name = name;
		this.longName = longName;
	}

	public String getName() {
		return name;
	}

	public void addTrip(String[] stops) {
		// Not very efficient, but I don't want to have additional Set with strings or
		// hash representation
		GTFSTrip gtfsTrip = new GTFSTrip(stops);

		if (!trips.stream().anyMatch(t -> t.equals(gtfsTrip))) {
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

	public void setTripCandidates(List<OSMObject> list) {
		this.possibleOSMTrips = list;
	}

	public void matchTrips(OSMData osmData) {
		this.trips.forEach(gtfsTrip -> {
			int bubble = 0;
			OSMObject bestFitOSMTrip = null;
			boolean exact = false;

			if (this.possibleOSMTrips != null && !this.possibleOSMTrips.isEmpty()) {
				for (OSMObject osmTrip : this.possibleOSMTrips) {

					String gtfsAsString = StringUtils.join(gtfsTrip.getStops(), " ");
					List<String> osmStops = getOSMTripStops(osmTrip, osmData).stream()
						.map(n -> n.getTags().get("ref")).collect(Collectors.toList());

					String osmAsString = StringUtils.join(osmStops, " ");

					if(gtfsAsString.equals(osmAsString)) {
						bestFitOSMTrip = osmTrip;
						exact = true;
						break;
					}
					else {
						int score = scoreTrip(gtfsTrip, osmStops);
						if (score > bubble) {
							bubble = score;
							bestFitOSMTrip = osmTrip;
						}
					}
				}
			}

			if (exact || bubble >= gtfsTrip.getStops().length * 0.75) {
				gtfsTrip.setMatchedOSMTrip(bestFitOSMTrip);
				gtfsTrip.setExactMatch(exact);

				List<String> osmStops = getOSMTripStops(bestFitOSMTrip, osmData).stream()
						.map(n -> n.getTags().get("ref")).collect(Collectors.toList());

				System.out.println("Route " + bestFitOSMTrip.getTags().get("ref") + " matched trips:");
				System.out.println("gtfs: " + StringUtils.join(gtfsTrip.getStops(), " "));
				System.out.println("osm: " + StringUtils.join(osmStops, " "));
			}
		});

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

	public static int scoreTrip(GTFSTrip gtfsTrip, List<String> osmStops) {
		List<String> gtfsStops = Arrays.asList(gtfsTrip.getStops());

		List<Integer> osmStopIndexes = new ArrayList<>();
		osmStops.forEach(osmStop -> {
			int osmStopIndex = gtfsStops.indexOf(osmStop);
			if (osmStopIndex >= 0) {
				osmStopIndexes.add(osmStopIndex);
			}
		});

		if (osmStopIndexes.size() > 1) {
			int matches = 0;
			for (int i = 1; i < osmStopIndexes.size(); i++) {
				int prevIndex = osmStopIndexes.get(i - 1);
				int currIndex = osmStopIndexes.get(i);

				if (currIndex > prevIndex) {
					matches++;
				}
			}
			return matches;
		}

		return osmStopIndexes.size();
	}

	public static List<OSMObject> getOSMTripStops(OSMObject osmTrip, OSMData osmData) {
		List<OSMObject> stops = new ArrayList<>();

		osmTrip.listMembers().forEach(m -> {
			if ("node".equals(m.type) && osmData.getNodes().get(m.ref) != null) {
				OSMObject node = osmData.getNodes().get(m.ref);
				// TODO: Make a full check later
				if ("bus_stop".equals(node.getTags().get("highway"))) {
					stops.add(node);
				}
			}
		});

		return stops;
	}

}
