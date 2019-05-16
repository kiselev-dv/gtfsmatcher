package me.osm.gtfsmatcher.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restexpress.Request;
import org.restexpress.Response;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.jodah.expiringmap.ExpiringMap;

public class ChangesetAPI {

	private static final ExpiringMap<String, String> cache = ExpiringMap.builder()
			.expiration(30, TimeUnit.SECONDS).maxSize(100).build();
	
	public String read(Request req, Response res) throws IOException, ParserConfigurationException, TransformerException {
		if(req.getHeader("load") != null) {
			return cache.get(req.getHeader("load"));
		}
		
		String data = req.getHeader("data");
		JSONObject changes = new JSONObject(data);
		
		return encodeChanges(changes);
	}
	
	public String create(Request req, Response res) throws IOException, ParserConfigurationException, TransformerException {
		res.setContentType("text/xml; charset=UTF-8");

		ByteBuffer bb = req.getBodyAsByteBuffer();
		byte[] bytes = new byte[bb.remaining()];
		bb.get(bytes);
		
		String body = new String(bytes, StandardCharsets.UTF_8);
		JSONObject changes = new JSONObject(body);
		
		String result = encodeChanges(changes);
		if ("true".equals(req.getHeader("save"))) {
			String uuid = UUID.randomUUID().toString();
			cache.put(uuid, result);
			return uuid;
		}

		return result;
	}

	private String encodeChanges(JSONObject changes) throws ParserConfigurationException,
			TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			
			Document doc = docBuilder.newDocument();
			Element osm = doc.createElement("osm");
			doc.appendChild(osm);
			
			osm.setAttribute("version", "0.6");
			osm.setAttribute("generator", "gtfs-conflate v0.0.1");
			
			setBBOX(changes, doc, osm);
			
			JSONArray create = changes.optJSONArray("create");
			if(create != null) {
				addNodes(doc, osm, create, false);
			}
			JSONArray update = changes.optJSONArray("update");
			if(update != null) {
				addNodes(doc, osm, update, true);
			}
			
			DOMSource source = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(source, result);
			
			return writer.toString();
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private void addNodes(Document doc, Element osm, JSONArray create, boolean update) {
		for(int i = 0; i < create.length(); i++) {
			JSONObject obj = create.getJSONObject(i);
			Element element = doc.createElement(obj.getString("type"));
			
			element.setAttribute("id", obj.get("id").toString());
			element.setAttribute("visible", "true");

			if("node".equals(obj.getString("type"))) {
				element.setAttribute("lat", obj.get("lat").toString());
				element.setAttribute("lon", obj.get("lon").toString());
			}
			
			if(StringUtils.stripToNull(obj.optString("timestamp")) != null) {
				element.setAttribute("timestamp", obj.optString("timestamp"));
			}
			
			if(StringUtils.stripToNull(obj.optString("user")) != null) {
				element.setAttribute("user", obj.optString("user"));
			}
			
			if(StringUtils.stripToNull(obj.optString("version")) != null) {
				element.setAttribute("version", obj.optString("version"));
			}

			if("relation".equals(obj.getString("type"))) {
				JSONArray membersArr = obj.getJSONArray("members");
				membersArr.forEach(m -> {
					JSONObject member = (JSONObject)m;

					Element memberXML = doc.createElement("member");
					memberXML.setAttribute("type", member.getString("type"));
					memberXML.setAttribute("ref", member.get("ref").toString());
					memberXML.setAttribute("role", StringUtils.stripToEmpty(member.optString("role")));

					element.appendChild(memberXML);
				});
			}
			
			osm.appendChild(element);
			
			Map<String, Object> tags = obj.getJSONObject("tags").toMap();
			tags.entrySet().forEach(kv -> {
				Element tag = doc.createElement("tag");
				tag.setAttribute("k", kv.getKey().toString());
				tag.setAttribute("v", kv.getValue().toString());
				element.appendChild(tag);
			});
			
			if (update) {
				element.setAttribute("action", "modify");
			}
		}
	}

	private void setBBOX(JSONObject changes, Document doc, Element osm) {
		JSONArray bbox = changes.optJSONArray("bboxes").getJSONArray(0);
		
		// <bounds minlat='51.5076478723889' 
		//         minlon='-0.127989783553507' 
		//         maxlat='51.5077445145483' 
		//         maxlon='-0.127774884645096' 
		//         origin='OpenStreetMap server' />
		
		Element bounds = doc.createElement("bounds");
		bounds.setAttribute("minlon", new Double(bbox.getDouble(0)).toString());
		bounds.setAttribute("minlat", new Double(bbox.getDouble(1)).toString());
		bounds.setAttribute("maxlon", new Double(bbox.getDouble(2)).toString());
		bounds.setAttribute("maxlat", new Double(bbox.getDouble(3)).toString());
		bounds.setAttribute("origin", "OpenStreetMap server");
		
		osm.appendChild(bounds);
	}

	private String getXmlHeader() {
		return "<?xml version='1.0' encoding='UTF-8'?>";
	}

}
