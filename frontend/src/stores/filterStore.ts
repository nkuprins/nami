import { computed, reactive, ref, watch } from 'vue';
import { defineStore } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import {
  ALL_FILTER_KEYS,
  DEFAULT_FILTER_STATE,
  FilterKey,
  type FilterState,
  type StreetFilter,
} from '../types/filter';
import { Category, ListingType } from '../types/listingItem';
import { categoryProfile } from '../types/categoryRegistry';
import { logger } from '../utils/logger';
import { FilterCodec } from '../utils/filterCodec';
import { SortKey } from '../types/sort';
import { Location } from '../data/rawLocations';
import {
  cityByName,
  cityBySlug,
  districtNameBySlug,
  districtSlugByName,
} from '../data/locations';

export const useFiltersStore = defineStore('filters', () => {
  const route = useRoute();
  const router = useRouter();

  const state = reactive<FilterState>(FilterCodec.fromQuery(route.query));

  // Bumped to trigger a listings refetch for deferred (Search-gated) filters.
  const searchNonce = ref(0);
  logger.info(
    '[FiltersStore] Initial structural query parsing complete.',
    FilterCodec.toQuery(state)
  );

  const locations = computed<Location[]>(() => {
    return state.loc.flatMap((l) => {
      const cityName = cityBySlug.get(l.city);
      const districtName = districtNameBySlug.get(l.district);
      return cityName && districtName
        ? [{ city: cityName, district: districtName }]
        : [];
    });
  });

  const districts = computed<string[]>(() =>
    locations.value.map((l) => l.district)
  );
  const cities = computed<string[]>(() => [
    ...new Set(locations.value.map((l) => l.city)),
  ]);

  function syncUrl() {
    if (route.name !== 'home') return;
    const nextQuery = FilterCodec.toQuery(state);
    if (FilterCodec.isEqual(route.query, nextQuery)) return;
    router.replace({ query: nextQuery });
  }

  // Handles when a user is interacting with checkboxes, dropdowns, and inputs on screen
  watch(state, syncUrl, { deep: true });

  // Re-syncs URL when navigating back to HomeView with stale params (e.g. from /add-property)
  watch(() => route.path, syncUrl);

  // --- Actions ---

  function setType(type: ListingType) {
    if (state.type === type) return;
    logger.info(`[FiltersStore] Mode mutation: ${state.type} ➔ ${type}`);
    state.type = type;
    state.page = 1;
  }

  // Drops filter values that don't apply to the selected category, so a lingering
  // filter can't wrongly exclude every listing (e.g. a land-area bound on apartments).
  function clearInapplicableFilters(kind: Category | undefined) {
    const profile = kind ? categoryProfile(kind) : undefined;
    if (!profile || !profile.completion) state.completion = undefined;
    if (!profile || profile.plotArea === 'hidden') {
      state.landM2Min = undefined;
      state.landM2Max = undefined;
    }
    if (!profile || profile.rooms === 'hidden') state.rooms = [];
    if (!profile || !profile.floors) {
      state.floorMin = undefined;
      state.floorMax = undefined;
      state.notGround = undefined;
      state.notTop = undefined;
    }
    if (!profile || profile.subtype !== 'commercial') {
      state.commercialSubtype = undefined;
    }
    if (!profile || profile.subtype !== 'landUse') state.landUse = undefined;
  }

  function setKind(kind: Category | undefined) {
    if (state.kind === kind) return;
    logger.info(`[FiltersStore] Category mutation: ${state.kind} ➔ ${kind}`);
    state.kind = kind;
    state.page = 1;
    clearInapplicableFilters(kind);
  }

  function setLocations(humanLocs: Location[]) {
    logger.info('[FiltersStore] Changed location:', humanLocs);

    state.loc = humanLocs.flatMap((l) => {
      const citySlug = cityByName.get(l.city);
      const districtSlug = districtSlugByName.get(l.district);

      if (citySlug && districtSlug) {
        return [
          {
            city: citySlug,
            district: districtSlug,
          },
        ];
      }

      return [];
    });
  }

  function setStreets(streets: StreetFilter[]) {
    logger.info('[FiltersStore] Changed streets:', streets);
    state.streets = [...streets];
  }

  function setPriceRange(min: number | undefined, max: number | undefined) {
    logger.info(
      `[FiltersStore] Price bracket modification: Min(${min}) - Max(${max})`
    );
    state.priceMin = min;
    state.priceMax = max;
  }

  function setVatIncluded(v: boolean) {
    state.vatIncluded = v || undefined;
  }

  function setRooms(rooms: number[]) {
    logger.info('[FiltersStore] Targeted rooms selection changed:', rooms);
    state.rooms = [...rooms];
  }

  function setM2Range(min: number | undefined, max: number | undefined) {
    logger.info(`[FiltersStore] Area range: Min(${min}) - Max(${max})`);
    state.m2Min = min;
    state.m2Max = max;
  }

  function setLandRange(min: number | undefined, max: number | undefined) {
    logger.info(`[FiltersStore] Land range: Min(${min}) - Max(${max})`);
    state.landM2Min = min;
    state.landM2Max = max;
  }

  function setFloorRange(min: number | undefined, max: number | undefined) {
    logger.info(`[FiltersStore] Floor range: Min(${min}) - Max(${max})`);
    state.floorMin = min;
    state.floorMax = max;
  }

  function setNotGround(v: boolean) {
    state.notGround = v || undefined;
  }

  function setNotTop(v: boolean) {
    state.notTop = v || undefined;
  }

  // Commits the deferred pill filters (location / price / rooms) and refetches.
  function applySearch() {
    state.page = 1;
    searchNonce.value++;
  }

  function setSort(sort: SortKey) {
    logger.info(
      `[FiltersStore] Sort rearrangement applied: ${state.sort} ➔ ${sort}`
    );
    state.sort = sort;
    state.page = 1;
  }

  function setPage(page: number) {
    state.page = Math.max(1, page);
  }

  function applyAdvanced(patch: Partial<FilterState>) {
    logger.info(
      '[FiltersStore] Custom advanced filters payload appended:',
      patch
    );
    Object.assign(state, patch);
    state.page = 1;
    searchNonce.value++;
  }

  function resetAdvanced() {
    logger.info('[FiltersStore] Discarding active advanced parameters.');
    // Area, land and floor (incl. its ground/top toggles) are inline pills now.
    state.yearMin = undefined;
    state.yearMax = undefined;
    state.maintenanceCostMax = undefined;
    state.bathroomLayout = undefined;
    state.bedrooms = [];
    state.bathrooms = [];
    state.heating = [];
    state.energyClass = [];
    state.sewage = [];
    state.ventilation = [];
    state.roof = [];
    state.features = [];
    state.ventilationSystems = [];
    state.communications = [];
    state.stove = [];
    state.security = [];
    state.extras = [];
    state.parking = [];
    state.completion = undefined;
    state.page = 1;
    searchNonce.value++;
  }

  function resetAll() {
    logger.info(
      '[FiltersStore] Flushing state matrix back to application defaults.'
    );
    ALL_FILTER_KEYS.forEach((key: FilterKey) => {
      (state[key] as FilterState[FilterKey]) = DEFAULT_FILTER_STATE[key];
    });
    searchNonce.value++;
  }

  return {
    locations,
    districts,
    cities,
    state,
    searchNonce,
    setType,
    setKind,
    setLocations,
    setStreets,
    setPriceRange,
    setVatIncluded,
    setRooms,
    setM2Range,
    setLandRange,
    setFloorRange,
    setNotGround,
    setNotTop,
    applySearch,
    setSort,
    setPage,
    applyAdvanced,
    resetAdvanced,
    resetAll,
  };
});
