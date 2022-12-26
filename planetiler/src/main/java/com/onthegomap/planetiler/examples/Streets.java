package com.onthegomap.planetiler.examples;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.osm.OsmElement;
import com.onthegomap.planetiler.reader.osm.OsmRelationInfo;
import java.nio.file.Path;
import java.util.List;


public class Streets implements Profile {

  private record RouteRelationInfo(
    @Override long id,
    String name, String ref, String route, String network
  ) implements OsmRelationInfo {}

  @Override
  public List<OsmRelationInfo> preprocessOsmRelation(OsmElement.Relation relation) {
    return null;
  }

  @Override
  public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {

    // wood layer
    if (sourceFeature.canBePolygon() && (
        sourceFeature.hasTag("landuse", "forest") ||
        sourceFeature.hasTag("natural", "wood")
    )) {
      features.polygon("wood")
        .setMinZoom(7);
    }

    // residential layer
    if (sourceFeature.canBePolygon() && (
      sourceFeature.hasTag("amenity",
      "grave_yard") ||
      sourceFeature.hasTag("landuse",
        "cemetery",
        "commercial",
        "farmyard",
        "industrial",
        "residential",
        "retail") ||
      sourceFeature.hasTag("leisure", 
      "park")
    )) {
      features.polygon("residential")
        .setMinZoom(10);
    }

    // building layer
    if (sourceFeature.canBePolygon() && sourceFeature.hasTag("building") && !sourceFeature.hasTag("building", "no")) {
      features.polygon("building")
        .setMinZoom(14);
    }

    // boundary layer
    if (sourceFeature.canBeLine() && (
      sourceFeature.hasTag("boundary", "administrative") &&
      sourceFeature.hasTag("admin_level", "2")
    )) {
      features.line("boundary")
        .setMinPixelSize(0)
        .setMinZoom(0);
    }
  }

  @Override
  public List<VectorTile.Feature> postProcessLayerFeatures(String layer, int zoom,
    List<VectorTile.Feature> items) {

    if ("wood".equals(layer)) {
      try {
        return FeatureMerge.mergeOverlappingPolygons(items, 4);
      }
      catch (GeometryException e) {
        return null;
      }
    }

    if ("residential".equals(layer)) {
      try {
        return FeatureMerge.mergeOverlappingPolygons(items, 4);
      }
      catch (GeometryException e) {
        return null;
      }
    }

    if ("boundary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    return null;
  }

  @Override
  public String name() {
    return "Streets";
  }

  @Override
  public String description() {
    return "Streets from OpenStreetMap";
  }

  @Override
  public boolean isOverlay() {
    return true;
  }

  @Override
  public String attribution() {
    return """
      <a href="https://www.openstreetmap.org/copyright" target="_blank">&copy; OpenStreetMap contributors</a>
      """.trim();
  }

  /*
   * Main entrypoint for this example program
   */
  public static void main(String[] args) throws Exception {
    run(Arguments.fromArgsOrConfigFile(args));
  }

  static void run(Arguments args) throws Exception {
    String area = args.getString("area", "geofabrik area to download", "monaco");
    // Planetiler is a convenience wrapper around the lower-level API for the most common use-cases.
    // See ToiletsOverlayLowLevelApi for an example using the lower-level API
    Planetiler.create(args)
      .setProfile(new Streets())
      // override this default with osm_path="path/to/data.osm.pbf"
      .addOsmSource("osm", Path.of("data", "sources", area + ".osm.pbf"), "geofabrik:" + area)
      // override this default with mbtiles="path/to/output.mbtiles"
      .overwriteOutput("mbtiles", Path.of("data", "streets.mbtiles"))
      .run();
  }
}