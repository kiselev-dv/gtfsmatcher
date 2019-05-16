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
		String osmRef = neighbour.getTags().get("ref");
		return stop.getCode().equals(osmRef) || stop.getId().equals(osmRef);
	}

	@Override
	public boolean match(GTFSStop stop, OSMObject neighbour) {
		return refMatch(stop, neighbour) || nameMatch(stop, neighbour);
	}
	
}
