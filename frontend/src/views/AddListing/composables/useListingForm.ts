import { computed, nextTick, reactive, ref } from 'vue';
import type {
  Feature,
  ListingType,
  PropertyDetails,
  Translations,
} from '../../../types/listingItem';
import type { Location } from '../../../data/rawLocations';
import type { PropertyFieldsForm, ListingFieldsForm } from './formTypes';
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
  type: '',
  alsoRent: false,
  propertyKind: 'apartment',
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
  completion: '',
  features: [],
  phones: [''],
  videoUrl: '',
  coords: null,
  durationMonths: 3,
};

export function buildTranslations(form: ListingFieldsForm): Translations {
  const t: Translations = {};
  if (form.titleLv.trim())
    t.lv = {
      title: form.titleLv.trim(),
      description: form.descriptionLv.trim() || undefined,
    };
  if (form.titleEn.trim())
    t.en = {
      title: form.titleEn.trim(),
      description: form.descriptionEn.trim() || undefined,
    };
  if (form.titleRu.trim())
    t.ru = {
      title: form.titleRu.trim(),
      description: form.descriptionRu.trim() || undefined,
    };
  return t;
}

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
  duplicate?: { blocked: () => boolean; confirmed: () => boolean }
) {
  const { localePush } = useLocaleRoute();
  const form = reactive<ListingFormState>({ ...INITIAL_FORM });
  const touched = ref(false);
  const submitting = ref(false);
  const submitError = ref('');
  const rentListingWarning = ref(false);

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
    if (
      form.alsoRent &&
      (!form.rentPrice ||
        isNaN(parseDecimal(form.rentPrice)) ||
        parseDecimal(form.rentPrice) <= 0)
    )
      e.rentPrice = 'Enter a valid rent price';
    if (!getLocation()) e.district = 'Required';
    if (!form.address.trim()) e.address = 'Required';
    if (!form.rooms || isNaN(Number(form.rooms)) || Number(form.rooms) < 1)
      e.rooms = 'Enter number of rooms';
    if (!form.m2 || isNaN(parseDecimal(form.m2)) || parseDecimal(form.m2) <= 0)
      e.m2 = 'Enter area in m²';
    if (photoUpload.photos.value.length === 0)
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

      const created = await createListing({
        type: form.type as ListingType,
        propertyKind: form.propertyKind,
        price: {
          amount: parseDecimal(form.price),
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
      });

      if (form.alsoRent) {
        try {
          await addListing(created.propertyId, {
            type: 'rent',
            price: {
              amount: parseDecimal(form.rentPrice),
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
      submitError.value =
        e instanceof DuplicatePropertyError
          ? e.nearDuplicate
            ? 'This looks very similar to a property you already have.'
            : 'You already have a property at this address.'
          : 'Something went wrong. Please try again.';
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
