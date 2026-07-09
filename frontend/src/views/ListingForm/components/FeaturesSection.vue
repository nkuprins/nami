<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import type { PropertyFieldsForm } from '../composables/formTypes';
import type { Feature } from '../../../types/listingItem';

const { t } = useI18n();
const { featureOptions } = usePropertyLabels();

const form = defineModel<PropertyFieldsForm>('form', { required: true });

function isSelected(id: Feature): boolean {
  return form.value.features.includes(id);
}

function toggle(id: Feature) {
  form.value.features = isSelected(id)
    ? form.value.features.filter((f) => f !== id)
    : [...form.value.features, id];
}
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.featuresSection') }}
    </h2>
    <div class="flex gap-2 flex-wrap">
      <button
        v-for="opt in featureOptions"
        :key="opt.id"
        type="button"
        class="inline-flex items-center gap-2 h-9 pl-3 pr-4 rounded-full text-sm font-medium border transition-colors"
        :class="
          isSelected(opt.id)
            ? opt.category === 'comfort'
              ? 'bg-accent-2 border-accent-2 text-bg'
              : 'bg-feature-building-2 border-feature-building-2 text-bg'
            : opt.category === 'comfort'
              ? 'bg-accent/8 border-accent/25 text-accent-2 hover:border-accent/50'
              : 'bg-feature-building/8 border-feature-building/25 text-feature-building-2 hover:border-feature-building/50'
        "
        @click="toggle(opt.id)"
      >
        <span class="size-4.5 shrink-0"><component :is="opt.icon" /></span>
        {{ opt.label }}
      </button>
    </div>
  </section>
</template>
