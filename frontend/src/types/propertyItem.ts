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

interface BasePropertyItem {
  id: string;
  type: PropertyType;
  title: string;
  description: string;
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
  coords: { lat: number; lng: number };
  photos: string[];
  videoUrl?: string;
  postedAt: string;
  completion?: PropertyCompletion;
}

export interface ApartmentItem extends BasePropertyItem {
  propertyKind: 'apartment';
}

export interface HouseItem extends BasePropertyItem {
  propertyKind: 'house';
}

export type PropertyItem = ApartmentItem | HouseItem;
