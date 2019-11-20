import {StopsService, RoutesService, OptionsService} from '/js/api-services.js'
import stopsMenu from '/js/vue/stops-menu.js'
import map from '/js/vue/map.js'

StopsService.list().then(stops => {
	stopsMenu.orphants = stops.orphants;
	stopsMenu.gtfs = stops.gtfs;
	
	map.gtfsLocations = stops.gtfs;
});

const appTemplate = `
<md-app style="height: 100%;">
	<md-app-drawer md-permanent="full" >
		<stops-menu @stop-clicked="menuStopClicked"></stops-menu>
	</md-app-drawer>
	<md-app-content>
		<map></map>
	</md-app-content>
</md-app>
`;

const app = new Vue({
	el: '#editor-app',
	template: appTemplate,
	mounted: function() {
		stopsMenu.$mount('stops-menu');
		map.$mount('map');
	},
	methods: {
		menuStopClicked: function(stop) {
			alert('Stop clicked');
			//map.show(stop);
		}
	}
});