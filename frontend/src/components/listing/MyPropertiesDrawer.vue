<script setup lang="ts">
import { ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import Drawer from '../ui/Drawer.vue';
import { getMyProperties } from '../../api/properties';
import { formatPrice } from '../../utils/format';
import type { PropertyItem } from '../../types/propertyItem';

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const items = ref<PropertyItem[]>([]);
const loading = ref(false);
const error = ref(false);

async function load() {
  if (!props.open) return;
  loading.value = true;
  error.value = false;
  try {
    items.value = await getMyProperties();
  } catch {
    error.value = true;
    items.value = [];
  }
  loading.value = false;
}

watch(() => props.open, load);
</script>

<template>
  <Drawer
    :open="open"
    title="My properties"
    @update:open="emit('update:open', $event)"
  >
    <div v-if="loading" class="flex flex-col gap-3">
      <div
        v-for="i in 3"
        :key="i"
        class="h-20 rounded-xl bg-surface animate-pulse"
      />
    </div>

    <div v-else-if="error" class="py-16 text-center">
      <p class="text-sm text-ink-2">Failed to load properties.</p>
    </div>

    <div
      v-else-if="items.length === 0"
      class="flex flex-col items-center justify-center gap-3 py-16 text-center"
    >
      <p class="text-sm text-ink-2">You haven't listed any properties yet.</p>
      <RouterLink
        to="/add-property"
        class="text-sm text-ink underline underline-offset-2"
        @click="emit('update:open', false)"
      >
        Add a property
      </RouterLink>
    </div>

    <div v-else class="flex flex-col gap-3">
      <RouterLink
        v-for="item in items"
        :key="item.id"
        :to="`/property/${item.id}`"
        class="flex gap-3 rounded-xl border border-line overflow-hidden hover:border-ink/30 transition-colors"
        @click="emit('update:open', false)"
      >
        <div class="shrink-0 w-24 sm:w-28 h-20 overflow-hidden bg-surface">
          <img
            v-if="item.photos[0]"
            :src="item.photos[0]"
            :alt="item.title"
            class="w-full h-full object-cover"
          />
        </div>

        <div class="flex-1 min-w-0 py-3 pr-3 flex flex-col justify-between">
          <div>
            <p class="text-sm font-medium text-ink line-clamp-1">
              {{ item.title }}
            </p>
            <p class="text-xs text-ink-3 mt-0.5 line-clamp-1">
              {{ item.district }}, {{ item.city }}
            </p>
          </div>
          <p class="text-sm font-semibold text-ink">
            {{ formatPrice(item.price, item.type) }}
          </p>
        </div>
      </RouterLink>
    </div>
  </Drawer>
</template>
