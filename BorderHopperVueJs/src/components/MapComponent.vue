<template>
  <div class="map-container">
    <div id="map" :style="{ height: '100%', backgroundColor: 'black' }"></div>
  </div>
</template>

<script>
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import axios from 'axios';

export default {
  data() {
    return {
      map: null,
      startingUnit: null,
      endingUnit: null,
      geoJsonLayers: [],
      geoJsonLayerNames: [],
      outlineLayers: new Map(),
      markers: [],
      gameOver: false,
      unitsGuessed: new Set(),
      mode: null
    };
  },
  mounted() {
    this.map = L.map('map', {
      center: [0, 0],
      zoom: 1,
      attributionControl: false
    });
  },
  methods: {
    async startNewGame(mode) {
      try {
        this.mode = mode;
        const response = await axios.get('http://localhost:8081/api/randomConnected?type=' + this.mode);
        const unitPair = response.data;
        this.startingUnit = unitPair.first;
        this.endingUnit = unitPair.second;
        this.clearMap();
        await this.addUnitGeometry(this.startingUnit, 'Starting');
        await this.addUnitGeometry(this.endingUnit, 'Ending');
        this.fitAroundUnits();
      } catch (error) {
        console.error('Error fetching random unit:', error);
      }
    },
    async unitGuessed(unitName) {
      if (this.startingUnit == null) {
        return 'NoGameStarted';
      }
      if (this.startingUnit === unitName) {
        return 'Guessed';
      }
      if (this.endingUnit === unitName) {
        return 'Guessed';
      }
      if (this.unitsGuessed.has(unitName)) {
        return 'Guessed';
      }
      const currentDistance = (await axios.get(`http://localhost:8081/api/distanceRemaining?type=${this.mode}&start=${this.startingUnit}&end=${this.endingUnit}&unitsGuessed=${Array.from(this.unitsGuessed).join(',')}`)).data;
      this.unitsGuessed.add(unitName);
      const newDistance = (await axios.get(`http://localhost:8081/api/distanceRemaining?type=${this.mode}&start=${this.startingUnit}&end=${this.endingUnit}&unitsGuessed=${Array.from(this.unitsGuessed).join(',')}`)).data;
      let labelColor = 'white';
      if (newDistance == currentDistance) {
        labelColor = 'red'; // Red for bad guess;
      } else {
        labelColor = 'rgba(0, 255, 0, 1)'; // Lighter green for good guess;
      }
      const layer = await this.addUnitGeometry(unitName, 'Middle', labelColor);
      this.map.fitBounds(layer.getBounds());
      await this.updateConnectedUnits();
      if (currentDistance !=0 && newDistance == 0) {
        this.gameOver = true;
        return 'Won';
      }
      if (newDistance == currentDistance) {
        return 'Bad';
      } else {
        return 'Good';
      }
    },
    async updateConnectedUnits() {
      try {
        const response = await axios.get(`http://localhost:8081/api/getConnected?type=${this.mode}&start=${this.startingUnit}&end=${this.endingUnit}&unitsGuessed=${Array.from(this.unitsGuessed).join(',')}`);
        const connectedUnits = response.data;

        for (let i = 2; i < this.geoJsonLayers.length; i++) {
          const layer = this.geoJsonLayers[i];
          const unitName = this.geoJsonLayerNames[i];
          if (connectedUnits.includes(unitName)) {
            layer.setStyle({ fillOpacity: 0.7 }); // More opaque for connected units
          } else {
            layer.setStyle({ fillOpacity: 0.3 }); // Less opaque for non-connected units
          }
        }
      } catch (error) {
        console.error('Error fetching connected units:', error);
      }
    },
    async addUnitGeometry(unitName, position, labelColor = 'white') {
      try {
        const response = await axios.get(`http://localhost:8081/api/geometry/${unitName}?type=${this.mode}`);
        const geometry = response.data;

        if (this.outlineLayers.has(unitName)) {
          this.map.removeLayer(this.outlineLayers.get(unitName));
          this.outlineLayers.delete(unitName);
        }

        let fillColor, color;
        if (position === 'Starting') {
          fillColor = '#eba6ff'; // Magenta fill
          color = '#eba6ff'; // Magenta border
        } else if (position === 'Ending') {
          fillColor = '#00FFFF'; // Cyan fill
          color = '#00FFFF'; // Cyan border
        } else if (position === 'Middle') {
          fillColor = '#fae68e'; // Yellow (map-like) fill
          color = '#fae68e'; // Yellow border
        } else {
          fillColor = '#add8e6'; // Light blue fill as default (shouldn't happen)
          color = '#0000FF'; // Blue border as default (shouldn't happen)
        }

        const geoJsonLayer = L.geoJSON(geometry, {
          style: {
            fillColor: fillColor,
            fillOpacity: 0.7,
            color: color,
            weight: 1 // Border width
          }
        }).addTo(this.map);

        this.geoJsonLayers.push(geoJsonLayer);
        this.geoJsonLayerNames.push(unitName);
        this.addLabelToLargestPolygon(geoJsonLayer, unitName, labelColor);
        return geoJsonLayer;
      } catch (error) {
        console.error('Error fetching unit geometry:', error);
      }
    },
    addUnitOutline(unitName, unitGeojson) {
      try {
        if (this.outlineLayers.has(unitName) || this.unitsGuessed.has(unitName) || unitName == this.startingUnit || unitName == this.endingUnit) {
          return;
        }
        const outlineLayer = L.geoJSON(unitGeojson, {
          style: {
            fillColor: '#ffffff',
            fillOpacity: 0.1,
            color: '#ffffff',
            weight: 2
          }
        }).addTo(this.map);
        this.outlineLayers.set(unitName, outlineLayer);
      } catch (error) {
        console.error('Error fetching unit outline:', error);
      }
    },
    fitAroundUnits() {
      const bounds = L.latLngBounds([]);
      this.geoJsonLayers.forEach(layer => {
        bounds.extend(layer.getBounds());
      });
      this.map.fitBounds(bounds);
    },
    addLabelToLargestPolygon(geoJsonLayer, label, labelColor) {
      let largestArea = 0;
      let centroidOfLargest = null;

      // Iterate over each layer inside the GeoJSON layer
      geoJsonLayer.eachLayer((layer) => {
        const geometry = layer.feature.geometry;
        let area = 0;
        let centroid = null;

        if (geometry.type === 'Polygon') {
          area = this.calculatePolygonArea(layer);
          centroid = this.calculateCentroid(layer);
        } else if (geometry.type === 'MultiPolygon') {
          // Calculate the area and find the centroid of the largest polygon in the multipolygon
          geometry.coordinates.forEach((polygonCoords) => {
            // Create a temporary layer for the polygon to calculate area and centroid
            // For each polygon in the MultiPolygon, reverse the coordinates
            const reversedCoords = polygonCoords[0].map(coord => [coord[1], coord[0]]);
            const polygonLayer = L.polygon(reversedCoords); // Create the polygon with reversed coordinates
            const currentArea = this.calculatePolygonArea(polygonLayer);
            if (currentArea > area) {
              area = currentArea;
              centroid = this.calculateCentroid(polygonLayer);
            }
          });
        }

        if (area > largestArea) {
          largestArea = area;
          centroidOfLargest = centroid;
        }
      });

      if (centroidOfLargest) {
        this.addLabelMarker(centroidOfLargest, label, labelColor);
      }
    },
    calculateCentroid(layer) {
      return layer.getBounds().getCenter();
    },
    calculatePolygonArea(layer) {
      const bounds = layer.getBounds();
      const southWest = bounds.getSouthWest();
      const northEast = bounds.getNorthEast();

      // Calculate the width and height of the bounding box
      const width = northEast.lng - southWest.lng;
      const height = northEast.lat - southWest.lat;

      const area = Math.abs(width * height);
      return area;
    },
    addLabelMarker(latlng, label, color) {
      const marker = L.marker(latlng, {
        icon: L.divIcon({
          className: 'label',
          html: `<span class="label-text" style="color: ${color}">${label}</span>`,
          iconSize: [80, 20],
          iconAnchor: [40, 10] // Anchor point in the middle of the icon size
        })
      }).addTo(this.map);
      this.markers.push(marker);
    },
    async fetchNextUnit() {
      try {
        const response = await axios.get(`http://localhost:8081/api/nextUnit?type=${this.mode}&start=${this.startingUnit}&end=${this.endingUnit}&unitsGuessed=${Array.from(this.unitsGuessed).join(',')}`);
        const nextUnit = response.data;
        return nextUnit;
      } catch (error) {
        console.error('Error fetching next unit:', error);
      }
    },
    async showAllUnitsOutlines() {
      try {
        const response = await axios.get(`http://localhost:8081/api/geometries?type=${this.mode}`);
        const allGeometries = response.data;
        allGeometries.forEach(async (pair) => {
          const unitName = pair.first;
          const unitGeojson = JSON.parse(pair.second);
          this.addUnitOutline(unitName, unitGeojson);
        });
      } catch (error) {
        console.error('Error fetching all unit geometries:', error);
      }
    },

    async showNextUnitOutline() {
      try {
        const nextUnit = await this.fetchNextUnit();
        if (nextUnit) {
          const response = await axios.get(`http://localhost:8081/api/geometry/${nextUnit}?type=${this.mode}`);
          const geometry = response.data;
          this.addUnitOutline(nextUnit, geometry);
          this.map.fitBounds(this.outlineLayers.get(nextUnit).getBounds());
        } else {
          alert('No more units to show!');
        }
      } catch (error) {
        console.error('Error fetching next unit outline:', error);
      }
    },

    clearMap() {
      this.geoJsonLayers.forEach(layer => {
        this.map.removeLayer(layer);
      });
      this.outlineLayers.forEach(layer => {
        this.map.removeLayer(layer);
      });
      this.markers.forEach(marker => {
        this.map.removeLayer(marker);
      });
      this.geoJsonLayers = [];
      this.geoJsonLayerNames = [];
      this.outlineLayers = new Map();
      this.markers = [];
      this.unitsGuessed = new Set();
      this.gameOver = false;
    }
  }
};
</script>

<style>
.label .label-text {
  background-color: rgba(0, 0, 0, 0.2);
  font-size: 12px;
  font-weight: bold;
  padding: 2px 4px;
  border-radius: 4px;
  text-align: center;
  white-space: nowrap;
}

.map-container {
  height: 70vh;
  margin: 20px;
  margin-top: 10px;
  margin-bottom: 0px;
}
</style>
