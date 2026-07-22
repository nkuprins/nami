import { ref, toValue, watch, type MaybeRefOrGetter } from 'vue';
import type { FilterState } from '../types/filter';
import type { MapPin } from '../types/map';
import { listMapPins } from '../api/listingsApi';
import { logger } from '../utils/logger';

// Fetches all matching map pins in one request whenever the filters (or the
// search nonce) change — but only while `enabled` is true, so the map endpoint
// is never hit until the user actually opens the map (protects DB compute).
export function useMapPins(
  source: MaybeRefOrGetter<FilterState>,
  trigger: MaybeRefOrGetter<number>,
  enabled: MaybeRefOrGetter<boolean>
) {
  const pins = ref<MapPin[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  watch(
    () => {
      if (!toValue(enabled)) return null;
      const s = toValue(source);
      // Full filter serialization: unlike the list (which pages), any filter
      // change alters the pin set, so key on the whole query string + nonce.
      return `${mapFilterKey(s)}|${toValue(trigger)}`;
    },
    async (key, _old, onCleanup) => {
      if (key === null) return; // map closed — keep last pins, don't fetch

      const controller = new AbortController();
      onCleanup(() => controller.abort());

      loading.value = true;
      error.value = null;
      try {
        pins.value = await listMapPins(toValue(source), {
          signal: controller.signal,
        });
        loading.value = false;
      } catch (e) {
        if (e instanceof DOMException && e.name === 'AbortError') return;
        error.value = e instanceof Error ? e.message : 'Failed to load map.';
        logger.error('useMapPins refresh failed:', e);
        loading.value = false;
      }
    },
    { immediate: true }
  );

  return { pins, loading, error };
}

// A stable key over every filter field the map cares about. JSON of the state
// is enough (order is fixed by the FilterState shape); page/sort are excluded
// since the map ignores them. Exported so the map can recenter only when the
// filter set (not just the page) actually changes.
export function mapFilterKey(s: FilterState): string {
  const { page: _page, sort: _sort, ...rest } = s;
  return JSON.stringify(rest);
}
