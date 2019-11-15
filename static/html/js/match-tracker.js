// TODO: fix, more than one Stop might be assigned to osm node
const osm2gtfs = {};

function gtfs4OSM(osmobj) {
	if (osmobj.id) {
		return osm2gtfs[osmobj.id];
	}
	return null;
}

function setMatched(stop, matched) {
	osm2gtfs[matched.id] = stop;
}

function resetMatched(stop, matched) {
	osm2gtfs[matched.id] = null;
}

export default {
	gtfs4OSM: gtfs4OSM,
	setMatched: setMatched,
	resetMatched: resetMatched
};