#!/usr/bin/env bash
set -e
java  -cp target/*-with-deps.jar com.onthegomap.planetiler.examples.SwissMap \
  --download --area=planet --bounds=world \
  --download-threads=10 --download-chunk-size-mb=1000 \
  --fetch-wikidata \
  --nodemap-type=array --storage=mmap -Xmx32g 2>&1 | tee logs.txt
pmtiles convert data/swissmap.mbtiles data/swissmap.pmtiles 2>&1 | tee logs-pmtiles.txt
rclone copyto data/swissmap.pmtiles r2:swiss-map/swissmap.pmtiles 2>&1 | tee logs-rclone.txt
