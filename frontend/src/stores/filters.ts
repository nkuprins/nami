import {reactive, watch} from 'vue';
import {defineStore} from 'pinia';
import type {LocationQuery} from 'vue-router';
import {useRoute, useRouter} from 'vue-router';
import {
    DEFAULT_FILTER_STATE,
    type FilterState,
    type SortKey,
} from '../types/filter';
import {Feature, PropertyType} from '../types/propertyItem';
import {logger} from '../utils/logger';

// ==========================================
// 1. VALIDATION CONSTANTS & HOOKS
// ==========================================
const KNOWN_FEATURES: Feature[] = [
    'balcony',
    'parking',
    'elevator',
    'furnished',
    'pets',
    'new_building',
];
const KNOWN_SORTS: SortKey[] = [
    'newest',
    'price-asc',
    'price-desc',
    'price-per-m2-asc',
    'm2-desc',
];
const KNOWN_TYPES: PropertyType[] = ['buy', 'rent', 'new_project'];
const KNOWN_COMPLETION: Array<'ready' | 'not_ready'> = ['ready', 'not_ready'];

const ALL_FILTER_KEYS: Array<keyof FilterState> = [
    'type',
    'loc',
    'priceMin',
    'priceMax',
    'rooms',
    'm2Min',
    'm2Max',
    'floorMin',
    'floorMax',
    'notGround',
    'notTop',
    'yearMin',
    'yearMax',
    'features',
    'completion',
    'sort',
    'page',
];

// ==========================================
// 2. DATA TRANSFORMERS (PURL UTILITIES)
// ==========================================
const FilterCodec = {
    parseNumber(v: unknown): number | undefined {
        if (v === undefined || v === null || v === '') return undefined;
        const n = Number(v);
        return Number.isFinite(n) && n >= 0 ? Math.floor(n) : undefined;
    },

    parseList(v: unknown): string[] {
        if (typeof v !== 'string' || v === '') return [];
        return v
            .split(',')
            .map((s) => s.trim())
            .filter(Boolean);
    },

    fromQuery(q: LocationQuery): FilterState {
        const typeRaw = String(q.type ?? '');
        const sortRaw = String(q.sort ?? '');
        const completionRaw = String(q.completion ?? '');

        return {
            type: KNOWN_TYPES.includes(typeRaw as PropertyType)
                ? (typeRaw as PropertyType)
                : DEFAULT_FILTER_STATE.type,
            sort: KNOWN_SORTS.includes(sortRaw as SortKey)
                ? (sortRaw as SortKey)
                : DEFAULT_FILTER_STATE.sort,
            completion: KNOWN_COMPLETION.includes(completionRaw as any)
                ? (completionRaw as 'ready' | 'not_ready')
                : undefined,
            loc: this.parseList(q.loc),
            rooms: this.parseList(q.rooms)
                .map(this.parseNumber)
                .filter((n): n is number => n !== undefined),
            features: this.parseList(q.features).filter((s): s is Feature =>
                KNOWN_FEATURES.includes(s as Feature)
            ),
            priceMin: this.parseNumber(q.priceMin),
            priceMax: this.parseNumber(q.priceMax),
            m2Min: this.parseNumber(q.m2Min),
            m2Max: this.parseNumber(q.m2Max),
            floorMin: this.parseNumber(q.floorMin),
            floorMax: this.parseNumber(q.floorMax),
            yearMin: this.parseNumber(q.yearMin),
            yearMax: this.parseNumber(q.yearMax),
            notGround: q.notGround === '1' ? true : undefined,
            notTop: q.notTop === '1' ? true : undefined,
            page: Math.max(1, this.parseNumber(q.page) ?? 1),
        };
    },

    toQuery(state: FilterState): LocationQuery {
        const q: LocationQuery = {};
        if (state.type !== DEFAULT_FILTER_STATE.type) q.type = state.type;
        if (state.loc.length) q.loc = state.loc.join(',');
        if (state.priceMin !== undefined) q.priceMin = String(state.priceMin);
        if (state.priceMax !== undefined) q.priceMax = String(state.priceMax);
        if (state.rooms.length) q.rooms = state.rooms.join(',');
        if (state.m2Min !== undefined) q.m2Min = String(state.m2Min);
        if (state.m2Max !== undefined) q.m2Max = String(state.m2Max);
        if (state.floorMin !== undefined) q.floorMin = String(state.floorMin);
        if (state.floorMax !== undefined) q.floorMax = String(state.floorMax);
        if (state.notGround) q.notGround = '1';
        if (state.notTop) q.notTop = '1';
        if (state.yearMin !== undefined) q.yearMin = String(state.yearMin);
        if (state.yearMax !== undefined) q.yearMax = String(state.yearMax);
        if (state.features.length) q.features = state.features.join(',');
        if (state.completion) q.completion = state.completion;
        if (state.sort !== DEFAULT_FILTER_STATE.sort) q.sort = state.sort;
        if (state.page > 1) q.page = String(state.page);
        return q;
    },

    isEqual(a: LocationQuery, b: LocationQuery): boolean {
        const ka = Object.keys(a);
        if (ka.length !== Object.keys(b).length) return false;
        return ka.every((k) => String(a[k] ?? '') === String(b[k] ?? ''));
    },
};

// ==========================================
// 3. PINIA STATE STORE
// ==========================================
export const useFiltersStore = defineStore('filters', () => {
    const route = useRoute();
    const router = useRouter();

    const state = reactive<FilterState>(FilterCodec.fromQuery(route.query));
    logger.info(
        '[FiltersStore] Initial structural query parsing complete.',
        FilterCodec.toQuery(state)
    );

    // Handles when a user clicks the browser's Back button or lands on the site from a bookmarked link
    watch(
        () => route.query,
        (newQuery) => {
            const nextState = FilterCodec.fromQuery(newQuery);
            if (
                FilterCodec.isEqual(
                    FilterCodec.toQuery(state),
                    FilterCodec.toQuery(nextState)
                )
            )
                return;

            logger.debug(
                '[FiltersStore] Browser URL alteration detected. Syncing store state.'
            );
            Object.assign(state, nextState);
        }
    );

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

    function setLoc(loc: string[]) {
        logger.info('[FiltersStore] Location context updated:', loc);
        state.loc = [...loc];
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

        // Safe, clean, zero type-casting hacks.
        ALL_FILTER_KEYS.forEach((key) => {
            (state[key] as any) = DEFAULT_FILTER_STATE[key];
        });
    }

    return {
        state,
        setType,
        setLoc,
        setPriceRange,
        setRooms,
        setSort,
        setPage,
        applyAdvanced,
        resetAdvanced,
        resetAll,
    };
});
