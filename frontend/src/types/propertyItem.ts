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

export type PropertyType = (typeof KNOWN_TYPES)[number];
export type PropertyKind = (typeof KNOWN_KINDS)[number];
export type PropertyCompletion = (typeof KNOWN_COMPLETION)[number];
export type Feature = (typeof KNOWN_FEATURES)[number];

interface PropertyBase {
  id: string;
  ownerId?: string;
  type: PropertyType;
  propertyKind: PropertyKind;
  titleLv?: string;
  titleEn?: string;
  titleRu?: string;
  descriptionLv?: string;
  descriptionEn?: string;
  descriptionRu?: string;
  price: number;
  buyVatIncluded?: boolean;
  rentPrice?: number;
  rentVatIncluded?: boolean;
  rooms: number;
  m2: number;
  landM2?: number;
  floor?: number;
  totalFloors?: number;
  yearBuilt?: number;
  features: Feature[];
  district: string;
  city: string;
  address: string;
  postedAt: string;
  expiresAt?: string;
  completion?: PropertyCompletion;
}

export interface PropertySummary extends PropertyBase {
  photo: string;
}

export interface PropertyDetail extends PropertyBase {
  coords: { lat: number; lng: number };
  phones?: string[];
  photos: string[];
  plans?: string[];
  videoUrl?: string;
}

export type PropertyItem = PropertyDetail;

import type { Locale } from '../i18n';

export function resolveTitle(
  item: Pick<PropertyBase, 'titleLv' | 'titleEn' | 'titleRu'>,
  locale: Locale
): string {
  if (locale === 'en')
    return item.titleEn ?? item.titleLv ?? item.titleRu ?? '';
  if (locale === 'ru')
    return item.titleRu ?? item.titleLv ?? item.titleEn ?? '';
  return item.titleLv ?? item.titleEn ?? item.titleRu ?? '';
}

export function resolveDescription(
  item: Pick<PropertyBase, 'descriptionLv' | 'descriptionEn' | 'descriptionRu'>,
  locale: Locale
): string {
  if (locale === 'en')
    return item.descriptionEn ?? item.descriptionLv ?? item.descriptionRu ?? '';
  if (locale === 'ru')
    return item.descriptionRu ?? item.descriptionLv ?? item.descriptionEn ?? '';
  return item.descriptionLv ?? item.descriptionEn ?? item.descriptionRu ?? '';
}

export function hasLanguage(
  item: Pick<
    PropertyBase,
    | 'titleLv'
    | 'titleEn'
    | 'titleRu'
    | 'descriptionLv'
    | 'descriptionEn'
    | 'descriptionRu'
  >,
  locale: Locale
): boolean {
  if (locale === 'en') return Boolean(item.titleEn && item.descriptionEn);
  if (locale === 'ru') return Boolean(item.titleRu && item.descriptionRu);
  return Boolean(item.titleLv && item.descriptionLv);
}
