FROM nodered/node-red:latest

# Install dashboard + table + amqplib (raw library for function nodes)
RUN npm install node-red-dashboard node-red-node-ui-table amqplib ws

# Download Leaflet locally so the map renders without external CDN calls
RUN mkdir -p /data/static && \
    wget -q https://unpkg.com/leaflet@1.9.4/dist/leaflet.js  -O /data/static/leaflet.js && \
    wget -q https://unpkg.com/leaflet@1.9.4/dist/leaflet.css -O /data/static/leaflet.css

COPY settings.js /data/settings.js
COPY flows/ /data/
