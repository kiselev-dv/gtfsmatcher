package me.osm.gtfsmatcher;

import org.restexpress.Flags;
import org.restexpress.RestExpress;

import io.netty.handler.codec.http.HttpMethod;

public class GTFSMatcher {
	
	private RestExpress server;

	public GTFSMatcher() {
		createServer();
	}

	private void createServer() {
		server = new RestExpress();
		
		server.setPort(9080);
		
		server.uri("/stops/{region}/list.{format}", new StopAPI())
			.method(HttpMethod.GET)
			.name("feature")
			.flag(Flags.Auth.PUBLIC_ROUTE);
		
		server.uri("/routes/{region}/list.{format}", new RoutesAPI())
			.method(HttpMethod.GET)
			.name("feature")
			.flag(Flags.Auth.PUBLIC_ROUTE);
		
		server.uri("/format-changeset.xml", new ChangesetAPI())
			.flag(Flags.Auth.PUBLIC_ROUTE)
			.method(HttpMethod.POST).method(HttpMethod.GET).noSerialization();
		
		server.uri("/{filename}", new StaticAPI())
			.alias("/")
			.flag(Flags.Auth.PUBLIC_ROUTE)
			.method(HttpMethod.GET).noSerialization();
		
		server.bind(9080);
		server.awaitShutdown();
	}
	
	public static void main(String[] args) {
		new GTFSMatcher();
	}

}
