const searchParams = new URLSearchParams(window.location.search);
const region = searchParams.get('region');
const app = angular.module('gtfsmatch', []);

app.factory('StopsService', ['$http', function($http) {
	return {
		list: function() {
			return $http({
			    url: '/stops/' + region + '/list.json', 
			    method: "GET"
			});
		}
	};
}]);

app.factory('RoutesService', ['$http', function($http) {
	return {
		list: function() {
			return $http({
			    url: '/routes/' + region + '/list.json', 
			    method: "GET"
			});
		}
	};
}]);

app.factory('OptionsService', ['$http', function($http) {
	return {
		get: function() {
			return $http({
			    url: '/regions/' + region + '.json', 
			    method: "GET"
			});
		}
	};
}]);

app.factory('DataHolder', [function() {
	var stopById = {};
	return {
		trackStops: function(stops) {
			stops.forEach(function(s) {
				stopById[s.id] = s;
			});
		},
		getStop: function(id) {
			return stopById[id];
		}
	};
}]);