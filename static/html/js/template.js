function parseTemplate(templateString) {
	const template = {};
	
	const replacer = function(varName, regexpArgs, subj) {
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

	let i = 0;
	template.pattern = templateString.replace(/\$([\w]+).re\((.*\))/g, function(match, varName, regexp) {
		var replacementKey = 'replace' + (i++);
		
		template[replacementKey] = replacer.bind(template, varName, regexp);
		
		return replacementKey;
	});
	
	template.pattern = template.pattern.replace(/\$([\w]+)/g, function(match, varName) {
		const replacementKey = 'replace' + (i++);
		template[replacementKey] = replacer.bind(template, varName, null);
		return replacementKey;
	});
	
	template.render = function(obj) {
		const self = this;
		return this.pattern.replace(/replace[\d]+/g, function(match) {
			return self[match](obj);
		});
	}
	
	return template;
}
	
export default parseTemplate;