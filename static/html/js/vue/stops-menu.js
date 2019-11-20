const stopTemplate = `<div v-on:click="clicked">
	{{name}}
</div>`;

Vue.component('stop', {
	props: ['osmobj', 'gtfsobj'],
	computed: {
		name: function() {
			if (this.osmobj) {
				return this.osmobj.tags.name;
			}
			else if(this.gtfsobj) {
				return this.gtfsobj.name;
			}
		}
	},
	methods: {
		clicked: function() {
			this.$emit('clicked', this);
		}
	},
	template: stopTemplate
});

const stopsTemplate = 
`<div>
	<div>Orphants: {{orphantsCount}}, Matched: {{matchedCount}}, Unmatched: {{unmatchedCount}}</div>
	<md-tabs>
		<md-tab id="stops-unmatched" md-label="Unmatched" >
			<div>
				<stop v-for="stop in unmatched" @clicked="stopClick" v-bind:gtfsobj="stop">{{stop.name}}</stop>
			</div>	
		</md-tab>

		<md-tab id="stops-orphant" md-label="Orphants">
			<div>
				<stop @clicked="stopClick" v-bind:osmobj="stop" v-for="stop in orphants">{{stop.tags.name}}</stop>
			</div>	
		</md-tab>
		
		<md-tab id="stops-matched" md-label="Matched">
			<div>
				<stop @clicked="stopClick" v-bind:gtfsobj="stop" v-for="stop in matched">{{stop.name}}</stop>
			</div>	
		</md-tab>

	</md-tabs>
</div>
`;


const menu = new Vue({
	data: function() { 
		return {
			orphants: [],
			gtfs: []
		}
	},
	computed: {
		orphantsCount: function() {
			return this.orphants.length;
		},
		matched: function() {
			return this.gtfs.filter(s => s.matched);
		},
		matchedCount: function() {
			return this.gtfs.filter(s => s.matched).length;
		},
		unmatched: function() {
			return this.gtfs.filter(s => !s.matched);
		},
		unmatchedCount: function() {
			return this.gtfs.filter(s => !s.matched).length;
		}
	},
	methods: {
		stopClick: function(stop) {
			this.$emit('stop-clicked', stop);
		}
	},
	template: stopsTemplate
});

export default menu;