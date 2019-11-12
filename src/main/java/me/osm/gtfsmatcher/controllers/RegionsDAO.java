package me.osm.gtfsmatcher.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.osm.gtfsmatcher.model.server.Region;
import me.osm.gtfsmatcher.model.server.Regions;

public class RegionsDAO {
	
	private static final String DATA_FOLDER = System.getProperty("data.dir", System.getProperty("user.dir") + "/data");
	private static final File file = new File(DATA_FOLDER + "/regions.json");

	private static final RegionsDAO instance = new RegionsDAO();

	public static RegionsDAO getInstance() {
		return instance;
	}
	
	private LinkedHashMap<String, Region> regionByName = new LinkedHashMap<String, Region>();
	
	private RegionsDAO() {
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			if (!file.exists()) {
				file.createNewFile();
				
				save();
			}
			
			FileInputStream fileInputStream = new FileInputStream(file);
			Regions regions = objectMapper.readValue(fileInputStream, Regions.class);

			regions.getRegions().forEach(r -> {
				regionByName.put(r.getName(), r);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Region getRegion(String name) {
		return regionByName.get(name);
	}
	
	public List<Region> listRegions() {
		return new ArrayList<Region>(regionByName.values());
	}
	
	public void saveRegion(Region region) {
		regionByName.put(region.getName(), region);
		save();
	}

	public Region deleteRegion(String region) {
		Region removed = regionByName.remove(region);
		save();
		
		return removed;
	}

	private void save() {
		Regions regions = new Regions();
		regions.setRegions(listRegions());
		
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			objectMapper.writeValue(file, regions);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
