<script setup lang="ts">
import { COMPLETION_OPTIONS } from '../../../types/propertyLabels';
import { numericInput } from '../../../utils/utils';
import FormField from '../../../components/ui/FormField.vue';
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import type { PropertyFormState } from '../composables/usePropertyForm';

defineProps<{
  form: PropertyFormState;
  fieldError: (field: string) => string | undefined;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      Pricing
    </h2>

    <FormField
      id="ap-price"
      label="Price (€)"
      v-model="form.price"
      :error="fieldError('price')"
      required
      inputmode="numeric"
      placeholder="e.g. 185000"
      @beforeinput="numericInput"
    />

    <div v-if="form.type === 'new_project'" class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        Completion status <span class="text-red-500">*</span>
      </p>
      <ToggleButtons
        :options="COMPLETION_OPTIONS"
        :model-value="form.completion ?? ''"
        @update:model-value="form.completion = $event as typeof form.completion"
      />
      <p v-if="fieldError('completion')" class="text-xs text-red-500">
        {{ fieldError('completion') }}
      </p>
    </div>
  </section>
</template>
