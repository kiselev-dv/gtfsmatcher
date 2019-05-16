package me.osm.gtfsmatcher.matching;

import com.vividsolutions.jts.geom.Envelope;

import me.osm.gtfsmatcher.model.OSMData;

public interface OSMDataProvider {

	OSMData getOSMRoutes(Envelope envelope);

}
