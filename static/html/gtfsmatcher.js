
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

app.factory('Template', [function(){
	function parseTemplate(templateString) {
		var template = {};
		var i = 0;
		
		var replacer = function(varName, regexpArgs, subj) {
			var val = subj[varName];
			if (regexpArgs) {
				// Replace $args inside re arguments
				regexpArgs = regexpArgs.replace(/\$([\w]+)/g, function(match, vn) {
					return subj[vn];
				});

				var args = /['\/"](.*)['\/"],[\s]*['\/"](.*)['\/"]/g.exec(regexpArgs);
				return val.replace(RegExp(args[1]), args[2]);
			}
			
			return val;
		};

		template.pattern = templateString.replace(/\$([\w]+).re\((.*\))/g, function(match, varName, regexp) {
			var replacementKey = 'replace' + (i++);
			
			template[replacementKey] = replacer.bind(template, varName, regexp);
			
			return replacementKey;
		});
		
		template.pattern = template.pattern.replace(/\$([\w]+)/g, function(match, varName) {
			var replacementKey = 'replace' + (i++);
			template[replacementKey] = replacer.bind(template, varName, null);
			return replacementKey;
		});
		
		template['render'] = function(obj) {
			var self = this;
			return this.pattern.replace(/replace[\d]+/g, function(match) {
				return self[match](obj);
			})
		}
		
		return template;
	}
	
	return {
		parse: parseTemplate
	};
	
}]);

app.factory('Changeset', ['$http', '$sce', function($http, $sce) {
	var _bbox = [];
	var _create = {};
	var _update = {};
	var _counter = 0;
	
	$sce.trustAsResourceUrl('http://127.0.0.1:8111/import');
	
	function addBBOX(bbox) {
		_bbox.push(bbox);
	}
	
	function track(stop) {
		
	}
	
	function untrack(stop) {
		
	}
	
	function update(stop) {
		if(stop.matched.id && stop.matched.id > 0) {
			_update["node" + stop.matched.id] = stop.matched;
		}
	}

	function updateOSM(osmObject) {
		_update[osmObject.type + osmObject.id] = osmObject;
	}
	
	function createOSM(osmObject) {
		if (!osmObject.id) {
			osmObject.id = -(countByType(osmObject.type, _create) + 1);
		}
		_create[osmObject.type + osmObject.id] = osmObject;
	}
	
	function countByType(type, array) {
		var counter = 0;
		Object.keys(array).forEach(key => {
			if(key.indexOf(type) === 0) {
				counter ++;
			}
		});
		return counter;
	}
	
	function create(stop) {
		var node = stop.matched;
		if (!node.id) {
			node.id = -(++_counter);
		}
		_create["node" + node.id] = node;
	}
	
	function getChanges() {
		return {
			bboxes: _bbox,
			create: Object.values(_create),
			update: Object.values(_update)
		}
	}
	
	function getChangesetXML() {
		return $http({
		    url: '/format-changeset.xml', 
		    method: "POST",
		    data: getChanges()
		});
	}
	
	function openInJOSM() {
		$http({
		    url: '/format-changeset.xml', 
		    method: "POST",
		    data: getChanges(),
		    params: {
		    	save: true
		    }
		}).then(function(response) {
			$http({
				url: 'http://127.0.0.1:8111/import',
				method: "GET",
				params: {
					url: 'http://127.0.0.1:9080/format-changeset.xml?load=' + response.data
				}
			});
		});
	}
	
	return {
		addBBOX: addBBOX,
		track: track,
		untrack: untrack,
		update: update,
		create: create,
		getChanges: getChanges,
		getChangesetXML: getChangesetXML,
		openInJOSM: openInJOSM,
		updateOSM: updateOSM,
		createOSM: createOSM
	}
	
}]);

app.factory('MyMap', ['MatchTracker', function(tracker) {
	var mymap = L.map('mapid', {
		closePopupOnClick: false
	}).setView([51.505, -0.09], 13);
	
	var osmUrl='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
	var osmAttrib='Map data © <a href="https://openstreetmap.org">OpenStreetMap</a> contributors';
	var mapnik = new L.TileLayer(osmUrl, {maxZoom: 20, maxNativeZoom:18, attribution: osmAttrib});

	var sat = new L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
	    attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors, <a href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="https://www.mapbox.com/">Mapbox</a>',
	    maxZoom: 20,
	    maxNativeZoom: 18,
	    id: 'mapbox.streets-satellite',
	    accessToken: 'pk.eyJ1IjoiZW5kcG9pbnRjb3JwIiwiYSI6ImNqamFndTdiZjA1dmkzcG80bzU5bWh3N3AifQ.W7E1_Wt04nRuZIhoiR_P3g'
	});
	
	mapnik.addTo(mymap);
	L.control.layers({
		"Mapnik": mapnik,
		"MapBox": sat
	}, {}).addTo(mymap);
	
	
	var details = [];
	var _onCandidateClickCallback = null;
	var activeStop = null;
	var closePopupTimer = null;
	
	mymap.on('click', function() {
		closePopupTimer = setTimeout(function() {
			if (activeStop != null) {
				_deselect(activeStop);
			}
		}, 250);
	});
	
	function _select(stop) {
		if (!activeStop) {
			activeStop = stop;
			stop.onSelect();
			return;
		}
		
		if(stop.id != activeStop.id) {
			_deselect(activeStop);
			activeStop = stop;
			stop.onSelect();
		}
		
	}
	
	function _deselect(stop) {
		activeStop = null;
		stop.circle.closePopup();
		stop.onDeselect();
	}
	
	function showDetails(stop) {
		
		_select(stop);
		stop.circle.openPopup();
		
		if (stop.matched) {
			details.push(L.polyline([
				[stop.lat, stop.lon], 
				[stop.matched.lat, stop.matched.lon]], {color: 'green'}).addTo(mymap));
		}
		else if (stop.candidates.length > 0) {
			stop.candidates.forEach(function(c) {
				var parent = tracker.gtfs4OSM(c);
				if (parent) {
					details.push(L.polyline([
						[parent.lat, parent.lon], 
						[c.lat, c.lon]], {color: 'green'}).addTo(mymap));
				}
				
				var marker = L.marker([c.lat, c.lon]);
				marker.addTo(mymap);
				if (_onCandidateClickCallback) {
					marker.on('click', function() {
						//activeStop = stop;
						_select(stop);
						clearTimeout(closePopupTimer);
						_onCandidateClickCallback(stop, c);
					});
				}
				
				details.push(marker);
			});
		}
		mymap.setView([stop.lat, stop.lon], 18);
	}
	
	function hideDetails() {
		details.forEach(function(o) {
			o.remove();
		});
	}
	
	var bbox = [180.0, 90.0, -180.0, -90.0];
	
	var CIRCLE_RED = {
		color: 'red',
	    fillColor: '#f03',
	    fillOpacity: 0.5,
	    radius: 5
	};
	
	var CIRCLE_GREEN = {
		color: 'green',
	    fillColor: '#0f3',
	    fillOpacity: 0.5,
	    radius: 5
	};
	
	function displayStop(stop, onSelect, onDeselect) {
		bbox = [
			Math.min(bbox[0], stop.lon), 
			Math.min(bbox[1], stop.lat), 
			Math.max(bbox[2], stop.lon), 
			Math.max(bbox[3], stop.lat)];
		
		var circle = L.circle([stop.lat, stop.lon], stop.matched ? CIRCLE_GREEN : CIRCLE_RED)
			.bindPopup(stop.name, {offset: [0, -20], autoClose: false}).addTo(mymap);
		stop.circle = circle;
		
		stop.onSelect = onSelect;
		stop.onDeselect = onDeselect;
		
		circle.on('click', _select.bind(circle, stop));
		
		return circle;
	}
	
	function getBBOX() {
		return bbox;
	}
	
	function redrawStop(stop, codeTag) {
		if (stop.matched && stop.code == stop.matched.tags[codeTag]) {
			stop.circle.setStyle(CIRCLE_GREEN);
		}
		else {
			stop.circle.setStyle(CIRCLE_RED);
		}
		hideDetails(stop)
		showDetails(stop);
	}
	
	function reassignPosition(stop, done) {
		stop.circle.closePopup();
		
		var handler = function(evnt) {
			mymap.off('click', handler);
			
			// Save old coordinates
			if (stop.matched.initLL) {
				stop.matched.initLL = {
					lat: stop.matched.lat,
					lon: stop.matched.lon
				}
			}
			
			stop.matched.lat = evnt.latlng.lat;
			stop.matched.lon = evnt.latlng.lng;
			
			stop.circle.openPopup();
			
			done();
		};
		
		mymap.on('click', handler);
	}
	
	function fitBounds() {
		var bounds = L.latLngBounds(L.latLng(bbox[1], bbox[0]), L.latLng(bbox[3], bbox[2]));
		mymap.fitBounds(bounds);
	}
	
	function onCandidateClick(callback) {
		_onCandidateClickCallback = callback;
	}
	
	function showTrip(trip, route) {
		hideDetails();

		var coords = [];
		trip.forEach((s) => {
			if (s.matched) {
				coords.push([s.matched.lat, s.matched.lon]);
			}
			else {
				coords.push([s.lat, s.lon])
			}
		});
		
		var polyline = L.polyline(coords);
		var decor = addDecor(polyline);
		
		details.push(decor);
		details.push(polyline.addTo(mymap));
	}

	function addDecor(polyline) {
		return L.polylineDecorator(polyline, {
		    patterns: [{
	        	offset: '10%', 
	        	repeat: 50, 
	        	symbol: L.Symbol.arrowHead({
	        		pixelSize: 8, 
	        		polygon: false, 
	        		pathOptions: {stroke: true}
	        	})
		    }]
		}).addTo(mymap);
	}

	function showOSMRoute(route) {
		hideDetails();

		var coords = [];
		route.stopNodes.forEach((n) => {
			coords.push([n.lat, n.lon])
		});

		route.segments.forEach((s) => {
			details.push(L.polyline(coords).addTo(mymap));
		});

		var polyline = L.polyline(coords);
		var decor = addDecor(polyline);
		
		details.push(decor);
		details.push(polyline.addTo(mymap));
	}
	
	return {
		showDetails: showDetails,
		hideDetails: hideDetails,
		displayStop: displayStop,
		getBBOX: getBBOX,
		redrawStop: redrawStop,
		reassignPosition: reassignPosition,
		fitBounds: fitBounds,
		onCandidateClick: onCandidateClick,
		showTrip: showTrip,
		showOSMRoute: showOSMRoute
	};
	
}]);

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

