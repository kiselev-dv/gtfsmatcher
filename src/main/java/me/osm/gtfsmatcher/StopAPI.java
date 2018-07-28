package me.osm.gtfsmatcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restexpress.Request;
import org.restexpress.Response;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;

import me.osm.gtfsmatcher.matching.DefaultStopsMatcher;
import me.osm.gtfsmatcher.matching.StopsMatcher;
import me.osm.gtfsmatcher.model.MatchedStop;
import me.osm.gtfsmatcher.model.OSMObject;
import me.osm.gtfsmatcher.util.ByDistanceComparator;
import me.osm.gtfsmatcher.util.SphericalMercator;

public class StopAPI {
	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
	private static final String urlBase = "http://overpass-api.de/api/interpreter?data=";
	private static final String qTemplate = "[out:json][timeout:25];(node[\"highway\"=\"bus_stop\"]({{bbox}}););out meta;>;out meta qt;";
	
	private static final String DATA_FOLDER = System.getProperty("data.dir", System.getProperty("user.dir") + "/data"); 

	private static final StopsMatcher matcher = new DefaultStopsMatcher();
	
	private volatile static Envelope envelope = null;
	
	public List<MatchedStop> read(Request req, Response res) throws FileNotFoundException, IOException {
		try {
			
			List<MatchedStop> gtfsStops = readStopsFromGTFS();
			
			Envelope env = getEnvelope(gtfsStops);
			List<OSMObject> osmStops = getOSMStops(env);
			
			STRtree index = new STRtree();
			osmStops.forEach(stop -> {
				Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(
						SphericalMercator.lon2x(stop.getLon()), 
						SphericalMercator.lat2y(stop.getLat())));
				index.insert(point.getEnvelopeInternal(), stop);
			});
			index.build();
			
			gtfsStops.forEach(stop -> {
				double x = SphericalMercator.lon2x(stop.getLon());
				double y = SphericalMercator.lat2y(stop.getLat());
				
				Envelope qEnv = new Envelope(new Coordinate(x, y));
				qEnv.expandBy(500.0);
				
				@SuppressWarnings("unchecked")
				List<OSMObject> neighbours = index.query(qEnv);
				Collections.sort(neighbours, new ByDistanceComparator(x, y));
				
				stop.setCandidates(neighbours);
				
				List<OSMObject> matches = neighbours.stream()
						.filter(neighbour -> matcher.match(stop, neighbour))
						.collect(Collectors.toList());
				
				for (OSMObject o : matches) {
					if ( matcher.refMatch(stop, o)) {
						stop.setMatched(o);
					}
				}
				matches.remove(stop.getMatched());
				stop.setMatches(matches);
				
			});
			
			return gtfsStops; 
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public static Envelope getEnvelope() throws IOException {
		if (envelope != null) {
			return envelope;
		}
		
		return getEnvelope(readStopsFromGTFS());
	}

	private static Envelope getEnvelope(List<MatchedStop> stops) {
		if (envelope != null) {
			return envelope;
		}
		
		Envelope env = new Envelope();
		for (MatchedStop stop : stops) {
			env.expandToInclude(stop.getLon(), stop.getLat());
		}
		
		envelope = env;
		
		return env;
	}

	private List<OSMObject> getOSMStops(Envelope env) throws IOException, MalformedURLException {
		List<OSMObject> osmStops = new ArrayList<OSMObject>();
		
		String overpassQ = getOverpassQ(env);
		JSONObject overpassAnswer = new JSONObject(IOUtils.toString(new URL(urlBase + overpassQ).openStream()));
		JSONArray elements = overpassAnswer.optJSONArray("elements");
		
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
				
				osmStops.add(osmObject);
			}
		}
		
		return osmStops;
	}

	private String getOverpassQ(Envelope env) {
		String query = qTemplate.replace("{{bbox}}", env.getMinY() + "," + env.getMinX() + "," + env.getMaxY() + "," + env.getMaxX());
		try {
			return URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	private static List<MatchedStop> readStopsFromGTFS() throws IOException {
		try(ZipFile zipFile = new ZipFile(DATA_FOLDER + "/google_transit.zip")) {
			
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			
			while(entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				if(entry.getName().contains("stops.txt")) {
					InputStream stream = zipFile.getInputStream(entry);
					
					try(CSVParser csvParser = new CSVParser(
							new InputStreamReader(stream), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
						
						return readGTFSStops(csvParser);
					}
				}
			}
		}
		
		return new ArrayList<>();
	}

	private static List<MatchedStop> readGTFSStops(CSVParser csvParser) throws IOException {
		List<MatchedStop> stops = new ArrayList<MatchedStop>();
		
		for(CSVRecord rec : csvParser.getRecords()) {
			Map<String, String> recordAsMap = rec.toMap();
			
			MatchedStop stop = new MatchedStop();
			
			stop.setId(recordAsMap.get("stop_id"));
			stop.setCode(recordAsMap.get("stop_code"));

			stop.setName(recordAsMap.get("stop_name"));
			
			stop.setLon(recordAsMap.get("stop_lon"));
			stop.setLat(recordAsMap.get("stop_lat"));

			stops.add(stop);
		}
		
		return stops;
	}
}
