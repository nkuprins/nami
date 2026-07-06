import type { ComputedRef, Ref } from 'vue';
import type { Feature } from '../../../types/listingItem';
import type { PropertyFieldsForm } from './formTypes';
import { parseDecimal } from '../../../utils/utils';

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

// Validation for the physical-property slice of the form, shared by the
// create form (useListingForm) and the property-only edit form
// (usePropertyEditForm).
export function propertyFieldErrors(
  form: PropertyFieldsForm,
  opts: { hasLocation: boolean; hasPhotos: boolean }
): Record<string, string> {
  const e: Record<string, string> = {};
  if (!opts.hasLocation) e.district = 'Required';
  if (!form.address.trim()) e.address = 'Required';
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
