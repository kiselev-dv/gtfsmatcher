package me.osm.gtfsmatcher.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OSMObject {

	private String type;
	
	private double lon;
	private double lat;

	private long id;
	private Map<String, String> tags;
	
	private List<OSMRelationMember> members = null;

	private int version;

	private long uid;

	private String user;

	private String timestamp;

	public void setType(String type) {
		this.type = type;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}
	
	public void setLat(double lat) {
		this.lat = lat;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setTags(Map<String, Object> map) {
		this.tags = map.entrySet().stream()
			     .collect(Collectors.toMap(Map.Entry::getKey, e -> (String)e.getValue()));
	}

	public String getType() {
		return type;
	}

	public double getLon() {
		return lon;
	}

	public double getLat() {
		return lat;
	}

	public long getId() {
		return id;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void setUID(long uid) {
		this.uid = uid;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public void addMember(long ref, String type, String role) {
		if(members == null) {
			members = new ArrayList<>();
		}
		
		members.add(new OSMRelationMember(ref, type, role));
	}
	
}
