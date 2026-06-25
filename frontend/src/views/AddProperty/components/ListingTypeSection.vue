<script setup lang="ts">
import { TYPE_OPTIONS, KIND_OPTIONS } from '../../../types/propertyLabels';
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import type { PropertyFormState } from '../composables/usePropertyForm';

defineProps<{ form: PropertyFormState }>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      Listing type
    </h2>
    <div class="flex flex-col gap-3">
      <div class="flex flex-col gap-1.5">
        <p class="text-sm font-medium text-ink">Transaction type</p>
        <ToggleButtons
          :options="TYPE_OPTIONS"
          :model-value="form.type"
          @update:model-value="form.type = $event as typeof form.type"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <p class="text-sm font-medium text-ink">Property kind</p>
        <ToggleButtons
          :options="KIND_OPTIONS"
          :model-value="form.propertyKind"
          @update:model-value="
            form.propertyKind = $event as typeof form.propertyKind
          "
        />
      </div>
    </div>
  </section>
</template>
