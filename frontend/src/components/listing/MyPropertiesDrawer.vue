<script setup lang="ts">
import { ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import Drawer from '../ui/Drawer.vue';
import { getMyProperties, deleteProperty } from '../../api/properties';
import { formatPrice } from '../../utils/format';
import type { PropertyItem } from '../../types/propertyItem';
import {logger} from "../../utils/logger";

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const items = ref<PropertyItem[]>([]);
const loading = ref(false);
const error = ref(false);
const confirmId = ref<string | null>(null);
const deletingId = ref<string | null>(null);

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

function requestDelete(event: MouseEvent, id: string) {
  event.preventDefault();
  event.stopPropagation();
  confirmId.value = id;
}

function cancelDelete(event: MouseEvent) {
  event.preventDefault();
  event.stopPropagation();
  confirmId.value = null;
}

async function confirmDelete(event: MouseEvent, id: string) {
  event.preventDefault();
  event.stopPropagation();
  confirmId.value = null;
  deletingId.value = id;
  try {
    await deleteProperty(id);
    items.value = items.value.filter(item => item.id !== id);
  } catch {
    logger.error("[MyPropertiesDrawer] Could not delete: ", id)
  } finally {
    deletingId.value = null;
  }
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
      <div
          v-for="item in items"
          :key="item.id"
          class="relative flex rounded-xl border overflow-hidden transition-colors"
          :class="confirmId === item.id ? 'border-warn/40' : 'border-line hover:border-ink/30'"
      >
        <Transition name="fade">
          <div
              v-if="confirmId === item.id"
              class="absolute inset-0 z-10 flex items-center justify-between gap-2 px-4 bg-bg/95 backdrop-blur-sm"
          >
            <p class="text-sm text-ink bold"><span class="font-bold">Delete this listing?</span><br>
              You will not be able to recover it</p>
            <div class="flex gap-2">
              <button
                  class="text-xs px-3 py-1.5 rounded-lg border border-line text-ink-2 hover:bg-surface transition-colors"
                  @click="cancelDelete($event)"
              >
                Cancel
              </button>
              <button
                  class="text-xs px-3 py-1.5 rounded-lg bg-warn text-bg font-medium hover:bg-warn/90 transition-colors"
                  @click="confirmDelete($event, item.id)"
              >
                Delete
              </button>
            </div>
          </div>
        </Transition>

        <RouterLink
            :to="`/property/${item.id}`"
            class="flex flex-1 min-w-0 gap-3"
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

          <div class="flex-1 min-w-0 py-3 flex flex-col justify-between">
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

        <!-- Delete button -->
        <button
            class="shrink-0 self-stretch flex items-center justify-center w-10 border-l border-line text-ink-3 hover:text-warn hover:bg-warn/5 transition-colors"
            :disabled="deletingId === item.id"
            :aria-label="`Delete ${item.title}`"
            @click="requestDelete($event, item.id)"
        >
          <svg
              v-if="deletingId !== item.id"
              xmlns="http://www.w3.org/2000/svg"
              width="15"
              height="15"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.75"
              stroke-linecap="round"
              stroke-linejoin="round"
          >
            <path d="M3 6h18" />
            <path d="M19 6l-1 14H6L5 6" />
            <path d="M8 6V4h8v2" />
          </svg>
          <svg
              v-else
              xmlns="http://www.w3.org/2000/svg"
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="animate-spin"
          >
            <path d="M21 12a9 9 0 1 1-6.219-8.56" />
          </svg>
        </button>
      </div>
    </div>
  </Drawer>
</template>