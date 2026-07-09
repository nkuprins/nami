import type { ComputedRef, Ref } from 'vue';
import type {
  Feature,
  ListingDetail,
  PropertyDetails,
  PropertyMedia,
} from '../../../types/listingItem';
import type { PropertyFieldsForm } from './formTypes';
import { parseDecimal } from '../../../utils/utils';
import { requestPresignedUrls, uploadFilesToS3 } from '../../../api/uploadApi';

// Shared by every create/edit form composable so `touched`-gated field
// errors behave identically everywhere.
export function makeFieldError(
  touched: Ref<boolean>,
  errors: ComputedRef<Record<string, string>>
) {
  return (field: string): string =>
    touched.value ? (errors.value[field] ?? '') : '';
}

export function addPhone(form: { phones: string[] }): void {
  form.phones.push('');
}

export function removePhone(form: { phones: string[] }, index: number): void {
  form.phones.splice(index, 1);
}

export function toggleFeature(form: { features: Feature[] }, f: Feature): void {
  const i = form.features.indexOf(f);
  if (i === -1) form.features.push(f);
  else form.features.splice(i, 1);
}

// Empty baseline for the physical/media slice of a form. Spread into the create
// form (with location), the listing edit form and the add-another-listing form.
export const INITIAL_PROPERTY_FIELDS: PropertyFieldsForm = {
  propertyKind: 'apartment',
  address: '',
  rooms: '',
  bedrooms: '',
  bathrooms: '',
  bathroomLayout: '',
  m2: '',
  landM2: '',
  floor: '',
  totalFloors: '',
  yearBuilt: '',
  heating: '',
  energyClass: '',
  maintenanceCost: '',
  features: [],
  videoUrl: '',
  coords: null,
};

export function buildDetails(form: PropertyFieldsForm): PropertyDetails {
  return {
    rooms: Number(form.rooms),
    bedrooms: form.bedrooms ? Number(form.bedrooms) : undefined,
    bathrooms: form.bathrooms ? Number(form.bathrooms) : undefined,
    bathroomLayout: form.bathroomLayout || undefined,
    m2: parseDecimal(form.m2),
    landM2:
      form.propertyKind === 'house' && form.landM2
        ? parseDecimal(form.landM2)
        : undefined,
    floor: form.floor ? Number(form.floor) : undefined,
    totalFloors: form.totalFloors ? Number(form.totalFloors) : undefined,
    yearBuilt: form.yearBuilt ? Number(form.yearBuilt) : undefined,
    heating: form.heating || undefined,
    energyClass: form.energyClass || undefined,
    maintenanceCost: form.maintenanceCost
      ? parseDecimal(form.maintenanceCost)
      : undefined,
  };
}

export function buildMedia(
  form: PropertyFieldsForm,
  photos: string[],
  plans: string[]
): PropertyMedia {
  return {
    photos,
    plans: plans.length ? plans : null,
    videoUrl: form.videoUrl.trim() || null,
  };
}

// Seeds the physical/media slice of a form from a loaded listing — used by the
// listing edit form and (from a sibling) the add-another-listing form.
export function seedPropertyFields(
  form: PropertyFieldsForm,
  listing: ListingDetail
): void {
  const d = listing.details;
  form.propertyKind = listing.propertyKind;
  form.rooms = String(d.rooms);
  form.bedrooms = d.bedrooms != null ? String(d.bedrooms) : '';
  form.bathrooms = d.bathrooms != null ? String(d.bathrooms) : '';
  form.bathroomLayout = d.bathroomLayout ?? '';
  form.m2 = String(d.m2);
  form.landM2 = d.landM2 != null ? String(d.landM2) : '';
  form.floor = d.floor != null ? String(d.floor) : '';
  form.totalFloors = d.totalFloors != null ? String(d.totalFloors) : '';
  form.yearBuilt = d.yearBuilt != null ? String(d.yearBuilt) : '';
  form.heating = d.heating ?? '';
  form.energyClass = d.energyClass ?? '';
  form.maintenanceCost =
    d.maintenanceCost != null ? String(d.maintenanceCost) : '';
  form.features = listing.features ? [...listing.features] : [];
  form.videoUrl = listing.media.videoUrl ?? '';
}

export async function uploadNewFiles(files: File[]): Promise<string[]> {
  const slots = await requestPresignedUrls(files.map((f) => f.name));
  return uploadFilesToS3(files, slots);
}

// Validation for the physical/media slice of the form, shared by the create
// form, the listing edit form and the add-another-listing form. `requireLocation`
// is false for the listing-scoped forms, which inherit the property's location.
export function propertyFieldErrors(
  form: PropertyFieldsForm,
  opts: { hasLocation: boolean; hasPhotos: boolean; requireLocation?: boolean }
): Record<string, string> {
  const e: Record<string, string> = {};
  if (opts.requireLocation !== false) {
    if (!opts.hasLocation) e.district = 'Required';
    if (!form.address.trim()) e.address = 'Required';
  }
  if (!form.rooms || Number.isNaN(Number(form.rooms)) || Number(form.rooms) < 1)
    e.rooms = 'Enter number of rooms';
  if (
    !form.m2 ||
    Number.isNaN(parseDecimal(form.m2)) ||
    parseDecimal(form.m2) <= 0
  )
    e.m2 = 'Enter area in m²';
  if (!opts.hasPhotos) e.photos = 'At least one photo required';
  return e;
}
