# swiss-map
A simple map of Switzerland. The vector tiles are generated with [Planetiler](https://github.com/onthegomap/planetiler) and the maps is rendered in the browser with [MapLibre GL JS](https://github.com/maplibre/maplibre-gl-js).

The map style and tile set was adapted from https://github.com/baremaps/openstreetmap-vecto/.

## develop

Clone this repo, then build the tiles with:

```
docker run -v "$(pwd)/data":/data ghcr.io/onthegomap/planetiler:latest generate-custom --schema=/data/tileset.yml --download
```

Convert the mbtiles to pmtiles

```
pip3 install pmtiles
pmtiles-convert data/output.mbtiles output.pmtiles
```

And serve the website with:

```
npm install -g serve
serve .
```