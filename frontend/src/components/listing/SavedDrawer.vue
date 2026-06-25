<script setup lang="ts">
import { ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import Drawer from '../ui/Drawer.vue';
import EmptyState from '../ui/EmptyState.vue';
import PropertyListItem from './PropertyListItem.vue';
import IconHeart from '../icons/IconHeart.vue';
import { useSavedStore } from '../../stores/savedStore';
import { getProperty } from '../../api/propertiesApi';
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

    <EmptyState
      v-else-if="items.length === 0"
      message="No saved properties yet."
    >
      <template #icon>
        <span class="size-10 text-ink-3">
          <IconHeart :filled="false" />
        </span>
      </template>
      <template #action>
        <RouterLink
          to="/"
          class="text-sm text-ink underline underline-offset-2"
          @click="emit('update:open', false)"
        >
          Browse listings
        </RouterLink>
      </template>
    </EmptyState>

    <div v-else class="flex flex-col gap-3">
      <PropertyListItem
        v-for="item in items"
        :key="item.id"
        :id="item.id"
        :title="item.title"
        :district="item.district"
        :city="item.city"
        :price="item.price"
        :type="item.type"
        :photo="item.photos[0]"
        @navigate="emit('update:open', false)"
      >
        <template #action>
          <button
            type="button"
            class="shrink-0 w-10 grid place-items-center text-accent-2 hover:text-ink-2 transition-colors"
            @click="savedStore.toggle(item.id)"
          >
            <span class="size-5"
              ><IconHeart :filled="savedStore.isSaved(item.id)"
            /></span>
          </button>
        </template>
      </PropertyListItem>
    </div>
  </Drawer>
</template>
