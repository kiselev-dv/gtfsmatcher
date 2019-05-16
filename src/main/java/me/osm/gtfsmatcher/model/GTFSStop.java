package me.osm.gtfsmatcher.model;

import java.util.List;

public class GTFSStop {

	private String id;
	private String code;
	private String name;
	private double lon;
	private double lat;
	private List<OSMObject> candidates;
	private List<OSMObject> matches;
	private OSMObject matched;

	public void setId(String id) {
		this.id = id;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLon(String lonS) {
		this.lon = Double.parseDouble(lonS);
	}

	public void setLat(String latS) {
		this.lat = Double.parseDouble(latS);
	}

	public String getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public double getLon() {
		return lon;
	}

	public double getLat() {
		return lat;
	}

	public void setCandidates(List<OSMObject> candidates) {
		this.candidates = candidates;
	}

	public void setMatched(OSMObject matched) {
		this.matched = matched;
	}

	public OSMObject getMatched() {
		return this.matched;
	}

	public void setMatches(List<OSMObject> matches) {
		this.matches = matches;
	}
	
}
