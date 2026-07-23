<script setup lang="ts">
import LocationSection from '../LocationSection.vue';
import CadastreSection from '../CadastreSection.vue';
import DuplicateNudgeCard from '../../../../components/listing/DuplicateNudgeCard.vue';
import type { PropertyFieldsForm } from '../../composables/formTypes';
import type { Location } from '../../../../data/rawLocations';
import type { ListingSummary } from '../../../../types/listingItem';
import type { DuplicateMatchKind } from '../../composables/useDuplicatePropertyNudge';

const form = defineModel<PropertyFieldsForm>('form', { required: true });
defineProps<{
  fieldError: (field: string) => string | undefined;
  districtName: string;
  selectedLocation: Location | null;
  isOpen: boolean;
  matchKind: DuplicateMatchKind;
  match: ListingSummary | null;
  acknowledged: boolean;
}>();

defineEmits<{
  (e: 'update:isOpen', value: boolean): void;
  (e: 'select', value: Location[]): void;
  (e: 'add-listing'): void;
  (e: 'acknowledge-fuzzy'): void;
}>();
</script>

<template>
  <div class="flex flex-col gap-5">
    <LocationSection
      v-model:form="form"
      :field-error="fieldError"
      :district-name="districtName"
      :selected-location="selectedLocation"
      :is-open="isOpen"
      @update:is-open="$emit('update:isOpen', $event)"
      @select="$emit('select', $event)"
    />
    <CadastreSection v-model:form="form" />
    <DuplicateNudgeCard
      :match-kind="matchKind"
      :match="match"
      :acknowledged="acknowledged"
      @add-listing="$emit('add-listing')"
      @acknowledge-fuzzy="$emit('acknowledge-fuzzy')"
    />
  </div>
</template>
