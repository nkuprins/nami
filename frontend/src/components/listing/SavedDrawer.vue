<script setup lang="ts">
import { ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import Drawer from '../ui/Drawer.vue';
import IconHeart from '../icons/IconHeart.vue';
import { useSavedStore } from '../../stores/savedStore';
import { getProperty } from '../../api/propertiesApi';
import { formatPrice } from '../../utils/format';
import type { PropertyDetail } from '../../types/propertyItem';

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const savedStore = useSavedStore();
const items = ref<PropertyDetail[]>([]);
const loading = ref(false);

async function loadSaved() {
  if (!props.open) return;
  if (savedStore.ids.length === 0) {
    items.value = [];
    return;
  }

  loading.value = true;
  try {
    const results = await Promise.all(
      savedStore.ids.map((id) => getProperty(id))
    );
    items.value = results.filter((p): p is PropertyDetail => p !== undefined);
  } catch {
    items.value = [];
  }
  loading.value = false;
}

watch(() => props.open, loadSaved);
</script>

<template>
  <Drawer
    :open="open"
    title="Saved properties"
    @update:open="emit('update:open', $event)"
  >
    <div v-if="loading" class="flex flex-col gap-3">
      <div
        v-for="i in savedStore.ids.length"
        :key="i"
        class="h-20 rounded-xl bg-surface animate-pulse"
      />
    </div>

    <div
      v-else-if="items.length === 0"
      class="flex flex-col items-center justify-center gap-3 py-16 text-center"
    >
      <span class="size-10 text-ink-3">
        <IconHeart :filled="false" />
      </span>
      <p class="text-sm text-ink-2">No saved properties yet.</p>
      <RouterLink
        to="/"
        class="text-sm text-ink underline underline-offset-2"
        @click="emit('update:open', false)"
      >
        Browse listings
      </RouterLink>
    </div>

    <div v-else class="flex flex-col gap-3">
      <div
        v-for="item in items"
        :key="item.id"
        class="flex gap-3 rounded-xl border border-line overflow-hidden hover:border-ink/30 transition-colors"
      >
        <RouterLink
          :to="`/property/${item.id}`"
          class="shrink-0 w-24 sm:w-28 h-20 overflow-hidden bg-surface"
          @click="emit('update:open', false)"
        >
          <img
            v-if="item.photos[0]"
            :src="item.photos[0]"
            :alt="item.title"
            class="w-full h-full object-cover"
          />
        </RouterLink>

        <div class="flex-1 min-w-0 py-3 pr-2 flex flex-col justify-between">
          <div>
            <RouterLink
              :to="`/property/${item.id}`"
              class="text-sm font-medium text-ink line-clamp-1 hover:underline"
              @click="emit('update:open', false)"
            >
              {{ item.title }}
            </RouterLink>
            <p class="text-xs text-ink-3 mt-0.5 line-clamp-1">
              {{ item.district }}, {{ item.city }}
            </p>
          </div>
          <p class="text-sm font-semibold text-ink">
            {{ formatPrice(item.price, item.type) }}
          </p>
        </div>

        <button
          type="button"
          class="shrink-0 w-10 grid place-items-center text-accent-2 hover:text-ink-2 transition-colors"
          @click="savedStore.toggle(item.id)"
        >
          <span class="size-5"
            ><IconHeart :filled="savedStore.isSaved(item.id)"
          /></span>
        </button>
      </div>
    </div>
  </Drawer>
</template>
