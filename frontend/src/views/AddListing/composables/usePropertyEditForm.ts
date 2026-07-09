import { computed, nextTick, reactive, ref, type Ref } from 'vue';
import { getProperty, updateProperty } from '../../../api/listingsApi';
import type { Location } from '../../../data/rawLocations';
import type { PropertyFieldsForm } from './formTypes';
import { INITIAL_PROPERTY_FIELDS, makeFieldError } from './formHelpers';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';

const DEFAULT_COORDS = { lat: 56.946, lng: 24.105 };

// The property is a shared address now, so this edits only its location. The form
// keeps the full PropertyFieldsForm shape (LocationSection binds to it), but only
// its address/coords are read and sent.
export type PropertyEditFormState = PropertyFieldsForm;

export function usePropertyEditForm(
  propertyId: string,
  selectedLocation: Ref<Location | null>
) {
  const { localePush } = useLocaleRoute();
  const form = reactive<PropertyEditFormState>({ ...INITIAL_PROPERTY_FIELDS });
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
      form.address = p.location.address;
      form.coords = p.location.coords;
      selectedLocation.value = {
        city: p.location.city,
        district: p.location.district,
      };
      loading.value = false;
    })
    .catch(() => {
      localePush('/');
    });

  const errors = computed(() => {
    const e: Record<string, string> = {};
    if (!selectedLocation.value) e.district = 'Required';
    if (!form.address.trim()) e.address = 'Required';
    return e;
  });

  const isValid = computed(() => Object.keys(errors.value).length === 0);
  const fieldError = makeFieldError(touched, errors);

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
      const location = selectedLocation.value!;
      await updateProperty(propertyId, {
        location: {
          district: location.district,
          city: location.city,
          address: form.address.trim(),
          coords: form.coords ?? DEFAULT_COORDS,
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
    submit,
    loading,
  };
}
