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

Uses the `QRank.java` profile in planetiler at https://github.com/wipfli/planetiler/blob/qrank-all/planetiler-examples/src/main/java/com/onthegomap/planetiler/examples/QRank.java
