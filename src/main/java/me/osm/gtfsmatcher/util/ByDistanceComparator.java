package me.osm.gtfsmatcher.util;

import java.util.Comparator;

import me.osm.gtfsmatcher.model.OSMObject;
import static me.osm.gtfsmatcher.util.SphericalMercator.*;

public class ByDistanceComparator implements Comparator<OSMObject> {

	private double cx;
	private double cy;

	public ByDistanceComparator(double x, double y) {
		this.cx = x;
		this.cy = y;
	}

	@Override
	public int compare(OSMObject o1, OSMObject o2) {
		
		double x1 =  lon2x(o1.getLon());
		double y1 =  lat2y(o1.getLat());
		
		double x2 =  lon2x(o2.getLon());
		double y2 =  lat2y(o2.getLat());
		
		double dx1 = Math.abs(cx - x1);
		double dy1 = Math.abs(cy - y1);
		
		double dx2 = Math.abs(cx - x2);
		double dy2 = Math.abs(cy - y2);
		
		double d1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
		double d2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
		
		return Double.compare(d1, d2);
	}

	
}
