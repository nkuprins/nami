<script setup lang="ts">
import { FEATURE_OPTIONS } from '../../../types/propertyLabels';
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import type { PropertyFormState } from '../composables/usePropertyForm';

defineProps<{
  form: PropertyFormState;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      Features
    </h2>
    <ToggleButtons
      :options="FEATURE_OPTIONS"
      :model-value="form.features"
      multiple
      @update:model-value="form.features = $event as typeof form.features"
    />
  </section>
</template>
