<template>
    <div v-if="isVisible" class="win-popup-overlay">
      <div class="win-popup">
        <h2>Congratulations!</h2>
        <p>
          You have successfully completed the game!
          <br>
          You traveled from 
          <span class="starting-unit">{{ startingUnit }}</span>
          to 
          <span class="ending-unit">{{ endingUnit }}</span>
          in {{ totalSteps }} steps. Optimal path takes {{ optimalSteps }} steps!
        </p>
        <button @click="closePopup">OK</button>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    props: {
      startingUnit: String,
      endingUnit: String,
      totalSteps: Number,
      optimalSteps: Number,
      mode: String
    },
    data() {
      return {
        isVisible: false,
      };
    },
    methods: {
      showPopup() {
        this.isVisible = true;
      },
      closePopup() {
        this.isVisible = false;
      }
    }
  };
  </script>
  
  <style>
  .win-popup-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.7);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 100000;
  }
  
  .win-popup {
    background-color: #4CAF50;
    padding: 20px;
    border-radius: 10px;
    text-align: center;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
  }
  
  .starting-unit {
    color: #eba7fd;
  }
  
  .ending-unit {
    color: #57eeee;
  }
  
  .win-popup h2 {
    margin-top: 0;
  }
  
  .win-popup button {
    padding: 10px 20px;
    margin-top: 20px;
  }
  </style>
  