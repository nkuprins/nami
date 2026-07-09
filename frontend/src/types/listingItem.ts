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
] as const;
export const KNOWN_HEATING = [
  'central',
  'gas',
  'electric',
  'heat_pump',
  'solid_fuel',
  'none',
] as const;
export const KNOWN_ENERGY_CLASS = ['A', 'B', 'C', 'D', 'E', 'F', 'G'] as const;
export const KNOWN_BATHROOM_LAYOUT = ['separate', 'combined'] as const;

export type ListingType = (typeof KNOWN_TYPES)[number];
export type PropertyKind = (typeof KNOWN_KINDS)[number];
export type PropertyCompletion = (typeof KNOWN_COMPLETION)[number];
export type Feature = (typeof KNOWN_FEATURES)[number];
export type HeatingType = (typeof KNOWN_HEATING)[number];
export type EnergyClass = (typeof KNOWN_ENERGY_CLASS)[number];
export type BathroomLayout = (typeof KNOWN_BATHROOM_LAYOUT)[number];

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
  coords: { lat: number; lng: number } | null; // null on list cards
}

export interface PropertyMedia {
  photos: string[] | null;
  plans: string[] | null;
  videoUrl: string | null;
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

export function hasLanguage(
  item: Pick<ListingBase, 'translations'>,
  locale: Locale
): boolean {
  const entry = item.translations?.[locale];
  return Boolean(entry?.title && entry?.description);
}

// A listing is self-contained, so an address can host any number of listings of
// any type (sell the house + rent floor 2 + rent floor 3). No type is ever
// excluded — every type is always offerable.
export function offerableListingTypes(): ListingType[] {
  return [...KNOWN_TYPES];
}
