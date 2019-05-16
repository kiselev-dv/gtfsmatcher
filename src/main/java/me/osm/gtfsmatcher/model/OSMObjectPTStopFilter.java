package me.osm.gtfsmatcher.model;

public class OSMObjectPTStopFilter {
	public boolean isStop(OSMObject osmObj) {

		if (osmObj != null && osmObj.getTags() != null && !osmObj.getTags().isEmpty()) {
			String highway = osmObj.getTags().get("highway");
			boolean busStops = "bus_stop".equals(highway) || "bus_station".equals(highway) || "platform".equals(highway);
			
			String railway = osmObj.getTags().get("railway");
			boolean railwayStops = "tram_stop".equals(railway) || "halt".equals(railway) || "station".equals(railway) || 
					"platform".equals(railway) || "subway_entrance".equals(railway);
			
			String publicTransport = osmObj.getTags().get("public_transport");
			boolean pt = "platform".equals(publicTransport) || "platform".equals(railway) || "stop_position".equals(railway);
			
			return busStops /*|| railwayStops || pt*/;
		}
		
		return false;
	}
}
