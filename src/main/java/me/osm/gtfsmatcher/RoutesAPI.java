package me.osm.gtfsmatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restexpress.Request;
import org.restexpress.Response;

import com.vividsolutions.jts.geom.Envelope;

import me.osm.gtfsmatcher.model.OSMObject;

public class RoutesAPI {

	private static final String urlBase = "http://overpass-api.de/api/interpreter?data=";
	private static final String qTemplate = "[out:json][timeout:25];(" + "relation[type=route_master]({{bbox}});"
			+ "relation[type=route][\"route\"~\"^(bus|tram|ferry|light_rail|trolleybus)$\"]({{bbox}});"
			+ ");out meta;>;out meta qt;";

	private static final String DATA_FOLDER = System.getProperty("data.dir", System.getProperty("user.dir") + "/data");

	public RoutesData read(Request req, Response res ) throws IOException {
		
		RoutesBuilder rb = new RoutesBuilder();
		
		try(ZipFile zipFile = new ZipFile(DATA_FOLDER + "/google_transit.zip")) {
			
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			
			while(entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				if(entry.getName().contains("routes.txt") 
						|| entry.getName().contains("trips.txt") 
						|| entry.getName().contains("stop_times.txt")) {
					
					InputStream stream = zipFile.getInputStream(entry);
					
					try(CSVParser csvParser = new CSVParser(
							new InputStreamReader(stream), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
						
						if (entry.getName().contains("routes.txt")) {
							rb.readRoutes(csvParser);
						}
						if (entry.getName().contains("trips.txt")) {
							rb.readTrips(csvParser);
						}
						if (entry.getName().contains("stop_times.txt")) {
							rb.readStopTimes(csvParser);
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
			
			OSMData osmData = getOSMRoutes(StopAPI.getEnvelope());
			
			osmData.listRelations().forEach(r -> {
				if ("route".equals(r.getTags().get("type"))) {
					String ref = r.getTags().get("ref");
					if (ref != null) {
						if(routeRelations.get(ref) == null) {
							routeRelations.put(ref, new ArrayList<>());
						}
						routeRelations.get(ref).add(r);
					}
					else {
						// Check for ref on route_master
					}
				}
			});
			
			routeGTFS.values().forEach(gtfsRoute -> {
				String name = gtfsRoute.getName();
				List<OSMObject> osmTrips = routeRelations.remove(name);
				if (osmTrips != null && !osmTrips.isEmpty()) {
					osmTrips.forEach(t -> {
						System.out.println("Matched: " + name + " with " + t.getId());
					});
				}
				gtfsRoute.setTripCandidates(osmTrips);
			});
		
			System.out.println("OSM: orphants");
			List<OSMObject> orphants = new ArrayList<>();
			
			routeRelations.values().forEach(orphants::addAll);
			
			Comparator<OSMObject> comparator = 
					Comparator.comparingInt(r -> {
						String alphaString = r.getTags().get("ref").replaceAll("\\D", "");
						if (StringUtils.isBlank(alphaString)) {
							return 0;
						}
						return Integer.parseInt(alphaString);
					});
			comparator = comparator.thenComparing(r -> r.getTags().get("ref").replaceAll("\\d", ""));
			
			Collections.sort(orphants, comparator);
			orphants.forEach(o -> System.out.println(o.getTags().get("ref") + " " + o.getId()));

			routes.forEach(r -> r.matchTrips(osmData));
			
			return new RoutesData(routes, orphants, osmData);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
	}

	private OSMData getOSMRoutes(Envelope env) throws IOException, MalformedURLException {

		String overpassQ = getOverpassQ(env);
		JSONObject overpassAnswer = new JSONObject(IOUtils.toString(new URL(urlBase + overpassQ).openStream()));
		JSONArray elements = overpassAnswer.optJSONArray("elements");

		OSMData data = new OSMData();

		for (int i = 0; i < elements.length(); i++) {
			JSONObject element = elements.getJSONObject(i);
			OSMObject osmObject = new OSMObject();

			osmObject.setType(element.optString("type"));
			osmObject.setId(element.getLong("id"));

			osmObject.setVersion(element.getInt("version"));

			osmObject.setUID(element.optLong("uid", 0));
			osmObject.setUser(element.optString("user", null));

			osmObject.setTimestamp(element.optString("timestamp", null));

			if (element.has("tags")) {
				osmObject.setTags(element.getJSONObject("tags").toMap());
			}

			if ("node".equals(element.optString("type"))) {
				osmObject.setLon(element.getDouble("lon"));
				osmObject.setLat(element.getDouble("lat"));
			}

			if ("way".equals(element.optString("type"))) {
				List<Long> nodes = element.getJSONArray("nodes").toList().stream().map(o -> ((Number) o).longValue())
						.collect(Collectors.toList());
				osmObject.setNodes(nodes);
			}

			if ("relation".equals(element.optString("type"))) {
				JSONArray membersArray = element.optJSONArray("members");
				for (int m = 0; m < membersArray.length(); m++) {
					JSONObject member = membersArray.getJSONObject(m);
					osmObject.addMember(member.getLong("ref"), member.getString("type"), member.optString("role"));
				}
			}

			data.add(osmObject);
		}

		return data;
	}

	private String getOverpassQ(Envelope env) {
		String query = qTemplate.replace("{{bbox}}",
				env.getMinY() + "," + env.getMinX() + "," + env.getMaxY() + "," + env.getMaxX());
		try {
			return URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
}
