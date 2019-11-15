import {StopsService, RoutesService, OptionsService} from '/js/api-services.js'
import MatchTracker from '/js/match-tracker.js'
import DataHolder from '/js/data-holder.js'

import changeset from '/js/changeset.js'
import mymap from '/js/map.js';

import parseTemplate from '/js/template.js'

angular.module('gtfsmatch', []).controller('StopsController', ['$scope', '$anchorScroll', '$timeout',
	function($scope, $anchorScroll, $timeout) {
	
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
	
	OptionsService.get().then(options => {
		$scope.settings = {
			namePattern: options.nameTemplate || '',
			codeTag: options.gtfsRefTag || 'ref'
		};
	});
	
	StopsService.list().then(function(stops) {
		
		stops.gtfs.forEach(function(stop) {
			if (stop.matched) {
				MatchTracker.setMatched(stop, stop.matched);
			}
			mymap.displayStop(stop, showDetails.bind(this, stop), hideDetails.bind(this, stop));
		});
		
		DataHolder.trackStops(stops.gtfs);
		
		changeset.addBBOX(mymap.getBBOX());
		mymap.fitBounds();
		$scope.stops = stops.gtfs;
		$scope.orphantStops = stops.orphants;

		RoutesService.list().then(function(routes) {
			$scope.routes = routes.routes;
			$scope.orphantOSMRoutes = routes.orphants;
			$scope.osmRoutesData = routes.data;
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
		var parent = MatchTracker.gtfs4OSM(candidate);
		if (parent && !confirm('This osm node already assigned to ' + 
				parent.code + " " + parent.name + ". " + "" +
				"Reassign osm obj to new Stop ?")) {
			return;
		}
		stop.matched = candidate;
		MatchTracker.setMatched(stop, stop.matched);
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
		MatchTracker.resetMatched(stop, stop.matched);
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
		MatchTracker.setMatched(stop, stop.matched);
		
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
		changeset.getChangesetXML().then(text => {
			$scope.changesetXML = text;
		});
	};
	
	$scope.openJOSM = function() {
		changeset.openInJOSM()
			.then(response => {
				if (response.status == 200) {
					console.log('Opened');
				}
			})
			.catch(err => {
				alert('Failed to open in JOSM');
			});
	};
	
	$scope.showTrip = function(trip, route) {
		let stops = [];
		trip.stops.forEach((sid) => {
			stops.push(DataHolder.getStop(sid));
		});
		mymap.showTrip(stops);
		$scope.selectRoute(route);
	};
	
	var nameTemplate = null;
	function templateName (stop) {
		if (!nameTemplate) {
			nameTemplate = parseTemplate($scope.settings.namePattern);
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
		var stop = DataHolder.getStop(stopid);
		$scope.activeTab = 'stops';
		$scope.selectStop(stop);
		$scope.showTrip($scope.getTripForStop(stop, route), route);
	};
	
	$scope.isStopMatched = function(stopid) {
		return !!DataHolder.getStop(stopid).matched;
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
			if(DataHolder.getStop(s).matched) {
				matchedOSMTrip.members.push({
					type: 'node',
					ref: DataHolder.getStop(s).matched.id,
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
			let stop = DataHolder.getStop(s);
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


