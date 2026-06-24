import { computed, ref, toValue, watch, type MaybeRefOrGetter } from 'vue';
import type { PropertyItem } from '../types/propertyItem';
import { type FilterState, PAGE_SIZE } from '../types/filter';
import { listProperties } from '../api/propertiesApi';
import { logger } from '../utils/logger';

export function useListings(source: MaybeRefOrGetter<FilterState>) {
  const items = ref<PropertyItem[]>([]);
  const total = ref(0);
  const loading = ref(true);
  const error = ref<string | null>(null);
  const pageCount = computed(() =>
    Math.max(1, Math.ceil(total.value / PAGE_SIZE))
  );

  watch(
    () => JSON.stringify(toValue(source)),
    async (_newValue, _oldValue, onCleanup) => {
      const controller = new AbortController();
      onCleanup(() => controller.abort());

      loading.value = true;
      error.value = null;

      try {
        const q = toValue(source);
        const out = await listProperties(q, { signal: controller.signal });

        items.value = out.items;
        total.value = out.total;
        loading.value = false;
      } catch (e) {
        if (e instanceof DOMException && e.name === 'AbortError') return;
        error.value =
          e instanceof Error ? e.message : 'Failed to load listings.';
        logger.error('useListings refresh failed:', e);
        loading.value = false;
      }
    },
    { immediate: true }
  );

  return { items, total, pageCount, loading, error };
}
