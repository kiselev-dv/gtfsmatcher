package me.osm.gtfsmatcher;

import com.beust.jcommander.Parameter;

public class BatchOptions {
	
	@Parameter
	private String batch;
	
	@Parameter(names="--output-dir")
	private String outDir;

	public String getBatch() {
		return batch;
	}

	public void setBatch(String batch) {
		this.batch = batch;
	}

	public String getOutDir() {
		return outDir;
	}

	public void setOutDir(String outDir) {
		this.outDir = outDir;
	}
	
}
