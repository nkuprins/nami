<script setup lang="ts">
import type { PropertyKind } from '../../../types/listingItem';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import IconApartment from '../../../components/icons/IconApartment.vue';
import IconHouse from '../../../components/icons/IconHouse.vue';

defineProps<{
  modelValue: PropertyKind | undefined;
  counts?: { apartment: number; house: number };
}>();
const emit = defineEmits<{
  'update:modelValue': [value: PropertyKind];
}>();

const { kindLabel } = usePropertyLabels();

const kindIcons = { apartment: IconApartment, house: IconHouse } as const;
const kinds = ['apartment', 'house'] as const;

function pick(kind: PropertyKind) {
  emit('update:modelValue', kind);
}
</script>

<template>
  <div class="flex items-stretch border-b border-line">
    <button
      v-for="kind in kinds"
      :key="kind"
      type="button"
      role="tab"
      :aria-selected="modelValue === kind"
      @click="pick(kind)"
      class="focus-ring relative flex-1 flex items-center justify-between gap-2 px-4 sm:px-6 py-3 transition-colors duration-200"
      :class="
        modelValue === kind
          ? 'bg-surface text-ink'
          : 'text-ink-2 hover:text-ink hover:bg-surface/50'
      "
    >
      <span
        class="inline-flex items-center gap-2.5 border-b-2 pb-0.5"
        :class="modelValue === kind ? 'border-accent-2' : 'border-transparent'"
      >
        <span class="size-7 inline-block shrink-0">
          <component :is="kindIcons[kind]" />
        </span>
        <span class="text-base font-semibold leading-tight">{{
          kindLabel(kind)
        }}</span>
      </span>
      <span v-if="counts" class="text-sm font-semibold text-ink-2 tabular">{{
        counts[kind]
      }}</span>
    </button>
  </div>
</template>
