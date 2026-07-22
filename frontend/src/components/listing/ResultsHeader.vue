<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useFiltersStore } from '../../stores/filterStore';
import IconChevron from '../icons/IconChevron.vue';
import IconGrid from '../icons/IconGrid.vue';
import IconList from '../icons/IconList.vue';
import Popover from '../ui/Popover.vue';
import { useSortOptions } from '../../composables/useSortOptions';
import { useViewMode } from '../../composables/useViewMode';
import type { SortKey } from '../../types/sort';

defineProps<{ total: number; loading: boolean }>();

const { t } = useI18n();
const { state, setSort } = useFiltersStore();
const { mode, setMode } = useViewMode();
const sortOptions = useSortOptions();

const open = ref(false);
const anchor = ref<HTMLButtonElement | null>(null);

const currentSortLabel = computed(() => {
  return (
    sortOptions.value.find((s) => s.id === state.sort)?.label ?? t('sort.sort')
  );
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
      <p class="micro-label">{{ t('results.results') }}</p>
      <h2 class="display-headline text-3xl sm:text-4xl text-ink">
        <span class="display-price">{{ total }}</span>
        <span class="text-ink-2">
          {{
            ' ' + (total === 1 ? t('results.listing') : t('results.listings'))
          }}</span
        >
      </h2>
      <p v-if="loading" class="text-xs text-ink-3">
        {{ t('results.updating') }}
      </p>
    </div>

    <div class="flex items-center gap-3 flex-wrap sm:mb-1">
      <div
        class="inline-flex items-center h-11 p-1 rounded-full border border-line-2 bg-bg"
        role="group"
        :aria-label="t('view.view')"
      >
        <button
          type="button"
          @click="setMode('grid')"
          :aria-pressed="mode === 'grid'"
          class="focus-ring inline-flex items-center gap-2 h-9 px-3.5 rounded-full text-sm transition-colors"
          :class="mode === 'grid' ? 'bg-ink text-bg' : 'text-ink-2 hover:text-ink'"
        >
          <span class="size-4"><IconGrid /></span>
          <span>{{ t('view.grid') }}</span>
        </button>
        <button
          type="button"
          @click="setMode('list')"
          :aria-pressed="mode === 'list'"
          class="focus-ring inline-flex items-center gap-2 h-9 px-3.5 rounded-full text-sm transition-colors"
          :class="mode === 'list' ? 'bg-ink text-bg' : 'text-ink-2 hover:text-ink'"
        >
          <span class="size-4"><IconList /></span>
          <span>{{ t('view.list') }}</span>
        </button>
      </div>

      <div class="relative">
        <button
          ref="anchor"
          type="button"
          @click="open = !open"
          :aria-expanded="open"
          class="focus-ring inline-flex items-center gap-3 h-11 px-5 rounded-full border border-line-2 bg-bg hover:border-ink-3 transition-colors text-sm"
        >
          <span class="micro-label">{{ t('sort.sort') }}</span>
          <span class="text-ink">{{ currentSortLabel }}</span>
          <span class="size-4 text-ink-2"
            ><IconChevron :dir="open ? 'up' : 'down'"
          /></span>
        </button>

        <Popover
        :open="open"
        :anchor-el="anchor"
        :title="t('sort.sortListings')"
        align="end"
        :width="280"
        @update:open="(v) => (open = v)"
      >
        <div class="space-y-1">
          <button
            v-for="opt in sortOptions"
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
              class="size-1.5 rounded-full bg-accent-2 shrink-0"
              aria-hidden="true"
            />
          </button>
        </div>
        </Popover>
      </div>
    </div>
  </div>
</template>
