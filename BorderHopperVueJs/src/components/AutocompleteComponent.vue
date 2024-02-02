<template>
  <div class="autocomplete-container">
    <input
      v-model="unitName"
      type="text"
      :placeholder="placeholderText"
      class="autocomplete-input"
      @input="fetchSuggestions"
    />
    <ul v-if="suggestions.length" class="suggestions-list">
      <li
        v-for="suggestion in suggestions"
        :key="suggestion"
        @click="selectSuggestion(suggestion)"
      >
        {{ suggestion }}
      </li>
    </ul>
    <button
      @click="submitUnit"
      class="submit-button"
      :disabled="unitName.length == 0 || mode === '' || (suggestions.length && suggestions[0] !== unitName)"
    >
      Submit
    </button>
    <div class="hints-and-controls">
      <div class="hints-container">Hints:</div>
      <button @click="nextUnitOutline" class="control-button">
        Next {{ mode === 'CountryUnfiltered' ? 'Country' : mode }} Outline
      </button>
      <button @click="showAllUnitsOutlines" class="control-button">
        All {{ mode === 'CountryUnfiltered' ? 'Country' : mode }} Outlines
      </button>
      <button @click="nextUnit" class="control-button">
        Next {{ mode === 'CountryUnfiltered' ? 'Country' : mode }}
      </button>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  props: {
    mode: String
  },
  data() {
    return {
      unitName: '',
      suggestions: [],
      invalidCharsPattern: /[^a-zA-Z0-9\sčćžšđČĆŽŠĐ.-]/
    };
  },
  computed: {
    placeholderText() {
      // Dynamically compute the placeholder based on the mode
      return `Enter a ${this.mode === '' ? '' : this.mode === 'CountryUnfiltered' ? 'Country ' : this.mode + ' '}name`;
    }
  },
  methods: {
    fetchSuggestions() {
      if (this.mode === '') {
        this.suggestions = [];
        return;
      }
      if (this.unitName && !this.invalidCharsPattern.test(this.unitName)) {
        axios.get(`http://localhost:8081/api/suggestUnits?type=${this.mode}&searchString=${this.unitName}&topN=300`)
          .then(response => {
            this.suggestions = response.data;
          })
          .catch(error => {
            console.error('Error fetching suggestions:', error);
            this.suggestions = [];
          });
      } else {
        this.suggestions = [];
      }
    },
    clearInput() {
      this.unitName = '';
      this.suggestions = [];
    },
    selectSuggestion(suggestion) {
      this.unitName = suggestion;
      this.suggestions = [];
    },
    submitUnit() {
      this.$emit('submit-unit', this.unitName);
    },
    nextUnitOutline() {
      this.$emit('next-unit-outline');
    },
    showAllUnitsOutlines() {
      this.$emit('show-all-units-outlines');
    },
    nextUnit() {
      this.$emit('next-unit');
    }
  }
};
</script>

<style>
.autocomplete-container {
  text-align: center;
  align-items: center;
  align-content: center;
  position: relative;
  margin: 10px auto;
  margin-top: 0px;
  width: 70%;
  position: relative;
}
.hints-container {
  color: #ffffff;
  align-items: center;
  align-self: center;
  padding: 10px 15px;
  border-radius: 4px;
  margin-top: 5px;
  font-size: 18px;
  font-weight: bold;
}
.actions-container {
  display: flex;
  justify-content: center;
}

.hints-and-controls {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.autocomplete-input {
  width: 80%;
  padding: 8px;
  margin-right: 10px;
  font-size: 16px;
  border: 1px solid #ccc;
  border-radius: 4px;
  margin-top: 10;
}

.submit-button {
  width: 15%;
  padding: 8px 15px;
  cursor: pointer;
  margin-top: 10;
  font-size: 16px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background-color: #f8f8f8;
  transition: background-color 0.3s;
}

.submit-button:hover {
  background-color: #e0e0e0;
}

.suggestions-list {
  list-style-type: none;
  padding: 0;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 4px rgba(0,0,0,.2);
  position: absolute;
  width: 97%;
  bottom: 90%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  max-height: 250px;
  overflow-y: auto;
  border-bottom: none;
}

.suggestions-list li {
  padding: 10px;
  cursor: pointer;
  border-bottom: 1px solid #ddd;
}

.suggestions-list li:hover {
  background-color: #f0f0f0;
}

.control-button {
  width: 20%;
  padding: 5px 10px;
  cursor: pointer;
  font-size: 16px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background-color: #f8f8f8;
  transition: background-color 0.3s;
  margin-left: 5px;
  margin-top: 10px;
}

.control-button:hover {
  background-color: #e0e0e0;
}
</style>
