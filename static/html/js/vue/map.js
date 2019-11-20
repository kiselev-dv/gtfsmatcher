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

let bbox = [180.0, 90.0, -180.0, -90.0];

class Service {
	
	createMap(container) {
		this.map = L.map(container, {
			closePopupOnClick: false
		}).setView([51.505, -0.09], 13);
		
		mapnik.addTo(this.map);
		L.control.layers({
			"Mapnik": mapnik,
			"Satellite": sat
		}, {}).addTo(this.map);
	}
	
	updateLocations(gtfsLocations) {
		this.gtfsLocations = gtfsLocations;
		
		const self = this;
		gtfsLocations.forEach(stop => {
			bbox = [
				Math.min(bbox[0], stop.lon), 
				Math.min(bbox[1], stop.lat), 
				Math.max(bbox[2], stop.lon), 
				Math.max(bbox[3], stop.lat)];
		
			const popupOptions = {
				minWidth: 300,
				offset: [0, -20], 
				autoClose: false
			};
			const circle = L.circle([stop.lat, stop.lon], stop.matched ? CIRCLE_GREEN : CIRCLE_RED)
				.bindPopup('<stop-popup></stop-popup>', popupOptions).addTo(self.map);
			
			circle.on('click', createStopClickHandler(stop, self));
		});
		
		this.fitBounds();
	}
	
	fitBounds() {
		this.map.fitBounds(L.latLngBounds(L.latLng(bbox[1], bbox[0]), L.latLng(bbox[3], bbox[2])));
	}
}

function createStopClickHandler(stop, service) {
	function handler(stop) {
		new Vue({
			el: 'stop-popup',
			data: {
				stop: stop
			},
			methods: {
				alert: function() {
					alert('Click on stop popup');
				}
			},
			template: '<div style="width: 100%;"><h4>{{stop.name}}</h4><button @click="alert">Test</button></div>'
		}).$mount();
	}
	return handler.bind(service, stop);
}

const service = new Service();

const element = new Vue({
	template: '<div style="width: 100%; height: 100%;" id="map"></div>',
	data: function() {
		return {
			gtfsLocations: null,
			osmLocations: null,
			routes: null
		};
	},
	watch: {
		gtfsLocations: function(val) {
			service.updateLocations(val);
		} 
	},
	mounted: function() {
		service.createMap(this.$el);
	}
});

export default element;