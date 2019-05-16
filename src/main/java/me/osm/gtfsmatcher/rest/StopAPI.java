package me.osm.gtfsmatcher.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.restexpress.Request;
import org.restexpress.Response;

import me.osm.gtfsmatcher.matching.StopMatcher;
import me.osm.gtfsmatcher.model.GTFSStop;

public class StopAPI {
	
	private static final String DATA_FOLDER = System.getProperty("data.dir", System.getProperty("user.dir") + "/data"); 

	private StopMatcher _matcher = new StopMatcher(); 
	
	public List<GTFSStop> read(Request req, Response res) throws FileNotFoundException, IOException {
		try {
			return _matcher.matchStops(new File(DATA_FOLDER + "/google_transit.zip")); 
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
}
