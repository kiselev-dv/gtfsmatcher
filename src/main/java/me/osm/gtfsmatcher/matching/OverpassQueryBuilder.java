package me.osm.gtfsmatcher.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Envelope;

public class OverpassQueryBuilder {
	
	private Map<String, Object> headers = new HashMap<String, Object>();
	
	private String out = "out meta;>;out meta qt;";

	private OverpassQueryBuilderUnion union;
	
	public static class OverpassQueryBuilderUnion {
		
		private OverpassQueryBuilderUnion() {
		}
		
		private List<OverpassQueryElement> elements = new ArrayList<OverpassQueryBuilder.OverpassQueryElement>();
		
		public OverpassQueryBuilderUnion addElement(String type) {
			elements.add(new OverpassQueryElement(type));
			return this;
		}
		
		public OverpassQueryBuilderUnion addElement(String type, String key, String value) {
			elements.add(new OverpassQueryElement(type, key, value));
			return this;
		}

		public String build() {
			StringBuilder sb = new StringBuilder();
			sb.append('(');
			
			elements.forEach(e -> {
				sb.append(e.build());
			});
			
			sb.append(')').append(';');
			return sb.toString();
		}

	}

	public static OverpassQueryBuilderUnion union() {
		return new OverpassQueryBuilderUnion();
	}
	
	private static class OverpassQueryElement {
		
		private String type;
		private List<TagFilter> tags;
		
		public OverpassQueryElement(String type) {
			this.type = type;
		}

		public String build() {
			StringBuilder sb = new StringBuilder();
			
			sb.append(this.type);
			
			if (tags != null) {
				tags.forEach(t -> {
					sb.append(t.build());
				});
			}
			
			sb.append(';');
			
			return sb.toString();
		}

		public OverpassQueryElement(String type, String key, String value) {
			this.type = type;
			this.tags = new ArrayList<OverpassQueryBuilder.TagFilter>();
			this.tags.add(new TagFilter(key, value));
		}
	}
	
	private static class TagFilter {

		private String operator = "=";
		private String key;
		private String value;

		public TagFilter(String key, String value) {
			this.key = key;
			this.value = value;
		}
		
		public String build() {
			StringBuilder sb = new StringBuilder();
			
			sb.append('[')
				.append('"').append(this.key).append('"')
				.append(this.operator)
				.append('"').append(this.value).append('"')
			.append(']');
			
			return sb.toString();
		}
		
	}
	
	private OverpassQueryBuilder() {
		addHeader("out", "json");
		addHeader("timeout", 25);
	}

	public static String bboxToString(Envelope bbox) {
		return bbox.getMinY() + "," + bbox.getMinX() + "," + bbox.getMaxY() + "," + bbox.getMaxX();
	}
	
	public static OverpassQueryBuilder builder() {
		return new OverpassQueryBuilder();
	}
	
	public OverpassQueryBuilder addHeader(String header, Object value) {
		headers.put(header, value);
		return this;
	}
	
	public OverpassQueryBuilder timeout(int timeout) {
		headers.put("timeout", timeout);
		return this;
	}
	
	public OverpassQueryBuilder out(String out) {
		this.out = out;
		return this;
	}
	
	public OverpassQueryBuilder bbox(Envelope bbox) {
		headers.put("bbox", bboxToString(bbox));
		return this;
	}
	
	public OverpassQueryBuilder addUnion(OverpassQueryBuilderUnion union) {
		this.union = union;
		return this;
	}

	public String build() {
		StringBuilder result = new StringBuilder();
		
		headers.forEach((key, value) -> {
			result.append('[').append(key).append(':').append(value).append(']');
		});
		result.append(';');
		
		result.append(this.union.build());
		
		result.append(out);
		
		return result.toString();
	}


}
