import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type {
  Feature,
  PropertyCompletion,
  PropertyKind,
  PropertyType,
} from '../../../types/propertyItem';
import type { Location } from '../../../data/rawLocations';
import { addProperty } from '../../../api/propertiesApi';
import { requestPresignedUrls, uploadFilesToS3 } from '../../../api/uploadApi';
import type { PhotoEntry } from './usePhotoUpload';

export interface PropertyFormState {
  type: PropertyType;
  propertyKind: PropertyKind;
  title: string;
  description: string;
  price: string;
  address: string;
  rooms: string;
  m2: string;
  landM2: string;
  floor: string;
  totalFloors: string;
  yearBuilt: string;
  completion: PropertyCompletion | '';
  features: Feature[];
  videoUrl: string;
  coords: { lat: number; lng: number } | null;
}

const INITIAL_FORM: PropertyFormState = {
  type: 'buy',
  propertyKind: 'apartment',
  title: '',
  description: '',
  price: '',
  address: '',
  rooms: '',
  m2: '',
  landM2: '',
  floor: '',
  totalFloors: '',
  yearBuilt: '',
  completion: '',
  features: [],
  videoUrl: '',
  coords: null,
};

export function usePropertyForm(
  getLocation: () => Location | null,
  getPhotos: () => PhotoEntry[]
) {
  const router = useRouter();
  const form = reactive<PropertyFormState>({ ...INITIAL_FORM });
  const touched = ref(false);
  const submitting = ref(false);
  const submitError = ref('');

  const errors = computed(() => {
    const e: Record<string, string> = {};
    if (!form.title.trim()) e.title = 'Required';
    if (!form.price || isNaN(Number(form.price)) || Number(form.price) <= 0)
      e.price = 'Enter a valid price';
    if (!getLocation()) e.district = 'Required';
    if (!form.address.trim()) e.address = 'Required';
    if (!form.rooms || isNaN(Number(form.rooms)) || Number(form.rooms) < 1)
      e.rooms = 'Enter number of rooms';
    if (!form.m2 || isNaN(Number(form.m2)) || Number(form.m2) <= 0)
      e.m2 = 'Enter area in m²';
    if (getPhotos().length === 0) e.photos = 'At least one photo required';
    if (form.type === 'new_project' && !form.completion)
      e.completion = 'Required for new projects';
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
    if (!isValid.value) return;

    submitting.value = true;
    submitError.value = '';

    try {
      const photos = getPhotos();
      const slots = await requestPresignedUrls(photos.map((p) => p.file.name));
      const photoUrls = await uploadFilesToS3(
        photos.map((p) => p.file),
        slots
      );
      const location = getLocation()!;

      const item = await addProperty({
        type: form.type,
        propertyKind: form.propertyKind,
        title: form.title.trim(),
        description: form.description.trim(),
        price: Number(form.price),
        rooms: Number(form.rooms),
        m2: Number(form.m2),
        landM2:
          form.propertyKind === 'house' && form.landM2
            ? Number(form.landM2)
            : undefined,
        floor: form.floor ? Number(form.floor) : undefined,
        totalFloors: form.totalFloors ? Number(form.totalFloors) : undefined,
        yearBuilt: form.yearBuilt ? Number(form.yearBuilt) : undefined,
        features: form.features,
        district: location.district,
        city: location.city,
        address: form.address.trim(),
        coords: form.coords ?? { lat: 56.946, lng: 24.105 },
        photos: photoUrls,
        videoUrl: form.videoUrl.trim() || undefined,
        completion:
          form.type === 'new_project' && form.completion
            ? form.completion
            : undefined,
      });

      await router.push(`/property/${item.id}`);
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
  };
}
