#!/usr/bin/env bash
set -e
java --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  -cp target/*-with-deps.jar com.onthegomap.planetiler.examples.QRank \
  --download --area=planet --bounds=world \
  --download-threads=10 --download-chunk-size-mb=1000 \
  --fetch-wikidata \
  --nodemap-type=array --storage=mmap -Xmx32g 2>&1 | tee logs.txt
pmtiles convert data/qrank.mbtiles data/qrank.pmtiles 2>&1 | tee logs-pmtiles.txt
rclone copyto data/qrank.pmtiles r2:swiss-map/qrank.pmtiles 2>&1 | tee logs-rclone.txt
