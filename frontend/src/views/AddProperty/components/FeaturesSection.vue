<script setup lang="ts">
import type { Feature } from '../../../types/propertyItem';
import { FEATURE_OPTIONS } from '../../../types/propertyLabels';
import type { PropertyFormState } from '../composables/usePropertyForm';

defineProps<{
  form: PropertyFormState;
  toggleFeature: (id: Feature) => void;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      Features
    </h2>
    <div class="flex flex-wrap gap-2">
      <button
        v-for="opt in FEATURE_OPTIONS"
        :key="opt.id"
        type="button"
        class="h-9 px-4 rounded-full text-sm font-medium border transition-colors"
        :class="
          form.features.includes(opt.id)
            ? 'bg-ink text-bg border-ink'
            : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
        "
        @click="toggleFeature(opt.id)"
      >
        {{ opt.label }}
      </button>
    </div>
  </section>
</template>
