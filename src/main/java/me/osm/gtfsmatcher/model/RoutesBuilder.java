package me.osm.gtfsmatcher.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.StringUtils;

public class RoutesBuilder {
	
	private Map<String, GTFSRoute> routeById = new HashMap<>();
	private Map<String, String> trip2Route = new HashMap<>();
	private Map<String, String[]> trip2Stops = new HashMap<>();
	
	public void readRoutes(CSVParser csvParser) {
		csvParser.forEach(csv -> {
			String id = csv.get("route_id");
			String name = csv.get("route_short_name");
			String longName = csv.get("route_long_name");
			
			routeById.put(id, new GTFSRoute(id, name, longName));
			
		});
	}

	public void readTrips(CSVParser csvParser) {
		csvParser.forEach(csv -> {
			String trip = csv.get("trip_id");
			String route = csv.get("route_id");
			
			trip2Route.put(trip, route);
		});
	}

	public void readStopTimes(CSVParser csvParser) {
		csvParser.forEach(csv -> {
			String trip = csv.get("trip_id");
			String stop = csv.get("stop_id");
			
			// For the cases when stop sequence starts from 0
			int seq = Integer.parseInt(csv.get("stop_sequence")) + 1;
			
			if(trip2Stops.get(trip) == null) {
				trip2Stops.put(trip, new String[seq]);
			}
			
			if (trip2Stops.get(trip).length < seq) {
				trip2Stops.put(trip, Arrays.copyOf(trip2Stops.get(trip), seq));
			}
			
			trip2Stops.get(trip)[seq - 1] = stop;
		});
	}

	public Collection<GTFSRoute> buildRoutes() {
		
		for(Entry<String, String[]> t2s: trip2Stops.entrySet()) {
			String trip = t2s.getKey();
			String[] stops = t2s.getValue();
			
			GTFSRoute gtfsRoute = routeById.get(trip2Route.get(trip));
			gtfsRoute.addTrip(trimArray(stops), trip);
		}
		
		// Free some memory
		trip2Stops.clear();
		trip2Route.clear();
		
		System.out.println("Found " + routeById.values().size() + " routes");
		
		for(GTFSRoute r : routeById.values()) {
			r.sortAndMergeTrips();
		}
		
		List<GTFSRoute> routes = new ArrayList<>(routeById.values());
		
		Comparator<GTFSRoute> comparator = 
				Comparator.comparingInt(r -> {
					String alphaString = r.getName().replaceAll("\\D", "");
					if (StringUtils.isBlank(alphaString)) {
						return 0;
					}
					return Integer.parseInt(alphaString);
				});
		comparator = comparator.thenComparing(r -> r.getName().replaceAll("\\d", ""));
		
		routes.sort(comparator);
		return routes;
	}

	private String[] trimArray(String[] stops) {
		List<String> list = Arrays.asList(stops);
		
		list = trimHead(list);
		Collections.reverse(list);
		list = trimHead(list);
		Collections.reverse(list);
		
		return list.toArray(new String[list.size()]);
	}

	private List<String> trimHead(List<String> list) {
		List<String> result = new ArrayList<>();
		for(String s : list) {
			if(s != null) {
				result.add(s);
			}
			else if(!result.isEmpty()) {
				result.add(s);
			}
		}
		return result;
	}

}
