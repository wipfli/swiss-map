#!/usr/bin/env bash
set -e
java --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  -cp target/*-with-deps.jar com.onthegomap.planetiler.examples.QRank \ 
  --download --area=monaco --bounds=world \
  --download-threads=10 --download-chunk-size-mb=1000 \
  --fetch-wikidata \
  --nodemap-type=array --storage=mmap -Xmx32g 2>&1 | tee logs.txt
