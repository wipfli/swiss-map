from fastapi import FastAPI
import uvicorn
import json
from shapely.geometry import shape, Point

with open('states.geojson') as f:
    data = json.load(f)

geometries = [shape(feature['geometry']) for feature in data['features']]



app = FastAPI()

@app.get("/")
async def root():
    return {"message": "Hello World"}

@app.get("/get_language/{lat}/{lon}")
async def get_language(lat: float, lon: float):
    for i in range(len(data['features'])):
        if geometries[i].contains(Point(lon, lat)):
            if 'language' in data['features'][i]['properties']:
                return data['features'][i]['properties']['language']
            else:
                return 'hi'
    return ''

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
