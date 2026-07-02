import { computed, nextTick, reactive, ref, type Ref } from 'vue';
import { getProperty, updateProperty } from '../../../api/listingsApi';
import type { Location } from '../../../data/rawLocations';
import type { Feature } from '../../../types/listingItem';
import type { PropertyFieldsForm } from './formTypes';
import { buildDetails, uploadNewFiles } from './useListingForm';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';
import { parseDecimal } from '../../../utils/utils';
import type { usePhotoUpload } from './usePhotoUpload';

export type PropertyEditFormState = PropertyFieldsForm;

const INITIAL_FORM: PropertyEditFormState = {
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

export function usePropertyEditForm(
  propertyId: string,
  selectedLocation: Ref<Location | null>,
  photoUpload: ReturnType<typeof usePhotoUpload>,
  planUpload: ReturnType<typeof usePhotoUpload>
) {
  const { localePush } = useLocaleRoute();
  const form = reactive<PropertyEditFormState>({ ...INITIAL_FORM });
  const touched = ref(false);
  const submitting = ref(false);
  const submitError = ref('');
  const loading = ref(true);

  getProperty(propertyId)
    .then((p) => {
      if (!p) {
        localePush('/');
        return;
      }
      form.propertyKind = p.propertyKind;
      form.address = p.location.address;
      form.rooms = String(p.details.rooms);
      form.bedrooms =
        p.details.bedrooms != null ? String(p.details.bedrooms) : '';
      form.bathrooms =
        p.details.bathrooms != null ? String(p.details.bathrooms) : '';
      form.bathroomLayout = p.details.bathroomLayout ?? '';
      form.m2 = String(p.details.m2);
      form.landM2 = p.details.landM2 != null ? String(p.details.landM2) : '';
      form.floor = p.details.floor != null ? String(p.details.floor) : '';
      form.totalFloors =
        p.details.totalFloors != null ? String(p.details.totalFloors) : '';
      form.yearBuilt =
        p.details.yearBuilt != null ? String(p.details.yearBuilt) : '';
      form.heating = p.details.heating ?? '';
      form.energyClass = p.details.energyClass ?? '';
      form.maintenanceCost =
        p.details.maintenanceCost != null
          ? String(p.details.maintenanceCost)
          : '';
      form.features = p.features ? [...p.features] : [];
      form.videoUrl = p.media.videoUrl ?? '';
      form.coords = p.location.coords;
      selectedLocation.value = {
        city: p.location.city,
        district: p.location.district,
      };
      photoUpload.seed(p.media.photos ?? []);
      planUpload.seed(p.media.plans ?? []);
      loading.value = false;
    })
    .catch(() => {
      localePush('/');
    });

  const errors = computed(() => {
    const e: Record<string, string> = {};
    if (!selectedLocation.value) e.district = 'Required';
    if (!form.address.trim()) e.address = 'Required';
    if (!form.rooms || isNaN(Number(form.rooms)) || Number(form.rooms) < 1)
      e.rooms = 'Enter number of rooms';
    if (!form.m2 || isNaN(parseDecimal(form.m2)) || parseDecimal(form.m2) <= 0)
      e.m2 = 'Enter area in m²';
    if (photoUpload.photos.value.length === 0)
      e.photos = 'At least one photo required';
    return e;
  });

  const isValid = computed(() => Object.keys(errors.value).length === 0);

  function fieldError(field: string): string {
    return touched.value ? (errors.value[field] ?? '') : '';
  }

  function toggleFeature(f: Feature) {
    const i = form.features.indexOf(f);
    if (i === -1) form.features.push(f);
    else form.features.splice(i, 1);
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
      const location = selectedLocation.value!;

      await updateProperty(propertyId, {
        propertyKind: form.propertyKind,
        details: buildDetails(form),
        features: form.features,
        media: {
          photos,
          plans: plans.length ? plans : null,
          videoUrl: form.videoUrl.trim() || null,
        },
        location: {
          district: location.district,
          city: location.city,
          address: form.address.trim(),
          coords: form.coords ?? { lat: 56.946, lng: 24.105 },
        },
      });
      await localePush('/');
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
    submit,
    loading,
  };
}
