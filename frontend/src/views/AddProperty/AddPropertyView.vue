<script setup lang="ts">
import { usePhotoUpload } from './composables/usePhotoUpload';
import { useLocationDropdown } from './composables/useLocationDropdown';
import { usePropertyForm } from './composables/usePropertyForm';

import ListingTypeSection from './components/ListingTypeSection.vue';
import BasicInfoSection from './components/BasicInfoSection.vue';
import PricingSection from './components/PricingSection.vue';
import LocationSection from './components/LocationSection.vue';
import DetailsSection from './components/DetailsSection.vue';
import FeaturesSection from './components/FeaturesSection.vue';
import PhonesSection from './components/PhonesSection.vue';
import PhotosSection from './components/PhotosSection.vue';

const { photos, addFiles, remove: removePhoto } = usePhotoUpload();
const { selectedLocation, isOpen, districtName, onSelect } =
  useLocationDropdown();
const {
  form,
  submitting,
  submitError,
  fieldError,
  addPhone,
  removePhone,
  submit,
} = usePropertyForm(
  () => selectedLocation.value,
  () => photos.value
);
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 sm:px-6 py-10 sm:py-14">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-ink">Add a property</h1>
      <p class="text-sm text-ink-3 mt-1">
        Fill in the details below to list your property.
      </p>
    </div>

    <form class="flex flex-col gap-10" @submit.prevent="submit">
      <ListingTypeSection :form="form" />

      <BasicInfoSection :form="form" :field-error="fieldError" />

      <PricingSection :form="form" :field-error="fieldError" />

      <LocationSection
        :form="form"
        :field-error="fieldError"
        :district-name="districtName"
        :selected-location="selectedLocation"
        v-model:is-open="isOpen"
        @select="onSelect"
      />

      <DetailsSection :form="form" :field-error="fieldError" />

      <FeaturesSection :form="form" />

      <PhonesSection
        :form="form"
        :field-error="fieldError"
        @add-phone="addPhone"
        @remove-phone="removePhone"
      />

      <PhotosSection
        :form="form"
        :photos="photos"
        :field-error="fieldError"
        @add-files="addFiles"
        @remove-photo="removePhoto"
      />

      <div class="flex items-center gap-4 pt-2">
        <button
          type="submit"
          :disabled="submitting"
          class="h-11 px-8 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ submitting ? 'Submitting…' : 'Publish listing' }}
        </button>
        <RouterLink
          to="/"
          class="text-sm text-ink-2 hover:text-ink underline underline-offset-2 transition-colors"
        >
          Cancel
        </RouterLink>
      </div>

      <p v-if="submitError" class="text-sm text-red-500">{{ submitError }}</p>
    </form>
  </div>
</template>
