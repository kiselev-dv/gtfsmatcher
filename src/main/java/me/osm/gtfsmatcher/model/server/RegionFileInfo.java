package me.osm.gtfsmatcher.model.server;

public class RegionFileInfo {
	
	private boolean exists;
	private long lastModified;
	
	public boolean isExists() {
		return exists;
	}
	public void setExists(boolean exists) {
		this.exists = exists;
	}
	
	public long getLastModified() {
		return lastModified;
	}
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
	
}
