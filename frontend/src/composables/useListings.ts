import {computed, ref, type Ref, watch} from 'vue';
import {PropertyItem} from "../types/propertyItem";
import {DEFAULT_FILTER_STATE, FilterState, PAGE_SIZE} from "../types/filter";
import {countProperties, listProperties} from '../api/listings';
import {logger} from '../utils/logger';

type Source = FilterState | Ref<FilterState> | (() => FilterState);

function read(src: Source): FilterState {
    if (typeof src === 'function') return src();
    if ('value' in src) return src.value;
    return src as FilterState;
}

export function useListings(source: Source) {
    const items = ref<PropertyItem[]>([]);
    const total = ref(0);
    const loading = ref(true);
    const error = ref<string | null>(null);
    const pageCount = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)));

    let token = 0;

    async function refresh() {
        const id = ++token;
        loading.value = true;
        error.value = null;
        try {
            const q = read(source);
            const out = await listProperties(q);
            if (id !== token) return;
            items.value = out.items;
            total.value = out.total;
        } catch (e) {
            if (id !== token) return;
            error.value = e instanceof Error ? e.message : 'Failed to load listings.';
            logger.error('useListings refresh failed:', e);
        } finally {
            if (id === token) loading.value = false;
        }
    }

    watch(
        () => JSON.stringify(read(source)),
        () => {
            void refresh();
        },
        {immediate: true},
    );

    return {items, total, pageCount, loading, error, refresh};
}

export function useListingCount(draft: () => Partial<FilterState>, base: () => FilterState) {
    const count = ref(0);
    const error = ref<string | null>(null);
    let token = 0;

    async function refresh() {
        const id = ++token;
        const merged: FilterState = {
            ...DEFAULT_FILTER_STATE,
            ...base(),
            ...draft(),
            page: 1,
        };
        try {
            const n = await countProperties(merged);
            if (id !== token) return;
            count.value = n;
            error.value = null;
        } catch (e) {
            if (id !== token) return;
            error.value = e instanceof Error ? e.message : 'Failed to count listings.';
            logger.error('useListingCount refresh failed:', e);
        }
    }

    watch(
        () => JSON.stringify({...base(), ...draft()}),
        () => {
            void refresh();
        },
        {immediate: true},
    );
    return {count, error};
}
