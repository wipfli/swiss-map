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
import java.util.Map;

import java.net.MalformedURLException;
import java.net.ProtocolException;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;


public class RoadNames implements Profile {

    // copied from
    // https://github.com/protomaps/basemaps/blob/30ba3afe0078d39322936eaa4850843534ea9095/tiles/src/main/java/com/protomaps/basemap/names/OsmNames.java
    public static FeatureCollector.Feature setOsmNames(FeatureCollector.Feature feature, SourceFeature source,
            int minzoom) {
        for (Map.Entry<String, Object> tag : source.tags().entrySet()) {
            var key = tag.getKey();
            if (key.equals("name") || key.startsWith("name:")) {
                String markedString = "DEFAULT";
                if (source.getTag(key).toString().matches("[\\p{ASCII}]+")) {
                    markedString = source.getTag(key).toString();
                } else {
                    // The string contains non-Latin characters.
                    try {
                        String encodedString = URLEncoder.encode(source.getTag(key).toString(),
                                StandardCharsets.UTF_8.toString());
                        String urlString = String.format("http://localhost:3000/get_marked_string/%s", encodedString);
                        // System.out.println(entry.getKey().toString());
                        // System.out.println(urlString);
                        URL url = new URL(urlString);

                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");

                        // Get the response from the server
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        String markedStringEncoded = response.toString();
                        markedStringEncoded = markedStringEncoded.substring(1, markedStringEncoded.length() - 1); // remove
                                                                                                                  // ""
                                                                                                                  // around
                                                                                                                  // string
                        markedString = URLDecoder.decode(markedStringEncoded, StandardCharsets.UTF_8.toString());
                        // System.out.println(markedString);

                        // Print the response as a string
                        // String language = response.toString();
                        // System.out.println(language);
                    } catch (MalformedURLException e) {
                        System.out.println("MalformedURLException: " + e.getMessage());
                    } catch (ProtocolException e) {
                        System.out.println("ProtocolException: " + e.getMessage());
                    } catch (IOException e) {
                        System.out.println("IOException: " + e.getMessage());
                    }
                }
                feature.setAttr(key, markedString);
            }
        }

        return feature;
    }

    @Override
    public List<OsmRelationInfo> preprocessOsmRelation(OsmElement.Relation relation) {
        return null;
    }

    @Override
    public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {
        if (sourceFeature.canBeLine() && sourceFeature.hasTag("name") && sourceFeature.hasTag("highway")) {
            var feat = features.line("highway")
                    .setMinPixelSize(0)
                    .setMinZoom(14)
                    .setMaxZoom(14);

            setOsmNames(feat, sourceFeature, 14);
        }
    }

    @Override
    public List<VectorTile.Feature> postProcessLayerFeatures(String layer, int zoom,
            List<VectorTile.Feature> items) {

        if ("highway".equals(layer)) {
            return FeatureMerge.mergeLineStrings(items,
                    0.5,
                    zoom < 13 ? 0.5 : 0.1,
                    4);
        }

        return null;
    }

    @Override
    public String name() {
        return "RoadNames";
    }

    @Override
    public String description() {
        return "RoadNames from OpenStreetMap";
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
                .setProfile(new RoadNames())
                .addOsmSource("osm", Path.of("data", "sources", area + ".osm.pbf"),
                        "planet".equals(area) ? "aws:latest" : ("geofabrik:" + area))
                .overwriteOutput("mbtiles", Path.of("data", "roadnames.mbtiles"))
                .run();
    }
}