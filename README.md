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

To use local tiles, change the source in `style.json` to something like:

```diff
   "sources": {
     "swissmap": {
       "type": "vector",
-      "tiles": ["pmtiles://https://swiss-map.s3.eu-central-1.amazonaws.com/output.pmtiles/{z}/{x}/{y}"],
+      "tiles": ["pmtiles://output.pmtiles/{z}/{x}/{y}"],
       "maxzoom": 12
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
