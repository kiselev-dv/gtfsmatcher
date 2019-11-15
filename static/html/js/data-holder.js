const stopById = {};

export default {
	
	trackStops: function(stops) {
		stops.forEach(function(s) {
			stopById[s.id] = s;
		});
	},
	
	getStop: function(id) {
		return stopById[id];
	}
};