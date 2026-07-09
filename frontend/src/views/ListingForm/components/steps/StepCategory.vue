<script setup lang="ts">
import ListingTypeSection from '../ListingTypeSection.vue';
import type { ListingFormState } from '../../composables/formTypes';

const form = defineModel<ListingFormState>('form', { required: true });
defineProps<{
  fieldError: (field: string) => string | undefined;
}>();
</script>

<template>
  <ListingTypeSection v-model:form="form" :field-error="fieldError" />
</template>
