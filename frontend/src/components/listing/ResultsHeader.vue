<script setup lang="ts">
import { computed, ref } from 'vue';
import { useFiltersStore } from '../../stores/filterStore';
import IconChevron from '../icons/IconChevron.vue';
import Popover from '../ui/Popover.vue';
import { SORT_OPTIONS, SortKey } from '../../types/sort';

defineProps<{ total: number; loading: boolean }>();

const { state, setSort } = useFiltersStore();

const open = ref(false);
const anchor = ref<HTMLButtonElement | null>(null);

const currentSortLabel = computed(() => {
  return SORT_OPTIONS.find((s) => s.id === state.sort)?.label ?? 'Sort';
});

function pick(id: SortKey) {
  setSort(id);
  open.value = false;
}
</script>

<template>
  <div
    class="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4 mb-8"
  >
    <div class="space-y-1">
      <p class="micro-label">Results</p>
      <h2 class="display-headline text-3xl sm:text-4xl text-ink">
        <span class="tabular">{{ total }}</span>
        <span class="text-ink-2">
          {{ total === 1 ? ' listing' : ' listings' }}</span
        >
      </h2>
      <p v-if="loading" class="text-xs text-ink-3">Updating…</p>
    </div>

    <div class="relative">
      <button
        ref="anchor"
        type="button"
        @click="open = !open"
        :aria-expanded="open"
        class="focus-ring inline-flex items-center gap-3 h-11 px-4 rounded-md border border-line bg-bg hover:border-line-2 transition-colors text-sm"
      >
        <span class="micro-label">Sort</span>
        <span class="text-ink">{{ currentSortLabel }}</span>
        <span class="size-4 text-ink-2"
          ><IconChevron :dir="open ? 'up' : 'down'"
        /></span>
      </button>

      <Popover
        :open="open"
        :anchor-el="anchor"
        title="Sort listings"
        align="end"
        :width="280"
        @update:open="(v) => (open = v)"
      >
        <div class="space-y-1">
          <button
            v-for="opt in SORT_OPTIONS"
            :key="opt.id"
            type="button"
            @click="pick(opt.id)"
            class="focus-ring w-full text-left px-3 py-2 rounded-md hover:bg-surface transition-colors flex items-center justify-between gap-3"
            :class="state.sort === opt.id ? 'bg-cream/60' : ''"
          >
            <div class="space-y-0.5">
              <span class="text-sm text-ink block">{{ opt.label }}</span>
              <p class="micro-label">{{ opt.hint }}</p>
            </div>

            <span
              v-if="state.sort === opt.id"
              class="size-1.5 rounded-full bg-accent shrink-0"
              aria-hidden="true"
            />
          </button>
        </div>
      </Popover>
    </div>
  </div>
</template>
