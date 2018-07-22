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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restexpress.Request;
import org.restexpress.Response;

import com.vividsolutions.jts.geom.Envelope;

import me.osm.gtfsmatcher.model.OSMObject;

public class RoutesAPI {
	
	private static final String urlBase = "https://overpass-api.de/api/interpreter?data=";
	private static final String qTemplate = "[out:json][timeout:25];("
			+	 "relation[type=route_master]({{bbox}});"
			+	 "relation[type=route][\"route\"~\"^(bus|tram|ferry|light_rail|trolleybus)$\"]({{bbox}});"
			+ ");out meta;>;out meta qt;";
	
	public Collection<GTFSRoute> read(Request req, Response res ) throws IOException {
		
		RoutesBuilder rb = new RoutesBuilder();
		
		try(ZipFile zipFile = new ZipFile("/home/dkiselev/osm/data/gtfs/google_transit.zip")) {
			
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
		
		Collection<GTFSRoute> routes = rb.buildRoutes();
		List<OSMObject> osmRoutes = getOSMRoutes(StopAPI.getEnvelope());
		
		return routes;
		
	}
	
	private List<OSMObject> getOSMRoutes(Envelope env) throws IOException, MalformedURLException {
		
		String overpassQ = getOverpassQ(env);
		JSONObject overpassAnswer = new JSONObject(IOUtils.toString(new URL(urlBase + overpassQ).openStream()));
		JSONArray elements = overpassAnswer.optJSONArray("elements");
		
		List<OSMObject> data = new ArrayList<>();
		
		for (int i = 0; i < elements.length(); i++) {
			JSONObject element = elements.getJSONObject(i);
			OSMObject osmObject = new OSMObject();
			
			osmObject.setType(element.optString("type"));
			osmObject.setId(element.getLong("id"));
			
			osmObject.setVersion(element.getInt("version"));

			osmObject.setUID(element.optLong("uid", 0));
			osmObject.setUser(element.optString("user", null));
			
			osmObject.setTimestamp(element.optString("timestamp", null));
			
			osmObject.setTags(element.getJSONObject("tags").toMap());
			
			if ("node".equals(element.optString("type"))) {
				osmObject.setLon(element.getDouble("lon"));
				osmObject.setLat(element.getDouble("lat"));
			}
			
			if ("way".equals(element.optString("type"))) {
				System.out.println(element);
			}
			
			if ("relation".equals(element.optString("type"))) {
				JSONArray membersArray = element.optJSONArray("members");
				for(int m = 0; m < membersArray.length(); m++) {
					JSONObject member = membersArray.getJSONObject(m);
					osmObject.addMember(member.getLong("ref"), member.getString("type"), member.optString("role"));
				}
			}
			
			data.add(osmObject);
		}
		
		return data;
	}
	
	private String getOverpassQ(Envelope env) {
		String query = qTemplate.replace("{{bbox}}", env.getMinY() + "," + env.getMinX() + "," + env.getMaxY() + "," + env.getMaxX());
		try {
			return URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
}
