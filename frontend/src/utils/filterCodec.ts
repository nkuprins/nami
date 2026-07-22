import type { LocationQuery } from 'vue-router';
import { DEFAULT_FILTER_STATE } from '../types/filter';
import { type FilterState, type StreetFilter } from '../types/filter';
import {
  BathroomLayout,
  Category,
  CommercialType,
  Communication,
  EnergyClass,
  Feature,
  HeatingType,
  KNOWN_BATHROOM_LAYOUT,
  KNOWN_CATEGORIES,
  KNOWN_COMMERCIAL_TYPES,
  KNOWN_COMMUNICATIONS,
  KNOWN_COMPLETION,
  KNOWN_ENERGY_CLASS,
  KNOWN_EXTRAS,
  KNOWN_FEATURES,
  KNOWN_HEATING,
  KNOWN_LAND_USE,
  KNOWN_PARKING,
  KNOWN_ROOF,
  KNOWN_SECURITY,
  KNOWN_SEWAGE,
  KNOWN_STOVE,
  KNOWN_TYPES,
  KNOWN_VENTILATION,
  KNOWN_VENTILATION_SYSTEMS,
  LandUse,
  ParkingType,
  PropertyCompletion,
  PropertyExtra,
  RoofType,
  SecurityFeature,
  SewageType,
  StoveType,
  VentilationSystem,
  VentilationType,
} from '../types/listingItem';
import { KNOWN_SORTS } from '../types/sort';
import { Location } from '../data/rawLocations';
import { cityBySlug, districtNameBySlug } from '../data/locations';

const parse = {
  string(v: unknown): string {
    if (typeof v === 'string') return v;
    if (Array.isArray(v) && v.length > 0)
      return typeof v[0] === 'string' ? v[0] : '';
    return '';
  },

  number(v: unknown): number | undefined {
    if (v === undefined || v === null || v === '') return undefined;
    const normalized = typeof v === 'string' ? v : parse.string(v);
    const n = Number(normalized);
    return Number.isFinite(n) && n >= 0 ? Math.floor(n) : undefined;
  },

  boolean(v: unknown): boolean | undefined {
    return parse.string(v) === '1' ? true : undefined;
  },

  list(v: unknown): string[] {
    const str = typeof v === 'string' ? v : parse.string(v);
    if (!str) return [];
    return str
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
  },

  enum<T extends string>(v: unknown, allowed: readonly T[], fallback: T): T {
    const raw = parse.string(v);
    return allowed.includes(raw as T) ? (raw as T) : fallback;
  },

  optionalEnum<T extends string>(
    v: unknown,
    allowed: readonly T[]
  ): T | undefined {
    const raw = parse.string(v);
    return allowed.includes(raw as T) ? (raw as T) : undefined;
  },

  numericList(v: unknown): number[] {
    return parse
      .list(v)
      .map(parse.number)
      .filter((n): n is number => n !== undefined);
  },

  enumList<T extends string>(v: unknown, allowed: readonly T[]): T[] {
    return parse.list(v).filter((s): s is T => allowed.includes(s as T));
  },

  locationList(v: unknown): Location[] {
    return parse
      .list(v)
      .map((item) => {
        const parts = item.split(':');
        if (parts.length !== 2) return undefined;

        const [cSlug, dSlug] = parts;
        if (!cityBySlug.has(cSlug) || !districtNameBySlug.has(dSlug))
          return undefined;

        return { city: cSlug, district: dSlug };
      })
      .filter((l): l is Location => l !== undefined);
  },

  // Each entry is `code:name` (the register street code plus its display name).
  streetList(v: unknown): StreetFilter[] {
    return parse
      .list(v)
      .map((item) => {
        const colon = item.indexOf(':');
        if (colon <= 0 || colon >= item.length - 1) return undefined;

        const code = Number(item.slice(0, colon));
        const name = item.slice(colon + 1).trim();
        if (!Number.isInteger(code) || code <= 0 || !name) return undefined;

        return { code, name };
      })
      .filter((s): s is StreetFilter => s !== undefined);
  },
};

