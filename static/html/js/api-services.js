import region from '/js/region.js'

const StopsService = {
	list: function() {
		return fetch('/stops/' + region + '/list.json').then(response => response.json());
	}
};

const RoutesService = {
	list: function() {
		return fetch('/routes/' + region + '/list.json').then(response => response.json());
	}
};

const OptionsService = {
	get: function() {
		return fetch('/regions/' + region + '.json').then(response => response.json());
	}
};

export { StopsService, RoutesService, OptionsService };
