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
import com.onthegomap.planetiler.util.ZoomFunction;

import java.nio.file.Path;
import java.util.List;

public class SwissMap implements Profile {

  private String[] highwayCategories = {
    "railway",
    "unclassified",
    "tertiary",
    "secondary",
    "primary",
    "trunk",
    "motorway",
    "aeroway"
  };

  int tunnelAndBridgeMinZoom = 14;
  int linkMinZoom = 11;
  int globalMaxZoom = 14;

  private record RouteRelationInfo(
    @Override long id,
    String name, String ref, String route, String network
  ) implements OsmRelationInfo {}

  private boolean isTunnel(SourceFeature sourceFeature) {
    return sourceFeature.hasTag("tunnel", "yes", "building_passage") || sourceFeature.hasTag("covered", "yes");
  }

  private boolean isBridge(SourceFeature sourceFeature) {
    return sourceFeature.hasTag("bridge");
  }

  private boolean isNotTunnelOrBridge(SourceFeature sourceFeature) {
    return !(isTunnel(sourceFeature) || isBridge(sourceFeature));
  }

  private Integer getLayer(SourceFeature sourceFeature) {
    if (sourceFeature.hasTag("layer")) {
      try {
        return Integer.parseInt(sourceFeature.getTag("layer").toString());
      }
      catch (NumberFormatException e) {
        return null;
      }
    }
    else {
      return null;
    }
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
      ) ||
      sourceFeature.hasTag("aeroway",
        "taxiway"
      )
    );
  }

  @Override
  public List<OsmRelationInfo> preprocessOsmRelation(OsmElement.Relation relation) {
    return null;
  }

  class LineWidth implements ZoomFunction<Number> {
    boolean isCasing;
    boolean isLink;
    double[] levels;

    public LineWidth(boolean isCasing, boolean isLink, double[] levels) {
      this.isCasing = isCasing;
      this.isLink = isLink;
      this.levels = levels;
    }
    @Override
    public Number apply(int value) {
      double width = levels[value];
      if (isCasing) {
        width += 2.0;
      }
      if (isLink) {
        width -= 1.5;
      }
      return width;
    }
  }

  class LineSortKey implements ZoomFunction<Number> {
    int categoryIndex;
    boolean isLink;
    boolean isBridge;
    boolean isTunnel;
    Integer layer;
    boolean isCasing;

    public LineSortKey(int categoryIndex, boolean isLink, boolean isBridge, boolean isTunnel, Integer layer, boolean isCasing) {
      this.categoryIndex = categoryIndex; 
      this.isLink = isLink; 
      this.isBridge = isBridge; 
      this.isTunnel = isTunnel; 
      this.layer = layer;
      this.isCasing = isCasing;
    }
    @Override
    public Number apply(int value) {
      int result = 0;
      if (value < 14) {
        result += 2 * categoryIndex + (isCasing ? 0 : 1) + (isLink ? 0 : 2 * highwayCategories.length);
      }
      else {
        result += categoryIndex + (isCasing ? 0 : 1) * 2 * highwayCategories.length + (isLink ? 0 : highwayCategories.length);
      }

      if (14 <= value) {
        if (isBridge) {
          result += (layer == null ? 1 : layer) * 4 * highwayCategories.length;
        }
        if (isTunnel) {
          result += (layer == null ? -1 : layer) * 4 * highwayCategories.length;
        }
      }
      return result;
    }
  }

  class LineColor implements ZoomFunction<String> {
    boolean isTunnel;
    boolean isCasing;
    String[] levels;

    public LineColor(boolean isTunnel, boolean isCasing, String[] levels) {
      this.isTunnel = isTunnel;
      this.isCasing = isCasing;
      this.levels = levels;
    }

    @Override
    public String apply(int value) {
      if (value < 14) {
        return levels[value];
      }
      else {
        if (isTunnel) {
          return isCasing ? "#C5C5C5" : "#e4e4e4";
        }
        else {
          return levels[value];
        }
      }
    }
  }

  @Override
  public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {

    // wood layer
    if (sourceFeature.canBePolygon() && (
        sourceFeature.hasTag("landuse", "forest") ||
        sourceFeature.hasTag("natural", "wood")
    )) {
      features.polygon("wood")
        .setMinZoom(0);
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
      "park") ||
      sourceFeature.hasTag("aeroway", 
      "aerodrome")
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
      sourceFeature.hasTag("admin_level", "2") &&
      !sourceFeature.hasTag("maritime", "yes")
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
      int minZoom = 0;
      if ("ocean".equals(sourceFeature.getSource())) {
        minZoom = 0;
      }
      else {
        if (sourceFeature.hasTag("waterway", "dock", "canal")) {
          minZoom = 10;
        }
        else {
          minZoom = 4;
        }
      }
      features.polygon("water")
        .setMinZoom(minZoom);
    }

    // glacier layer
    if (sourceFeature.canBePolygon() && sourceFeature.hasTag("natural", "glacier")) {
      features.polygon("glacier")
        .setMinZoom(4);
    }




    if (sourceFeature.canBeLine() && sourceFeature.hasTag("railway", "rail", "narrow_gauge")) {

      int categoryIndex = 0;
      boolean isLink = false;
      boolean isTunnel = isTunnel(sourceFeature);
      boolean isBridge = isBridge(sourceFeature);
      int minZoom = sourceFeature.hasTag("service") ? 13 : 11;
      int maxZoom = globalMaxZoom;
      Integer layer = getLayer(sourceFeature);

      double[] lineWidthLevels = {
        0.0, // z0
        0.0, // z1
        0.0, // z2
        0.0, // z3
        0.0, // z4
        0.0, // z5
        0.0, // z6
        0.0, // z7
        -1.0, // z8
        -1.0, // z9
        -1.0, // z10
        -1.0, // z11
        -1.0, // z12
        -1.0, // z13
        -1.0, // z14
      };

      String[] casingLineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "lightgray", // z8
        "lightgray", // z9
        "lightgray", // z10
        "lightgray", // z11
        "lightgray", // z12
        "lightgray", // z13
        "lightgray", // z14
      };
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, true))
        .setAttr("line-color", new LineColor(isTunnel, true, casingLineColorLevels))
        .setAttr("line-width", new LineWidth(true, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5);
    }

    if (sourceFeature.canBeLine() && isUnclassified(sourceFeature)) {

      int categoryIndex = 1;
      boolean isLink = false;
      boolean isTunnel = isTunnel(sourceFeature);
      boolean isBridge = isBridge(sourceFeature);
      
      int minZoom = 0;
      if (sourceFeature.hasTag("highway", "unclassified")) {
        minZoom = 12;
      }
      else if (sourceFeature.hasTag("highway", "service")) {
        minZoom = 14;
      }
      else {
        minZoom = 13;
      }

      int maxZoom = globalMaxZoom;
      Integer layer = getLayer(sourceFeature);

      double[] lineWidthLevels = {
        0.0, // z0
        0.0, // z1
        0.0, // z2
        0.0, // z3
        0.0, // z4
        0.0, // z5
        0.0, // z6
        0.0, // z7
        0.0, // z8
        0.0, // z9
        0.0, // z10
        0.0, // z11
        1.0, // z12
        1.5, // z13
        2.0, // z14
      };

      String[] casingLineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "", // z8
        "", // z9
        "", // z10
        "", // z11
        "#ccc", // z12
        "#ccc", // z13
        sourceFeature.hasTag("highway", "unclassified") ? "#bbb" : "#ccc", // z14
      };

      String[] lineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "", // z8
        "", // z9
        "", // z10
        "", // z11
        "white", // z12
        "white", // z13
        "white", // z14
      };

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, true))
        .setAttr("line-color", new LineColor(isTunnel, true, casingLineColorLevels))
        .setAttr("kind", "unclassified")
        .setAttr("is-casing", "yes")
        .setAttr("line-width", new LineWidth(true, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)) + 2);
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, false))
        .setAttr("line-color", new LineColor(isTunnel, false, lineColorLevels))
        .setAttr("kind", "unclassified")
        .setAttr("is-casing", "no")
        .setAttr("line-width", new LineWidth(false, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)));
    }

    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "tertiary", "tertiary_link")) {

      int categoryIndex = 2;
      boolean isLink = sourceFeature.getTag("highway").toString().endsWith("_link");
      boolean isTunnel = isTunnel(sourceFeature);
      boolean isBridge = isBridge(sourceFeature);
      int minZoom = isLink ? 14 : 10;
      int maxZoom = globalMaxZoom;
      Integer layer = getLayer(sourceFeature);

      double[] lineWidthLevels = {
        0.0, // z0
        0.0, // z1
        0.0, // z2
        0.0, // z3
        0.0, // z4
        0.0, // z5
        0.0, // z6
        0.0, // z7
        0.0, // z8
        0.0, // z9
        1.0, // z10
        1.0, // z11
        2.0, // z12
        2.5, // z13
        3.0, // z14
      };

      String[] casingLineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "", // z8
        "", // z9
        "#ccc", // z10
        "#ccc", // z11
        "#bbb", // z12
        "#bbb", // z13
        "#bbb", // z14
      };

      String[] lineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "", // z8
        "", // z9
        "white", // z10
        "white", // z11
        "white", // z12
        "white", // z13
        "white", // z14
      };

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, true))
        .setAttr("line-color", new LineColor(isTunnel, true, casingLineColorLevels))
        .setAttr("kind", "tertiary")
        .setAttr("is-casing", "yes")
        .setAttr("line-width", new LineWidth(true, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)) + 2);
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, false))
        .setAttr("line-color", new LineColor(isTunnel, false, lineColorLevels))
        .setAttr("kind", "tertiary")
        .setAttr("is-casing", "no")
        .setAttr("line-width", new LineWidth(false, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)));
    }

    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "secondary", "secondary_link")) {

      int categoryIndex = 3;
      boolean isLink = sourceFeature.getTag("highway").toString().endsWith("_link");
      boolean isTunnel = isTunnel(sourceFeature);
      boolean isBridge = isBridge(sourceFeature);
      int minZoom = isLink ? linkMinZoom : 9;
      int maxZoom = globalMaxZoom;
      Integer layer = getLayer(sourceFeature);

      double[] lineWidthLevels = {
        0.0, // z0
        0.0, // z1
        0.0, // z2
        0.0, // z3
        0.0, // z4
        0.0, // z5
        0.0, // z6
        0.0, // z7
        0.0, // z8
        1.0, // z9
        1.0, // z10
        2.0, // z11
        2.5, // z12
        3.0, // z13
        3.5, // z14
      };

      String[] casingLineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "", // z8
        "#bbb", // z9
        "#bbb", // z10
        "#bbb", // z11
        "#bbb", // z12
        "#bbb", // z13
        "#bbb", // z14
      };

      String[] lineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "", // z8
        "white", // z9
        "white", // z10
        "white", // z11
        "white", // z12
        "white", // z13
        "white", // z14
      };

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, true))
        .setAttr("line-color", new LineColor(isTunnel, true, casingLineColorLevels))
        .setAttr("kind", "secondary")
        .setAttr("is-casing", "yes")
        .setAttr("line-width", new LineWidth(true, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)) + 2);
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, false))
        .setAttr("line-color", new LineColor(isTunnel, false, lineColorLevels))
        .setAttr("kind", "secondary")
        .setAttr("is-casing", "no")
        .setAttr("line-width", new LineWidth(false, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)));
    }

    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "primary", "primary_link")) {

      int categoryIndex = 4;
      boolean isLink = sourceFeature.getTag("highway").toString().endsWith("_link");
      boolean isTunnel = isTunnel(sourceFeature);
      boolean isBridge = isBridge(sourceFeature);
      int minZoom = isLink ? linkMinZoom : 8;
      int maxZoom = globalMaxZoom;
      Integer layer = getLayer(sourceFeature);

      double[] lineWidthLevels = {
        0.0, // z0
        0.0, // z1
        0.0, // z2
        0.0, // z3
        0.0, // z4
        0.0, // z5
        0.0, // z6
        0.0, // z7
        1.0, // z8
        1.0, // z9
        1.0, // z10
        2.0, // z11
        2.5, // z12
        3.0, // z13
        3.5, // z14
      };

      String[] casingLineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "#FCD374", // z8
        "#FCD374", // z9
        "#FCD374", // z10
        "#FCD374", // z11
        "#fab724", // z12
        "#fab724", // z13
        "#fab724", // z14
      };

      String[] lineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "#feefc3", // z8
        "#feefc3", // z9
        "#feefc3", // z10
        "#feefc3", // z11
        "#feefc3", // z12
        "#feefc3", // z13
        "#feefc3", // z14
      };

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, true))
        .setAttr("line-color", new LineColor(isTunnel, true, casingLineColorLevels))
        .setAttr("kind", "primary")
        .setAttr("is-casing", "yes")
        .setAttr("line-width", new LineWidth(true, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)) + 2);
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, false))
        .setAttr("line-color", new LineColor(isTunnel, false, lineColorLevels))
        .setAttr("kind", "primary")
        .setAttr("is-casing", "no")
        .setAttr("line-width", new LineWidth(false, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)));
    }

    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "trunk", "trunk_link")) {

      int categoryIndex = 5;
      boolean isLink = sourceFeature.getTag("highway").toString().endsWith("_link");
      boolean isTunnel = isTunnel(sourceFeature);
      boolean isBridge = isBridge(sourceFeature);
      int minZoom = isLink ? linkMinZoom : 7;
      int maxZoom = globalMaxZoom;
      Integer layer = getLayer(sourceFeature);

      double[] lineWidthLevels = {
        0.0, // z0
        0.0, // z1
        0.0, // z2
        0.0, // z3
        0.0, // z4
        0.0, // z5
        0.0, // z6
        1.0, // z7
        1.5, // z8
        2.0, // z9
        2.5, // z10
        3.0, // z11
        3.5, // z12
        4.0, // z13
        4.5, // z14
      };

      String[] casingLineColorLevels = {
        "#fab724", // z0
        "#fab724", // z1
        "#fab724", // z2
        "#fab724", // z3
        "#fab724", // z4
        "#fab724", // z5
        "#fab724", // z6
        "#fab724", // z7
        "#fab724", // z8
        "#fab724", // z9
        "#fab724", // z10
        "#fab724", // z11
        "#fab724", // z12
        "#fab724", // z13
        "#fab724", // z14
      };

      String[] lineColorLevels = {
        "#feefc3", // z0
        "#feefc3", // z1
        "#feefc3", // z2
        "#feefc3", // z3
        "#feefc3", // z4
        "#feefc3", // z5
        "#feefc3", // z6
        "#feefc3", // z7
        "#feefc3", // z8
        "#feefc3", // z9
        "#feefc3", // z10
        "#feefc3", // z11
        "#feefc3", // z12
        "#feefc3", // z13
        "#feefc3", // z14
      };

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, true))
        .setAttr("line-color", new LineColor(isTunnel, true, casingLineColorLevels))
        .setAttr("kind", "trunk")
        .setAttr("is-casing", "yes")
        .setAttr("line-width", new LineWidth(true, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)) + 2);
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, false))
        .setAttr("line-color", new LineColor(isTunnel, false, lineColorLevels))
        .setAttr("kind", "trunk")
        .setAttr("is-casing", "no")
        .setAttr("line-width", new LineWidth(false, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)));
    }

    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "motorway", "motorway_link")) {

      int categoryIndex = 6;
      boolean isLink = sourceFeature.getTag("highway").toString().endsWith("_link");
      boolean isTunnel = isTunnel(sourceFeature);
      boolean isBridge = isBridge(sourceFeature);
      int minZoom = isLink ? linkMinZoom : 5;
      int maxZoom = globalMaxZoom;
      Integer layer = getLayer(sourceFeature);

      double[] lineWidthLevels = {
        0.0, // z0
        0.0, // z1
        0.0, // z2
        0.0, // z3
        0.0, // z4
        1.0, // z5
        1.0, // z6
        1.5, // z7
        2.0, // z8
        2.5, // z9
        3.0, // z10
        3.5, // z11
        4.0, // z12
        4.5, // z13
        5.0, // z14
      };

      String[] casingLineColorLevels = {
        "#fab724", // z0
        "#fab724", // z1
        "#fab724", // z2
        "#fab724", // z3
        "#fab724", // z4
        "#fab724", // z5
        "#fab724", // z6
        "#fab724", // z7
        "#fab724", // z8
        "#fab724", // z9
        "#fab724", // z10
        "#fab724", // z11
        "#fab724", // z12
        "#fab724", // z13
        "#fab724", // z14
      };

      String[] lineColorLevels = {
        "#feefc3", // z0
        "#feefc3", // z1
        "#feefc3", // z2
        "#feefc3", // z3
        "#feefc3", // z4
        "#feefc3", // z5
        "#feefc3", // z6
        "#feefc3", // z7
        "#feefc3", // z8
        "#feefc3", // z9
        "#feefc3", // z10
        "#feefc3", // z11
        "#feefc3", // z12
        "#feefc3", // z13
        "#feefc3", // z14
      };

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, true))
        .setAttr("line-color", new LineColor(isTunnel, true, casingLineColorLevels))
        .setAttr("kind", "motorway")
        .setAttr("is-casing", "yes")
        .setAttr("line-width", new LineWidth(true, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)) + 2);
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, isLink, isBridge, isTunnel, layer, false))
        .setAttr("line-color", new LineColor(isTunnel, false, lineColorLevels))
        .setAttr("kind", "motorway")
        .setAttr("is-casing", "no")
        .setAttr("line-width", new LineWidth(false, isLink, lineWidthLevels))
        .setAttr("line-width-z20", 5 * (lineWidthLevels[14] - (isLink ? 1.5 : 0)));
    }

    if (sourceFeature.canBeLine() && sourceFeature.hasTag("aeroway", "runway")) {

      int categoryIndex = 7;
      int minZoom = 10;
      int maxZoom = globalMaxZoom;
      Integer layer = getLayer(sourceFeature);

      double[] lineWidthLevels = {
        0.0, // z0
        0.0, // z1
        0.0, // z2
        0.0, // z3
        0.0, // z4
        0.0, // z5
        0.0, // z6
        0.0, // z7
        0.0, // z8
        0.0, // z9
        3.5, // z10
        4.0, // z11
        4.5, // z12
        5.0, // z13
        5.5, // z14
      };

      String[] casingLineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "", // z8
        "", // z9
        "#aaa", // z10
        "#aaa", // z11
        "#aaa", // z12
        "#aaa", // z13
        "#aaa", // z14
      };

      String[] lineColorLevels = {
        "", // z0
        "", // z1
        "", // z2
        "", // z3
        "", // z4
        "", // z5
        "", // z6
        "", // z7
        "", // z8
        "", // z9
        "white", // z10
        "white", // z11
        "white", // z12
        "white", // z13
        "white", // z14
      };

      int z20LineWidth = 40;
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, false, false, false, layer, true))
        .setAttr("line-color", new LineColor(false, true, casingLineColorLevels))
        .setAttr("line-width", new LineWidth(true, false, lineWidthLevels))
        .setAttr("line-width-z20", z20LineWidth);
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(minZoom)
        .setMaxZoom(maxZoom)
        .setAttr("line-sort-key", new LineSortKey(categoryIndex, false, false, false, layer, false))
        .setAttr("line-color", new LineColor(false, false, lineColorLevels))
        .setAttr("line-width", new LineWidth(false, false, lineWidthLevels))
        .setAttr("line-width-z20", z20LineWidth);
    }

    // highway-tracktype-2 layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("tracktype", "grade2")) {
      features.line("highway-tracktype-2")
        .setMinPixelSize(0)
        .setMinZoom(14);
    }

    // highway-tracktype-3-4-5 layer
    if (sourceFeature.canBeLine() && (
        sourceFeature.hasTag("tracktype", "grade3", "grade4", "grade5") || (
          sourceFeature.hasTag("highway", "track") &&
          !sourceFeature.hasTag("tracktype")
        )
      )
    ) {
      features.line("highway-tracktype-3-4-5")
        .setMinPixelSize(0)
        .setMinZoom(14);
    }

    // highway-path layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "path")) {
      features.line("highway-path")
        .setMinPixelSize(0)
        .setMinZoom(12);
    }

    // highway-footway layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "footway")) {
      features.line("highway-footway")
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

    if ("highway".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        zoom < 13 ? 0.5 : 0.1,
        4
      );
    }

    if ("highway-tracktype-2".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.5,
        4
      );
    }

    if ("highway-tracktype-3-4-5".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.1,
        4
      );
    }

    if ("highway-path".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.1,
        4
      );
    }

    if ("highway-footway".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        0.5,
        0.1,
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
      .addOsmSource("osm", Path.of("data", "sources", area + ".osm.pbf"), "planet".equals(area) ? "aws:latest" : ("geofabrik:" + area))
      .addShapefileSource("ocean", Path.of("data", "sources", "water-polygons-split-3857.zip"),
        "https://osmdata.openstreetmap.de/download/water-polygons-split-3857.zip")
      .overwriteOutput("mbtiles", Path.of("data", "swissmap.mbtiles"))
      .run();
  }
}