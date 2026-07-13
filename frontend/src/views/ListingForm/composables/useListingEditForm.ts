import { reactive, ref } from 'vue';
import {
  getListing,
  getMyListings,
  updateListing,
  updateProperty,
} from '../../../api/listingsApi';
import type { ListingType } from '../../../types/listingItem';
import type { ListingFormState } from './formTypes';
import {
  buildListingBody,
  INITIAL_LISTING_FIELDS,
  listingScopedErrors,
} from './useListingFields';
import {
  INITIAL_PROPERTY_FIELDS,
  seedPropertyFields,
  uploadNewFiles,
  useListingFormControls,
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
  // render the locked Location card and to save a corrected map pin. The
  // register linkage (building code + apartment) is carried through untouched
  // so a pin save never downgrades a structured address to free text.
  const propertyId = ref('');
  const districtDisplay = ref('');
  const cityDisplay = ref('');
  const arBuildingCode = ref<number | null>(null);
  const apartment = ref<string | null>(null);
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
      arBuildingCode.value = p.location.arBuildingCode ?? null;
      apartment.value = p.location.apartment ?? null;
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
          arBuildingCode: arBuildingCode.value ?? undefined,
          apartment: apartment.value ?? undefined,
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

  const errors = listingScopedErrors(form, photoUpload);
  const {
    isValid,
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
