export const SORT_OPTIONS = [
    {id: 'newest', label: 'Newest first', hint: 'Most recently listed'},
    {id: 'price-asc', label: 'Price - low to high', hint: '€ ascending'},
    {id: 'price-desc', label: 'Price - high to low', hint: '€ descending'},
    {id: 'price-per-m2-asc', label: 'Best € / m²', hint: 'Lowest price per m²'},
    {id: 'm2-desc', label: 'Largest first', hint: 'm² descending'},
] as const;

export type SortKey = (typeof SORT_OPTIONS)[number]['id'];

export const KNOWN_SORTS = SORT_OPTIONS.map((opt) => opt.id);
