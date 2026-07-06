import { computed, nextTick, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  compatibleListingTypes,
  type ListingType,
  type Translations,
} from '../../../types/listingItem';
import type { ListingFieldsForm } from './formTypes';
import {
  addListing,
  getMyListings,
  ListingTypeExistsError,
} from '../../../api/listingsApi';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';
import {
  addPhone as addPhoneHelper,
  makeFieldError,
  removePhone as removePhoneHelper,
} from './formHelpers';

// Default values for the listing-only slice of the form. Spread into the full
// create form (useListingForm) and used standalone by the add-to-property
// drawer, so both start from an identical baseline.
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

// Validation shared by every listing form. `requireRentPrice` is only true in
// the create form's dual buy+rent mode; the drawer always passes false.
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

// Backs the "add another listing type to an existing property" view. The
// property (location/media/details) already exists, so this only collects the
// listing-specific fields — filtering the type picker to what the property
// doesn't already have — and posts them via addListing.
export function useAddListingToProperty(propertyId: string) {
  const { t } = useI18n();
  const { localePush } = useLocaleRoute();
  const { typeOptions } = usePropertyLabels();

  const form = reactive<ListingFieldsForm>({
    ...INITIAL_LISTING_FIELDS,
    phones: [''],
  });
  const touched = ref(false);
  const submitting = ref(false);
  const submitError = ref('');
  const loading = ref(true);

  const alreadyHas = ref<ListingType[]>([]);
  const availableTypeOptions = computed(() => {
    const compatible = new Set(compatibleListingTypes(alreadyHas.value));
    return typeOptions.value.filter((o) => compatible.has(o.id));
  });

  // A property always has at least one listing, so no match means it isn't the
  // current user's property (or doesn't exist) — bail to home.
  getMyListings()
    .then((all) => {
      const mine = all.filter((l) => l.propertyId === propertyId);
      if (mine.length === 0) {
        localePush('/');
        return;
      }
      alreadyHas.value = mine.map((l) => l.type);
      form.type = availableTypeOptions.value[0]?.id ?? '';
      loading.value = false;
    })
    .catch(() => localePush('/'));

  const errors = computed(() =>
    listingFieldErrors(form, { requireRentPrice: false })
  );
  const isValid = computed(() => Object.keys(errors.value).length === 0);
  const fieldError = makeFieldError(touched, errors);

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
      const created = await addListing(propertyId, {
        type: form.type as ListingType,
        price: {
          amount: Number(form.price),
          vatIncluded: form.vatIncluded || undefined,
        },
        translations: buildTranslations(form),
        phones: form.phones.filter((p) => p.trim()),
        completion:
          form.type === 'new_project' && form.completion
            ? form.completion
            : undefined,
        durationMonths: form.durationMonths,
      });
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
    fieldError,
    addPhone,
    removePhone,
    submit,
  };
}
