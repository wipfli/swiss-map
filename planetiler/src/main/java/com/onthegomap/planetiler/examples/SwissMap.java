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




    int numberOfHighwayLayers = 6;

    // highway-tunnel-unclassified layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && isUnclassified(sourceFeature)) {

      int layerGroupIndex = 0;
      int indexWithinGroup = 0;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(12)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-unclassified-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#7f8c8d")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "0")
        .setAttr("line-width-z14", "4")
        .setAttr("line-width-z20", "12");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(12)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-unclassified")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#E5E5E5")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "0")
        .setAttr("line-width-z14", "3.5")
        .setAttr("line-width-z20", "10");
    }

    // highway-ground-unclassified layer
    if (sourceFeature.canBeLine() && isUnclassified(sourceFeature)) {

      int layerGroupIndex = 1;
      int indexWithinGroup = 0;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(12)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 12)
        .setAttr("category", "highway-ground-unclassified-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#C5C5C5")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "0")
        .setAttr("line-width-z14", "4")
        .setAttr("line-width-z20", "12");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(12)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 12)
        .setAttr("category", "highway-ground-unclassified")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "white")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "0")
        .setAttr("line-width-z14", "3.5")
        .setAttr("line-width-z20", "10");
    }

    // highway-bridge-unclassified layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && isUnclassified(sourceFeature)) {

      int layerGroupIndex = 2;
      int indexWithinGroup = 0;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(12)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-unclassified-casing")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#C5C5C5")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "0")
        .setAttr("line-width-z14", "4")
        .setAttr("line-width-z20", "12");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(12)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-unclassified")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "white")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "0")
        .setAttr("line-width-z14", "3.5")
        .setAttr("line-width-z20", "10");
    }

    // highway-tunnel-tertiary layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "tertiary", "tertiary_link")) {
 
      int layerGroupIndex = 0;
      int indexWithinGroup = 1;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-tertiary-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#7f8c8d")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "4")
        .setAttr("line-width-z14", "6")
        .setAttr("line-width-z20", "16");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-tertiary")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#E5E5E5")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "3.5")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-ground-tertiary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "tertiary", "tertiary_link")) {

      int layerGroupIndex = 1;
      int indexWithinGroup = 1;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(10)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11)
        .setAttr("category", "highway-ground-tertiary-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#C5C5C5")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "4")
        .setAttr("line-width-z14", "6")
        .setAttr("line-width-z20", "16");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(10)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11)
        .setAttr("category", "highway-ground-tertiary")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "white")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "3.5")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-bridge-tertiary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "tertiary", "tertiary_link")) {

      int layerGroupIndex = 2;
      int indexWithinGroup = 1;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-tertiary-casing")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#C5C5C5")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "4")
        .setAttr("line-width-z14", "6")
        .setAttr("line-width-z20", "16");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-tertiary")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "white")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "3.5")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-tunnel-secondary layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "secondary", "secondary_link")) {
 
      int layerGroupIndex = 0;
      int indexWithinGroup = 2;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-secondary-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#F9BD11")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "4")
        .setAttr("line-width-z14", "6")
        .setAttr("line-width-z20", "16");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-secondary")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#FFFBE0")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "1")
        .setAttr("line-width-z12", "3.5")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-ground-secondary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "secondary", "secondary_link")) {

      int layerGroupIndex = 1;
      int indexWithinGroup = 2;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(9)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11)
        .setAttr("category", "highway-ground-secondary-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#F9BD11")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "4")
        .setAttr("line-width-z14", "6")
        .setAttr("line-width-z20", "16");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(9)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11)
        .setAttr("category", "highway-ground-secondary")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#FDEE93")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "1")
        .setAttr("line-width-z12", "3.5")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-bridge-secondary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "secondary", "secondary_link")) {
  
      int layerGroupIndex = 2;
      int indexWithinGroup = 2;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-secondary-casing")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#F9BD11")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "0")
        .setAttr("line-width-z12", "4")
        .setAttr("line-width-z14", "6")
        .setAttr("line-width-z20", "16");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-secondary")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#FDEE93")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0")
        .setAttr("line-width-z10", "1")
        .setAttr("line-width-z12", "3.5")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-tunnel-primary layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "primary", "primary_link")) {
      
      int layerGroupIndex = 0;
      int indexWithinGroup = 3;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-primary-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#f9b011")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "1.125")
        .setAttr("line-width-z10", "2.25")
        .setAttr("line-width-z12", "4.5")
        .setAttr("line-width-z14", "6.5")
        .setAttr("line-width-z20", "16.5");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-primary")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#FFF4D3")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0.875")
        .setAttr("line-width-z10", "1.75")
        .setAttr("line-width-z12", "3.5")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-ground-primary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "primary", "primary_link")) {

      int layerGroupIndex = 1;
      int indexWithinGroup = 3;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(8)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11)
        .setAttr("category", "highway-ground-primary-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#f9b011")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "1.125")
        .setAttr("line-width-z10", "2.25")
        .setAttr("line-width-z12", "4.5")
        .setAttr("line-width-z14", "6.5")
        .setAttr("line-width-z20", "16.5");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(8)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11)
        .setAttr("category", "highway-ground-primary")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#fde293")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0.875")
        .setAttr("line-width-z10", "1.75")
        .setAttr("line-width-z12", "3.5")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-bridge-primary layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "primary", "primary_link")) {

      int layerGroupIndex = 2;
      int indexWithinGroup = 3;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-primary-casing")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#f9b011")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "1.125")
        .setAttr("line-width-z10", "2.25")
        .setAttr("line-width-z12", "4.5")
        .setAttr("line-width-z14", "6.5")
        .setAttr("line-width-z20", "16.5");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-primary")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#fde293")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "0")
        .setAttr("line-width-z9", "0.875")
        .setAttr("line-width-z10", "1.75")
        .setAttr("line-width-z12", "3.5")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-tunnel-trunk layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "trunk", "trunk_link")) {
 
      int layerGroupIndex = 0;
      int indexWithinGroup = 4;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-trunk-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#FCD277")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.5")
        .setAttr("line-width-z9", "4")
        .setAttr("line-width-z10", "4.8")
        .setAttr("line-width-z12", "6.5")
        .setAttr("line-width-z14", "9")
        .setAttr("line-width-z20", "16.5");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-trunk")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#FFF4D3")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.33")
        .setAttr("line-width-z9", "2")
        .setAttr("line-width-z10", "2")
        .setAttr("line-width-z12", "2")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-ground-trunk layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "trunk", "trunk_link")) {

      int layerGroupIndex = 1;
      int indexWithinGroup = 4;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(6)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11)
        .setAttr("category", "highway-ground-trunk-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#fab724")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.5")
        .setAttr("line-width-z9", "4")
        .setAttr("line-width-z10", "4.8")
        .setAttr("line-width-z12", "6.5")
        .setAttr("line-width-z14", "9")
        .setAttr("line-width-z20", "16.5");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(6)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11)
        .setAttr("category", "highway-ground-trunk")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#feefc3")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.33")
        .setAttr("line-width-z9", "2")
        .setAttr("line-width-z10", "2")
        .setAttr("line-width-z12", "2")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-bridge-trunk layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "trunk", "trunk_link")) {

      int layerGroupIndex = 2;
      int indexWithinGroup = 4;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-trunk-casing")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#fab724")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.5")
        .setAttr("line-width-z9", "4")
        .setAttr("line-width-z10", "4.8")
        .setAttr("line-width-z12", "6.5")
        .setAttr("line-width-z14", "9")
        .setAttr("line-width-z20", "16.5");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-trunk")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#feefc3")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.33")
        .setAttr("line-width-z9", "2")
        .setAttr("line-width-z10", "2")
        .setAttr("line-width-z12", "2")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-tunnel-motorway layer
    if (sourceFeature.canBeLine() && isTunnel(sourceFeature) && sourceFeature.hasTag("highway", "motorway", "motorway_link")) {
 
      int layerGroupIndex = 0;
      int indexWithinGroup = 5;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-motorway-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#FCD277")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.5")
        .setAttr("line-width-z9", "4")
        .setAttr("line-width-z10", "4.8")
        .setAttr("line-width-z12", "6.5")
        .setAttr("line-width-z14", "9")
        .setAttr("line-width-z20", "16.5");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-tunnel-motorway")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#FFF4D3")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.33")
        .setAttr("line-width-z9", "2")
        .setAttr("line-width-z10", "2")
        .setAttr("line-width-z12", "2")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-ground-motorway layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "motorway", "motorway_link")) {

      int layerGroupIndex = 1;
      int indexWithinGroup = 5;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(6)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11)
        .setAttr("category", "highway-ground-motorway-casing")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#fab724")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.5")
        .setAttr("line-width-z9", "4")
        .setAttr("line-width-z10", "4.8")
        .setAttr("line-width-z12", "6.5")
        .setAttr("line-width-z14", "9")
        .setAttr("line-width-z20", "16.5");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(6)
        .setMaxZoom(isNotTunnelOrBridge(sourceFeature) ? 14 : 11)
        .setAttr("category", "highway-ground-motorway")
        .setAttr("line-cap", "round")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#feefc3")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.33")
        .setAttr("line-width-z9", "2")
        .setAttr("line-width-z10", "2")
        .setAttr("line-width-z12", "2")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-bridge-motorway layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("bridge", "yes") && sourceFeature.hasTag("highway", "motorway", "motorway_link")) {

      int layerGroupIndex = 2;
      int indexWithinGroup = 5;
      int lineSortKey = layerGroupIndex * 2 * numberOfHighwayLayers + indexWithinGroup;

      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-motorway-casing")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey)
        .setAttr("line-color", "#fab724")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.5")
        .setAttr("line-width-z9", "4")
        .setAttr("line-width-z10", "4.8")
        .setAttr("line-width-z12", "6.5")
        .setAttr("line-width-z14", "9")
        .setAttr("line-width-z20", "16.5");
      
      features.line("highway")
        .setMinPixelSize(0)
        .setMinZoom(11)
        .setMaxZoom(14)
        .setAttr("category", "highway-bridge-motorway")
        .setAttr("line-cap", "butt")
        .setAttr("line-sort-key", lineSortKey + numberOfHighwayLayers)
        .setAttr("line-color", "#feefc3")
        .setAttr("line-width-z6", "0")
        .setAttr("line-width-z8", "1.33")
        .setAttr("line-width-z9", "2")
        .setAttr("line-width-z10", "2")
        .setAttr("line-width-z12", "2")
        .setAttr("line-width-z14", "5")
        .setAttr("line-width-z20", "14");
    }

    // highway-tracktype-2 layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("tracktype", "grade2")) {
      features.line("highway-tracktype-2")
        .setMinPixelSize(0)
        .setMinZoom(13);
    }

    // highway-tracktype-3-4-5 layer
    if (sourceFeature.canBeLine() && sourceFeature.hasTag("tracktype", "grade3", "grade4", "grade5")
    ) {
      features.line("highway-tracktype-3-4-5")
        .setMinPixelSize(0)
        .setMinZoom(13);
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