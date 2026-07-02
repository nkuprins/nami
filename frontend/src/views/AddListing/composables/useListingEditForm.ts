import { computed, nextTick, reactive, ref } from 'vue';
import { getListing, updateListing } from '../../../api/listingsApi';
import type { ListingType } from '../../../types/listingItem';
import type { ListingFieldsForm } from './formTypes';
import { buildTranslations } from './useListingForm';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';
import { parseDecimal } from '../../../utils/utils';

export type ListingEditFormState = ListingFieldsForm;

const INITIAL_FORM: ListingEditFormState = {
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

export function useListingEditForm(listingId: string) {
  const { localePush } = useLocaleRoute();
  const form = reactive<ListingEditFormState>({ ...INITIAL_FORM });
  const touched = ref(false);
  const submitting = ref(false);
  const submitError = ref('');
  const loading = ref(true);

  getListing(listingId)
    .then((p) => {
      if (!p) {
        localePush('/');
        return;
      }
      form.type = p.type;
      form.titleLv = p.translations.lv?.title ?? '';
      form.titleEn = p.translations.en?.title ?? '';
      form.titleRu = p.translations.ru?.title ?? '';
      form.descriptionLv = p.translations.lv?.description ?? '';
      form.descriptionEn = p.translations.en?.description ?? '';
      form.descriptionRu = p.translations.ru?.description ?? '';
      form.price = String(p.price.amount);
      form.vatIncluded = p.price.vatIncluded ?? false;
      form.completion = p.completion ?? '';
      form.phones = p.phones?.length ? [...p.phones] : [''];
      loading.value = false;
    })
    .catch(() => {
      localePush('/');
    });

  const errors = computed(() => {
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
    if (
      !form.price ||
      isNaN(parseDecimal(form.price)) ||
      parseDecimal(form.price) <= 0
    )
      e.price = 'Enter a valid price';
    if (form.type === 'new_project' && !form.completion)
      e.completion = 'Required for new projects';

    const filledPhones = form.phones.filter((p) => p.trim());
    if (filledPhones.length === 0) {
      e.phones = 'At least one phone number required';
    }
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
  });

  const isValid = computed(() => Object.keys(errors.value).length === 0);

  function fieldError(field: string): string {
    return touched.value ? (errors.value[field] ?? '') : '';
  }

  function addPhone() {
    form.phones.push('');
  }

  function removePhone(index: number) {
    form.phones.splice(index, 1);
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

    const filledPhones = form.phones.filter((p) => p.trim());

    try {
      const item = await updateListing(listingId, {
        type: form.type as ListingType,
        price: {
          amount: parseDecimal(form.price),
          vatIncluded: form.vatIncluded || undefined,
        },
        translations: buildTranslations(form),
        phones: filledPhones,
        completion:
          form.type === 'new_project' && form.completion
            ? form.completion
            : undefined,
      });
      await localePush(`/listing/${item.id}`);
    } catch {
      submitError.value = 'Something went wrong. Please try again.';
      submitting.value = false;
    }
  }

  return {
    form,
    touched,
    submitting,
    submitError,
    errors,
    isValid,
    fieldError,
    addPhone,
    removePhone,
    submit,
    loading,
  };
}
