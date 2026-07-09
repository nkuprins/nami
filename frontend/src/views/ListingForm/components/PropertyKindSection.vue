<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import type { PropertyKind } from '../../../types/listingItem';

const { t } = useI18n();
const { kindOptions } = usePropertyLabels();

defineProps<{
  propertyKind: PropertyKind;
}>();

defineEmits<{
  (e: 'update:propertyKind', value: PropertyKind): void;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.propertyKind') }}
    </h2>
    <ToggleButtons
      :options="kindOptions"
      :model-value="propertyKind"
      @update:model-value="$emit('update:propertyKind', $event as PropertyKind)"
    />
  </section>
</template>
