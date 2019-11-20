package me.osm.gtfsmatcher.matching;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import me.osm.gtfsmatcher.model.GTFSStop;
import me.osm.gtfsmatcher.model.OSMObject;

public class DefaultStopsMatcher implements StopsMatcher {

	public boolean nameMatch(GTFSStop stop, OSMObject neighbour) {
		
		String osmName = neighbour.getTags().get("name");
		
		if (osmName != null) {
			Set<String> osmNameTokens = new HashSet(Arrays.asList(StringUtils.split(osmName, " ,()[]'\"\\|/-")));
			Set<String> gtfsNameTokens = new HashSet(Arrays.asList(StringUtils.split(stop.getName(), " ,()[]'\"\\|/-")));
			
			long matchedTokens = osmNameTokens.stream().filter(gtfsNameTokens::contains).count();
			int matchTreshold = Math.max(2, osmNameTokens.size());
			
			return (matchedTokens >= matchTreshold);
		}
		
		return false;
	}

	public boolean refMatch(GTFSStop stop, OSMObject neighbour) {
		Set<String> osmRefVariants = new HashSet<>();
		
		osmRefVariants.add(neighbour.getTags().get("ref"));
		osmRefVariants.add(neighbour.getTags().get("gtfs:ref"));
		osmRefVariants.add(neighbour.getTags().get("ref:gtfs"));
		osmRefVariants.add(neighbour.getTags().get("gtfs_code"));
		osmRefVariants.add(neighbour.getTags().get("gtfs_stop_code"));
		
		osmRefVariants.remove(null);
		
		return osmRefVariants.contains(stop.getCode()) || osmRefVariants.contains(stop.getId());
	}

	@Override
	public boolean match(GTFSStop stop, OSMObject neighbour) {
		return refMatch(stop, neighbour) || nameMatch(stop, neighbour);
	}
	
}
