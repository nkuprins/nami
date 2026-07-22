import { ref, watch } from 'vue';

export type ViewMode = 'grid' | 'list';

const KEY = 'baltnami:view';

// Module-level singleton so every caller (ResultsHeader writer, HomeView reader)
// shares the same reactive value without prop plumbing.
const mode = ref<ViewMode>(
  localStorage.getItem(KEY) === 'list' ? 'list' : 'grid'
);

watch(mode, (v) => localStorage.setItem(KEY, v));

export function useViewMode() {
  return {
    mode,
    setMode: (v: ViewMode) => {
      mode.value = v;
    },
  };
}
