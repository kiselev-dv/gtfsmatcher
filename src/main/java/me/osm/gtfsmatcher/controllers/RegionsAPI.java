package me.osm.gtfsmatcher.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.restexpress.Request;
import org.restexpress.Response;

import me.osm.gtfsmatcher.model.server.FormField;
import me.osm.gtfsmatcher.model.server.FormTemplate;
import me.osm.gtfsmatcher.model.server.FormTemplateField;
import me.osm.gtfsmatcher.model.server.Region;
import me.osm.gtfsmatcher.model.server.RegionFileInfo;

public class RegionsAPI {
	
	private static final String DATA_FOLDER = System.getProperty("data.dir", System.getProperty("user.dir") + "/data");
	
	public List<Region> list(Request req, Response res ) throws Exception {
		return RegionsDAO.getInstance().listRegions(); 
	}
	
	public Region create(Request req, Response res) throws Exception {
		Region region = req.getBodyAs(Region.class);
		
		RegionsDAO.getInstance().saveRegion(region);
		
		return region;
	}
	
	public Region delete(Request req, Response res) throws Exception {
		String region = req.getHeader("region");
		return RegionsDAO.getInstance().deleteRegion(region);
	}
	
	public Region update(Request req, Response res) throws Exception {
		return create(req, res);
	}
	
	public Region read(Request req, Response res) throws Exception {
		String region = req.getHeader("region");
		return RegionsDAO.getInstance().getRegion(region);
	}
	
	public String download(Request req, Response res ) throws Exception {
		String region = req.getHeader("region");
		Region r = RegionsDAO.getInstance().getRegion(region);
		
		Path path = Paths.get(DATA_FOLDER, region, "google_transit.zip");
		path.toFile().getParentFile().mkdirs();

		try(
			InputStream is = new URL(r.getGtfsSource()).openStream();
			OutputStream os = new FileOutputStream(path.toFile());
		) {
			System.out.println("Start downlod " + r.getGtfsSource());
			IOUtils.copy(is, os);
			os.flush();
			System.out.println(region + " downloaded");
		}

		return "downloaded"; 
	}
	
	public RegionFileInfo fileInfo(Request req, Response res) {
		String region = req.getHeader("region");
		Path path = Paths.get(DATA_FOLDER, region, "google_transit.zip");
		File file = path.toFile();
		
		RegionFileInfo regionFileInfo = new RegionFileInfo();
		regionFileInfo.setExists(file.exists());
		if (file.exists()) {
			regionFileInfo.setLastModified(file.lastModified());
		}
		
		return regionFileInfo;
	}
	
	public FormTemplate template(Request req, Response res ) throws Exception {
		FormTemplate template = new FormTemplate();
		List<FormTemplateField> fields = new ArrayList<FormTemplateField>();
		template.setFields(fields);
		
		for(Field field: Region.class.getDeclaredFields()) {
		    if (field.isAnnotationPresent(FormField.class)) {
		    	FormField formField = field.getAnnotation(FormField.class);
		    	
		    	FormTemplateField templateField = new FormTemplateField();
		    	
		    	templateField.setName(field.getName());
		    	templateField.setTitle(formField.title());
		    	
		    	fields.add(templateField);
		    }
		}
		
		return template; 
	}

}
