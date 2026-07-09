// Maps each wizard step to the form-error keys it owns, so "Continue" can be
// gated per-step against the same flat `errors` record every form composable
// already produces (listingFieldErrors + propertyFieldErrors) — no changes
// needed to that validation itself.
export type ListingWizardStep =
  'location' | 'category' | 'description' | 'photos' | 'publish' | 'confirm';

const STEP_MATCHERS: Record<ListingWizardStep, (key: string) => boolean> = {
  location: (k) => k === 'district' || k === 'address',
  category: (k) => k === 'type',
  description: (k) =>
    k === 'title' || k === 'description' || k === 'rooms' || k === 'm2',
  photos: (k) => k === 'photos',
  publish: (k) =>
    k === 'price' ||
    k === 'rentPrice' ||
    k === 'completion' ||
    k === 'phones' ||
    k.startsWith('phone_'),
  confirm: () => false,
};

export function stepHasErrors(
  step: ListingWizardStep,
  errors: Record<string, string>
): boolean {
  return Object.keys(errors).some(STEP_MATCHERS[step]);
}
