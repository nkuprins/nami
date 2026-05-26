<script setup lang="ts">
import {ref} from 'vue';
import {useFilters} from '../../composables/useFilters';
import type {SortKey} from '../../types/filter';
import IconChevron from '../ui/IconChevron.vue';
import Popover from '../ui/PopOver.vue';

defineProps<{ total: number; loading: boolean }>();

const {state, setSort} = useFilters();

const SORT_OPTIONS: Array<{ id: SortKey; label: string; hint: string }> = [
  {id: 'newest', label: 'Newest first', hint: 'Most recently listed'},
  {id: 'price-asc', label: 'Price - low to high', hint: '€ ascending'},
  {id: 'price-desc', label: 'Price - high to low', hint: '€ descending'},
  {id: 'm2-asc', label: 'Best € / m²', hint: 'Lowest price per m²'},
  {id: 'm2-desc', label: 'Largest first', hint: 'm² descending'},
];

const open = ref(false);
const anchor = ref<HTMLButtonElement | null>(null);

function pick(id: SortKey) {
  setSort(id);
  open.value = false;
}
</script>

<template>
  <div class="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4 mb-8">
    <div class="space-y-1">
      <p class="micro-label">Results</p>
      <h2 class="display-headline text-3xl sm:text-4xl text-ink">
        <span class="tabular">{{ total }}</span>
        <span class="text-ink-2"> {{ total === 1 ? ' listing' : ' listings' }}</span>
      </h2>
      <p v-if="loading" class="text-xs text-ink-3">Updating…</p>
    </div>

    <div class="relative">
      <button
          ref="anchor"
          type="button"
          @click="open = !open"
          :aria-expanded="open"
          class="focus-ring inline-flex items-center gap-3 h-11 px-4 rounded-md
               border border-line bg-bg hover:border-line-2 transition-colors text-sm"
      >
        <span class="micro-label">Sort</span>
        <span class="text-ink">{{ SORT_OPTIONS.find((s) => s.id === state.sort)?.label }}</span>
        <span class="size-4 text-ink-2"><IconChevron :dir="open ? 'up' : 'down'"/></span>
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
              class="focus-ring w-full text-left px-3 py-2 rounded-md hover:bg-surface transition-colors
           flex items-center justify-between gap-3"
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