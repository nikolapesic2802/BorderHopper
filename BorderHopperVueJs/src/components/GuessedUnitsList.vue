<template>
    <div class="guessed-units-list">
      <div class="guesses-title">Guesses:</div>
      <div v-for="(unit, index) in units" :key="index" :class="['unit-box', unit.status.toLowerCase()]">
        {{ unit.name }}
      </div>
    </div>
  </template>
  
<script>
export default {
  props: {
    mode: String
  },
  data() {
    return {
      units: []
    };
  },
  methods: {
    addUnit(name, status) {
      this.units.push({ name, status });
    },
    clearUnits() {
      this.units = [];
    }
  }
};
</script>
  
<style>
.guessed-units-list {
  display: flex;
  flex-wrap: wrap;
  padding: 10px;
}

.unit-box {
  margin-top: 5px;
  margin-right: 10px;
  padding: 5px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.guesses-title {
  color: #ffffff;
  font-size: 18px;
  font-weight: bold;
  margin-right: 10px;
  margin-top: 10px;
  margin-bottom: 5px;
}

.good {
  background-color: green;
  color: white;
}

.bad {
  background-color: red;
  color: white;
}

.won {
  background-color: gold;
  color: black;
}
</style>
  