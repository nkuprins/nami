import { computed, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  offerableListingTypes,
  type ListingSummary,
  type ListingType,
  type PhoneContact,
  type PropertyLocation,
  type Translations,
} from '../../../types/listingItem';
import type {
  ListingFieldsForm,
  ListingFormState,
  PhoneContactForm,
} from './formTypes';
import type {
  AddListingPayload,
  UpdateListingPayload,
} from '../../../api/listingsApi';
import {
  addListing,
  getListing,
  getMyListings,
  ListingTypeExistsError,
} from '../../../api/listingsApi';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';
import { useAuthStore } from '../../../stores/authStore';
import type { usePhotoUpload } from './usePhotoUpload';
import {
  buildDetails,
  buildMedia,
  INITIAL_PROPERTY_FIELDS,
  propertyFieldErrors,
  seedPropertyFields,
  uploadNewFiles,
  useListingFormControls,
} from './formHelpers';

// Default values for the listing-only slice of the form. Spread into the full
// create form (useListingForm) and reused by the listing edit / add-another
// forms, so all four start from an identical baseline.
export const INITIAL_LISTING_FIELDS: ListingFieldsForm = {
  type: '',
  alsoRent: false,
  titleLv: '',
  titleEn: '',
  titleRu: '',
  descriptionLv: '',
  descriptionEn: '',
  descriptionRu: '',
  price: '',
  vatIncluded: false,
  rentPrice: '',
  rentVatIncluded: false,
  rentDurationMonths: 3,
  durationMonths: 3,
  completion: '',
  phones: [{ phone: '', name: '', email: '' }],
};

export function buildTranslations(form: ListingFieldsForm): Translations {
  const t: Translations = {};
  if (form.titleLv.trim())
    t.lv = {
      title: form.titleLv.trim(),
      description: form.descriptionLv.trim() || undefined,
    };
  if (form.titleEn.trim())
    t.en = {
      title: form.titleEn.trim(),
      description: form.descriptionEn.trim() || undefined,
    };
  if (form.titleRu.trim())
    t.ru = {
      title: form.titleRu.trim(),
      description: form.descriptionRu.trim() || undefined,
    };
  return t;
}

// Trims each entry and drops rows with no phone number typed, converting the
// form's phone entries into the shape the API expects. A blank name/email is
// sent through as-is — the backend fills it from the account holder.
export function filledPhones(phones: PhoneContactForm[]): PhoneContact[] {
  return phones
    .filter((p) => p.phone.trim())
    .map((p) => ({
      phone: p.phone.trim(),
      name: p.name.trim(),
      email: p.email.trim(),
    }));
}

// The self-contained listing body shared by add + update (update omits duration).
export function buildListingBody(
  form: ListingFormState,
  photos: string[],
  plans: string[]
): UpdateListingPayload {
  return {
    type: form.type as ListingType,
    propertyKind: form.propertyKind,
    price: {
      amount: Number(form.price),
      vatIncluded: form.vatIncluded || undefined,
    },
    details: buildDetails(form),
    translations: buildTranslations(form),
    features: form.features,
    ventilationSystems: form.ventilationSystems,
    communications: form.communications,
    stove: form.stove,
    security: form.security,
    extras: form.extras,
    parking: form.parking,
    media: buildMedia(form, photos, plans),
    phones: filledPhones(form.phones),
    completion:
      form.type === 'new_project' && form.completion
        ? form.completion
        : undefined,
  };
}

// Validation shared by every listing form. `requireRentPrice` is only true in
// the create form's dual buy+rent mode; the other forms always pass false.
export function listingFieldErrors(
  form: ListingFieldsForm,
  opts: { requireRentPrice: boolean }
): Record<string, string> {
  const e: Record<string, string> = {};
  if (!form.type) e.type = 'Select a transaction type';
  if (!form.titleLv.trim() && !form.titleEn.trim() && !form.titleRu.trim())
    e.title = 'Enter a title in at least one language';
  if (
    !form.descriptionLv.trim() &&
    !form.descriptionEn.trim() &&
    !form.descriptionRu.trim()
  )
    e.description = 'Enter a description in at least one language';
  if (!form.price || Number(form.price) <= 0) e.price = 'Enter a valid price';
  if (opts.requireRentPrice && (!form.rentPrice || Number(form.rentPrice) <= 0))
    e.rentPrice = 'Enter a valid rent price';
  if (form.type === 'new_project' && !form.completion)
    e.completion = 'Required for new projects';

  const filled = form.phones.filter((p) => p.phone.trim());
  if (filled.length === 0) e.phones = 'At least one phone number required';
  const phoneRe = /^\+?[\d\s\-()]{7,}$/;
  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const seen = new Set<string>();
  form.phones.forEach((p, i) => {
    const normalized = p.phone.replace(/[\s\-()]/g, '');
    if (p.phone.trim() && !phoneRe.test(p.phone.trim()))
      e[`phone_${i}`] = 'Invalid phone format';
    else if (normalized && seen.has(normalized))
      e[`phone_${i}`] = 'Duplicate phone number';
    if (normalized) seen.add(normalized);
    if (p.email.trim() && !emailRe.test(p.email.trim()))
      e[`email_${i}`] = 'Invalid email format';
  });

  return e;
}

