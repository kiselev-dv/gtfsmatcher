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