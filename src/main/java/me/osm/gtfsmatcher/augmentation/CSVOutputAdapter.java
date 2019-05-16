package me.osm.gtfsmatcher.augmentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class CSVOutputAdapter implements OutputAdapter {

	private ZipOutputStream outZip;
	private CSVPrinter printer;

	public CSVOutputAdapter(File out) throws FileNotFoundException {
		this.outZip = new ZipOutputStream(new FileOutputStream(out));
	}

	@Override
	public void close() throws Exception {
		this.outZip.close();
	}

	@Override
	public void newEntry(String name, List<String> columns) {
		try {
			ZipEntry outEntry = new ZipEntry(name);
			outZip.putNextEntry(outEntry);
			
			// We don't need to close it, cause it will close the whole zip output stream
			this.printer = new CSVPrinter(
					new PrintWriter(this.outZip), CSVFormat.DEFAULT.withFirstRecordAsHeader());
			
			this.printer.printRecord(columns);
		}
		catch (Exception e) {
			throw new Error(e);
		}
	}

	@Override
	public void closeEntry() {
		try {
			outZip.closeEntry();
		}
		catch (Exception e) {
			throw new Error(e);
		}
	}

	@Override
	public void printRecord(List<String> rowStrings) {
		try {
			this.printer.printRecord(rowStrings);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

}
