package me.osm.gtfsmatcher.controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.restexpress.Request;
import org.restexpress.Response;

import me.osm.gtfsmatcher.matching.StopMatcher;
import me.osm.gtfsmatcher.model.GTFSStop;

public class StopAPI {
	
	private static final String DATA_FOLDER = System.getProperty("data.dir", System.getProperty("user.dir") + "/data"); 

	private StopMatcher matcher = new StopMatcher(); 
	
	public List<GTFSStop> read(Request req, Response res) throws FileNotFoundException, IOException {
		try {
			String region = req.getHeader("region");
			
			Path path = Paths.get(DATA_FOLDER, region, "google_transit.zip");
			
			return matcher.matchStops(path.toFile()); 
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
}
