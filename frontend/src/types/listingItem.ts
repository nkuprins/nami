export const KNOWN_TYPES = ['buy', 'rent', 'new_project'] as const;
export const KNOWN_KINDS = ['apartment', 'house'] as const;
export const KNOWN_COMPLETION = ['ready', 'not_ready'] as const;
export const KNOWN_FEATURES = [
  'balcony',
  'parking',
  'elevator',
  'furnished',
  'pets',
  'new_building',
  'basement',
  'renovated',
  'air_conditioning',
  'terrace',
  'sauna',
  'fireplace',
  'underfloor_heating',
  'individual_meters',
  'storage_room',
  'walk_in_closet',
  'pool',
  'bathtub',
  'shower',
  'washing_machine',
  'boiler',
  'glazed_balcony',
  'french_balcony',
  'loggia',
] as const;
export const KNOWN_SEWAGE = ['central', 'local'] as const;
export const KNOWN_VENTILATION = [
  'natural',
  'mechanical',
  'recuperation',
] as const;
export const KNOWN_HEATING = [
  'central',
  'central_gas',
  'gas',
  'electric',
  'heat_pump',
  'air_water_heat_pump',
  'geothermal',
  'solid_fuel',
  'stove',
  'combined',
  'none',
] as const;
export const KNOWN_ENERGY_CLASS = ['A', 'B', 'C', 'D', 'E', 'F', 'G'] as const;
export const KNOWN_BATHROOM_LAYOUT = ['separate', 'combined'] as const;
export const KNOWN_ROOF = [
  'bitumen',
  'eternit',
  'pvc',
  'roll_material',
  'steel',
  'stone',
  'tile',
  'white_tin',
  'zinc_plate',
] as const;
export const KNOWN_VENTILATION_SYSTEMS = [
  'climate_control',
  'supply_ventilation',
  'air_conditioner',
] as const;
export const KNOWN_COMMUNICATIONS = [
  'cable_tv',
  'internet',
  'telephone',
  'digital_tv',
] as const;
export const KNOWN_STOVE = ['electric_stove', 'wood_burning', 'gas_stove'] as const;
export const KNOWN_SECURITY = [
  'locking_entrance',
  'guard',
  'security_system',
  'steel_door',
  'video_cameras',
] as const;
export const KNOWN_EXTRAS = [
  'separate_entrance',
  'enclosed_yard',
  'private_garden',
  'furniture',
  'furniture_possible',
] as const;
export const KNOWN_PARKING = [
  'free_parking',
  'paid_parking',
  'no_parking',
  'underground_parking',
  'own_parking_space',
] as const;

export type ListingType = (typeof KNOWN_TYPES)[number];
export type PropertyKind = (typeof KNOWN_KINDS)[number];
export type PropertyCompletion = (typeof KNOWN_COMPLETION)[number];
export type Feature = (typeof KNOWN_FEATURES)[number];
export type HeatingType = (typeof KNOWN_HEATING)[number];
export type EnergyClass = (typeof KNOWN_ENERGY_CLASS)[number];
export type BathroomLayout = (typeof KNOWN_BATHROOM_LAYOUT)[number];
export type SewageType = (typeof KNOWN_SEWAGE)[number];
export type VentilationType = (typeof KNOWN_VENTILATION)[number];
export type RoofType = (typeof KNOWN_ROOF)[number];
export type VentilationSystem = (typeof KNOWN_VENTILATION_SYSTEMS)[number];
export type Communication = (typeof KNOWN_COMMUNICATIONS)[number];
export type StoveType = (typeof KNOWN_STOVE)[number];
export type SecurityFeature = (typeof KNOWN_SECURITY)[number];
export type PropertyExtra = (typeof KNOWN_EXTRAS)[number];
export type ParkingType = (typeof KNOWN_PARKING)[number];

export interface PriceInfo {
  amount: number;
  vatIncluded?: boolean;
}

export interface PropertyDetails {
  rooms: number;
  bedrooms?: number;
  bathrooms?: number;
  bathroomLayout?: BathroomLayout;
  m2: number;
  landM2?: number;
  floor?: number;
  totalFloors?: number;
  yearBuilt?: number;
  heating?: HeatingType;
  energyClass?: EnergyClass;
  maintenanceCost?: number;
  sewage?: SewageType;
  ventilation?: VentilationType;
  roof?: RoofType;
}

export interface LocalizedText {
  title: string;
  description?: string;
}

// Keys are locale codes ('lv' | 'en' | 'ru'); only present locales are included.
export type Translations = Record<string, LocalizedText>;

export interface PropertyLocation {
  district: string; // slug on the wire, display name once translated
  city: string; // slug on the wire, display name once translated
  address: string;
  // State Address Register building the address was picked from, plus the
  // free-typed apartment. Absent on legacy free-text addresses.
  arBuildingCode?: number | null;
  apartment?: string | null;
  coords: { lat: number; lng: number } | null; // null on list cards
}

export interface PropertyMedia {
  photos: string[] | null;
  plans: string[] | null;
  videoUrl: string | null;
  websiteUrl: string | null;
}

interface ListingBase {
  id: string; // listing id
  propertyId: string; // id of the underlying physical property
  ownerId?: string;
  type: ListingType;
  propertyKind: PropertyKind;
  price: PriceInfo;
  details: PropertyDetails;
  translations: Translations;
  location: PropertyLocation;
  features: Feature[] | null;
  ventilationSystems: VentilationSystem[] | null;
  communications: Communication[] | null;
  stove: StoveType[] | null;
  security: SecurityFeature[] | null;
  extras: PropertyExtra[] | null;
  parking: ParkingType[] | null;
  postedAt: string;
  expiresAt?: string;
  completion?: PropertyCompletion;
}

export interface ListingSummary extends ListingBase {
  photo: string | null;
}

export interface ListingDetail extends ListingBase {
  media: PropertyMedia;
  phones: string[] | null;
  // Locales the listing actually has. Present when the detail was fetched for a
  // single locale (display); absent on the all-locales edit fetch.
  availableLocales?: string[];
}

export type ListingItem = ListingDetail;

// The owner's editable address record. Every physical/media attribute lives on
// the listing now, so the property carries only its shared location.
export interface PropertyDetail {
  id: string; // property id
  ownerId: string;
  location: PropertyLocation;
}

import type { Locale } from '../i18n';

const LOCALE_FALLBACK_ORDER: Record<Locale, Locale[]> = {
  lv: ['lv', 'en', 'ru'],
  en: ['en', 'lv', 'ru'],
  ru: ['ru', 'lv', 'en'],
};

export function resolveTitle(
  item: Pick<ListingBase, 'translations'>,
  locale: Locale
): string {
  for (const l of LOCALE_FALLBACK_ORDER[locale]) {
    const title = item.translations?.[l]?.title;
    if (title) return title;
  }
  return '';
}

export function resolveDescription(
  item: Pick<ListingBase, 'translations'>,
  locale: Locale
): string {
  for (const l of LOCALE_FALLBACK_ORDER[locale]) {
    const description = item.translations?.[l]?.description;
    if (description) return description;
  }
  return '';
}

// A listing is self-contained, so an address can host any number of listings of
// any type (sell the house + rent floor 2 + rent floor 3). No type is ever
// excluded — every type is always offerable.
export function offerableListingTypes(): ListingType[] {
  return [...KNOWN_TYPES];
}
