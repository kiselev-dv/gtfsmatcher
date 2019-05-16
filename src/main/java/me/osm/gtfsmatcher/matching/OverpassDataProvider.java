package me.osm.gtfsmatcher.matching;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.vividsolutions.jts.geom.Envelope;

import me.osm.gtfsmatcher.model.OSMData;
import me.osm.gtfsmatcher.model.OSMObject;

public class OverpassDataProvider implements OSMDataProvider {
	
	private static final double WIDTH_THRESHOLD = 5.0;
	private static final double HEIGHT_THRESHOLD = 5.0;
	
	private static final String urlBase = "http://overpass-api.de/api/interpreter?data=";
	private static final String qTemplate = 
			  "[out:json][timeout:480];(relation[type=route_master]({{bbox}});"
			
			+ "relation[type=route][route=bus]({{bbox}});"
			+ "relation[type=route][route=funicular]({{bbox}});"
			+ "relation[type=route][route=light_rail]({{bbox}});"
			+ "relation[type=route][route=monorail]({{bbox}});"
			+ "relation[type=route][route=share_taxi]({{bbox}});"
			+ "relation[type=route][route=subway]({{bbox}});"
			+ "relation[type=route][route=train]({{bbox}});"
			+ "relation[type=route][route=tram]({{bbox}});"
			+ "relation[type=route][route=trolleybus]({{bbox}});"
			+ "relation[type=route][route=ferry]({{bbox}});"
			
			+ ");out meta;>;out meta qt;";

	@Override
	public OSMData getOSMRoutes(Envelope envelope) {

		try {
			Collection<Envelope> envelopes = split(envelope);
			OSMData data = new OSMData();
			
			for (Envelope env : envelopes) {
				String overpassQ = getOverpassQ(env);
				System.out.println("Query OSM data for " + env.toString());
				
				JSONObject overpassAnswer = new JSONObject(IOUtils.toString(new URL(urlBase + overpassQ).openStream()));
				JSONArray elements = overpassAnswer.optJSONArray("elements");
				
				if (elements == null || elements.length() == 0) {
					System.out.println("Overpass returned an empty answer");
					System.exit(1);
				}
				
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

				try {
					System.out.println("Throttle queries");
					Thread.sleep(1000 * 15);
				} catch (InterruptedException e) {
				}
			}
			
			return data;
		}
		catch(Exception e) {
			throw new Error(e);
		}
	}

	private Collection<Envelope> split(Envelope env) {
		
		Collection<Envelope> splitX = splitX(Collections.singletonList(env));
		
		return splitY(splitX);
	}

	private Collection<Envelope> splitX(Collection<Envelope> envs) {
		Collection<Envelope> result = new ArrayList<>();
		for (Envelope e : envs) {
			if(e.getWidth() > WIDTH_THRESHOLD) {
				result.addAll(splitX(splitXInHalf(e)));
			}
			else {
				result.add(e);
			}
		}
		return result;
	}
	
	private Collection<Envelope> splitY(Collection<Envelope> envs) {
		Collection<Envelope> result = new ArrayList<>();
		for (Envelope e : envs) {
			if(e.getHeight() > HEIGHT_THRESHOLD) {
				result.addAll(splitY(splitYInHalf(e)));
			}
			else {
				result.add(e);
			}
		}
		return result;
	}
	
	private Collection<Envelope> splitYInHalf(Envelope e) {
		double median = e.getMinY() + e.getHeight() / 2.0;
		
		return Arrays.asList(new Envelope[] {
				new Envelope(e.getMinX(), e.getMaxX(), e.getMinY(), median * 1.001),
				new Envelope(e.getMinX(), e.getMaxX(), median * 0.999, e.getMaxY()),
		});
	}

	private Collection<Envelope> splitXInHalf(Envelope e) {
		double median = e.getMinX() + e.getWidth() / 2.0;
		return Arrays.asList(new Envelope[] {
				new Envelope(e.getMinX(), median * 1.001, e.getMinY(), e.getMaxY()),
				new Envelope(median * 0.999, e.getMaxX(), e.getMinY(), e.getMaxY()),
		});
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
