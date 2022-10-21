# swiss-map
A simple map of Switzerland. The vector tiles are generated with [Planetiler](https://github.com/onthegomap/planetiler) and the maps is rendered in the browser with [MapLibre GL JS](https://github.com/maplibre/maplibre-gl-js).

The map style and tile set was adapted from Baremaps:
* [data/tileset.yml](data/tileset.yml) mostly follows https://github.com/baremaps/openstreetmap-vecto/blob/main/tileset.json
* [style.json](style.json) mostly follows https://github.com/baremaps/openstreetmap-vecto/blob/main/style.json

## demo

https://wipfli.github.io/swiss-map/

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

To use local tiles, change the source in `style.json` to something like:

```diff
   "sources": {
     "swissmap": {
       "type": "vector",
-      "tiles": ["pmtiles://https://swiss-map.s3.eu-central-1.amazonaws.com/output.pmtiles/{z}/{x}/{y}"],
+      "tiles": ["pmtiles://output.pmtiles/{z}/{x}/{y}"],
       "maxzoom": 14
     }
   },
```

## S3 setup

The pmtiles file is stored on AWS S3. The bucket policy is

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "PublicReadGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::swiss-map/*"
        }
    ]
}
```

And the CORS settings are:

```json
[
    {
        "AllowedHeaders": [
            "*"
        ],
        "AllowedMethods": [
            "GET"
        ],
        "AllowedOrigins": [
            "https://wipfli.github.io"
        ],
        "ExposeHeaders": []
    }
]
``` 

## Shortbread

I would like to put an OpenMapTiles style onto shortbread tiles.

Generate shortbread mbtiles with planetiler:

```
docker run -v "$(pwd)/data":/data ghcr.io/onthegomap/planetiler:latest generate-custom --schema=/data/shortbread.yml --download
```

```

Serve shortbread mbtiles with tileserver-gl:
```
docker run --rm -it -v "$(pwd)/data":/data -p 8080:8080 maptiler/tileserver-gl -p 8080
```

Serve `index-shortbread.html` and style with:
```
npx serve .
```

Credits: Map style derived from OpenMapTiles.
