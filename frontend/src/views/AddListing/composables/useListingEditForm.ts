import { computed, nextTick, reactive, ref } from 'vue';
import { getListing, updateListing } from '../../../api/listingsApi';
import type { ListingType } from '../../../types/listingItem';
import type { ListingFieldsForm } from './formTypes';
import {
  buildTranslations,
  INITIAL_LISTING_FIELDS,
  listingFieldErrors,
} from './useListingFields';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';

export type ListingEditFormState = ListingFieldsForm;

export function useListingEditForm(listingId: string) {
  const { localePush } = useLocaleRoute();
  const form = reactive<ListingEditFormState>({
    ...INITIAL_LISTING_FIELDS,
    phones: [''],
  });
  const touched = ref(false);
  const submitting = ref(false);
  const submitError = ref('');
  const loading = ref(true);
  const loadedType = ref<ListingType | ''>('');

  getListing(listingId)
    .then((p) => {
      if (!p) {
        localePush('/');
        return;
      }
      form.type = p.type;
      loadedType.value = p.type;
      form.titleLv = p.translations.lv?.title ?? '';
      form.titleEn = p.translations.en?.title ?? '';
      form.titleRu = p.translations.ru?.title ?? '';
      form.descriptionLv = p.translations.lv?.description ?? '';
      form.descriptionEn = p.translations.en?.description ?? '';
      form.descriptionRu = p.translations.ru?.description ?? '';
      form.price = String(Math.round(p.price.amount));
      form.vatIncluded = p.price.vatIncluded ?? false;
      form.completion = p.completion ?? '';
      form.phones = p.phones?.length ? [...p.phones] : [''];
      loading.value = false;
    })
    .catch(() => {
      localePush('/');
    });

  const errors = computed(() =>
    listingFieldErrors(form, { requireRentPrice: false })
  );

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
          amount: Number(form.price),
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
    loadedType,
  };
}
