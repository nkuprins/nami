<script setup lang="ts">
import {ref} from 'vue';
import {useFiltersStore} from '../stores/filters';
import {useListings} from '../composables/useListings';
import ResultsHeader from "../components/listing/ResultsHeader.vue";
import PropertyGrid from "../components/listing/PropertyGrid.vue";
import Pagination from "../components/listing/Pagination.vue";
import HeroSection from "../components/hero/HeroSection.vue";
import MoreFiltersDrawer from "../components/filters/MoreFiltersDrawer.vue";

const {state, setPage, resetAll} = useFiltersStore();
const {items, total, pageCount, loading} = useListings(() => state);

const drawerOpen = ref(false);
const gridRef = ref<HTMLElement | null>(null);

function scrollToGrid() {
  gridRef.value?.scrollIntoView({behavior: 'smooth', block: 'start'});
}

function goToPage(p: number) {
  setPage(p);
  scrollToGrid();
}
</script>

<template>
  <HeroSection
      @search="scrollToGrid"
      @open-more="drawerOpen = true"
  />
  <MoreFiltersDrawer
      :open="drawerOpen"
      @update:open="(v) => (drawerOpen = v)"
  />

  <section
      ref="gridRef"
      class="mx-auto max-w-360 px-6 lg:px-10 pt-16 sm:pt-20 scroll-mt-20"
  >
    <ResultsHeader :total="total" :loading="loading"/>

    <PropertyGrid :items="items" :loading="loading">
      <template #empty>
        <div
            class="border border-dashed border-line rounded-2xl py-24 px-8 text-center bg-cream/40"
        >
          <p class="micro-label mb-4">No matches</p>
          <h3 class="display-headline text-3xl text-ink mb-3">
            Nothing fits these filters
            <em class="display-eyebrow text-accent-2">— yet.</em>
          </h3>
          <p class="text-sm text-ink-2 max-w-md mx-auto mb-8">
            Loosen one of the filters above, or start over with a fresh search.
          </p>
          <button
              type="button"
              @click="resetAll"
              class="focus-ring inline-flex items-center gap-2 h-11 px-5 rounded-full
                   bg-ink text-bg text-sm hover:bg-accent-2 transition-colors"
          >
            Clear all filters
          </button>
        </div>
      </template>
    </PropertyGrid>

    <Pagination
        :page="state.page"
        :page-count="pageCount"
        @change="goToPage"
    />
  </section>
</template>