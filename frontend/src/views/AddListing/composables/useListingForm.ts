import { computed, nextTick, reactive, ref } from 'vue';
import type {
  Feature,
  ListingType,
  PropertyDetails,
} from '../../../types/listingItem';
import type { Location } from '../../../data/rawLocations';
import type { PropertyFieldsForm, ListingFieldsForm } from './formTypes';
import {
  buildTranslations,
  INITIAL_LISTING_FIELDS,
  listingFieldErrors,
} from './useListingFields';
import {
  addPhone as addPhoneHelper,
  makeFieldError,
  propertyFieldErrors,
  removePhone as removePhoneHelper,
  toggleFeature as toggleFeatureHelper,
} from './formHelpers';
import {
  addListing,
  createListing,
  DuplicatePropertyError,
} from '../../../api/listingsApi';
import { requestPresignedUrls, uploadFilesToS3 } from '../../../api/uploadApi';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';
import { parseDecimal } from '../../../utils/utils';
import type { usePhotoUpload } from './usePhotoUpload';

const DEFAULT_COORDS = { lat: 56.946, lng: 24.105 };

export type ListingFormState = PropertyFieldsForm & ListingFieldsForm;

const INITIAL_FORM: ListingFormState = {
  ...INITIAL_LISTING_FIELDS,
  propertyKind: 'apartment',
  address: '',
  rooms: '',
  bedrooms: '',
  bathrooms: '',
  bathroomLayout: '',
  m2: '',
  landM2: '',
  floor: '',
  totalFloors: '',
  yearBuilt: '',
  heating: '',
  energyClass: '',
  maintenanceCost: '',
  features: [],
  videoUrl: '',
  coords: null,
};

export function buildDetails(form: PropertyFieldsForm): PropertyDetails {
  return {
    rooms: Number(form.rooms),
    bedrooms: form.bedrooms ? Number(form.bedrooms) : undefined,
    bathrooms: form.bathrooms ? Number(form.bathrooms) : undefined,
    bathroomLayout: form.bathroomLayout || undefined,
    m2: parseDecimal(form.m2),
    landM2:
      form.propertyKind === 'house' && form.landM2
        ? parseDecimal(form.landM2)
        : undefined,
    floor: form.floor ? Number(form.floor) : undefined,
    totalFloors: form.totalFloors ? Number(form.totalFloors) : undefined,
    yearBuilt: form.yearBuilt ? Number(form.yearBuilt) : undefined,
    heating: form.heating || undefined,
    energyClass: form.energyClass || undefined,
    maintenanceCost: form.maintenanceCost
      ? parseDecimal(form.maintenanceCost)
      : undefined,
  };
}

export async function uploadNewFiles(files: File[]): Promise<string[]> {
  const slots = await requestPresignedUrls(files.map((f) => f.name));
  return uploadFilesToS3(files, slots);
}

export function useListingForm(
  getLocation: () => Location | null,
  photoUpload: ReturnType<typeof usePhotoUpload>,
  planUpload: ReturnType<typeof usePhotoUpload>,
  duplicate?: { blocked: () => boolean; confirmed: () => boolean },
  turnstile?: { token: () => string; reset: () => void }
) {
  const { localePush } = useLocaleRoute();
  const form = reactive<ListingFormState>({ ...INITIAL_FORM });
  const touched = ref(false);
  const submitting = ref(false);
  const submitError = ref('');
  const rentListingWarning = ref(false);

  const errors = computed(() => ({
    ...listingFieldErrors(form, { requireRentPrice: form.alsoRent }),
    ...propertyFieldErrors(form, {
      hasLocation: !!getLocation(),
      hasPhotos: photoUpload.photos.value.length > 0,
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

    // A duplicate property is blocking submission — the nudge dialog is showing.
    if (duplicate?.blocked()) return;

    submitting.value = true;
    submitError.value = '';
    rentListingWarning.value = false;

    const filledPhones = form.phones.filter((p) => p.trim());

    try {
      const photos = await photoUpload.buildFinalUrls(uploadNewFiles);
      const plans = await planUpload.buildFinalUrls(uploadNewFiles);
      const location = getLocation()!;
      const translations = buildTranslations(form);

      const created = await createListing(
        {
          type: form.type as ListingType,
          propertyKind: form.propertyKind,
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
            coords: form.coords ?? DEFAULT_COORDS,
          },
          features: form.features,
          media: {
            photos,
            plans: plans.length ? plans : null,
            videoUrl: form.videoUrl.trim() || null,
          },
          phones: filledPhones,
          completion:
            form.type === 'new_project' && form.completion
              ? form.completion
              : undefined,
          durationMonths: form.durationMonths,
          confirmedDuplicate: duplicate?.confirmed() || undefined,
        },
        turnstile?.token()
      );

      if (form.alsoRent) {
        try {
          await addListing(created.propertyId, {
            type: 'rent',
            price: {
              amount: Number(form.rentPrice),
              vatIncluded: form.rentVatIncluded || undefined,
            },
            translations,
            phones: filledPhones,
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
  };
}
