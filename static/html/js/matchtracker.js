app.factory('MatchTracker', [function() {
	
	// TODO: fix, more than one Stop might be assigned to osm node
	var osm2gtfs = {};
	
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
	
	return {
		gtfs4OSM: gtfs4OSM,
		setMatched: setMatched,
		resetMatched: resetMatched
	};
	
}]);