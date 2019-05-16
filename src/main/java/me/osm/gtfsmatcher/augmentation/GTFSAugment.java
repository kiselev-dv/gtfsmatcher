package me.osm.gtfsmatcher.augmentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import me.osm.gtfsmatcher.AugmentOptions;
import me.osm.gtfsmatcher.matching.RoutesMatcher;
import me.osm.gtfsmatcher.matching.StopMatcher;
import me.osm.gtfsmatcher.model.GTFSRoute;
import me.osm.gtfsmatcher.model.GTFSStop;
import me.osm.gtfsmatcher.model.GTFSTrip;
import me.osm.gtfsmatcher.model.OSMObject;
import me.osm.gtfsmatcher.model.RoutesData;

public class GTFSAugment {

	public GTFSAugment(AugmentOptions aug) {
		run(new File(aug.getIn()), new File(aug.getOut()), aug);
	}

	private void run(File gtfs, File out, AugmentOptions options) {
		
		try {
			StopMatcher matcher = new StopMatcher();
			List<GTFSStop> matches = matcher.matchStops(gtfs);
			
			RoutesMatcher routesMatcher = new RoutesMatcher();
			
			String osmData = options.getOsmData();
			File osmDataFile = osmData == null ? null : new File(osmData);
			
			RoutesData matchedRoutes = routesMatcher.matchRoutes(gtfs, options.isTripsFirst(), osmDataFile);
			
			Map<String, GTFSStop> gtfsStopCode2Stop = new HashMap<>();
			matches.forEach(s -> gtfsStopCode2Stop.put(s.getId(), s));
			
			Map<String, OSMObject> gtfsTripId2MatchedOSMTrip = new HashMap<>();
			
			for(GTFSRoute gr : matchedRoutes.getRoutes()) {
				for(GTFSTrip gt : gr.getTrips()) {
					OSMObject osmTrip = gt.getMatchedOSMTrip();
					if (osmTrip != null) {
						for(String gtfsTripId : gt.getGtfsTripsIds()) {
							gtfsTripId2MatchedOSMTrip.put(gtfsTripId, osmTrip);
						}
					}
				}
			} 
			
			System.out.println(gtfsTripId2MatchedOSMTrip.keySet().size() + " gtfs trips matched");
			
			try(ZipFile zipFile = new ZipFile(gtfs); 
				OutputAdapter outAdapter = getOutputAdapter(out, options.isSqlite())) {
				
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				
				while(entries.hasMoreElements()){
					ZipEntry entry = entries.nextElement();
					InputStream stream = zipFile.getInputStream(entry);

					try(CSVParser csvParser = new CSVParser(
							new InputStreamReader(stream), 
							CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
						
						List<String> columns = new ArrayList<>(csvParser.getHeaderMap().keySet());
						
						if("stops.txt".equals(entry.getName())) {
							columns.sort(Comparator.comparingInt(h -> csvParser.getHeaderMap().get(h)));
							columns.add("osm_type");
							columns.add("osm_id");
						}
						if("trips.txt".equals(entry.getName())) {
							columns.sort(Comparator.comparingInt(h -> csvParser.getHeaderMap().get(h)));
							columns.add("osm_id");
						}

						outAdapter.newEntry(entry.getName(), columns);

						System.out.println("Process " + entry.getName());
						if("stops.txt".equals(entry.getName())) { 
							augStops(gtfsStopCode2Stop, csvParser, outAdapter);
						} 
						else if("trips.txt".equals(entry.getName())) {
							augTrips(matchedRoutes, gtfsTripId2MatchedOSMTrip, csvParser, outAdapter);
						}
						else {
							echo(csvParser, outAdapter);
						}
						
						outAdapter.closeEntry();
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void augTrips(RoutesData matchedRoutes, 
			Map<String, OSMObject> gtfsTripId2MatchedOSMTrip, CSVParser csvParser, OutputAdapter out) {
		
		for(CSVRecord rec : csvParser) {
			List<String> row = new ArrayList<>();
			rec.forEach(row::add);

			OSMObject osmTrip = gtfsTripId2MatchedOSMTrip.get(rec.get("trip_id"));
			if (osmTrip != null) {
				row.add(String.valueOf(osmTrip.getId()));
			}
			else {
				row.add(null);
			}
			
			out.printRecord(row);
		} 
	}

	private OutputAdapter getOutputAdapter(File out, boolean sqlite) throws FileNotFoundException {
		return sqlite ? new SQLiteOutputAdapter(out) : new CSVOutputAdapter(out);
	}

	private void echo(CSVParser csvParser, OutputAdapter out) {
		csvParser.forEach(csvRow -> {
			List<String> row = new ArrayList<>();
			csvRow.forEach(row::add);
			out.printRecord(row);
		});
	}

	private void augStops(Map<String, GTFSStop> byid, CSVParser csvParser, OutputAdapter outAdapter) throws IOException {
		int[] matched = new int[1];
		csvParser.forEach(row -> {
			GTFSStop matchedStop = byid.get(row.get("stop_id"));
			OSMObject matchedOSM = matchedStop.getMatched();
			if (matchedOSM != null) {
				matched[0]++;
			}
			writeStop(outAdapter, row, matchedOSM);
		});
		System.out.println(matched[0] + " stops matched");
	}

	private void writeStop(OutputAdapter outAdapter, CSVRecord row, OSMObject osmObject) {
		List<String> rowStrings = new ArrayList<>();
		row.iterator().forEachRemaining(rowStrings::add);
		if (osmObject != null) {
			rowStrings.add(osmObject.getType());
			rowStrings.add(String.valueOf(osmObject.getId()));
		}
		else {
			rowStrings.add(null);
			rowStrings.add(null);
		}
		outAdapter.printRecord(rowStrings);
	}

}