app.controller('StopsController', ['$scope', '$anchorScroll', 'StopsService', 'RoutesService', 
	'Changeset', 'MyMap', 'MatchTracker', 'DataHolder', 'Template', '$timeout',
	function($scope, $anchorScroll, stops, routes, changeset, mymap, tracker, data, Template, $timeout) {
	
	mymap.onCandidateClick(function(stop, candidate) {
		$scope.$apply(function() {
			$scope.assignMatched(stop, candidate);
		});
	});

	function showDetails(stop) {
		mymap.showDetails(stop);
		
		if (!$scope.reassignPositionMode) {
			$scope.$apply(function() {
				$anchorScroll('stop' + stop.id);
				$scope.selectedStop = stop;
			});
		}
	}
	
	function hideDetails(stop) {
		mymap.hideDetails(stop);
		
		if (!$scope.reassignPositionMode) {
			$scope.$apply(function() {
				$scope.selectedStop = null;
			});
		}
	}

	$scope.settings = {
		namePattern: '',
		codeTag: 'ref'
	};
	
	stops.list().then(function(response) {
		
		response.data.forEach(function(stop) {
			if (stop.matched) {
				tracker.setMatched(stop, stop.matched);
			}
			mymap.displayStop(stop, showDetails.bind(this, stop), hideDetails.bind(this, stop));
		});
		
		data.trackStops(response.data);
		
		changeset.addBBOX(mymap.getBBOX());
		mymap.fitBounds();
		$scope.stops = response.data;

		routes.list().then(function(response) {
			$scope.routes = response.data.routes;
			$scope.orphantOSMRoutes = response.data.orphants;
			$scope.osmRoutesData = response.data.data;
		});
	});
	
	$scope.selectStop = function(stop) {
		$scope.selectedStop = stop;
		// Exit $scope digest flow
		setTimeout(function() {
			mymap.showDetails(stop);
		}, 0);
	};
	
	$scope.assignMatched = function(stop, candidate) {
		var parent = tracker.gtfs4OSM(candidate);
		if (parent && !confirm('This osm node already assigned to ' + 
				parent.code + " " + parent.name + ". " + "" +
				"Reassign osm obj to new Stop ?")) {
			return;
		}
		stop.matched = candidate;
		tracker.setMatched(stop, stop.matched);
		mymap.redrawStop(stop, $scope.settings.codeTag);
		
		changeset.track(stop);
	};
	
	$scope.editTags = function(stop, k, v) {
		stop.matched.tags[k] = v;
		stop.tagsChanged = true;
		changeset.update(stop);
	};
	
	$scope.stopSetName = function(stop) {
		var name = stop.name;
		if ($scope.settings.namePattern) {
			name = templateName(stop);
		}
		
		stop.matched.tags.name = name;
		stop.tagsChanged = true;
		
		changeset.update(stop);
	};
	
	$scope.stopSetRef = function(stop) {
		stop.matched.tags[$scope.settings.codeTag] = stop.code;
		stop.tagsChanged = true;
		
		changeset.update(stop);
		
		mymap.redrawStop(stop, $scope.settings.codeTag);
	};
	
	$scope.resetMatch = function(stop) {
		changeset.untrack(stop);
		tracker.resetMatched(stop, stop.matched);
		stop.matched = null;
		mymap.redrawStop(stop, $scope.settings.codeTag);
	};
	
	$scope.isStopCodeMatch = function(stop) {
		return stop.matched && stop.code == stop.matched.tags[$scope.settings.codeTag];
	};
	
	$scope.createMatched = function(stop) {
		var name = stop.name;
		if ($scope.settings.namePattern) {
			name = templateName(stop);
		}
		
		var newNode = {
			type: 'node',
			lat: stop.lat,
			lon: stop.lon,
			tags: {
				name: name,
				ref: stop.code,
				highway: 'bus_stop'
			}
		};
		
		stop.candidates.unshift(newNode);
		
		// save to changeset new stop!
		stop.matched = newNode;
		
		// id assigned
		changeset.create(stop);
		tracker.setMatched(stop, stop.matched);
		
		mymap.redrawStop(stop, $scope.settings.codeTag);
	};
	
	$scope.moveMatched = function(stop) {
		$scope.reassignPositionMode = true;
		mymap.reassignPosition(stop, function() {
			changeset.update(stop);
			$scope.$apply(function(){
				$scope.reassignPositionMode = false;
			});
		});
	};
	
	$scope.$watch('activeTab', function(tab) {
		// Update changes
		if (tab == 'changes') {
			$scope.changes = changeset.getChanges();
		}
	});
	
	$scope.getChangesetXML = function() {
		changeset.getChangesetXML().then(function(response) {
			$scope.changesetXML = response.data;
		});
	};
	
	$scope.openJOSM = function() {
		changeset.openInJOSM();
	};
	
	$scope.showTrip = function(trip, route) {
		let stops = [];
		trip.stops.forEach((sid) => {
			stops.push(data.getStop(sid));
		});
		mymap.showTrip(stops);
		$scope.selectRoute(route);
	};
	
	var nameTemplate = null;
	function templateName (stop) {
		if (!nameTemplate) {
			nameTemplate = Template.parse($scope.settings.namePattern);
		}
		return nameTemplate.render(stop);
	}
	
	$scope.$watch('settings.namePattern', function() {
		nameTemplate = null;
	});
	
	$scope.getStopRoutes = function(stop) {
		if ($scope.routes) {
			if (stop['routes']) {
				return stop['routes'];
			}
			stop['routes'] = $scope.routes.filter(r => r.trips.some(t => t.stops.some(s => s == stop.code)));
			return stop['routes'];
		}
		else {
			$timeout(function() {
				$scope.getStopRoutes(stop);
			}, 1000);
		}
	};
	
	$scope.getTripForStop = function(stop, route) {
		return route.trips.find(t => t.stops.some(s => s == stop.code));
	};
	
	$scope.selectStopInRoute = function(stopid, route) {
		var stop = data.getStop(stopid);
		$scope.activeTab = 'stops';
		$scope.selectStop(stop);
		$scope.showTrip($scope.getTripForStop(stop, route), route);
	};
	
	$scope.isStopMatched = function(stopid) {
		return !!data.getStop(stopid).matched;
	};

	$scope.selectRoute = function(route) {
		$scope.selectedRoute = route;
	};

	$scope.selectOSMOrphanedRoute = function(route) {
		$scope.selectedOSMRoute = route;

		if ($scope.osmRoutesData) {
			let stopNodes = [];
			let segments = [];
			$scope.selectedOSMRoute.members.forEach(m => {
				if(m.type == 'node') {
					stopNodes.push($scope.osmRoutesData.nodes[m.ref]);
				}
				if(m.type == 'way') {
					let segmentNodes = [];
					$scope.osmRoutesData.ways[m.ref].nodes.forEach(n => {
						segmentNodes.push(n);
					});
					segments.push(segmentNodes);
				}
			});
			
			mymap.showOSMRoute({
				stopNodes: stopNodes,
				segments: segments
			});
		}

	};

	$scope.showTripStopsComparison = function(gtfsTrip, osmTrip) {
		$scope.tripComparison = {
			gtfsTrip: gtfsTrip,
			osmTrip: osmTrip
		};
	};

	$scope.getOSMStopInTrip = function(osmTrip, gtfsStop, stopIndex) {
		let stopNodes = [];
		osmTrip.members.forEach(m => {
			if(m.type == 'node') {
				let osmNode = $scope.osmRoutesData.nodes[m.ref];
				if (osmNode.tags.highway == 'bus_stop') {
					stopNodes.push(osmNode);
				}
			}
		});

		return stopNodes[stopIndex];
	};

	$scope.setTripStops = function(gtfsTrip, matchedOSMTrip) {
		matchedOSMTrip.members = matchedOSMTrip.members.filter(m => {
			if(m.type == 'node') {
				let osmNode = $scope.osmRoutesData.nodes[m.ref];
				if (osmNode.tags.highway == 'bus_stop'){
					return false;
				}
			}
			return true;
		});

		let segments = matchedOSMTrip.members;
		matchedOSMTrip.members = [];
		gtfsTrip.stops.forEach((s) => {
			if(data.getStop(s).matched) {
				matchedOSMTrip.members.push({
					type: 'node',
					ref: data.getStop(s).matched.id,
					role: ''
				});
			}	
		});
		segments.forEach(s => {
			matchedOSMTrip.members.push(s);
		});

		changeset.updateOSM(matchedOSMTrip);
	};

	$scope.modeAssignOSMTrip = function(trip) {
		$scope.tripAssignSubj = trip;
		
	};
	
	$scope.assignThisTrip = function(osmTrip) {
		$scope.tripAssignSubj.matchedOSMTrip = osmTrip; 
		$scope.tripAssignSubj = null;
	};
	
	$scope.createOSMRelation = function(route, trip) {
		var osmRelation = {
			type: 'relation',
			tags: {
				type: 'route',
				'public_transport:version': '2',
				route: 'bus',
				ref: route.name,
			},
			members: []
		};
		trip.stops.forEach(s => {
			let stop = data.getStop(s);
			if (stop.matched) {
				osmRelation.members.push({
					ref: stop.matched.id,
					type: 'node',
					role: ''
				});
			}
		});
		changeset.createOSM(osmRelation);
		
		trip.matchedOSMTrip = osmRelation;
		trip.exactMatch = true;
	};

	$scope.updateOSMRoute = function(osmroute) {
		changeset.updateOSM(osmroute);
	};

	$scope.closeSelectedStop = function() {
		$scope.selectedStop = null;
	};

	$scope.closeSelectedRoute = function() {
		$scope.selectedRoute = null;
	};

	$scope.closeSelectedOSMRoute = function() {
		$scope.selectedOSMRoute = null;
	};
	
}]);


