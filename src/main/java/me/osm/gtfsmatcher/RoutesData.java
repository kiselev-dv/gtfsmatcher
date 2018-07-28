package me.osm.gtfsmatcher;

import java.util.Collection;
import java.util.List;

import me.osm.gtfsmatcher.model.OSMObject;

public class RoutesData {

	private Collection<GTFSRoute> routes;
	private List<OSMObject> orphants;
	private OSMData data;

	public RoutesData(Collection<GTFSRoute> routes, List<OSMObject> orphants, OSMData data) {
		this.routes = routes;
		this.orphants = orphants;
		this.data = data;
	}

}
