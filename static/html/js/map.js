import MatchTracker from '/js/match-tracker.js'

const mymap = L.map('mapid', {
	closePopupOnClick: false
}).setView([51.505, -0.09], 13);

const osmUrl='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
const osmAttrib='Map data © <a href="https://openstreetmap.org">OpenStreetMap</a> contributors';
const mapnik = new L.TileLayer(osmUrl, {maxZoom: 20, maxNativeZoom:18, attribution: osmAttrib});

const sat = new L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
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


const details = [];

let _onCandidateClickCallback = null;
let activeStop = null;
let closePopupTimer = null;

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
			const parent = MatchTracker.gtfs4OSM(c);
			if (parent) {
				details.push(L.polyline([
					[parent.lat, parent.lon], 
					[c.lat, c.lon]], {color: 'green'}).addTo(mymap));
			}
			
			const marker = L.marker([c.lat, c.lon]);
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

let bbox = [180.0, 90.0, -180.0, -90.0];

const CIRCLE_RED = {
	color: 'red',
    fillColor: '#f03',
    fillOpacity: 0.5,
    radius: 5
};

const CIRCLE_GREEN = {
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
	
	const circle = L.circle([stop.lat, stop.lon], stop.matched ? CIRCLE_GREEN : CIRCLE_RED)
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
	mymap.invalidateSize();
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

	route.segments.forEach(() => {
		details.push(L.polyline(coords).addTo(mymap));
	});

	const polyline = L.polyline(coords);
	const decor = addDecor(polyline);
	
	details.push(decor);
	details.push(polyline.addTo(mymap));
}

export default {
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