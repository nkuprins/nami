<script setup lang="ts">
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import CategoryTabs from './CategoryTabs.vue';
import FilterPill from './FilterPill.vue';
import LocationPopover from '../../../components/listing/LocationPopover.vue';
import PricePopover from './popovers/PricePopover.vue';
import RoomsPopover from './popovers/RoomsPopover.vue';
import IconSearch from '../../../components/icons/IconSearch.vue';
import IconSliders from '../../../components/icons/IconSliders.vue';
import { useFiltersStore } from '../../../stores/filterStore';

const fmtNum = new Intl.NumberFormat('en-IE', { maximumFractionDigits: 0 });

const emit = defineEmits<{ search: []; openMore: [] }>();

const store = useFiltersStore();
const { districts, locations } = storeToRefs(store);
const { state, setType, setLocations, setPriceRange, setRooms } =
  useFiltersStore();

const locSummary = computed(() => {
  if (!state.loc.length) return '';
  if (state.loc.length === 1) return districts.value[0];
  if (state.loc.length <= 3) return districts.value.join(', ');
  return `${districts.value[0]} +${state.loc.length - 1}`;
});

const priceSummary = computed(() => {
  const { priceMin, priceMax, type } = state;
  const suffix = type === 'rent' ? '/mo' : '';
  const fmt = (n: number) => fmtNum.format(n);
  if (priceMin !== undefined && priceMax !== undefined)
    return `${fmt(priceMin)} – ${fmt(priceMax)}${suffix}`;
  if (priceMin !== undefined) return `From ${fmt(priceMin)}${suffix}`;
  if (priceMax !== undefined) return `Up to ${fmt(priceMax)}${suffix}`;
  return '';
});

const roomsSummary = computed(() => {
  if (!state.rooms.length) return '';
  const sorted = [...state.rooms].sort((a, b) => a - b);
  return sorted.map((n) => (n >= 7 ? '7+' : `${n}`)).join(', ') + ' rm';
});

const advancedCount = computed(() => {
  let n = 0;
  if (state.m2Min !== undefined || state.m2Max !== undefined) n++;
  if (state.floorMin !== undefined || state.floorMax !== undefined) n++;
  if (state.notGround) n++;
  if (state.notTop) n++;
  if (state.yearMin !== undefined || state.yearMax !== undefined) n++;
  n += state.features.length;
  if (state.completion) n++;
  return n;
});
</script>

<template>
  <div
    class="bg-bg/97 backdrop-blur-md rounded-2xl shadow-lift border border-line/60 overflow-hidden"
  >
    <CategoryTabs :model-value="state.type" @update:model-value="setType" />

    <div class="p-4 sm:p-5">
      <div
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-[1.4fr_1fr_1fr_auto_auto] gap-2"
      >
        <FilterPill
          label="Location"
          :summary="locSummary"
          :active="state.loc.length > 0"
          :width="380"
        >
          <LocationPopover
            :model-value="locations"
            @update:model-value="setLocations"
          />
        </FilterPill>

        <FilterPill
          label="Price"
          :summary="priceSummary"
          :active="state.priceMin !== undefined || state.priceMax !== undefined"
          :width="340"
        >
          <PricePopover
            :min="state.priceMin"
            :max="state.priceMax"
            :type="state.type"
            @update:range="(mn, mx) => setPriceRange(mn, mx)"
          />
        </FilterPill>

        <FilterPill
          label="Rooms"
          :summary="roomsSummary"
          :active="state.rooms.length > 0"
          :width="300"
        >
          <RoomsPopover
            :model-value="state.rooms"
            @update:model-value="setRooms"
          />
        </FilterPill>

        <button
          type="button"
          @click="emit('openMore')"
          class="focus-ring inline-flex items-center justify-center gap-2 h-12 px-4 rounded-md border border-line bg-bg text-sm text-ink-2 hover:text-ink hover:border-line-2 transition-colors"
        >
          <span class="size-4 inline-block"><IconSliders /></span>
          <span>More filters</span>
          <span
            v-if="advancedCount > 0"
            class="tabular text-[0.6875rem] px-1.5 h-5 inline-flex items-center rounded-full bg-ink text-cream"
          >
            {{ advancedCount }}
          </span>
        </button>

        <button
          type="button"
          @click="emit('search')"
          class="focus-ring inline-flex items-center justify-center gap-2 h-12 px-5 rounded-md bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors"
        >
          <span class="size-4 inline-block"><IconSearch /></span>
          <span>Search</span>
        </button>
      </div>
    </div>
  </div>
</template>
