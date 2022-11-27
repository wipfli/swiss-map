# swiss-map
A simple map of Switzerland (and the planet).

Uses: 
* [Planetiler](https://github.com/onthegomap/planetiler) to generate vector tiles.
* [Shortbread](https://shortbread.geofabrik.de/) as a vector tile schema.
* [PMTiles](https://github.com/protomaps/PMTiles) for hosting and serving tiles on Cloudflare.
* [MapLibre GL JS](https://github.com/maplibre/maplibre-gl-js) for rendering in the web browser.
* [qrank](https://qrank.wmcloud.org) for finding `place=*` label importance

## tile hosting

The tiles are hosted on the Cloudflare R2 storage service. A Cloudflare Worker extracts the data from a `shortbread-planet.pmtiles` file stored on R2 and serves tiles at a `{z}/{x}/{y}.pbf` endpoint. For instructions how to host PMTiles on Cloudflare, see https://protomaps.com/docs/cdn/cloudflare.

## demo

https://wipfli.github.io/swiss-map/

## name

The name of this map style is "Swiss Map" because it takes some inspiration from Swiss mapping conventions and has the goal to look good for areas in Switzerland, despite the global coverage of the map.

## qrank

[QRank](https://qrank.wmcloud.org) is a system which tells you how important a wikimedia entry is by counting how oftem the associated wikipedia pages were accessed in the last year. Many OpenStreetMap entries have a `wikimedia=Q*` tag. We use the wikimedia tag when a point has `place=*` and look up the qrank of the place. This is then used to figure out which place labels to display.

### steps

1. download [Planetiler](https://github.com/onthegomap/planetiler)
2. `cd planetiler/planetiler-examples`
3. download qrank.csv file and unzip it, put it in the same folder as the `OsmQaTiles.java` example
4. fill the `OsmQaTiles.java` example with something like this:
```java
package com.onthegomap.planetiler.examples;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.osm.OsmSourceFeature;
import com.onthegomap.planetiler.util.ZoomFunction;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

public class OsmQaTiles implements Profile {
  static HashMap<String, String> qranks;

  public static void main(String[] args) throws Exception {
    run(Arguments.fromArgsOrConfigFile(args));
  }

  static void run(Arguments inArgs) throws Exception {
    qranks = new HashMap<String, String>();

    System.out.println("Start reading qrank.csv...");
    BufferedReader reader;
    reader = new BufferedReader(new FileReader("src/main/java/com/onthegomap/planetiler/examples/qrank.csv"));
    String line = reader.readLine();
    while (line != null) {
      String[] parts = line.split(",");
      qranks.put(parts[0].trim(), parts[1].trim());
      line = reader.readLine();
    }
    reader.close();

    var args = inArgs.orElse(Arguments.of(
      "minzoom", 0,
      "maxzoom", 10,
      "tile_warning_size_mb", 100
    ));
    String area = args.getString("area", "geofabrik area to download", "monaco");
    Planetiler.create(args)
      .setProfile(new OsmQaTiles())
      .addOsmSource("osm",
        Path.of("data", "sources", area + ".osm.pbf"),
        "planet".equalsIgnoreCase(area) ? "aws:latest" : ("geofabrik:" + area)
      )
      .overwriteOutput("mbtiles", Path.of("data", "qa.mbtiles"))
      .run();
  }

  static int getQRank(Object wikidata) {
    String qrank = qranks.get(wikidata.toString());
    if (qrank == null) {
      return 0;
    }
    else {
      return Integer.parseInt(qrank);
    }
  }

  @Override
  public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {
    if (!sourceFeature.tags().isEmpty() && sourceFeature instanceof OsmSourceFeature osmFeature) {
      var feature = sourceFeature.isPoint() && sourceFeature.hasTag("wikidata") && sourceFeature.hasTag("place") && sourceFeature.hasTag("name")? features.point("osm") : null;
      if (feature != null) {
        feature
          .setZoomRange(0, 10)
          .setSortKey(-getQRank(sourceFeature.getTag("wikidata")))
          .setPointLabelGridSizeAndLimit(
            12, // only limit at z12 and below
            32, // break the tile up into 32x32 px squares
            4 // any only keep the 4 nodes with lowest sort-key in each 32px square
          )
          .setBufferPixelOverrides(ZoomFunction.maxZoom(12, 32));
        feature.setAttr("name", sourceFeature.getTag("name"));
        feature.setAttr("@qrank", getQRank(sourceFeature.getTag("wikidata")));
        feature.setAttr("wikidata", sourceFeature.getTag("wikidata"));
      }
    }
  }

  @Override
  public String name() {
    return "osm qa";
  }

  @Override
  public String attribution() {
    return """
      <a href="https://www.openstreetmap.org/copyright" target="_blank">&copy; OpenStreetMap contributors</a>
      """.trim();
  }
}
```
5. build and run planetiler
6. convert mbtiles to pmtiles, deploy to cloudflare
7. yeah sorry if that is a bad description, just want to share the main planetiler snippet...
