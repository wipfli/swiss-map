#!/usr/bin/env bash
set -e
java -cp target/*-with-deps.jar com.onthegomap.planetiler.examples.QRank \
  --download --area=planet --bounds=world \
  --download-threads=10 --download-chunk-size-mb=1000 \
  --fetch-wikidata \
  --nodemap-type=array --storage=mmap -Xmx32g 2>&1 | tee logs.txt
