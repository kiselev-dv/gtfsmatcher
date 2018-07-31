# gtfsmatcher
Match GTFS data with osm

# Build & run
From the root of the project:

```
# Build
mvn clean compile package

# Then run
java -jar target/gtfsmatcher.jar

```
By default gtfs file should be `data/google_transit.zip` 
you can overwrite data forlder via `-Ddata.dir=some/other/path` parameter

Now open http://localhost:9080/