// Merged listing + property validation shared verbatim by the two listing-scoped
// forms (edit, add-another): both inherit the property's location, so neither
// requires location and neither has a rent leg.
export function listingScopedErrors(
  form: ListingFormState,
  photoUpload: ReturnType<typeof usePhotoUpload>
) {
  return computed(() => ({
    ...listingFieldErrors(form, { requireRentPrice: false }),
    ...propertyFieldErrors(form, {
      hasLocation: true,
      hasPhotos: photoUpload.photos.value.length > 0,
      requireLocation: false,
    }),
  }));
}

// Backs the "add another listing at this address" view. A listing is
// self-contained, so this collects the whole listing (its own physical
// attributes, media and terms) — prefilled from a sibling listing so the user
// can tweak the scope and trim the photo set — and posts it via addListing.
export function useAddListingToProperty(
  propertyId: string,
  photoUpload: ReturnType<typeof usePhotoUpload>,
  planUpload: ReturnType<typeof usePhotoUpload>
) {
  const { t } = useI18n();
  const { localePush } = useLocaleRoute();
  const { typeOptions } = usePropertyLabels();
  const authStore = useAuthStore();

  const form = reactive<ListingFormState>({
    ...INITIAL_PROPERTY_FIELDS,
    ...INITIAL_LISTING_FIELDS,
    phones: [
      {
        phone: '',
        name: authStore.user?.name ?? '',
        email: authStore.user?.email ?? '',
      },
    ],
  });
  const touched = ref(false);
  const submitting = ref(false);
  const submitError = ref('');
  const loading = ref(true);
  const propertyLocation = ref<PropertyLocation | null>(null);
  const siblings = ref<ListingSummary[]>([]);
  const sourceChosen = ref(false);

  const offerable = new Set(offerableListingTypes());
  const availableTypeOptions = computed(() =>
    typeOptions.value.filter((o) => offerable.has(o.id))
  );

  // Seed the new listing's physical/media fields from the sibling the user
  // picked as a starting point. Every listing at an address describes the same
  // apartment, so any sibling is a valid template — but they can diverge, so
  // which one to copy is the user's call (the view offers the choice).
  async function selectSource(listingId: string) {
    const sibling = await getListing(listingId);
    if (sibling) {
      seedPropertyFields(form, sibling);
      propertyLocation.value = sibling.location;
      photoUpload.seed(sibling.media.photos ?? []);
      planUpload.seed(sibling.media.plans ?? []);
    }
    form.type = availableTypeOptions.value[0]?.id ?? '';
    sourceChosen.value = true;
  }

  // A property always has at least one listing, so no match means it isn't the
  // current user's property (or doesn't exist) — bail to home. A lone sibling is
  // seeded automatically; when several exist the user picks which one to copy.
  getMyListings()
    .then(async (all) => {
      const mine = all.filter((l) => l.propertyId === propertyId);
      if (mine.length === 0) {
        localePush('/');
        return;
      }
      siblings.value = mine;
      // Location is the only property-shared field, so the address header can
      // render before a template is chosen.
      propertyLocation.value = mine[0].location;
      if (mine.length === 1) await selectSource(mine[0].id);
      loading.value = false;
    })
    .catch(() => localePush('/'));

  const errors = listingScopedErrors(form, photoUpload);
  const {
    fieldError,
    toggleFeature,
    addPhone,
    removePhone,
    validateForSubmit,
  } = useListingFormControls(form, touched, errors);

  async function submit() {
    if (!(await validateForSubmit())) return;

    submitting.value = true;
    submitError.value = '';
    try {
      const photos = await photoUpload.buildFinalUrls(uploadNewFiles);
      const plans = await planUpload.buildFinalUrls(uploadNewFiles);
      const payload: AddListingPayload = {
        ...buildListingBody(form, photos, plans),
        durationMonths: form.durationMonths,
      };
      const created = await addListing(propertyId, payload);
      await localePush(`/listing/${created.id}`);
    } catch (e) {
      submitError.value =
        e instanceof ListingTypeExistsError
          ? t('drawers.addListingTypeExists')
          : t('drawers.addListingFailed');
      submitting.value = false;
    }
  }

  return {
    form,
    availableTypeOptions,
    loading,
    siblings,
    sourceChosen,
    selectSource,
    submitting,
    submitError,
    errors,
    touched,
    fieldError,
    propertyLocation,
    toggleFeature,
    addPhone,
    removePhone,
    submit,
  };
}
