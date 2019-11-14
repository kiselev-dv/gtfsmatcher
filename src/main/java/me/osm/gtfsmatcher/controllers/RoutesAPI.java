package me.osm.gtfsmatcher.controllers;

import java.io.File;
import java.nio.file.Paths;

import org.restexpress.Request;
import org.restexpress.Response;

import me.osm.gtfsmatcher.matching.RoutesMatcher;
import me.osm.gtfsmatcher.model.RoutesData;

public class RoutesAPI {

	private static final String DATA_FOLDER = System.getProperty("data.dir", System.getProperty("user.dir") + "/data");
	
	private RoutesMatcher matcher = new RoutesMatcher();

	public RoutesData read(Request req, Response res ) throws Exception {
		
		File file = Paths.get(DATA_FOLDER, req.getHeader("region"), "google_transit.zip").toFile();
		
		return matcher.matchRoutes(file, false, null);
	}
	
}
