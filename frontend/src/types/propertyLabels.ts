import type {
  Feature,
  PropertyCompletion,
  PropertyKind,
  PropertyType,
} from './propertyItem';
import { KNOWN_TYPES } from './propertyItem';

export const TYPES_LABELS: Record<PropertyType, string> = {
  buy: 'For sale',
  rent: 'For rent',
  new_project: 'New project',
};

export const KIND_LABELS: Record<PropertyKind, string> = {
  apartment: 'Apartment',
  house: 'House',
};

export const COMPLETION_LABELS: Record<PropertyCompletion, string> = {
  ready: 'Ready',
  not_ready: 'Under construction',
};

export const FEATURE_LABELS: Record<Feature, string> = {
  balcony: 'Balcony',
  parking: 'Parking',
  elevator: 'Elevator',
  furnished: 'Furnished',
  pets: 'Pets allowed',
  new_building: 'New building',
};

export const TYPE_OPTIONS = Object.entries(TYPES_LABELS).map(([id, label]) => ({
  id: id as PropertyType,
  label,
}));

const CATEGORY_META: Record<PropertyType, { label: string; hint: string }> = {
  buy: { label: 'Buy', hint: 'For sale' },
  rent: { label: 'Rent', hint: 'Monthly' },
  new_project: { label: 'New projects', hint: '' },
};
export const CATEGORY_OPTIONS = Object.entries(CATEGORY_META).map(
  ([id, meta]) => ({ id: id as PropertyType, ...meta })
);

export const KIND_OPTIONS = Object.entries(KIND_LABELS).map(([id, label]) => ({
  id: id as PropertyKind,
  label,
}));

export const COMPLETION_OPTIONS = Object.entries(COMPLETION_LABELS).map(
  ([id, label]) => ({ id: id as PropertyCompletion, label })
);

export const FEATURE_OPTIONS = Object.entries(FEATURE_LABELS).map(
  ([id, label]) => ({ id: id as Feature, label })
);