export const FilterCodec = {
  fromQuery(q: LocationQuery): FilterState {
    const defaults = DEFAULT_FILTER_STATE;

    return {
      // Strict Enums & Union Layouts (No manual inline casting!)
      type: parse.enum(q.type, KNOWN_TYPES, defaults.type),
      sort: parse.enum(q.sort, KNOWN_SORTS, defaults.sort),
      completion: parse.optionalEnum(q.completion, KNOWN_COMPLETION) as
        PropertyCompletion | undefined,
      bathroomLayout: parse.optionalEnum(
        q.bathroomLayout,
        KNOWN_BATHROOM_LAYOUT
      ) as BathroomLayout | undefined,
      kind: (parse.optionalEnum(q.kind, KNOWN_CATEGORIES) ?? defaults.kind) as
        Category | undefined,
      commercialSubtype: parse.optionalEnum(
        q.commercialSubtype,
        KNOWN_COMMERCIAL_TYPES
      ) as CommercialType | undefined,
      landUse: parse.optionalEnum(q.landUse, KNOWN_LAND_USE) as
        LandUse | undefined,

      // Arrays & CSV Collections
      loc: parse.locationList(q.loc),
      streets: parse.streetList(q.street),
      features: parse.enumList(q.features, KNOWN_FEATURES) as Feature[],
      rooms: parse.numericList(q.rooms),
      bedrooms: parse.numericList(q.bedrooms),
      bathrooms: parse.numericList(q.bathrooms),
      heating: parse.enumList(q.heating, KNOWN_HEATING) as HeatingType[],
      energyClass: parse.enumList(
        q.energyClass,
        KNOWN_ENERGY_CLASS
      ) as EnergyClass[],
      sewage: parse.enumList(q.sewage, KNOWN_SEWAGE) as SewageType[],
      ventilation: parse.enumList(
        q.ventilation,
        KNOWN_VENTILATION
      ) as VentilationType[],
      roof: parse.enumList(q.roof, KNOWN_ROOF) as RoofType[],
      ventilationSystems: parse.enumList(
        q.ventilationSystems,
        KNOWN_VENTILATION_SYSTEMS
      ) as VentilationSystem[],
      communications: parse.enumList(
        q.communications,
        KNOWN_COMMUNICATIONS
      ) as Communication[],
      stove: parse.enumList(q.stove, KNOWN_STOVE) as StoveType[],
      security: parse.enumList(q.security, KNOWN_SECURITY) as SecurityFeature[],
      extras: parse.enumList(q.extras, KNOWN_EXTRAS) as PropertyExtra[],
      parking: parse.enumList(q.parking, KNOWN_PARKING) as ParkingType[],

      // Range Parameters (Numbers)
      priceMin: parse.number(q.priceMin),
      priceMax: parse.number(q.priceMax),
      m2Min: parse.number(q.m2Min),
      m2Max: parse.number(q.m2Max),
      landM2Min: parse.number(q.landM2Min),
      landM2Max: parse.number(q.landM2Max),
      floorMin: parse.number(q.floorMin),
      floorMax: parse.number(q.floorMax),
      yearMin: parse.number(q.yearMin),
      yearMax: parse.number(q.yearMax),
      maintenanceCostMax: parse.number(q.maintenanceCostMax),

      // Boolean Flags & Pagination Guards
      notGround: parse.boolean(q.notGround),
      notTop: parse.boolean(q.notTop),
      vatIncluded: parse.boolean(q.vatIncluded),
      page: Math.max(1, parse.number(q.page) ?? 1),
    };
  },

  toQuery(state: FilterState): LocationQuery {
    const q: LocationQuery = {};
    const defaults = DEFAULT_FILTER_STATE;

    if (state.type !== defaults.type) q.type = state.type;
    if (state.kind && state.kind !== defaults.kind) q.kind = state.kind;
    if (state.commercialSubtype) q.commercialSubtype = state.commercialSubtype;
    if (state.landUse) q.landUse = state.landUse;

    if (state.loc.length) {
      q.loc = state.loc.map((l) => `${l.city}:${l.district}`).join(',');
    }

    if (state.streets.length) {
      q.street = state.streets.map((s) => `${s.code}:${s.name}`).join(',');
    }

    const listKeys = [
      'rooms',
      'bedrooms',
      'bathrooms',
      'heating',
      'energyClass',
      'sewage',
      'ventilation',
      'roof',
      'features',
      'ventilationSystems',
      'communications',
      'stove',
      'security',
      'extras',
      'parking',
    ] as const;
    for (const key of listKeys) {
      const value = state[key];
      if (value.length) q[key] = value.join(',');
    }

    const numericKeys = [
      'priceMin',
      'priceMax',
      'm2Min',
      'm2Max',
      'landM2Min',
      'landM2Max',
      'floorMin',
      'floorMax',
      'yearMin',
      'yearMax',
      'maintenanceCostMax',
    ] as const;
    for (const key of numericKeys) {
      const value = state[key];
      if (value !== undefined) q[key] = String(value);
    }

    if (state.notGround) q.notGround = '1';
    if (state.notTop) q.notTop = '1';
    if (state.vatIncluded) q.vatIncluded = '1';
    if (state.bathroomLayout) q.bathroomLayout = state.bathroomLayout;
    if (state.completion) q.completion = state.completion;
    if (state.sort !== defaults.sort) q.sort = state.sort;
    if (state.page > 1) q.page = String(state.page);

    return q;
  },

  isEqual(a: LocationQuery, b: LocationQuery): boolean {
    const ka = Object.keys(a);
    if (ka.length !== Object.keys(b).length) return false;
    return ka.every((k) => String(a[k] ?? '') === String(b[k] ?? ''));
  },
};
