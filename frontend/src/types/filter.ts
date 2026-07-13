import {
  BathroomLayout,
  Communication,
  EnergyClass,
  Feature,
  HeatingType,
  ParkingType,
  PropertyCompletion,
  PropertyExtra,
  PropertyKind,
  ListingType,
  RoofType,
  SecurityFeature,
  SewageType,
  StoveType,
  VentilationSystem,
  VentilationType,
} from './listingItem';
import { SortKey } from './sort';
import { Location } from '../data/rawLocations';

export interface FilterState {
  type: ListingType;
  kind?: PropertyKind;
  loc: Location[];
  priceMin?: number;
  priceMax?: number;
  rooms: number[];
  bedrooms: number[];
  bathrooms: number[];
  m2Min?: number;
  m2Max?: number;
  landM2Min?: number;
  landM2Max?: number;
  floorMin?: number;
  floorMax?: number;
  notGround?: boolean;
  notTop?: boolean;
  yearMin?: number;
  yearMax?: number;
  maintenanceCostMax?: number;
  bathroomLayout?: BathroomLayout;
  vatIncluded?: boolean;
  heating: HeatingType[];
  energyClass: EnergyClass[];
  sewage: SewageType[];
  ventilation: VentilationType[];
  roof: RoofType[];
  features: Feature[];
  ventilationSystems: VentilationSystem[];
  communications: Communication[];
  stove: StoveType[];
  security: SecurityFeature[];
  extras: PropertyExtra[];
  parking: ParkingType[];
  completion?: PropertyCompletion;
  sort: SortKey;
  page: number;
}

export type FilterKey = keyof FilterState;
export const ALL_FILTER_KEYS = Object.keys({
  type: 0,
  kind: 0,
  loc: 0,
  priceMin: 0,
  priceMax: 0,
  rooms: 0,
  bedrooms: 0,
  bathrooms: 0,
  m2Min: 0,
  m2Max: 0,
  landM2Min: 0,
  landM2Max: 0,
  floorMin: 0,
  floorMax: 0,
  notGround: 0,
  notTop: 0,
  yearMin: 0,
  yearMax: 0,
  maintenanceCostMax: 0,
  bathroomLayout: 0,
  vatIncluded: 0,
  heating: 0,
  energyClass: 0,
  sewage: 0,
  ventilation: 0,
  roof: 0,
  features: 0,
  ventilationSystems: 0,
  communications: 0,
  stove: 0,
  security: 0,
  extras: 0,
  parking: 0,
  completion: 0,
  sort: 0,
  page: 0,
} satisfies Record<FilterKey, 0>) as FilterKey[];

export const DEFAULT_FILTER_STATE: FilterState = {
  type: 'buy',
  kind: 'house',
  loc: [],
  rooms: [],
  bedrooms: [],
  bathrooms: [],
  heating: [],
  energyClass: [],
  sewage: [],
  ventilation: [],
  roof: [],
  features: [],
  ventilationSystems: [],
  communications: [],
  stove: [],
  security: [],
  extras: [],
  parking: [],
  sort: 'newest',
  page: 1,
};

export const PAGE_SIZE = 12;

// Highest room / bedroom / bathroom bucket
// The top value means "N or more" (rendered as "N+")
export const ROOM_COUNT_MAX = 7;

export const ROOM_COUNT_OPTIONS: number[] = Array.from(
  { length: ROOM_COUNT_MAX },
  (_, i) => i + 1
);

export function roomCountLabel(n: number): string {
  return n >= ROOM_COUNT_MAX ? `${n}+` : String(n);
}
