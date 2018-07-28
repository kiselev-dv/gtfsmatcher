package me.osm.gtfsmatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import me.osm.gtfsmatcher.model.OSMObject;

public class OSMData {

	private Map<Long, OSMObject> nodes = new HashMap<>();
	private Map<Long, OSMObject> ways = new HashMap<>();
	private Map<Long, OSMObject> relations = new HashMap<>();

	public void add(OSMObject osmObject) {
		if ("node".equals(osmObject.getType())) {
			this.nodes.put(osmObject.getId(), osmObject);
		} 
		else if("way".equals(osmObject.getType())) {
			this.ways.put(osmObject.getId(), osmObject);
		}
		else if("relation".equals(osmObject.getType())) {
			this.relations.put(osmObject.getId(), osmObject);
		}
	}

	public Collection<OSMObject> listRelations() {
		return this.relations.values();
	}
	
	public Map<Long, OSMObject> getNodes() {
		return nodes;
	}

	public Map<Long, OSMObject> getWays() {
		return ways;
	}

	public Map<Long, OSMObject> getRelations() {
		return relations;
	}
}
