import { computed, nextTick, type ComputedRef, type Ref } from 'vue';
import type {
  Feature,
  ListingDetail,
  PropertyDetails,
  PropertyMedia,
} from '../../../types/listingItem';
import type {
  ListingFieldsForm,
  ListingFormState,
  PropertyFieldsForm,
} from './formTypes';
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

// Derived state and mutators shared verbatim by all three listing form
// composables (create, edit, add-another). Given the reactive form, its
// `touched` flag and the merged `errors`, it wires validity, the touched-gated
// field-error accessor, the feature/phone mutators, and the submit-time
// validation gate — which marks the form touched, scrolls to the first error
// if invalid, and reports whether the caller should proceed with submission.
export function useListingFormControls(
  form: ListingFormState,
  touched: Ref<boolean>,
  errors: ComputedRef<Record<string, string>>
) {
  const isValid = computed(() => Object.keys(errors.value).length === 0);
  const fieldError = makeFieldError(touched, errors);

  return {
    isValid,
    fieldError,
    toggleFeature: (f: Feature) => toggleFeature(form, f),
    addPhone: () => addPhone(form),
    removePhone: (index: number) => removePhone(form, index),
    async validateForSubmit(): Promise<boolean> {
      touched.value = true;
      if (!isValid.value) {
        await nextTick();
        document
          .querySelector('.text-xs.text-warn')
          ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return false;
      }
      return true;
    },
  };
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

// The shared `price` field means "rent price" only in single-listing rent
// mode; everywhere else (buy, new_project, or the buy leg of dual buy+rent
// mode) it means "sale price". Crossing that boundary must clear the field —
// otherwise a price typed for one meaning is silently reused for the other.
function priceMeaning(
  type: ListingFieldsForm['type'],
  alsoRent: boolean
): 'rent' | 'sale' {
  return type === 'rent' && !alsoRent ? 'rent' : 'sale';
}

export function setTransactionType(
  form: Pick<ListingFieldsForm, 'type' | 'alsoRent' | 'price' | 'vatIncluded'>,
  mutate: () => void
): void {
  const before = priceMeaning(form.type, form.alsoRent);
  mutate();
  const after = priceMeaning(form.type, form.alsoRent);
  if (before !== after) {
    form.price = '';
    form.vatIncluded = false;
  }
}

// The register building the address form resolves to: the picked house number,
// or the street pick itself when it is a rural named house.
export function selectedBuildingCode(
  form: Pick<PropertyFieldsForm, 'street' | 'building'>
): number | undefined {
  if (form.building) return form.building.code;
  if (form.street?.kind === 'house') return form.street.code;
  return undefined;
}

// Display address composed from the register picks, mirroring the backend's
// canonical form: "Brīvības iela 12 - 4", or a quoted rural house name.
export function composeAddress(
  form: Pick<PropertyFieldsForm, 'street' | 'building' | 'apartment'>
): string {
  if (!form.street) return '';
  const base =
    form.street.kind === 'house'
      ? `"${form.street.name}"`
      : form.building
        ? `${form.street.name} ${form.building.name}`
        : form.street.name;
  const apartment = form.apartment.trim();
  return apartment ? `${base} - ${apartment}` : base;
}

// Official coordinates of the picked building, when the register has them.
export function registerCoords(
  form: Pick<PropertyFieldsForm, 'street' | 'building'>
): { lat: number; lng: number } | null {
  const source = form.building ?? (form.street?.kind === 'house' ? form.street : null);
  return source?.lat != null && source.lng != null
    ? { lat: source.lat, lng: source.lng }
    : null;
}

// Empty baseline for the physical/media slice of a form. Spread into the create
// form (with location), the listing edit form and the add-another-listing form.
export const INITIAL_PROPERTY_FIELDS: PropertyFieldsForm = {
  propertyKind: 'apartment',
  address: '',
  street: null,
  building: null,
  apartment: '',
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
  sewage: '',
  ventilation: '',
  roof: '',
  features: [],
  ventilationSystems: [],
  communications: [],
  stove: [],
  security: [],
  extras: [],
  parking: [],
  videoUrl: '',
  websiteUrl: '',
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
    sewage: form.sewage || undefined,
    ventilation: form.ventilation || undefined,
    roof: form.roof || undefined,
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
    websiteUrl: form.websiteUrl.trim() || null,
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
  form.sewage = d.sewage ?? '';
  form.ventilation = d.ventilation ?? '';
  form.roof = d.roof ?? '';
  form.features = listing.features ? [...listing.features] : [];
  form.ventilationSystems = listing.ventilationSystems
    ? [...listing.ventilationSystems]
    : [];
  form.communications = listing.communications
    ? [...listing.communications]
    : [];
  form.stove = listing.stove ? [...listing.stove] : [];
  form.security = listing.security ? [...listing.security] : [];
  form.extras = listing.extras ? [...listing.extras] : [];
  form.parking = listing.parking ? [...listing.parking] : [];
  form.videoUrl = listing.media.videoUrl ?? '';
  form.websiteUrl = listing.media.websiteUrl ?? '';
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
    if (!form.street) e.street = 'Pick a street from the list';
    // A rural named house is terminal; a street needs its house number picked.
    else if (form.street.kind === 'street' && !form.building)
      e.building = 'Pick a house number from the list';
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
