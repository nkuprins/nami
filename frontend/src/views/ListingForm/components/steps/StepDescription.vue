<script setup lang="ts">
import BasicInfoSection from '../BasicInfoSection.vue';
import DetailsSection from '../DetailsSection.vue';
import FeaturesSection from '../FeaturesSection.vue';
import type { ListingFormState } from '../../composables/formTypes';

const form = defineModel<ListingFormState>('form', { required: true });
defineProps<{
  fieldError: (field: string) => string | undefined;
}>();
</script>

<template>
  <div class="flex flex-col gap-10">
    <BasicInfoSection v-model:form="form" :field-error="fieldError" />
    <DetailsSection v-model:form="form" :field-error="fieldError" />
    <FeaturesSection v-model:form="form" />
  </div>
</template>
