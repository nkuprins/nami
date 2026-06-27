import { computed, reactive, ref } from 'vue';
import type {
  Feature,
  PropertyCompletion,
  PropertyDetail,
  PropertyKind,
  PropertyType,
} from '../../../types/propertyItem';
import type { Location } from '../../../data/rawLocations';
import {
  addProperty,
  getProperty,
  updateProperty,
} from '../../../api/propertiesApi';
import { requestPresignedUrls, uploadFilesToS3 } from '../../../api/uploadApi';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';
import type { PhotoEntry } from './usePhotoUpload';

export interface PropertyFormState {
  type: PropertyType;
  propertyKind: PropertyKind;
  titleLv: string;
  titleEn: string;
  titleRu: string;
  descriptionLv: string;
  descriptionEn: string;
  descriptionRu: string;
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
  phones: string[];
  videoUrl: string;
  coords: { lat: number; lng: number } | null;
  durationMonths: number;
}

const INITIAL_FORM: PropertyFormState = {
  type: 'buy',
  propertyKind: 'apartment',
  titleLv: '',
  titleEn: '',
  titleRu: '',
  descriptionLv: '',
  descriptionEn: '',
  descriptionRu: '',
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
  phones: [''],
  videoUrl: '',
  coords: null,
  durationMonths: 3,
};

export interface EditPrefill {
  district: string;
  city: string;
  address: string;
  coords: { lat: number; lng: number };
  photos: string[];
}

export function usePropertyForm(
  getLocation: () => Location | null,
  getPhotos: () => PhotoEntry[],
  getPlans: () => PhotoEntry[],
  editId?: string
) {
  const { localePush } = useLocaleRoute();
  const form = reactive<PropertyFormState>({ ...INITIAL_FORM });
  const touched = ref(false);
  const submitting = ref(false);
  const submitError = ref('');
  const isEdit = !!editId;
  const loading = ref(false);
  const prefill = ref<EditPrefill | null>(null);

  if (editId) {
    loading.value = true;
    getProperty(editId)
      .then((p) => {
        if (!p) {
          localePush('/');
          return;
        }
        form.type = p.type;
        form.propertyKind = p.propertyKind;
        form.titleLv = p.titleLv ?? '';
        form.titleEn = p.titleEn ?? '';
        form.titleRu = p.titleRu ?? '';
        form.descriptionLv = p.descriptionLv ?? '';
        form.descriptionEn = p.descriptionEn ?? '';
        form.descriptionRu = p.descriptionRu ?? '';
        form.price = String(p.price);
        form.address = p.address;
        form.rooms = String(p.rooms);
        form.m2 = String(p.m2);
        form.landM2 = p.landM2 != null ? String(p.landM2) : '';
        form.floor = p.floor != null ? String(p.floor) : '';
        form.totalFloors = p.totalFloors != null ? String(p.totalFloors) : '';
        form.yearBuilt = p.yearBuilt != null ? String(p.yearBuilt) : '';
        form.completion = p.completion ?? '';
        form.features = [...p.features];
        form.phones = p.phones?.length ? [...p.phones] : [''];
        form.videoUrl = p.videoUrl ?? '';
        form.coords = p.coords;
        prefill.value = {
          district: p.district,
          city: p.city,
          address: p.address,
          coords: p.coords,
          photos: p.photos,
        };
        loading.value = false;
      })
      .catch(() => {
        localePush('/');
      });
  }

  const errors = computed(() => {
    const e: Record<string, string> = {};
    if (!form.titleLv.trim() && !form.titleEn.trim() && !form.titleRu.trim())
      e.title = 'Enter a title in at least one language';
    if (!form.price || isNaN(Number(form.price)) || Number(form.price) <= 0)
      e.price = 'Enter a valid price';
    if (!isEdit && !getLocation()) e.district = 'Required';
    if (!isEdit && !form.address.trim()) e.address = 'Required';
    if (!form.rooms || isNaN(Number(form.rooms)) || Number(form.rooms) < 1)
      e.rooms = 'Enter number of rooms';
    if (!form.m2 || isNaN(Number(form.m2)) || Number(form.m2) <= 0)
      e.m2 = 'Enter area in m²';
    if (!isEdit && getPhotos().length === 0)
      e.photos = 'At least one photo required';
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

  function toggleFeature(f: Feature) {
    const i = form.features.indexOf(f);
    if (i === -1) form.features.push(f);
    else form.features.splice(i, 1);
  }

  function addPhone() {
    form.phones.push('');
  }

  function removePhone(index: number) {
    form.phones.splice(index, 1);
  }

  async function submit() {
    touched.value = true;
    if (!isValid.value) return;

    submitting.value = true;
    submitError.value = '';

    try {
      if (isEdit) {
        const item = await updateProperty(editId!, {
          type: form.type,
          propertyKind: form.propertyKind,
          titleLv: form.titleLv.trim() || undefined,
          titleEn: form.titleEn.trim() || undefined,
          titleRu: form.titleRu.trim() || undefined,
          descriptionLv: form.descriptionLv.trim() || undefined,
          descriptionEn: form.descriptionEn.trim() || undefined,
          descriptionRu: form.descriptionRu.trim() || undefined,
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
          phones: form.phones.filter((p) => p.trim()).length
            ? form.phones.filter((p) => p.trim())
            : undefined,
          videoUrl: form.videoUrl.trim() || undefined,
          completion:
            form.type === 'new_project' && form.completion
              ? form.completion
              : undefined,
        });
        await localePush(`/property/${item.id}`);
      } else {
        const photos = getPhotos();
        const slots = await requestPresignedUrls(
          photos.map((p) => p.file.name)
        );
        const photoUrls = await uploadFilesToS3(
          photos.map((p) => p.file),
          slots
        );

        let planUrls: string[] = [];
        const planFiles = getPlans();
        if (planFiles.length > 0) {
          const planSlots = await requestPresignedUrls(
            planFiles.map((p) => p.file.name)
          );
          planUrls = await uploadFilesToS3(
            planFiles.map((p) => p.file),
            planSlots
          );
        }

        const location = getLocation()!;

        const item = await addProperty({
          type: form.type,
          propertyKind: form.propertyKind,
          titleLv: form.titleLv.trim() || undefined,
          titleEn: form.titleEn.trim() || undefined,
          titleRu: form.titleRu.trim() || undefined,
          descriptionLv: form.descriptionLv.trim() || undefined,
          descriptionEn: form.descriptionEn.trim() || undefined,
          descriptionRu: form.descriptionRu.trim() || undefined,
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
          phones: form.phones.filter((p) => p.trim()).length
            ? form.phones.filter((p) => p.trim())
            : undefined,
          photos: photoUrls,
          plans: planUrls.length ? planUrls : undefined,
          videoUrl: form.videoUrl.trim() || undefined,
          completion:
            form.type === 'new_project' && form.completion
              ? form.completion
              : undefined,
          durationMonths: form.durationMonths,
        });

        await localePush(`/property/${item.id}`);
      }
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
    isEdit,
    loading,
    prefill,
  };
}
