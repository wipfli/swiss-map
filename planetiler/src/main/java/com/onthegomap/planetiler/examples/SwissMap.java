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


public class SwissMap implements Profile {

  private record RouteRelationInfo(
    @Override long id,
    String name, String ref, String route, String network
  ) implements OsmRelationInfo {}

  private boolean isTunnel(SourceFeature sourceFeature) {
    return sourceFeature.hasTag("tunnel", "yes", "building_passage") || sourceFeature.hasTag("covered", "yes");
  }

  private boolean isNotTunnelOrBridge(SourceFeature sourceFeature) {
    return !(isTunnel(sourceFeature) || sourceFeature.hasTag("bridge", "yes"));
  }

  private boolean isUnclassified(SourceFeature sourceFeature) {
    return !sourceFeature.hasTag("tracktype", "grade2", "grade3", "grade4", "grade5") &&
    (
      sourceFeature.hasTag("highway",
        "service",
        "residential",
        "unclassified",
        "living_street",
        "pedestrian"
      ) || 
      sourceFeature.hasTag("tracktype", 
        "grade1"
      )
    );
  }

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

    // waterway layer
    if (sourceFeature.canBeLine() && (
      sourceFeature.hasTag("waterway",
        "canal",
        "river",
        "stream",
        "ditch"
      ) &&
      !sourceFeature.hasTag("tunnel")
    )) {
      features.line("waterway")
        .setMinPixelSize(0)
        .setMinZoom(sourceFeature.hasTag("waterway", "river", "canal") ? 9 : 14);
    }

    // water layer
    if (sourceFeature.canBePolygon() && (
      sourceFeature.hasTag("natural", "water") ||
      sourceFeature.hasTag("waterway", 
        "riverbank",
        "dock",
        "canal"
      ) ||
      sourceFeature.hasTag("landuse",
        "reservoir",
        "basin"
      ) ||
      "ocean".equals(sourceFeature.getSource())
    )) {
      features.polygon("water")
        .setMinZoom(sourceFeature.hasTag("waterway", "dock", "canal") ? 10 : 4);
    }

    // glacier layer
    if (sourceFeature.canBePolygon() && sourceFeature.hasTag("natural", "glacier")) {
      features.polygon("glacier")
        .setMinZoom(4);
    }

