# gtfsmatcher
Match GTFS data with osm

# Build & run
From the root of the project:

```
# Build
mvn compile package
mvn dependency:copy-dependencies

# Then run
java -cp target/gtfsmatcher-0.0.1-SNAPSHOT.jar:target/dependency/* me.osm.gtfsmatcher.GTFSMatcher

```
By default gtfs file should be `data/google_transit.zip` 
you can overwrite data forlder via `-Ddata.dir=some/other/path` parameter

Now open http://localhost:9080/
