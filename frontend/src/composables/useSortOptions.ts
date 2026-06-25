import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import type { SortKey } from '../types/sort';

export function useSortOptions() {
  const { t } = useI18n();

  return computed(() => [
    {
      id: 'newest' as SortKey,
      label: t('sort.newest'),
      hint: t('sort.newestHint'),
    },
    {
      id: 'price-asc' as SortKey,
      label: t('sort.priceAsc'),
      hint: t('sort.priceAscHint'),
    },
    {
      id: 'price-desc' as SortKey,
      label: t('sort.priceDesc'),
      hint: t('sort.priceDescHint'),
    },
    {
      id: 'price-per-m2-asc' as SortKey,
      label: t('sort.bestPerM2'),
      hint: t('sort.bestPerM2Hint'),
    },
    {
      id: 'm2-desc' as SortKey,
      label: t('sort.largestFirst'),
      hint: t('sort.largestHint'),
    },
  ]);
}