    // highway-tunnel-motorway layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "motorway", "motorway_link")) {
      features.line("highway-tunnel-motorway")
        .setMinPixelSize(0)
        .setMinZoom(11);
    }

    // highway-ground-motorway layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "motorway", "motorway_link")) {
      features.line("highway-ground-motorway")
        .setMinPixelSize(0)
        .setMinZoom(5)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11);
    }

    // highway-bridge-motorway layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "motorway", "motorway_link")) {
      features.line("highway-bridge-motorway")
        .setMinPixelSize(0)
        .setMinZoom(11);
    }

    // highway-tunnel-trunk layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "trunk", "trunk_link")) {
      features.line("highway-tunnel-trunk")
        .setMinPixelSize(0)
        .setMinZoom(11);
    }

    // highway-ground-trunk layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "trunk", "trunk_link")) {
      features.line("highway-ground-trunk")
        .setMinPixelSize(0)
        .setMinZoom(6)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11);
    }

    // highway-bridge-trunk layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "trunk", "trunk_link")) {
      features.line("highway-bridge-trunk")
        .setMinPixelSize(0)
        .setMinZoom(11);
    }

    // highway-tunnel-primary layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "primary", "primary_link")) {
      features.line("highway-tunnel-primary")
        .setMinPixelSize(0)
        .setMinZoom(11);
    }

    // highway-ground-primary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "primary", "primary_link")) {
      features.line("highway-ground-primary")
        .setMinPixelSize(0)
        .setMinZoom(8)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11);
    }

    // highway-bridge-primary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "primary", "primary_link")) {
      features.line("highway-bridge-primary")
        .setMinPixelSize(0)
        .setMinZoom(11);
    }

    // highway-tunnel-secondary layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "secondary", "secondary_link")) {
      features.line("highway-tunnel-secondary")
        .setMinPixelSize(0)
        .setMinZoom(11);
    }

    // highway-ground-secondary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "secondary", "secondary_link")) {
      features.line("highway-ground-secondary")
        .setMinPixelSize(0)
        .setMinZoom(9)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11);
    }

    // highway-bridge-secondary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "secondary", "secondary_link")) {
      features.line("highway-bridge-secondary")
        .setMinPixelSize(0)
        .setMinZoom(11);
    }

    // highway-tunnel-tertiary layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "tertiary", "tertiary_link")) {
      features.line("highway-tunnel-tertiary")
        .setMinPixelSize(0)
        .setMinZoom(11);
    }

    // highway-ground-tertiary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "tertiary", "tertiary_link")) {
      features.line("highway-ground-tertiary")
        .setMinPixelSize(0)
        .setMinZoom(10)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11);
    }

    // highway-bridge-tertiary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "tertiary", "tertiary_link")) {
      features.line("highway-bridge-tertiary")
        .setMinPixelSize(0)
        .setMinZoom(11);
    }

    // highway-tunnel-unclassified layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && isUnclassified(sourceFeature)) {
      features.line("highway-tunnel-unclassified")
        .setMinPixelSize(0)
        .setMinZoom(12);
    }

    // highway-ground-unclassified layer
    if (sourceFeature.canBeLine() && isUnclassified(sourceFeature)) {
      features.line("highway-ground-unclassified")
        .setMinPixelSize(0)
        .setMinZoom(12)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 12);
    }

    // highway-bridge-unclassified layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && isUnclassified(sourceFeature)) {
      features.line("highway-bridge-unclassified")
        .setMinPixelSize(0)
        .setMinZoom(12);
    }

    // highway-tunnel-tracktype-2 layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("tracktype", "grade2")) {
      features.line("highway-tunnel-tracktype-2")
        .setMinPixelSize(0)
        .setMinZoom(13);
    }

    // highway-ground-tracktype-2 layer
    if (sourceFeature.canBeLine() && isNotTunnelOrBridge(sourceFeature) && sourceFeature.hasTag("tracktype", "grade2")) {
      features.line("highway-ground-tracktype-2")
        .setMinPixelSize(0)
        .setMinZoom(13);
    }

    // highway-bridge-tracktype-2 layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("tracktype", "grade2")) {
      features.line("highway-bridge-tracktype-2")
        .setMinPixelSize(0)
        .setMinZoom(13);
    }

    // highway-tunnel-tracktype-3-4-5 layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && 
      sourceFeature.hasTag("tracktype", "grade3", "grade4", "grade5")
    ) {
      features.line("highway-tunnel-tracktype-3-4-5")
        .setMinPixelSize(0)
        .setMinZoom(13);
    }

    // highway-ground-tracktype-3-4-5 layer
    if (sourceFeature.canBeLine() && isNotTunnelOrBridge(sourceFeature) && 
      sourceFeature.hasTag("tracktype", "grade3", "grade4", "grade5")
    ) {
      features.line("highway-ground-tracktype-3-4-5")
        .setMinPixelSize(0)
        .setMinZoom(13);
    }

    // highway-bridge-tracktype-3-4-5 layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && 
      sourceFeature.hasTag("tracktype", "grade3", "grade4", "grade5")
    ) {
      features.line("highway-bridge-tracktype-3-4-5")
        .setMinPixelSize(0)
        .setMinZoom(13);
    }

    // highway-tunnel-path layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "path")) {
      features.line("highway-tunnel-path")
        .setMinPixelSize(0)
        .setMinZoom(12);
    }

    // highway-ground-path layer
    if (sourceFeature.canBeLine() && isNotTunnelOrBridge(sourceFeature) && sourceFeature.hasTag("highway", "path")) {
      features.line("highway-ground-path")
        .setMinPixelSize(0)
        .setMinZoom(12);
    }

    // highway-bridge-path layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "path")) {
      features.line("highway-bridge-path")
        .setMinPixelSize(0)
        .setMinZoom(12);
    }

    // highway-tunnel-footway layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "footway")) {
      features.line("highway-tunnel-footway")
        .setMinPixelSize(0)
        .setMinZoom(14);
    }

    // highway-ground-footway layer
    if (sourceFeature.canBeLine() && isNotTunnelOrBridge(sourceFeature) && sourceFeature.hasTag("highway", "footway")) {
      features.line("highway-ground-footway")
        .setMinPixelSize(0)
        .setMinZoom(14);
    }

    // highway-bridge-footway layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "footway")) {
      features.line("highway-bridge-footway")
        .setMinPixelSize(0)
        .setMinZoom(14);
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

    if ("waterway".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("water".equals(layer)) {
      try {
        return FeatureMerge.mergeOverlappingPolygons(items, 4);
      }
      catch (GeometryException e) {
        return null;
      }
    }

    if ("glacier".equals(layer)) {
      try {
        return FeatureMerge.mergeOverlappingPolygons(items, 4);
      }
      catch (GeometryException e) {
        return null;
      }
    }

    if ("highway-tunnel-motorway".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-ground-motorway".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-bridge-motorway".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-tunnel-trunk".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-ground-trunk".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-bridge-trunk".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-tunnel-primary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-ground-primary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-bridge-primary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-tunnel-secondary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-ground-secondary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-bridge-secondary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-tunnel-tertiary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-ground-tertiary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-bridge-tertiary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-tunnel-unclassified".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-ground-unclassified".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-bridge-unclassified".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-tunnel-tracktype-2".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-ground-tracktype-2".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-bridge-tracktype-2".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-tunnel-tracktype-3-4-5".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-ground-tracktype-3-4-5".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-bridge-tracktype-3-4-5".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-tunnel-path".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-ground-path".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-bridge-path".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-tunnel-footway".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-ground-footway".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-bridge-footway".equals(layer)) {
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
  public String attribution() {
    return "<a href=\"https://www.openstreetmap.org/copyright\" target=\"_blank\">&copy; OpenStreetMap contributors</a>";
  }

  public static void main(String[] args) throws Exception {
    run(Arguments.fromArgsOrConfigFile(args));
  }

  static void run(Arguments args) throws Exception {
    String area = args.getString("area", "geofabrik area to download", "monaco");
    Planetiler.create(args)
      .setProfile(new SwissMap())
      .addOsmSource("osm", Path.of("data", "sources", area + ".osm.pbf"), "geofabrik:" + area)
      .addShapefileSource("ocean", Path.of("data", "sources", "water-polygons-split-3857.zip"),
        "https://osmdata.openstreetmap.de/download/water-polygons-split-3857.zip")
      .overwriteOutput("mbtiles", Path.of("data", "streets.mbtiles"))
      .run();
  }
}