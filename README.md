# swiss-map
A simple map of Switzerland. The vector tiles are generated with [Planetiler](https://github.com/onthegomap/planetiler) and the maps is rendered in the browser with [MapLibre GL JS](https://github.com/maplibre/maplibre-gl-js).

The map style and tile set was adapted from https://github.com/baremaps/openstreetmap-vecto/.

## develop

Clone this repo, then build the tiles with:

```
docker run -v "$(pwd)/data":/data ghcr.io/onthegomap/planetiler:latest generate-custom --schema=/data/tileset.yml --download
```

Serve the tiles with:

```
docker run --rm -it -v "$(pwd)/data":/data -p 8080:8080 maptiler/tileserver-gl -p 8080
```

And serve the website with:

```
python3 -m http.server
```

