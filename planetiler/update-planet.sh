#!/usr/bin/env bash
set -e
pyosmium-up-to-date --size 20000 -v data/sources/planet.osm.pbf --server https://planet.osm.org/replication/hour --ignore-osmosis-headers 2>&1 | tee logs.txt
