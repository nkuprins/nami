import {reactive, watch} from 'vue';
import type {LocationQuery} from 'vue-router';
import {useRoute, useRouter} from 'vue-router';
import {DEFAULT_FILTER_STATE, type FilterState, type SortKey} from '../types/filter';
import {Feature, PropertyType} from "../types/propertyItem";


const KNOWN_FEATURES: Feature[] = [
    'balcony', 'parking', 'elevator', 'furnished', 'pets', 'new-building',
];
const KNOWN_SORTS: SortKey[] = [
    'newest', 'price-asc', 'price-desc', 'price-per-m2-asc', 'm2-desc',
];
const KNOWN_TYPES: PropertyType[] = ['buy', 'rent', 'new-project'];
const KNOWN_COMPLETION: Array<'ready' | 'not-ready'> = ['ready', 'not-ready'];

function parseInt0(v: unknown): number | undefined {
    if (v === undefined || v === null || v === '') return undefined;
    const n = Number(v);
    return Number.isFinite(n) && n >= 0 ? Math.floor(n) : undefined;
}

function parseList(v: unknown): string[] {
    if (typeof v !== 'string' || v === '') return [];
    return v.split(',').map((s) => s.trim()).filter(Boolean);
}

function parseFromQuery(q: LocationQuery): FilterState {
    const typeRaw = String(q.type ?? '');
    const type = (KNOWN_TYPES as string[]).includes(typeRaw)
        ? (typeRaw as PropertyType)
        : DEFAULT_FILTER_STATE.type;

    const sortRaw = String(q.sort ?? '');
    const sort = (KNOWN_SORTS as string[]).includes(sortRaw)
        ? (sortRaw as SortKey)
        : DEFAULT_FILTER_STATE.sort;

    const rooms = parseList(q.rooms)
        .map((s) => parseInt0(s))
        .filter((n): n is number => n !== undefined);

    const features = parseList(q.features).filter((s): s is Feature =>
        (KNOWN_FEATURES as string[]).includes(s),
    );

    const completionRaw = String(q.completion ?? '');
    const completion = (KNOWN_COMPLETION as string[]).includes(completionRaw)
        ? (completionRaw as 'ready' | 'not-ready')
        : undefined;

    return {
        type,
        loc: parseList(q.loc),
        priceMin: parseInt0(q.priceMin),
        priceMax: parseInt0(q.priceMax),
        rooms,
        m2Min: parseInt0(q.m2Min),
        m2Max: parseInt0(q.m2Max),
        floorMin: parseInt0(q.floorMin),
        floorMax: parseInt0(q.floorMax),
        notGround: q.notGround === '1' ? true : undefined,
        notTop: q.notTop === '1' ? true : undefined,
        yearMin: parseInt0(q.yearMin),
        yearMax: parseInt0(q.yearMax),
        features,
        completion,
        sort,
        page: Math.max(1, parseInt0(q.page) ?? 1),
    };
}

function toQuery(state: FilterState): LocationQuery {
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
}

function eqQuery(a: LocationQuery, b: LocationQuery): boolean {
    const ka = Object.keys(a);
    const kb = Object.keys(b);
    if (ka.length !== kb.length) return false;
    return ka.every((k) => String(a[k] ?? '') === String(b[k] ?? ''));
}

let _state: FilterState | null = null;
let _wired = false;

export function useFilters() {
    const route = useRoute();
    const router = useRouter();

    if (!_state) {
        _state = reactive(parseFromQuery(route.query));
    }
    const state = _state;

    if (!_wired) {
        _wired = true;

        watch(
            () => route.query,
            (q) => {
                const next = parseFromQuery(q);
                Object.assign(state, next);
            },
        );

        watch(
            state,
            () => {
                const next = toQuery(state);
                if (eqQuery(route.query, next)) return;
                router.replace({path: '/', query: next});
            },
            {deep: true},
        );
    }

    function setType(type: PropertyType) {
        if (state.type === type) return;
        state.type = type;
        state.page = 1;
        if (type !== 'new-project') state.completion = undefined;
    }

    function setLoc(loc: string[]) {
        state.loc = [...loc];
        state.page = 1;
    }

    function setPriceRange(min: number | undefined, max: number | undefined) {
        state.priceMin = min;
        state.priceMax = max;
        state.page = 1;
    }

    function setRooms(rooms: number[]) {
        state.rooms = [...rooms];
        state.page = 1;
    }

    function setSort(sort: SortKey) {
        state.sort = sort;
        state.page = 1;
    }

    function setPage(page: number) {
        state.page = Math.max(1, page);
    }

    function applyAdvanced(patch: Partial<FilterState>) {
        Object.assign(state, patch);
        state.page = 1;
    }

    function resetAdvanced() {
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
        Object.assign(state, structuredClone(DEFAULT_FILTER_STATE));
    }

    return {
        state,
        setType, setLoc, setPriceRange, setRooms, setSort, setPage,
        applyAdvanced, resetAdvanced, resetAll,
    };
}