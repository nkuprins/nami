<script setup lang="ts">
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { usePhotoUpload } from './composables/usePhotoUpload';
import { useLocationDropdown } from './composables/useLocationDropdown';
import { usePropertyForm } from './composables/usePropertyForm';
import { useLocaleRoute } from '../../composables/useLocaleRoute';

import ListingTypeSection from './components/ListingTypeSection.vue';
import BasicInfoSection from './components/BasicInfoSection.vue';
import PricingSection from './components/PricingSection.vue';
import LocationSection from './components/LocationSection.vue';
import DetailsSection from './components/DetailsSection.vue';
import FeaturesSection from './components/FeaturesSection.vue';
import PhonesSection from './components/PhonesSection.vue';
import PhotosSection from './components/PhotosSection.vue';
import PlansSection from './components/PlansSection.vue';

const route = useRoute();
const editId = route.params.id as string | undefined;
const { t } = useI18n();
const { localePath } = useLocaleRoute();

const { photos, addFiles, remove: removePhoto } = usePhotoUpload();
const {
  photos: plans,
  addFiles: addPlanFiles,
  remove: removePlan,
} = usePhotoUpload(3);
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
  isEdit,
  loading,
  prefill,
} = usePropertyForm(
  () => selectedLocation.value,
  () => photos.value,
  () => plans.value,
  editId
);
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 sm:px-6 py-10 sm:py-14">
    <div v-if="loading" class="flex flex-col gap-4">
      <div class="h-8 w-48 rounded bg-surface animate-pulse" />
      <div class="h-4 w-64 rounded bg-surface animate-pulse" />
      <div class="h-64 rounded-xl bg-surface animate-pulse mt-4" />
    </div>

    <template v-else>
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-ink">
          {{ isEdit ? t('editProperty.title') : t('addProperty.title') }}
        </h1>
        <p class="text-sm text-ink-3 mt-1">
          {{ isEdit ? t('editProperty.subtitle') : t('addProperty.subtitle') }}
        </p>
      </div>

      <form class="flex flex-col gap-10" @submit.prevent="submit">
        <ListingTypeSection :form="form" />

        <BasicInfoSection :form="form" :field-error="fieldError" />

        <PricingSection :form="form" :field-error="fieldError" :is-edit="isEdit" />

        <template v-if="!isEdit">
          <LocationSection
            :form="form"
            :field-error="fieldError"
            :district-name="districtName"
            :selected-location="selectedLocation"
            v-model:is-open="isOpen"
            @select="onSelect"
          />
        </template>
        <section v-else-if="prefill" class="flex flex-col gap-4">
          <h2
            class="text-base font-semibold text-ink border-b border-line pb-2"
          >
            {{ t('addProperty.locationSection') }}
          </h2>
          <p class="text-sm text-ink-2">
            {{ prefill.district }}, {{ prefill.city }} &mdash;
            {{ prefill.address }}
          </p>
          <p class="text-xs text-ink-3">
            {{ t('addProperty.locationUnchanged') }}
          </p>
        </section>

        <DetailsSection :form="form" :field-error="fieldError" />

        <FeaturesSection :form="form" />

        <PhonesSection
          :form="form"
          :field-error="fieldError"
          @add-phone="addPhone"
          @remove-phone="removePhone"
        />

        <template v-if="!isEdit">
          <PhotosSection
            :form="form"
            :photos="photos"
            :field-error="fieldError"
            @add-files="addFiles"
            @remove-photo="removePhoto"
          />
          <PlansSection
            :plans="plans"
            @add-files="addPlanFiles"
            @remove-plan="removePlan"
          />
        </template>
        <section v-else-if="prefill" class="flex flex-col gap-4">
          <h2
            class="text-base font-semibold text-ink border-b border-line pb-2"
          >
            Photos
          </h2>
          <div class="grid grid-cols-3 gap-2">
            <img
              v-for="(url, i) in prefill.photos"
              :key="i"
              :src="url"
              class="w-full aspect-square object-cover rounded-lg"
              :alt="`Photo ${i + 1}`"
            />
          </div>
          <p class="text-xs text-ink-3">Photos cannot be changed.</p>
        </section>

        <div class="flex items-center gap-4 pt-2">
          <button
            type="submit"
            :disabled="submitting"
            class="h-11 px-8 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{
              submitting
                ? t('addProperty.submitting')
                : isEdit
                  ? t('editProperty.publishListing')
                  : t('addProperty.publishListing')
            }}
          </button>
          <RouterLink
            :to="localePath('/')"
            class="text-sm text-ink-2 hover:text-ink underline underline-offset-2 transition-colors"
          >
            {{ t('addProperty.cancel') }}
          </RouterLink>
        </div>

        <p v-if="submitError" class="text-sm text-red-500">{{ submitError }}</p>
      </form>
    </template>
  </div>
</template>
