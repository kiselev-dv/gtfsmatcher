package me.osm.gtfsmatcher.augmentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.StringUtils;

import me.osm.gtfsmatcher.AugmentOptions;
import me.osm.gtfsmatcher.BatchOptions;

public class AugmentBatch {

	public AugmentBatch(BatchOptions options) {
		
		File batchFile = new File(options.getBatch());
		try(CSVParser csvParser = new CSVParser(
				new InputStreamReader(new FileInputStream(batchFile)), 
				CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
			
			csvParser.forEach(row -> {
				String url = row.get("gtfs_url");
				String name = row.get("name");
				
				String fileName = StringUtils.replaceChars(name, " ", "_");
				try {
					URL gtfsURL = new URL(url);
					
					System.out.println("Download gtfs from " + url);
					
					ReadableByteChannel rbc = Channels.newChannel(gtfsURL.openStream());
					File file = File.createTempFile(fileName, ".gtfs");
					file.deleteOnExit();
					
					FileOutputStream fos = new FileOutputStream(file);
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					fos.close();
					
					AugmentOptions augOptions = new AugmentOptions();
					
					File out = new File(fileName + ".sqlite");
					if (StringUtils.stripToNull(options.getOutDir()) != null) {
						out = new File(options.getOutDir(), fileName + ".sqlite");
					}
					
					augOptions.setSqlite(true);
					augOptions.setIn(file.getPath());
					augOptions.setOut(out.getPath());
					
					System.out.println("Run augmentation for " + fileName + " saving to " + out.getPath());
					new GTFSAugment(augOptions);
					
					System.out.println("Done " + fileName);
					
				} catch (Exception e) {
					throw new Error(e);
				}
			});
			
		} catch (Exception e) {
			throw new Error(e);
		}
		
	}

}
