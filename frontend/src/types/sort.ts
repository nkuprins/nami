export const SORT_IDS = [
  'newest',
  'price-asc',
  'price-desc',
  'price-per-m2-asc',
  'm2-desc',
] as const;

export type SortKey = (typeof SORT_IDS)[number];
export const KNOWN_SORTS = [...SORT_IDS];

// Legacy static options kept for fallback / non-reactive contexts.
export const SORT_OPTIONS = [
  {
    id: 'newest' as SortKey,
    label: 'Newest first',
    hint: 'Most recently listed',
  },
  {
    id: 'price-asc' as SortKey,
    label: 'Price - low to high',
    hint: '€ ascending',
  },
  {
    id: 'price-desc' as SortKey,
    label: 'Price - high to low',
    hint: '€ descending',
  },
  {
    id: 'price-per-m2-asc' as SortKey,
    label: 'Best € / m²',
    hint: 'Lowest price per m²',
  },
  { id: 'm2-desc' as SortKey, label: 'Largest first', hint: 'm² descending' },
];
