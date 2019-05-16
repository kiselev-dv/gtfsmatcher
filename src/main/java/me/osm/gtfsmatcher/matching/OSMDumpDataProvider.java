package me.osm.gtfsmatcher.matching;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.vividsolutions.jts.geom.Envelope;

import me.osm.gtfsmatcher.model.OSMData;
import me.osm.gtfsmatcher.model.OSMObject;
import me.osm.gtfsmatcher.model.OSMObjectPTStopFilter;

public class OSMDumpDataProvider extends DefaultHandler implements OSMDataProvider {

	private File osmDataFile;
	private OSMObject osmObj;
	private OSMData osmData;
	private long readObjects;
	private long added;

	private static final OSMObjectPTStopFilter objFilter = new OSMObjectPTStopFilter();

	public OSMDumpDataProvider(File osmDataFile) {
		this.osmDataFile = osmDataFile;
		this.osmData = new OSMData();
	}

	@Override
	public OSMData getOSMRoutes(Envelope envelope) {
		System.out.println("Read OSM Data from " + this.osmDataFile);
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			InputStream is = new FileInputStream(this.osmDataFile);
			if (this.osmDataFile.getName().endsWith(".gz")) {
				is = new GZIPInputStream(is);
			}
			saxParser.parse(is, this);
			is.close();
			
			return this.osmData;
		}
		catch (Exception e) {
			throw new Error(e);
		}

	}
	
	@Override
	public void startElement(String uri, String localName, 
			String qName, Attributes attributes) throws SAXException {
		
		if("node".equals(qName) || "way".equals(qName) || "relation".equals(qName)) {
			this.osmObj = new OSMObject();
			
			this.osmObj.setType(qName);
			this.osmObj.setId(Long.valueOf(attributes.getValue("id")));
			
			if ("node".equals(qName)) {
				this.osmObj.setLon(Double.valueOf(attributes.getValue("lon")));
				this.osmObj.setLat(Double.valueOf(attributes.getValue("lat")));
			}

			this.osmObj.setTimestamp(attributes.getValue("timestamp"));
			this.osmObj.setUser(attributes.getValue("user"));
			if (attributes.getValue("uid") != null) {
				this.osmObj.setUID(Long.valueOf(attributes.getValue("uid")));
			}
			if (attributes.getValue("version") != null) {
				this.osmObj.setVersion(Integer.valueOf(attributes.getValue("version")));
			}
		}
		else {
			if("tag".equals(qName)) {
				if(this.osmObj.getTags() == null) {
					this.osmObj.setTags(new HashMap<>());
				}
				this.osmObj.getTags().put(attributes.getValue("k"), attributes.getValue("v"));
			}
			else if("nd".equals(qName)) {
				if(this.osmObj.getNodes() == null) {
					this.osmObj.setNodes(new ArrayList<>());
				}
				if (attributes.getValue("ref") != null) {
					this.osmObj.getNodes().add(Long.valueOf(attributes.getValue("ref")));
				}
			}
			else if("member".equals(qName)) {
				this.osmObj.addMember(Long.valueOf(attributes.getValue("ref")), 
						attributes.getValue("type"), attributes.getValue("role"));
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if("node".equals(qName) || "way".equals(qName) || "relation".equals(qName)) {

			// Ignore objects without tags
			if(osmObj.getTags() != null && !osmObj.getTags().isEmpty()) {
				if(filterObject(qName)) {
					this.osmData.add(osmObj);
					this.added++;
				}
			} 
			
			this.readObjects++;
			if (this.readObjects % 1_000_000 == 0) {
				System.out.println(this.readObjects + " OSM Objects read, " + 
						this.added + " objects loaded");
			}
		}
	}

	private boolean filterObject(String qName) {
		if ("relation".equals(qName)) {
			String routeTag = osmObj.getTags().get("route");
			
			boolean route = "bus".equals(routeTag) || "tram".equals(routeTag) || "trolleybus".equals(routeTag); 
			boolean routeMaster = "route_master".equals(osmObj.getTags().get("type"));
			
			return route || routeMaster;
		}
		if ("node".equals(qName)) {
			return objFilter.isStop(osmObj);
		}
		return false;
	}
	
	@Override
	public void endDocument() throws SAXException {
		System.out.println("Done reading");
	}

}
