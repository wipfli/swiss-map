# QA map

https://wipfli.github.io/swiss-map/qa

## Steps

```
docker run -v "$(pwd)/data":/data ghcr.io/onthegomap/planetiler:latest osm-qa --area=switzerland --download --bounds "7.5,47,8.5,48"

./pmtiles convert data/qa.mbtiles qa.pmtiles


```

