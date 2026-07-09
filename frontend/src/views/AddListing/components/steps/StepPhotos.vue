<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import PhotosSection from '../PhotosSection.vue';
import PlansSection from '../PlansSection.vue';
import type { PropertyFieldsForm } from '../../composables/formTypes';

const { t } = useI18n();

const form = defineModel<PropertyFieldsForm>('form', { required: true });
defineProps<{
  photos: Array<{ preview: string } | { url: string }>;
  plans: Array<{ preview: string } | { url: string }>;
  fieldError: (field: string) => string | undefined;
}>();

defineEmits<{
  (e: 'add-photo-files', event: Event): void;
  (e: 'remove-photo', index: number): void;
  (e: 'move-photo', from: number, to: number): void;
  (e: 'add-plan-files', event: Event): void;
  (e: 'remove-plan', index: number): void;
  (e: 'move-plan', from: number, to: number): void;
}>();
</script>

<template>
  <div class="flex flex-col gap-10">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.mediaSection') }}
    </h2>
    <PhotosSection
      v-model:form="form"
      :photos="photos"
      :field-error="fieldError"
      @add-files="$emit('add-photo-files', $event)"
      @remove-photo="$emit('remove-photo', $event)"
      @move="(from, to) => $emit('move-photo', from, to)"
    />
    <PlansSection
      :plans="plans"
      @add-files="$emit('add-plan-files', $event)"
      @remove-plan="$emit('remove-plan', $event)"
      @move="(from, to) => $emit('move-plan', from, to)"
    />
  </div>
</template>
