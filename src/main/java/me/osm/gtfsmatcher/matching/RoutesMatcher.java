package me.osm.gtfsmatcher.matching;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.StringUtils;

import me.osm.gtfsmatcher.model.GTFSRoute;
import me.osm.gtfsmatcher.model.GTFSStop;
import me.osm.gtfsmatcher.model.GtfsStopsIndex;
import me.osm.gtfsmatcher.model.OSMData;
import me.osm.gtfsmatcher.model.OSMObject;
import me.osm.gtfsmatcher.model.OSMRelationMember;
import me.osm.gtfsmatcher.model.RoutesBuilder;
import me.osm.gtfsmatcher.model.RoutesData;

public class RoutesMatcher {
	
	private static Comparator<OSMObject> alphanumComparator;
	static {
		Comparator<OSMObject> alphanumComparator = 
				Comparator.comparingInt(r -> {
					String alphaString = r.getTags().get("ref").replaceAll("\\D", "");
					if (StringUtils.isBlank(alphaString)) {
						return 0;
					}
					return Integer.parseInt(alphaString);
				});
		alphanumComparator = alphanumComparator.thenComparing(r -> r.getTags().get("ref").replaceAll("\\d", ""));
	}

	public RoutesData matchRoutes(File file, boolean tripsFirst, File osmDataFile) throws IOException, ZipException, Exception {
		RoutesBuilder rb = new RoutesBuilder();
		List<GTFSStop> stops = new ArrayList<>();
		
		try(ZipFile zipFile = new ZipFile(file)) {
			
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			
			while(entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				if(entry.getName().contains("routes.txt") 
						|| entry.getName().contains("trips.txt") 
						|| entry.getName().contains("stop_times.txt")
						|| entry.getName().contains("stops.txt")) {
					
					InputStream stream = zipFile.getInputStream(entry);
					
					try(CSVParser csvParser = new CSVParser(
							new InputStreamReader(stream), 
							CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
						
						if (entry.getName().contains("routes.txt")) {
							rb.readRoutes(csvParser);
						}
						if (entry.getName().contains("trips.txt")) {
							rb.readTrips(csvParser);
						}
						if (entry.getName().contains("stop_times.txt")) {
							rb.readStopTimes(csvParser);
						}
						if (entry.getName().contains("stops.txt")) {
							stops = StopMatcher.readGTFSStops(csvParser);
						}
					}
				}
			}
		}
		
		try {
			Map<String, List<OSMObject>> routeRelations = new HashMap<>();
			Map<String, GTFSRoute> routeGTFS = new HashMap<>();
			
			Collection<GTFSRoute> routes = rb.buildRoutes();
			routes.forEach(r -> {
				routeGTFS.put(r.getName(), r);
			});
			
			OSMDataProvider osmDataProvider = osmDataFile == null ? new OverpassDataProvider() : new OSMDumpDataProvider(osmDataFile);
			OSMData osmData = osmDataProvider.getOSMRoutes(StopMatcher.getEnvelope(stops));
			
			Map<Long, OSMObject> routeIdToMaster = new HashMap<>();
			for(OSMObject r : osmData.listRelations()) {
				if("route_master".equals(r.getTags().get("type"))) {
					if (r.listMembers() != null) {
						for(OSMRelationMember m : r.listMembers()) {
							routeIdToMaster.put(m.ref, r); 
						} 
					}
				}
			}
			
			for(OSMObject r : osmData.listRelations()) {
				if (isRelationAPTRoute(r)) {
					OSMObject master = routeIdToMaster.get(r.getId()); 
					String ref = null;
					
					if (master != null) {
						ref = master.getTags().get("ref");
					}

					if (ref == null) {
						ref = r.getTags().get("ref");
					}
					
					if (ref != null) {
						if(routeRelations.get(ref) == null) {
							routeRelations.put(ref, new ArrayList<>());
						}
						routeRelations.get(ref).add(r);
					}
				}
			}
			
			for(GTFSRoute gtfsRoute : routeGTFS.values()) {
				String name = gtfsRoute.getName();
				List<OSMObject> osmTrips = routeRelations.remove(name);
				if (osmTrips != null && !osmTrips.isEmpty()) {
					for(OSMObject t : osmTrips) {
						System.out.println("Matched: " + name + " with " + t.getId());
					}
				}
				gtfsRoute.setTripCandidates(osmTrips);
			}
		
			List<OSMObject> orphants = new ArrayList<>();
			
			routeRelations.values().forEach(orphants::addAll);
			
//			Collections.sort(orphants, alphanumComparator);
//
//			System.out.println("OSM: orphants");
//			for (OSMObject o : orphants) {
//				System.out.println(o.getTags().get("ref") + " " + o.getId());
//			}

			GtfsStopsIndex gtfsStopsAcc = new GtfsStopsIndex(stops);
			for (GTFSRoute r : routes) {
				r.matchTrips(osmData, gtfsStopsAcc);
			}
			
			return new RoutesData(routes, orphants, osmData);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private boolean isRelationAPTRoute(OSMObject r) {
		return "route".equals(r.getTags().get("type")) && "bus".equals(r.getTags().get("route"));
	}

}
