## Planetiler QRank Profile

Swiss Map is experimenting with [QRank](https://qrank.wmcloud.org, which measures how popular a Wikidata entry is by counting how often the associated Wikipedia page was read by users. This could be useful to determine which text labels to show on a map.

## Usage

Steps on Ubuntu are the following:

Get the QRank file and wikipedia -> wikidata files:

```
cd src/main/java/com/onthegomap/planetiler/examples
wget https://qrank.wmcloud.org/download/qrank.csv.gz
gzip -d qrank.csv.gz
wget https://qrank-storage.wmcloud.org/qsitelinks/qsitelinks.mdb.zst
zstd -d qsitelinks.mdb.zst
mv qsitelinks.mdb data.mdb
```

Build Planetiler (Requires Java, see [Planetiler->Contributing](https://github.com/onthegomap/planetiler/blob/main/CONTRIBUTING.md):

```
./mvnw clean package --file standalone.pom.xml
```

Run Planetiler in the background:

```
screen -d -m "./runworld.sh"
```

Watch the logs:

```
tail -f logs.txt
```

Look at the tiles:

```
docker run --rm -it -v "$(pwd)/data":/data -p 8080:8080 maptiler/tileserver-gl -p 8080
```

### Update an existing planet.osm.pbf

```
pip install osmium
pyosmium-up-to-date --size 20000 -v planet.osm.pbf
```

Takes roughly 30 min.
