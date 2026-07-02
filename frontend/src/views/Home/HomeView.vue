<script setup lang="ts">
import { nextTick, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useFiltersStore } from '../../stores/filterStore';
import { useListings } from '../../composables/useListings';
import ResultsHeader from '../../components/listing/ResultsHeader.vue';
import ListingGrid from '../../components/listing/ListingGrid.vue';
import Pagination from '../../components/listing/Pagination.vue';
import HeroSection from './components/HeroSection.vue';
import MoreFiltersDrawer from './components/MoreFiltersDrawer.vue';

const { t } = useI18n();

const HEADER_HEIGHT = 64;

const { state, setPage, resetAll } = useFiltersStore();
const { items, total, pageCount, loading } = useListings(() => state);

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
</script>

<template>
  <HeroSection @search="scrollToGrid" @open-more="drawerOpen = true" />
  <MoreFiltersDrawer
    :open="drawerOpen"
    @update:open="(v) => (drawerOpen = v)"
  />

  <section class="mx-auto max-w-360 px-6 lg:px-10 pt-16 sm:pt-20">
    <div ref="gridRef" />
    <ResultsHeader :total="total" :loading="loading" />

    <ListingGrid :items="items" :loading="loading">
      <template #empty>
        <div
          class="border border-dashed border-line rounded-2xl py-24 px-8 text-center bg-cream/40"
        >
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

    <Pagination :page="state.page" :page-count="pageCount" @change="goToPage" />
  </section>
</template>
