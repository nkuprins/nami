import {computed, reactive} from "vue";

const STORAGE_KEY = 'nami:saved';

function load(): Set<string> {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        const arr = raw ? JSON.parse(raw) : [];

        return Array.isArray(arr)
            ? new Set(arr.filter((x): x is string => typeof x === 'string'))
            : new Set();
    } catch {
        return new Set();
    }
}

const saved = reactive(load());

export function useSaved() {
    const count = computed(() => saved.size);

    function isSaved(id: string) {
        return saved.has(id);
    }

    function toggle(id: string) {
        if (saved.has(id)) saved.delete(id);
        else saved.add(id);
        localStorage.setItem(STORAGE_KEY, JSON.stringify([...saved]));
    }

    return {count, isSaved, toggle};
}