package me.osm.gtfsmatcher;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription="Augment GTFS with OSM data")
public class AugmentOptions {
	
	@Parameter(names= {"--in", "-i"})
	private String in;
	
	@Parameter(names= {"--out", "-o"})
	private String out;
	
	@Parameter(names="--sqlite")
	private boolean sqlite;
	
	@Parameter(names="--trips-first")
	private boolean tripsFirst;
	
	@Parameter(names="--osm-data")
	private String osmData;

	public String getIn() {
		return in;
	}

	public void setIn(String in) {
		this.in = in;
	}

	public String getOut() {
		return out;
	}

	public void setOut(String out) {
		this.out = out;
	}

	public boolean isSqlite() {
		return sqlite;
	}

	public void setSqlite(boolean sqlite) {
		this.sqlite = sqlite;
	}

	public boolean isTripsFirst() {
		return tripsFirst;
	}

	public void setTripsFirst(boolean tripsFirst) {
		this.tripsFirst = tripsFirst;
	}

	public String getOsmData() {
		return osmData;
	}

	public void setOsmData(String osmData) {
		this.osmData = osmData;
	}
	
}
