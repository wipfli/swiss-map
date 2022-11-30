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

See [Planetiler](./Planetiler).
