package me.osm.gtfsmatcher;

import java.util.Arrays;

import com.beust.jcommander.JCommander;

import me.osm.gtfsmatcher.augmentation.AugmentBatch;
import me.osm.gtfsmatcher.augmentation.GTFSAugment;

public class GTFSMatcher {

	public static void main(String[] args) {
		AugmentOptions aug = new AugmentOptions();
		ServerOptions serve = new ServerOptions();
		BatchOptions batch = new BatchOptions();
		
		JCommander jc = JCommander.newBuilder()
				.programName("gtfsmatcher")
				.addCommand("augment", aug)
				.addCommand("serve", serve)
				.addCommand("batch", batch)
				.build();
		
		if(Arrays.stream(args).anyMatch(a -> "--help".equals(a) || "-h".equals(a))) {
			jc.usage();
			System.exit(0);
		}
		
		try {
			jc.parse(args);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			jc.usage();
			
			System.exit(1);
		}
		
		String parsedCommand = jc.getParsedCommand();
		if (parsedCommand == null || parsedCommand.equals("serve")) {
			new GTFSMatcherServer(serve);
		}
		else if (parsedCommand.equals("augment")) {
			new GTFSAugment(aug);
		}
		else if (parsedCommand.equals("batch")) {
			new AugmentBatch(batch);
		}
		
	}
	
}
