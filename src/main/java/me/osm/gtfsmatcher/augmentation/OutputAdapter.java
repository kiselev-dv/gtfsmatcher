package me.osm.gtfsmatcher.augmentation;

import java.util.List;

import org.apache.commons.csv.CSVRecord;

public interface OutputAdapter extends AutoCloseable {

	void newEntry(String name, List<String> columns);

	void closeEntry();

	void printRecord(List<String> rowStrings);

}
