package me.osm.gtfsmatcher.model.server;

public class Region {

	@FormField(title = "Name")
	private String name;
	
	@FormField(title = "Title")
	private String title;
	
	@FormField(title = "GTFS Source url")
	private String gtfsSource;
	@FormField(title = "GTFS Real time source url")
	private String gtfsRTSource;
	
	@FormField(title = "GTFS Ref tag")
	private String gtfsRefTag;
	@FormField(title = "Name template")
	private String nameTemplate;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getGtfsSource() {
		return gtfsSource;
	}

	public void setGtfsSource(String gtfsSource) {
		this.gtfsSource = gtfsSource;
	}

	public String getGtfsRTSource() {
		return gtfsRTSource;
	}

	public void setGtfsRTSource(String gtfsRTSource) {
		this.gtfsRTSource = gtfsRTSource;
	}

	public String getGtfsRefTag() {
		return gtfsRefTag;
	}

	public void setGtfsRefTag(String gtfsRefTag) {
		this.gtfsRefTag = gtfsRefTag;
	}

	public String getNameTemplate() {
		return nameTemplate;
	}

	public void setNameTemplate(String nameTemplate) {
		this.nameTemplate = nameTemplate;
	}
	
}
