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
  descriptionLv?: string;
  descriptionEn?: string;
  price: number;
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
  completion?: PropertyCompletion;
}

export interface PropertySummary extends PropertyBase {
  photo: string;
}

export interface PropertyDetail extends PropertyBase {
  coords: { lat: number; lng: number };
  phones?: string[];
  photos: string[];
  videoUrl?: string;
}

export type PropertyItem = PropertyDetail;

export function resolveTitle(
  item: Pick<PropertyBase, 'titleLv' | 'titleEn'>,
  locale: 'lv' | 'en'
): string {
  if (locale === 'en') return item.titleEn ?? item.titleLv ?? '';
  return item.titleLv ?? item.titleEn ?? '';
}

export function resolveDescription(
  item: Pick<PropertyBase, 'descriptionLv' | 'descriptionEn'>,
  locale: 'lv' | 'en'
): string {
  if (locale === 'en') return item.descriptionEn ?? item.descriptionLv ?? '';
  return item.descriptionLv ?? item.descriptionEn ?? '';
}

export function hasLanguage(
  item: Pick<
    PropertyBase,
    'titleLv' | 'titleEn' | 'descriptionLv' | 'descriptionEn'
  >,
  locale: 'lv' | 'en'
): boolean {
  if (locale === 'en') return Boolean(item.titleEn && item.descriptionEn);
  return Boolean(item.titleLv && item.descriptionLv);
}
