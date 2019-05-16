package me.osm.gtfsmatcher.matching;

import me.osm.gtfsmatcher.model.GTFSStop;
import me.osm.gtfsmatcher.model.OSMObject;

public interface StopsMatcher {
	boolean match(GTFSStop stop, OSMObject neighbour);
	public boolean refMatch(GTFSStop stop, OSMObject neighbour);
}
