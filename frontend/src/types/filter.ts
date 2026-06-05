import {Feature, PropertyType} from "./propertyItem";

export type SortKey =
    | 'newest'
    | 'price-asc'
    | 'price-desc'
    | 'price-per-m2-asc'
    | 'm2-desc';

export interface FilterState {
    type: PropertyType;
    loc: string[];
    priceMin?: number;
    priceMax?: number;
    rooms: number[];
    m2Min?: number;
    m2Max?: number;
    floorMin?: number;
    floorMax?: number;
    notGround?: boolean;
    notTop?: boolean;
    yearMin?: number;
    yearMax?: number;
    features: Feature[];
    completion?: 'ready' | 'not-ready';
    sort: SortKey;
    page: number;
}

export const DEFAULT_FILTER_STATE: FilterState = {
    type: 'buy',
    loc: [],
    rooms: [],
    features: [],
    sort: 'newest',
    page: 1,
};

export const PAGE_SIZE = 12;