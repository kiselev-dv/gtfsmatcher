package me.osm.gtfsmatcher;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription="Start server")
public class ServerOptions {
	
	@Parameter(names= {"--port", "-p"})
	private int port = 9080; 
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
