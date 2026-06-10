<script setup lang="ts">
import {computed, ref} from 'vue';
import {DISTRICTS} from "../../data/locations";

const props = defineProps<{ modelValue: string[] }>();
const emit = defineEmits<{ 'update:modelValue': [value: string[]] }>();

const query = ref('');

const grouped = computed(() => {
  const filter = query.value.trim().toLowerCase();
  const filtered = filter
      ? DISTRICTS.filter(
          (d) =>
              d.name.toLowerCase().includes(filter) ||
              d.city.toLowerCase().includes(filter) ||
              d.slug.includes(filter),
      )
      : DISTRICTS;
  const map = new Map<string, typeof DISTRICTS>();
  for (const d of filtered) {
    const arr = map.get(d.city) ?? [];
    arr.push(d);
    map.set(d.city, arr);
  }
  return [...map.entries()];
});

function toggle(slug: string) {
  const set = new Set(props.modelValue);
  if (set.has(slug)) set.delete(slug);
  else set.add(slug);
  emit('update:modelValue', [...set]);
}

function clear() {
  emit('update:modelValue', []);
  query.value = '';
}
</script>

<template>
  <div class="space-y-4 min-w-70">
    <input
        v-model="query"
        type="search"
        placeholder="Find a district…"
        class="focus-ring w-full h-10 px-3 rounded-md border border-line bg-bg text-sm
             placeholder:text-ink-3"
    />
    <div class="space-y-4 max-h-72 overflow-y-auto pr-1">
      <div v-for="[city, items] in grouped" :key="city" class="space-y-1.5">
        <p class="micro-label">{{ city }}</p>
        <div class="grid grid-cols-2 gap-1">
          <label
              v-for="d in items"
              :key="d.slug"
              class="focus-ring flex items-center gap-2 px-2 py-1.5 rounded-md
                   text-sm text-ink cursor-pointer hover:bg-surface"
          >
            <input
                type="checkbox"
                class="accent-ink size-4"
                :checked="modelValue.includes(d.slug)"
                @change="toggle(d.slug)"
            />
            <span>{{ d.name }}</span>
          </label>
        </div>
      </div>
      <p v-if="grouped.length === 0" class="text-sm text-ink-3 py-2">
        Nothing matches “{{ query }}”.
      </p>
    </div>
    <div class="flex items-center justify-between pt-3 border-t border-line">
      <button
          type="button"
          class="focus-ring text-xs text-ink-2 underline underline-offset-4 hover:text-ink"
          @click="clear"
      >
        Clear
      </button>
      <p class="micro-label">{{ modelValue.length || 'Any' }} selected</p>
    </div>
  </div>
</template>