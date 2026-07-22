<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useI18n } from 'vue-i18n';
import { useFiltersStore } from '../../stores/filterStore';
import { useListings } from '../../composables/useListings';
import { useMapPins, mapFilterKey } from '../../composables/useMapPins';
import { useViewMode } from '../../composables/useViewMode';
import { useMapView } from '../../composables/useMapView';
import ResultsHeader from '../../components/listing/ResultsHeader.vue';
import ListingGrid from '../../components/listing/ListingGrid.vue';
import ResultsMap from '../../components/listing/ResultsMap.vue';
import Pagination from '../../components/listing/Pagination.vue';
import HeroSection from './components/HeroSection.vue';
import MoreFiltersDrawer from './components/MoreFiltersDrawer.vue';

const { t } = useI18n();

const HEADER_HEIGHT = 64;

const store = useFiltersStore();
const { state, setPage, applySearch, resetAll } = store;
const { searchNonce } = storeToRefs(store);
const { items, total, pageCount, loading } = useListings(
  () => state,
  () => searchNonce.value
);
const { mode } = useViewMode();
const { state: mapState } = useMapView();

// Pins are fetched only while the map panel is open (enabled gate).
const { pins, loading: mapLoading } = useMapPins(
  () => state,
  () => searchNonce.value,
  () => mapState.value !== 'off'
);
const fitKey = computed(() => mapFilterKey(state));

const drawerOpen = ref(false);
const gridRef = ref<HTMLElement | null>(null);

function scrollToGrid() {
  const el = gridRef.value;
  if (!el) return;
  const top = el.getBoundingClientRect().top + window.scrollY - HEADER_HEIGHT;
  window.scrollTo({ top: Math.max(0, top), behavior: 'instant' });
}

async function goToPage(p: number) {
  setPage(p);
  await nextTick();
  scrollToGrid();
}

function onSearch() {
  applySearch();
  scrollToGrid();
}
</script>

<template>
  <HeroSection @search="onSearch" @open-more="drawerOpen = true" />
  <MoreFiltersDrawer
    :open="drawerOpen"
    @update:open="(v) => (drawerOpen = v)"
  />

  <section
    class="mx-auto"
    :class="
      mapState === 'off'
        ? 'max-w-360 px-6 lg:px-10 pt-16 sm:pt-20'
        : 'max-w-none px-4 lg:px-8 pt-10'
    "
  >
    <div ref="gridRef" />

    <!-- Header spans the full width; the map opens below it, not beside it. -->
    <ResultsHeader :total="total" :loading="loading" />

    <div :class="mapState !== 'off' ? 'lg:flex lg:gap-6 lg:items-start' : ''">
      <!-- List panel (hidden on mobile when the map is open; gone entirely in full focus) -->
      <div
        v-if="mapState !== 'full'"
        :class="
          mapState === 'split'
            ? 'hidden lg:block lg:flex-1 min-w-0 lg:max-h-[calc(100dvh-7rem)] lg:overflow-y-auto lg:overscroll-contain lg:pr-2'
            : 'w-full'
        "
      >
        <ListingGrid
          :items="items"
          :loading="loading"
          :view="mode"
          :max-cols="mapState === 'split' ? 2 : 4"
        >
          <template #empty>
            <div class="rounded-2xl py-24 px-8 text-center bg-surface/60">
              <p class="micro-label mb-4">{{ t('results.noMatches') }}</p>
              <h3 class="display-headline text-3xl text-ink mb-3">
                {{ t('results.nothingFits') }}
                <em class="display-eyebrow text-accent-2">{{
                  t('results.yet')
                }}</em>
              </h3>
              <p class="text-sm text-ink-2 max-w-md mx-auto mb-8">
                {{ t('results.loosenFilters') }}
              </p>
              <button
                type="button"
                @click="resetAll"
                class="focus-ring inline-flex items-center gap-2 h-11 px-5 rounded-full bg-ink text-bg text-sm hover:bg-accent-2 transition-colors"
              >
                {{ t('results.clearAll') }}
              </button>
            </div>
          </template>
        </ListingGrid>

        <Pagination
          :page="state.page"
          :page-count="pageCount"
          @change="goToPage"
        />
      </div>

      <!-- Map panel (split: sticky 50% column; full: full width) -->
      <div
        v-if="mapState !== 'off'"
        :class="mapState === 'full' ? 'w-full' : 'w-full lg:flex-1 min-w-0'"
      >
        <div
          class="lg:sticky lg:top-24 h-[calc(100dvh-7rem)] rounded-2xl overflow-hidden border border-line-2 bg-surface"
        >
          <ResultsMap :pins="pins" :loading="mapLoading" :fit-key="fitKey" />
        </div>
      </div>
    </div>
  </section>
</template>
