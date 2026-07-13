// Maps each wizard step to the form-error keys it owns, so "Continue" can be
// gated per-step against the same flat `errors` record every form composable
// already produces (listingFieldErrors + propertyFieldErrors) — no changes
// needed to that validation itself.
export type ListingWizardStep =
  'location' | 'category' | 'description' | 'photos' | 'publish' | 'confirm';

const STEP_MATCHERS: Record<ListingWizardStep, (key: string) => boolean> = {
  location: (k) => k === 'district' || k === 'street' || k === 'building',
  category: (k) => k === 'type',
  description: (k) =>
    k === 'title' || k === 'description' || k === 'rooms' || k === 'm2',
  photos: (k) => k === 'photos',
  publish: (k) =>
    k === 'price' ||
    k === 'rentPrice' ||
    k === 'completion' ||
    k === 'phones' ||
    k.startsWith('phone_') ||
    k.startsWith('email_'),
  confirm: () => false,
};

export function stepHasErrors(
  step: ListingWizardStep,
  errors: Record<string, string>
): boolean {
  return Object.keys(errors).some(STEP_MATCHERS[step]);
}

// Canonical wizard step order. The create form uses all of them; the
// add-another-listing form omits 'location' (it inherits the property's
// location), so each view derives its own STEPS from this single source.
export const LISTING_WIZARD_STEPS: readonly ListingWizardStep[] = [
  'location',
  'category',
  'description',
  'photos',
  'publish',
  'confirm',
];

const STEP_LABEL_KEYS: Record<ListingWizardStep, string> = {
  location: 'addListing.stepperLocation',
  category: 'addListing.stepperCategory',
  description: 'addListing.stepperDescription',
  photos: 'addListing.stepperPhotos',
  publish: 'addListing.stepperPublish',
  confirm: 'addListing.stepperConfirm',
};

// Builds the stepper's {id, label} list for the given ordered steps, resolving
// each label through i18n — shared by both listing wizards.
export function buildStepperSteps(
  steps: readonly ListingWizardStep[],
  t: (key: string) => string
): { id: ListingWizardStep; label: string }[] {
  return steps.map((id) => ({ id, label: t(STEP_LABEL_KEYS[id]) }));
}
