package me.osm.gtfsmatcher.model;

import java.util.Collection;
import java.util.List;

public class RoutesData {

	private Collection<GTFSRoute> routes;
	private List<OSMObject> orphants;
	private OSMData data;

	public RoutesData(Collection<GTFSRoute> routes, List<OSMObject> orphants, OSMData data) {
		this.routes = routes;
		this.orphants = orphants;
		this.data = data;
	}

	public Collection<GTFSRoute> getRoutes() {
		return routes;
	}

	public List<OSMObject> getOrphants() {
		return orphants;
	}

	public OSMData getData() {
		return data;
	}

}
