<template>
  <div>
    <NewGameComponent 
      @start-new-game="startNewGame"
      :starting-unit="startingUnit"
      :ending-unit="endingUnit"
      :mode="mode"
    />
    <MapComponent ref="mapComponent" />
    <GuessedUnitsList ref="guessedUnitsList"
      :mode="mode"
    />
    <AutocompleteComponent ref="autocompleteComponent"
      @submit-unit="handleUnitSubmit"
      @next-unit-outline="handleNextUnitOutline"
      @show-all-units-outlines="handleShowAllUnitsOutlines"
      @next-unit="handleShowNextUnit"
      :mode="mode"
    />
    <WinPopupComponent
      ref="winPopup"
      :starting-unit="startingUnit"
      :ending-unit="endingUnit"
      :total-steps="totalSteps"
      :optimal-steps="optimalSteps"
      :mode="mode"
    />
  </div>
</template>

<script>
import AutocompleteComponent from './components/AutocompleteComponent.vue';
import MapComponent from './components/MapComponent.vue';
import NewGameComponent from './components/NewGameComponent.vue';
import GuessedUnitsList from './components/GuessedUnitsList.vue';
import {useToast} from 'vue-toastification';
import WinPopupComponent from './components/WinPopupComponent.vue';

export default {
  components: {
    AutocompleteComponent,
    MapComponent,
    NewGameComponent,
    GuessedUnitsList,
    WinPopupComponent
  },
  setup() {
    const toast = useToast();
    return { toast };
  },
  data() {
    return {
      startingUnit: null,
      endingUnit: null,
      totalSteps: 0,
      optimalSteps: 0,
      mode: ''
    };
  },
  methods: {
    async handleUnitSubmit(unitName) {
      if (this.startingUnit === null || this.endingUnit === null) {
        this.toast.info('You need to start a new game first!');
        return;
      }
      if (this.$refs.mapComponent) {
        const result = await this.$refs.mapComponent.unitGuessed(unitName);
        if (result === 'Guessed') {
          this.toast.warning(`${this.mode == 'CountryUnfiltered' ? 'Country' : this.mode } already on the map!`);
          return;
        }
        if (result === 'NoGameStarted') {
          this.toast.error('You need to start a new game first!');
          return;
        }
        this.$refs.autocompleteComponent.clearInput();
        if (result === 'Won') {
          this.$refs.guessedUnitsList.addUnit(unitName, 'Good');
          let cntGood = 0, total = 0;
          for (let i = 0; i < this.$refs.guessedUnitsList.units.length; i++) {
            if (this.$refs.guessedUnitsList.units[i].status === 'Good') {
              cntGood++;
            }
            total++;
          }
          this.totalSteps = total;
          this.optimalSteps = cntGood;
          this.$refs.winPopup.showPopup();
          return;
        }
        this.$refs.guessedUnitsList.addUnit(unitName, result);
      }
    },
    isGameActive() {
      return this.$refs.mapComponent && this.$refs.mapComponent.isGameActive();
    },
    async startNewGame(mode) {
      if (this.$refs.mapComponent) {
        await this.$refs.mapComponent.startNewGame(mode);
        // Update starting and ending units after starting a new game
        this.startingUnit = this.$refs.mapComponent.startingUnit;
        this.endingUnit = this.$refs.mapComponent.endingUnit;
        this.mode = mode;
        // Clear the guessed units list
        this.$refs.guessedUnitsList.clearUnits();
      }
    },
    async handleNextUnitOutline() {
      if (this.startingUnit === null || this.endingUnit === null) {
        this.toast.info('You need to start a new game first!');
        return;
      }
      if (this.$refs.mapComponent.gameOver) {
        this.toast.info('Game over! You need to start a new game first!');
        return;
      }
      await this.$refs.mapComponent.showNextUnitOutline();
    },
    async handleShowAllUnitsOutlines() {
      if (this.startingUnit === null || this.endingUnit === null) {
        this.toast.info('You need to start a new game first!');
        return;
      }
      await this.$refs.mapComponent.showAllUnitsOutlines();
    },
    async handleShowNextUnit() {
      if (this.startingUnit === null || this.endingUnit === null) {
        this.toast.info('You need to start a new game first!');
        return;
      }
      if (this.$refs.mapComponent.gameOver) {
        this.toast.info('Game over! You need to start a new game first!');
        return;
      }
      let name = await this.$refs.mapComponent.fetchNextUnit();
      await this.handleUnitSubmit(name);
    }
  }
};
</script>

<style>
body {
  background-color: #2c3e50;
  margin: 0;
  font-family: Arial, sans-serif;
}
</style>
