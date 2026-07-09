import { computed, nextTick, reactive, ref } from 'vue';
import {
  getListing,
  getMyListings,
  updateListing,
  updateProperty,
} from '../../../api/listingsApi';
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

  // Populated from the loaded listing's shared property location — used to
  // render the locked Location card and to save a corrected map pin.
  const propertyId = ref('');
  const districtDisplay = ref('');
  const cityDisplay = ref('');
  const savingPin = ref(false);
  const pinSaveError = ref('');
  const siblingListingsCount = ref(1);

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
      form.address = p.location.address;
      form.coords = p.location.coords;
      propertyId.value = p.propertyId;
      districtDisplay.value = p.location.district;
      cityDisplay.value = p.location.city;
      photoUpload.seed(p.media.photos ?? []);
      planUpload.seed(p.media.plans ?? []);
      loading.value = false;

      getMyListings()
        .then((all) => {
          siblingListingsCount.value = all.filter(
            (l) => l.propertyId === p.propertyId
          ).length;
        })
        .catch(() => {
          siblingListingsCount.value = 1;
        });
    })
    .catch(() => {
      localePush('/');
    });

  // Saves only a corrected map pin — the address/district/city stay whatever
  // they were, since the pin fix must never let a typo become a new address.
  async function savePin(newCoords: { lat: number; lng: number }) {
    savingPin.value = true;
    pinSaveError.value = '';
    try {
      await updateProperty(propertyId.value, {
        location: {
          district: districtDisplay.value,
          city: cityDisplay.value,
          address: form.address,
          coords: newCoords,
        },
      });
      form.coords = newCoords;
    } catch {
      pinSaveError.value = 'Something went wrong. Please try again.';
    } finally {
      savingPin.value = false;
    }
  }

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
    propertyId,
    districtDisplay,
    cityDisplay,
    savingPin,
    pinSaveError,
    siblingListingsCount,
    savePin,
  };
}
