import { computed, reactive, ref } from 'vue';
import type { ListingType } from '../../../types/listingItem';
import type { Location } from '../../../data/rawLocations';
import type { ListingFormState } from './formTypes';
import {
  buildListingBody,
  buildTranslations,
  filledPhones,
  INITIAL_LISTING_FIELDS,
  listingFieldErrors,
} from './useListingFields';
import { useAuthStore } from '../../../stores/authStore';
import {
  buildDetails,
  buildMedia,
  INITIAL_PROPERTY_FIELDS,
  propertyFieldErrors,
  registerCoords,
  selectedBuildingCode,
  uploadNewFiles,
  useListingFormControls,
} from './formHelpers';
import {
  addListing,
  createListing,
  DuplicatePropertyError,
} from '../../../api/listingsApi';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';
import { useCadastreAutofill } from './useCadastreAutofill';
import type { usePhotoUpload } from './usePhotoUpload';

const DEFAULT_COORDS = { lat: 56.946, lng: 24.105 };

const INITIAL_FORM: ListingFormState = {
  ...INITIAL_PROPERTY_FIELDS,
  ...INITIAL_LISTING_FIELDS,
};

export function useListingForm(
  getLocation: () => Location | null,
  photoUpload: ReturnType<typeof usePhotoUpload>,
  planUpload: ReturnType<typeof usePhotoUpload>,
  duplicate?: { blocked: () => boolean; confirmed: () => boolean },
  turnstile?: { token: () => string; reset: () => void }
) {
  const { localePush } = useLocaleRoute();
  const authStore = useAuthStore();
  const form = reactive<ListingFormState>({
    ...INITIAL_FORM,
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
  const rentListingWarning = ref(false);
  const official = useCadastreAutofill(form);

  const errors = computed(() => ({
    ...listingFieldErrors(form, { requireRentPrice: form.alsoRent }),
    ...propertyFieldErrors(form, {
      hasLocation: !!getLocation(),
      hasPhotos: photoUpload.photos.value.length > 0,
    }),
  }));

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

    // A duplicate property is blocking submission — the nudge dialog is showing.
    if (duplicate?.blocked()) return;

    submitting.value = true;
    submitError.value = '';
    rentListingWarning.value = false;

    const phones = filledPhones(form.phones);

    try {
      const photos = await photoUpload.buildFinalUrls(uploadNewFiles);
      const plans = await planUpload.buildFinalUrls(uploadNewFiles);
      const location = getLocation()!;
      const translations = buildTranslations(form);

      const created = await createListing(
        {
          type: form.type as ListingType,
          propertyKind: form.propertyKind,
          newProjectKind:
            form.propertyKind === 'new_project' && form.newProjectKind
              ? form.newProjectKind
              : undefined,
          commercialSubtype:
            form.propertyKind === 'commercial' && form.commercialSubtype
              ? form.commercialSubtype
              : undefined,
          landUse:
            (form.propertyKind === 'land' ||
              form.propertyKind === 'commercial') &&
            form.landUse
              ? form.landUse
              : undefined,
          price: {
            amount: Number(form.price),
            vatIncluded: form.vatIncluded || undefined,
          },
          details: buildDetails(form),
          translations,
          location: {
            district: location.district,
            city: location.city,
            address: form.address.trim(),
            arBuildingCode: selectedBuildingCode(form),
            apartment: form.apartment.trim() || undefined,
            cadastreParcelNr: form.cadastreParcelNr.trim() || undefined,
            coords: form.coords ?? registerCoords(form) ?? DEFAULT_COORDS,
          },
          features: form.features,
          ventilationSystems: form.ventilationSystems,
          communications: form.communications,
          stove: form.stove,
          security: form.security,
          extras: form.extras,
          parking: form.parking,
          media: buildMedia(form, photos, plans),
          phones,
          completion:
            form.propertyKind === 'new_project' && form.completion
              ? form.completion
              : undefined,
          durationMonths: form.durationMonths,
          confirmedDuplicate: duplicate?.confirmed() || undefined,
        },
        turnstile?.token()
      );

      // "Also list for rent" at the same address = a second self-contained listing
      // sharing this one's physical scope and photos, differing only in price/type.
      if (form.alsoRent) {
        try {
          await addListing(created.propertyId, {
            ...buildListingBody(form, photos, plans),
            type: 'rent',
            price: {
              amount: Number(form.rentPrice),
              vatIncluded: form.rentVatIncluded || undefined,
            },
            completion: undefined,
            durationMonths: form.rentDurationMonths,
          });
        } catch {
          rentListingWarning.value = true;
        }
      }

      await localePush(`/listing/${created.id}`);
    } catch (e) {
      // The Turnstile token is spent once the backend verifies it; reset the
      // widget so a retry gets a fresh one.
      turnstile?.reset();
      if (e instanceof DuplicatePropertyError) {
        submitError.value = e.nearDuplicate
          ? 'This looks very similar to a property you already have.'
          : 'You already have a property at this address.';
      } else {
        submitError.value = 'Something went wrong. Please try again.';
      }
      submitting.value = false;
    }
  }

  return {
    form,
    touched,
    submitting,
    submitError,
    rentListingWarning,
    errors,
    isValid,
    fieldError,
    toggleFeature,
    addPhone,
    removePhone,
    submit,
    official,
  };
}
