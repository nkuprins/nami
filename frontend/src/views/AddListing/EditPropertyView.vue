<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { usePhotoUpload } from './composables/usePhotoUpload';
import { useLocationDropdown } from './composables/useLocationDropdown';
import { usePropertyEditForm } from './composables/usePropertyEditForm';
import { useLocaleRoute } from '../../composables/useLocaleRoute';

import PropertyKindSection from './components/PropertyKindSection.vue';
import LocationSection from './components/LocationSection.vue';
import DetailsSection from './components/DetailsSection.vue';
import FeaturesSection from './components/FeaturesSection.vue';
import PhotosSection from './components/PhotosSection.vue';
import PlansSection from './components/PlansSection.vue';

const props = defineProps<{ id: string }>();

const { t } = useI18n();
const { localePath } = useLocaleRoute();

const photoUpload = usePhotoUpload();
const planUpload = usePhotoUpload(3);
const { selectedLocation, isOpen, districtName, onSelect } =
  useLocationDropdown();

const { form, submitting, submitError, fieldError, submit, loading } =
  usePropertyEditForm(props.id, selectedLocation, photoUpload, planUpload);
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
          {{ t('editProperty.title') }}
        </h1>
        <p class="text-sm text-ink-3 mt-1">{{ t('editProperty.subtitle') }}</p>
      </div>

      <form class="flex flex-col gap-10" @submit.prevent="submit">
        <PropertyKindSection
          :property-kind="form.propertyKind"
          @update:property-kind="form.propertyKind = $event"
        />

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

        <PhotosSection
          :form="form"
          :photos="photoUpload.photos.value"
          :field-error="fieldError"
          @add-files="photoUpload.addFiles"
          @remove-photo="photoUpload.remove"
          @move="photoUpload.move"
        />
        <PlansSection
          :plans="planUpload.photos.value"
          @add-files="planUpload.addFiles"
          @remove-plan="planUpload.remove"
          @move="planUpload.move"
        />

        <div class="flex items-center gap-4 pt-2">
          <button
            type="submit"
            :disabled="submitting"
            class="h-11 px-8 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{
              submitting
                ? t('addListing.submitting')
                : t('editProperty.submitLabel')
            }}
          </button>
          <RouterLink
            :to="localePath('/')"
            class="text-sm text-ink-2 hover:text-ink underline underline-offset-2 transition-colors"
          >
            {{ t('addListing.cancel') }}
          </RouterLink>
        </div>

        <p v-if="submitError" class="text-sm text-red-500">{{ submitError }}</p>
      </form>
    </template>
  </div>
</template>
