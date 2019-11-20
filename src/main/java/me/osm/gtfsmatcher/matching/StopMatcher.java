package me.osm.gtfsmatcher.matching;

import java.io.File;
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
import java.util.LinkedHashSet;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;

import me.osm.gtfsmatcher.matching.OverpassQueryBuilder.OverpassQueryBuilderUnion;
import me.osm.gtfsmatcher.model.GTFSStop;
import me.osm.gtfsmatcher.model.GTFSStopsMatch;
import me.osm.gtfsmatcher.model.OSMObject;
import me.osm.gtfsmatcher.util.ByDistanceComparator;
import me.osm.gtfsmatcher.util.SphericalMercator;

public class StopMatcher {
	
	private volatile static Envelope envelope = null;
	
	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
	private static final String urlBase = "http://overpass-api.de/api/interpreter?data=";
//	private static final String qTemplate = "[out:json][timeout:25];[bbox:{{bbox}}];"
//			+ "("
//			+ "    node[\"highway\"=\"bus_stop\"];"
//			+ "    node[\"public_transport\"=\"platform\"];"
//			+ "    way[\"public_transport\"=\"platform\"];"
//			+ "    node[\"railway\"=\"tram_stop\"];"
//			+ ");"
//			+ "out meta;>;out meta qt;";
	
	private static final StopsMatcher matcher = new DefaultStopsMatcher();
	
	public GTFSStopsMatch matchStops(File gtfs) throws IOException, MalformedURLException {
		List<GTFSStop> gtfsStops = readStopsFromGTFS(gtfs);
		
		Envelope env = getEnvelope(gtfsStops);
		LinkedHashSet<OSMObject> osmStops = new LinkedHashSet<OSMObject>(getOSMStops(env));
		
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
		
		gtfsStops.forEach(gtfsStop -> {
			osmStops.remove(gtfsStop.getMatched());
		});
		
		GTFSStopsMatch match = new GTFSStopsMatch();
		match.setGtfs(gtfsStops);
		match.setOrphants(new ArrayList<OSMObject>(osmStops));
		
		return match;
	}
	
	public static Envelope getEnvelope(File file) throws IOException {
		if (envelope != null) {
			return envelope;
		}
		
		return getEnvelope(readStopsFromGTFS(file));
	}

	public static Envelope getEnvelope(List<GTFSStop> stops) {
		if (envelope != null) {
			return envelope;
		}
		
		Envelope env = new Envelope();
		for (GTFSStop stop : stops) {
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
			
			JSONObject tags = element.optJSONObject("tags");
			if (tags == null) {
				if ("node".equals(element.optString("type"))) {
					System.out.println("Empty stop node: " + element);
				}
				continue;
			}

			osmObject.setTags(tags.toMap());
			
			if ("node".equals(element.optString("type"))) {
				osmObject.setLon(element.getDouble("lon"));
				osmObject.setLat(element.getDouble("lat"));
				
				osmStops.add(osmObject);
			}
		}
		
		return osmStops;
	}

	private String getOverpassQ(Envelope env) {
		
		OverpassQueryBuilderUnion union = OverpassQueryBuilder.union()
			.addElement("node", "highway", "bus_stop")
			.addElement("node", "railway", "tram_stop")
			.addElement("node", "public_transport", "platform");
		
		//union.addElement("way",  "public_transport", "platform");
		
		Envelope copy = new Envelope(env);
		copy.expandBy(0.01);
		
		String query = OverpassQueryBuilder.builder().addUnion(union).bbox(copy).build();
		
		try {
			return URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	private static List<GTFSStop> readStopsFromGTFS(File file) throws IOException {
		try(ZipFile zipFile = new ZipFile(file)) {
			
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

	public static List<GTFSStop> readGTFSStops(CSVParser csvParser) throws IOException {
		List<GTFSStop> stops = new ArrayList<GTFSStop>();
		
		for(CSVRecord rec : csvParser.getRecords()) {
			Map<String, String> recordAsMap = rec.toMap();
			
			GTFSStop stop = new GTFSStop();
			
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
