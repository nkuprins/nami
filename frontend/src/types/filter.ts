import {
  EnergyClass,
  Feature,
  HeatingType,
  PropertyCompletion,
  ListingType,
} from './listingItem';
import { SortKey } from './sort';
import { Location } from '../data/rawLocations';

export interface FilterState {
  type: ListingType;
  loc: Location[];
  priceMin?: number;
  priceMax?: number;
  rooms: number[];
  bedrooms: number[];
  bathrooms: number[];
  m2Min?: number;
  m2Max?: number;
  floorMin?: number;
  floorMax?: number;
  notGround?: boolean;
  notTop?: boolean;
  yearMin?: number;
  yearMax?: number;
  heating: HeatingType[];
  energyClass: EnergyClass[];
  features: Feature[];
  completion?: PropertyCompletion;
  sort: SortKey;
  page: number;
}

export type FilterKey = keyof FilterState;
export const ALL_FILTER_KEYS = Object.keys({
  type: 0,
  loc: 0,
  priceMin: 0,
  priceMax: 0,
  rooms: 0,
  bedrooms: 0,
  bathrooms: 0,
  m2Min: 0,
  m2Max: 0,
  floorMin: 0,
  floorMax: 0,
  notGround: 0,
  notTop: 0,
  yearMin: 0,
  yearMax: 0,
  heating: 0,
  energyClass: 0,
  features: 0,
  completion: 0,
  sort: 0,
  page: 0,
} satisfies Record<FilterKey, 0>) as FilterKey[];

export const DEFAULT_FILTER_STATE: FilterState = {
  type: 'buy',
  loc: [],
  rooms: [],
  bedrooms: [],
  bathrooms: [],
  heating: [],
  energyClass: [],
  features: [],
  sort: 'newest',
  page: 1,
};

export const PAGE_SIZE = 12;
