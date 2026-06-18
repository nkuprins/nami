import { computed, readonly, ref, watch } from 'vue';
import { defineStore } from 'pinia';
import { useAuthStore } from './authStore';
import { savedApi } from '../api/savedApi';

const STORAGE_KEY = 'nami:saved';

function load(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const arr = raw ? JSON.parse(raw) : [];
    return Array.isArray(arr)
      ? arr.filter((x): x is string => typeof x === 'string')
      : [];
  } catch {
    return [];
  }
}

export const useSavedStore = defineStore('saved', () => {
  const authStore = useAuthStore();
  const ids = ref<string[]>([]);
  const count = computed(() => ids.value.length);
  const asSet = computed(() => new Set(ids.value));

  watch(
    () => authStore.isAuthenticated,
    async (authenticated) => {
      if (authenticated) {
        ids.value = await savedApi.getSavedIds();
      } else {
        ids.value = load();
      }
    },
    { immediate: true }
  );

  function isSaved(id: string): boolean {
    return asSet.value.has(id);
  }

  async function toggle(id: string): Promise<void> {
    const saving = !asSet.value.has(id);
    ids.value = saving ? [...ids.value, id] : ids.value.filter((x) => x !== id);

    if (authStore.isAuthenticated) {
      if (saving) {
        await savedApi.save(id);
      } else {
        await savedApi.unsave(id);
      }
    } else {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(ids.value));
      } catch {
        // quota exceeded — in-memory state remains correct
      }
    }
  }

  return { ids: readonly(ids), count, isSaved, toggle };
});
