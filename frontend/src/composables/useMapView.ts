import { ref, watch } from 'vue';

// Map panel state, orthogonal to the grid/list toggle (useViewMode):
//   'off'   — no map, list/grid full width (default)
//   'split' — list/grid left, map right
//   'full'  — map only (full focus)
export type MapState = 'off' | 'split' | 'full';

const KEY = 'baltnami:map';

function initial(): MapState {
  const v = localStorage.getItem(KEY);
  return v === 'split' || v === 'full' ? v : 'off';
}

// Module-level singleton so the ResultsHeader button, HomeView layout, and the
// map's own controls all share one reactive value without prop plumbing.
const state = ref<MapState>(initial());

watch(state, (v) => localStorage.setItem(KEY, v));

export function useMapView() {
  return {
    state,
    open: () => {
      if (state.value === 'off') state.value = 'split';
    },
    close: () => {
      state.value = 'off';
    },
    toggle: () => {
      state.value = state.value === 'off' ? 'split' : 'off';
    },
    toggleFull: () => {
      state.value = state.value === 'full' ? 'split' : 'full';
    },
  };
}
