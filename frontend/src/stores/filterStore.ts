import {computed, reactive, watch} from 'vue';
import {defineStore} from 'pinia';
import {useRoute, useRouter} from 'vue-router';
import {
    ALL_FILTER_KEYS,
    DEFAULT_FILTER_STATE,
    FilterKey,
    type FilterState,
} from '../types/filter';
import {PropertyType} from '../types/propertyItem';
import {logger} from '../utils/logger';
import {FilterCodec} from '../utils/filterCodec';
import {SortKey} from '../types/sort';
import {Location} from '../data/rawLocations';
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
    logger.info(
        '[FiltersStore] Initial structural query parsing complete.',
        FilterCodec.toQuery(state)
    );

    const locations = computed<Location[]>(() => {
        return state.loc.flatMap((l) => {
            const cityName = cityBySlug.get(l.city);
            const districtName = districtNameBySlug.get(l.district);
            return cityName && districtName
                ? [{city: cityName, district: districtName}]
                : [];
        });
    });

    const districts = computed<string[]>(() =>
        locations.value.map((l) => l.district)
    );
    const cities = computed<string[]>(() => [
        ...new Set(locations.value.map((l) => l.city)),
    ]);

    // Handles when a user clicks the browser's Back button or lands on the site from a bookmarked link
    // watch(
    //     () => route.query,
    //     (newQuery) => {
    //         alert("HANDLE 1")
    //         const nextState = FilterCodec.fromQuery(newQuery);
    //         if (FilterCodec.isEqual(FilterCodec.toQuery(state), FilterCodec.toQuery(nextState)))
    //             return;
    //
    //         logger.debug('[FiltersStore] Browser URL alteration detected. Syncing store state.');
    //         Object.assign(state, nextState);
    //     }
    // );

    // Handles when a user is interacting with checkboxes, dropdowns, and inputs on screen
    watch(
        state,
        () => {
            const nextQuery = FilterCodec.toQuery(state);
            if (FilterCodec.isEqual(route.query, nextQuery)) return;

            logger.debug(
                '[FiltersStore] UI filter mutation detected. Replacing URL query variables.'
            );
            void router.replace({path: route.path, query: nextQuery});
        },
        {deep: true}
    );

    // --- Actions ---

    function setType(type: PropertyType) {
        if (state.type === type) return;
        logger.info(`[FiltersStore] Mode mutation: ${state.type} ➔ ${type}`);
        state.type = type;
        state.page = 1;
        if (type !== 'new_project') state.completion = undefined;
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

        state.page = 1;
    }

    function setPriceRange(min: number | undefined, max: number | undefined) {
        logger.info(
            `[FiltersStore] Price bracket modification: Min(${min}) - Max(${max})`
        );
        state.priceMin = min;
        state.priceMax = max;
        state.page = 1;
    }

    function setRooms(rooms: number[]) {
        logger.info('[FiltersStore] Targeted rooms selection changed:', rooms);
        state.rooms = [...rooms];
        state.page = 1;
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
    }

    function resetAdvanced() {
        logger.info('[FiltersStore] Discarding active advanced parameters.');
        state.m2Min = undefined;
        state.m2Max = undefined;
        state.floorMin = undefined;
        state.floorMax = undefined;
        state.notGround = undefined;
        state.notTop = undefined;
        state.yearMin = undefined;
        state.yearMax = undefined;
        state.features = [];
        state.completion = undefined;
        state.page = 1;
    }

    function resetAll() {
        logger.info(
            '[FiltersStore] Flushing state matrix back to application defaults.'
        );
        ALL_FILTER_KEYS.forEach((key: FilterKey) => {
            (state[key] as FilterState[FilterKey]) = DEFAULT_FILTER_STATE[key];
        });
    }

    return {
        locations,
        districts,
        cities,
        state,
        setType,
        setLocations,
        setPriceRange,
        setRooms,
        setSort,
        setPage,
        applyAdvanced,
        resetAdvanced,
        resetAll,
    };
});
