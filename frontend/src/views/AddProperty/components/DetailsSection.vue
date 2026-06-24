<script setup lang="ts">
import { numericInput } from '../../../utils/utils';
import FormField from '../../../components/ui/FormField.vue';
import type { PropertyFormState } from '../composables/usePropertyForm';

defineProps<{
  form: PropertyFormState;
  fieldError: (field: string) => string | undefined;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      Details
    </h2>

    <div class="grid grid-cols-2 gap-4">
      <FormField
        id="ap-rooms"
        label="Rooms"
        v-model="form.rooms"
        :error="fieldError('rooms')"
        required
        inputmode="numeric"
        placeholder="e.g. 3"
        @beforeinput="numericInput"
      />

      <FormField
        id="ap-m2"
        label="Area (m²)"
        v-model="form.m2"
        :error="fieldError('m2')"
        required
        inputmode="numeric"
        placeholder="e.g. 72"
        @beforeinput="numericInput"
      />
    </div>

    <FormField
      v-if="form.propertyKind === 'house'"
      id="ap-land"
      label="Land area (m²)"
      v-model="form.landM2"
      inputmode="numeric"
      placeholder="e.g. 600"
      @beforeinput="numericInput"
    />

    <div class="grid grid-cols-2 gap-4">
      <FormField
        id="ap-floor"
        label="Floor"
        v-model="form.floor"
        inputmode="numeric"
        placeholder="e.g. 4"
        @beforeinput="numericInput"
      />

      <FormField
        id="ap-total-floors"
        label="Total floors"
        v-model="form.totalFloors"
        inputmode="numeric"
        placeholder="e.g. 9"
        @beforeinput="numericInput"
      />
    </div>

    <FormField
      id="ap-year"
      label="Year built"
      v-model="form.yearBuilt"
      inputmode="numeric"
      placeholder="e.g. 2018"
      @beforeinput="numericInput"
    />
  </section>
</template>
