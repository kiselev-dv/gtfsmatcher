package me.osm.gtfsmatcher.model;

public class OSMRelationMember {

	private long ref;
	private String type;
	private String role;

	public OSMRelationMember(long ref, String type, String role) {
		this.ref = ref;
		this.type = type;
		this.role = role;
	}

}
