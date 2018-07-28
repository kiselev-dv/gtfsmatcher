package me.osm.gtfsmatcher.model;

public class OSMRelationMember {

	public final long ref;
	public final String type;
	public final String role;

	public OSMRelationMember(long ref, String type, String role) {
		this.ref = ref;
		this.type = type;
		this.role = role;
	}

}
