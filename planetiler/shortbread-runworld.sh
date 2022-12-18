#!/usr/bin/env bash
set -e
docker run -v "$(pwd)/data":/data ghcr.io/onthegomap/planetiler:latest /data/shortbread.yml \
  --download \
  --download-threads=10 --download-chunk-size-mb=1000 \
  --fetch-wikidata \
  --nodemap-type=array --storage=mmap -Xmx32g 2>&1 | tee logs.txt
