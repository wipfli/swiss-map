package com.onthegomap.planetiler.examples;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.util.ZoomFunction;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

public class QRank implements Profile {
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
      "maxzoom", 14,
      "tile_warning_size_mb", 100
    ));
    String area = args.getString("area", "geofabrik area to download", "monaco");
    Planetiler.create(args)
      .setProfile(new QRank())
      .addOsmSource("osm",
        Path.of("data", "sources", area + ".osm.pbf"),
        "planet".equalsIgnoreCase(area) ? "aws:latest" : ("geofabrik:" + area)
      )
      .overwriteOutput("mbtiles", Path.of("data", "qrank.mbtiles"))
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
    if (sourceFeature.isPoint() && sourceFeature.hasTag("wikidata") && sourceFeature.hasTag("name") && (sourceFeature.hasTag("place") || sourceFeature.hasTag("natural") || sourceFeature.hasTag("waterway", "waterfall")))
    {
      var feature = features.point("qrank");
      feature
        .setZoomRange(0, 14)
        .setSortKey(-getQRank(sourceFeature.getTag("wikidata")))
        .setPointLabelGridSizeAndLimit(
          12, // only limit at z_ and below
          128, // break the tile up into _x_ px squares
          5 // any only keep the _ nodes with lowest sort-key in each _px square
        )
        .setBufferPixelOverrides(ZoomFunction.maxZoom(12, 128))
        .setAttr("@qrank", getQRank(sourceFeature.getTag("wikidata")));
      for (var entry : sourceFeature.tags().entrySet()) {
        feature.setAttr(entry.getKey(), entry.getValue());
      }
    }
  }

  @Override
  public String name() {
    return "osm qrank";
  }

  @Override
  public String attribution() {
    return """
      <a href="https://www.openstreetmap.org/copyright" target="_blank">&copy; OpenStreetMap contributors</a>
      """.trim();
  }
}
