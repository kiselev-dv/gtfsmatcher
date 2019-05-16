package me.osm.gtfsmatcher;

import org.restexpress.Flags;
import org.restexpress.RestExpress;

import io.netty.handler.codec.http.HttpMethod;
import me.osm.gtfsmatcher.rest.ChangesetAPI;
import me.osm.gtfsmatcher.rest.RoutesAPI;
import me.osm.gtfsmatcher.rest.StaticAPI;
import me.osm.gtfsmatcher.rest.StopAPI;

public class GTFSMatcherServer {
	
	private RestExpress server;

	public GTFSMatcherServer(ServerOptions serve) {
		createServer(serve);
	}

	private void createServer(ServerOptions serve) {
		server = new RestExpress();
		
		server.setPort(9080);
		server.setMaxContentSize(524288000);
		
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
		
		server.bind(serve.getPort());
		server.awaitShutdown();
	}
	
	public static void main(String[] args) {
		new GTFSMatcherServer(new ServerOptions());
	}

}
