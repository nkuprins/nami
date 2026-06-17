import {computed, readonly, ref} from 'vue';
import {defineStore} from 'pinia';

const STORAGE_KEY = 'nami:saved';

function load(): string[] {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        const arr = raw ? JSON.parse(raw) : [];
        return Array.isArray(arr) ? arr.filter((x): x is string => typeof x === 'string') : [];
    } catch {
        return [];
    }
}

export const useSavedStore = defineStore('saved', () => {
    const ids = ref<string[]>(load());
    const count = computed(() => ids.value.length);
    const asSet = computed(() => new Set(ids.value));

    function isSaved(id: string): boolean {
        return asSet.value.has(id);
    }

    function toggle(id: string): void {
        if (asSet.value.has(id)) {
            ids.value = ids.value.filter(x => x !== id);
        } else {
            ids.value = [...ids.value, id];
        }
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(ids.value));
        } catch {
            // quota exceeded — in-memory state remains correct
        }
    }

    return {ids: readonly(ids), count, isSaved, toggle};
});
