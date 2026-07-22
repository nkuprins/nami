import type {
  Feature,
  PropertyCompletion,
  PropertyKind,
  ListingType,
} from './listingItem';

export const TYPES_LABELS: Record<ListingType, string> = {
  buy: 'For sale',
  rent: 'For rent',
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
  basement: 'Basement',
  renovated: 'Renovated',
  air_conditioning: 'Air conditioning',
  terrace: 'Terrace',
  sauna: 'Sauna',
  fireplace: 'Fireplace',
  underfloor_heating: 'Underfloor heating',
  individual_meters: 'Individual meters',
  storage_room: 'Storage room',
  walk_in_closet: 'Walk-in closet',
  pool: 'Swimming pool',
  bathtub: 'Bathtub',
  shower: 'Shower',
  washing_machine: 'Washing machine',
  boiler: 'Water heater',
  glazed_balcony: 'Glazed balcony',
  french_balcony: 'French balcony',
  loggia: 'Loggia',
};

export const TYPE_OPTIONS = Object.entries(TYPES_LABELS).map(([id, label]) => ({
  id: id as ListingType,
  label,
}));

const CATEGORY_META: Record<ListingType, { label: string; hint: string }> = {
  buy: { label: 'Buy', hint: 'For sale' },
  rent: { label: 'Rent', hint: 'Monthly' },
};
export const CATEGORY_OPTIONS = Object.entries(CATEGORY_META).map(
  ([id, meta]) => ({ id: id as ListingType, ...meta })
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
