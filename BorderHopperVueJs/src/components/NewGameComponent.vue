<template>
  <div class="new-game-container">
    <div class="game-controls">
      <div class="start-game-text">Start New Game:</div>
      <button @click="startNewGame('Country')" class="new-game-button">Country</button>
      <button @click="startNewGame('CountryUnfiltered')" class="new-game-button">Country (Unfiltered)</button>
      <button @click="startNewGame('Okrug')" class="new-game-button">Okrug</button>
      <button @click="startNewGame('Opstina')" class="new-game-button">Opstina</button>
      <button @click="startNewGame('Tablica')" class="new-game-button">Tablica</button>
    </div>
    <div v-if="startingUnit && endingUnit" class="travel-text">
      I would like to travel from 
      <span :style="{ color: startingColor }">{{ startingUnit }}</span>
      to 
      <span :style="{ color: endingColor }">{{ endingUnit }}</span>.
      <span v-if="mode === 'CountryUnfiltered'"> (Using unfiltered connections)</span>
    </div>
  </div>
</template>


  
<script>
  export default {
    props: {
      startingUnit: String,
      endingUnit: String,
      mode: String
    },
    data() {
      return {
        startingColor: '#eba6ff',
        endingColor: '#00FFFF'
      };
    },
    methods: {
      startNewGame(mode) {
        this.$emit('start-new-game', mode);
      }
    }
  };
  </script>
  
<style>
.new-game-container {
  text-align: center;
  margin-top: 10px;
  justify-content: center;
  align-items: center;
}

.new-game-button {
  padding: 10px 15px;
  font-size: 16px;
  cursor: pointer;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  margin-left: 10px;
}

.new-game-button:hover {
background-color: #45a049;
}

.travel-text {
  margin-right: 20px;
  font-size: 20px;
  color: white;
}

.start-game-text {
  color: #4CAF50;
  padding: 10px 15px;
  border-radius: 4px;
  font-size: 18px;
  font-weight: bold;
}

.game-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 10px;
}
</style>
  