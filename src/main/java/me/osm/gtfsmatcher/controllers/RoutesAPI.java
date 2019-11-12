package me.osm.gtfsmatcher.controllers;

import java.io.File;

import org.restexpress.Request;
import org.restexpress.Response;

import me.osm.gtfsmatcher.matching.RoutesMatcher;
import me.osm.gtfsmatcher.model.RoutesData;

public class RoutesAPI {

	private static final String DATA_FOLDER = System.getProperty("data.dir", System.getProperty("user.dir") + "/data");
	
	private RoutesMatcher matcher = new RoutesMatcher();

	public RoutesData read(Request req, Response res ) throws Exception {
		
		File file = new File(DATA_FOLDER + "/google_transit.zip");
		
		return matcher.matchRoutes(file, false, null);
	}
	
}
