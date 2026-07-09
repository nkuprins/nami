import { computed, nextTick, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  offerableListingTypes,
  type Feature,
  type ListingType,
  type PropertyLocation,
  type Translations,
} from '../../../types/listingItem';
import type { ListingFieldsForm, ListingFormState } from './formTypes';
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
import type { usePhotoUpload } from './usePhotoUpload';
import {
  addPhone as addPhoneHelper,
  buildDetails,
  buildMedia,
  INITIAL_PROPERTY_FIELDS,
  makeFieldError,
  propertyFieldErrors,
  removePhone as removePhoneHelper,
  seedPropertyFields,
  toggleFeature as toggleFeatureHelper,
  uploadNewFiles,
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
  phones: [''],
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
    media: buildMedia(form, photos, plans),
    phones: form.phones.filter((p) => p.trim()),
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

  const filledPhones = form.phones.filter((p) => p.trim());
  if (filledPhones.length === 0)
    e.phones = 'At least one phone number required';
  const phoneRe = /^\+?[\d\s\-()]{7,}$/;
  const seen = new Set<string>();
  form.phones.forEach((p, i) => {
    const normalized = p.replace(/[\s\-()]/g, '');
    if (p.trim() && !phoneRe.test(p.trim()))
      e[`phone_${i}`] = 'Invalid phone format';
    else if (normalized && seen.has(normalized))
      e[`phone_${i}`] = 'Duplicate phone number';
    if (normalized) seen.add(normalized);
  });

  return e;
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

  const form = reactive<ListingFormState>({
    ...INITIAL_PROPERTY_FIELDS,
    ...INITIAL_LISTING_FIELDS,
    phones: [''],
  });
  const touched = ref(false);
  const submitting = ref(false);
  const submitError = ref('');
  const loading = ref(true);
  const propertyLocation = ref<PropertyLocation | null>(null);

  const offerable = new Set(offerableListingTypes());
  const availableTypeOptions = computed(() =>
    typeOptions.value.filter((o) => offerable.has(o.id))
  );

  // A property always has at least one listing, so no match means it isn't the
  // current user's property (or doesn't exist) — bail to home. The first sibling
  // seeds the physical/media fields so the new listing starts from its scope.
  getMyListings()
    .then(async (all) => {
      const mine = all.filter((l) => l.propertyId === propertyId);
      if (mine.length === 0) {
        localePush('/');
        return;
      }
      const sibling = await getListing(mine[0].id);
      if (sibling) {
        seedPropertyFields(form, sibling);
        propertyLocation.value = sibling.location;
        photoUpload.seed(sibling.media.photos ?? []);
        planUpload.seed(sibling.media.plans ?? []);
      }
      form.type = availableTypeOptions.value[0]?.id ?? '';
      loading.value = false;
    })
    .catch(() => localePush('/'));

  const errors = computed(() => ({
    ...listingFieldErrors(form, { requireRentPrice: false }),
    ...propertyFieldErrors(form, {
      hasLocation: true,
      hasPhotos: photoUpload.photos.value.length > 0,
      requireLocation: false,
    }),
  }));
  const isValid = computed(() => Object.keys(errors.value).length === 0);
  const fieldError = makeFieldError(touched, errors);

  function toggleFeature(f: Feature) {
    toggleFeatureHelper(form, f);
  }
  function addPhone() {
    addPhoneHelper(form);
  }
  function removePhone(index: number) {
    removePhoneHelper(form, index);
  }

  async function submit() {
    touched.value = true;
    if (!isValid.value) {
      await nextTick();
      document
        .querySelector('.text-red-500')
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }

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
