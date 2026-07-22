import { ref, toValue, watch, type MaybeRefOrGetter } from 'vue';
import type { CategoryCounts, ListingType } from '../types/listingItem';
import { getKindCounts } from '../api/listingsApi';
import { logger } from '../utils/logger';

export function useKindCounts(type: MaybeRefOrGetter<ListingType>) {
  const counts = ref<CategoryCounts>();
  const loading = ref(true);

  watch(
    () => toValue(type),
    async (t, _old, onCleanup) => {
      const controller = new AbortController();
      onCleanup(() => controller.abort());

      loading.value = true;

      try {
        counts.value = await getKindCounts(t, { signal: controller.signal });
        loading.value = false;
      } catch (e) {
        if (e instanceof DOMException && e.name === 'AbortError') return;
        logger.error('useKindCounts refresh failed:', e);
        loading.value = false;
      }
    },
    { immediate: true }
  );

  return { counts, loading };
}
