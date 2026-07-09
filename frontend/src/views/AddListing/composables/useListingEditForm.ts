import { computed, nextTick, reactive, ref } from 'vue';
import { getListing, updateListing } from '../../../api/listingsApi';
import type { Feature, ListingType } from '../../../types/listingItem';
import type { ListingFormState } from './formTypes';
import {
  buildListingBody,
  INITIAL_LISTING_FIELDS,
  listingFieldErrors,
} from './useListingFields';
import {
  addPhone as addPhoneHelper,
  INITIAL_PROPERTY_FIELDS,
  makeFieldError,
  propertyFieldErrors,
  removePhone as removePhoneHelper,
  seedPropertyFields,
  toggleFeature as toggleFeatureHelper,
  uploadNewFiles,
} from './formHelpers';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';
import type { usePhotoUpload } from './usePhotoUpload';

export type ListingEditFormState = ListingFormState;

export function useListingEditForm(
  listingId: string,
  photoUpload: ReturnType<typeof usePhotoUpload>,
  planUpload: ReturnType<typeof usePhotoUpload>
) {
  const { localePush } = useLocaleRoute();
  const form = reactive<ListingEditFormState>({
    ...INITIAL_PROPERTY_FIELDS,
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
      seedPropertyFields(form, p);
      photoUpload.seed(p.media.photos ?? []);
      planUpload.seed(p.media.plans ?? []);
      loading.value = false;
    })
    .catch(() => {
      localePush('/');
    });

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
      const item = await updateListing(
        listingId,
        buildListingBody(form, photos, plans)
      );
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
    toggleFeature,
    addPhone,
    removePhone,
    submit,
    loading,
    loadedType,
  };
}
