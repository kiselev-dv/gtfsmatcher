package me.osm.gtfsmatcher.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.restexpress.Request;
import org.restexpress.Response;

public class StaticAPI {

	private static final File basePath = Paths.get(System.getProperty("user.dir"), "static", "html").toFile();
	
	public String read(Request req, Response res) throws FileNotFoundException, IOException {
		
		String requestPath = new URL(req.getUrl()).getPath();
		if (StringUtils.isEmpty(requestPath) || "/".equals(requestPath)) {
			requestPath = "/index.html";
		}

		File file = new File(basePath, requestPath);
		
		if (file.exists()) {
			String mime = Files.probeContentType(file.toPath());
			res.setContentType(mime + "; charset=UTF-8");
			return IOUtils.toString(new FileInputStream(file));
		}

		res.setResponseCode(404);
		return null;
	}

}
