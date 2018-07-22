package me.osm.gtfsmatcher.matching;

import me.osm.gtfsmatcher.model.MatchedStop;
import me.osm.gtfsmatcher.model.OSMObject;

public interface StopsMatcher {
	boolean match(MatchedStop stop, OSMObject neighbour);
	public boolean refMatch(MatchedStop stop, OSMObject neighbour);
}
