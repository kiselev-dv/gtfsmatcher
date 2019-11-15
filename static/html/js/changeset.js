const _bbox = [];
const _create = {};
const _update = {};

let _counter = 0;

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
	return fetch('/format-changeset.xml', {
	    method: "POST",
	    body: JSON.stringify(getChanges())
	}).then(response => response.text());
}

async function openInJOSM() {
	return fetch('/format-changeset.xml?save=true', {
	    method: "POST",
	    body: JSON.stringify(getChanges()),
	}).then(response => response.text()).then(text => {

		const urlParams = new URLSearchParams({
			url: 'http://127.0.0.1:9080/format-changeset.xml?load=' + text
		});
		
		const url = new URL('http://127.0.0.1:8111/import?' + urlParams.toString());
		
		return fetch(url);
	});
}

export default {
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