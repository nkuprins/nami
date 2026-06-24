<script setup lang="ts">
import { ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import Drawer from '../ui/Drawer.vue';
import ConfirmDialog from '../ui/ConfirmDialog.vue';
import { getMyProperties, deleteProperty } from '../../api/propertiesApi';
import { formatPrice } from '../../utils/format';
import type { PropertyItem } from '../../types/propertyItem';
import IconTrash from '../icons/IconTrash.vue';
import IconSpinner from '../icons/IconSpinner.vue';

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const items = ref<PropertyItem[]>([]);
const loading = ref(false);
const error = ref(false);
const confirmId = ref<string | null>(null);
const deletingId = ref<string | null>(null);
const deleteError = ref(false);

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

async function confirmDelete() {
  const id = confirmId.value;
  if (!id) return;
  confirmId.value = null;
  deletingId.value = id;
  try {
    await deleteProperty(id);
    items.value = items.value.filter((item) => item.id !== id);
  } catch {
    deleteError.value = true;
    setTimeout(() => (deleteError.value = false), 3000);
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
      <Transition name="fade">
        <p v-if="deleteError" class="text-xs text-warn text-center py-1">
          Failed to delete. Try again.
        </p>
      </Transition>

      <div
        v-for="item in items"
        :key="item.id"
        class="relative flex rounded-xl border border-line overflow-hidden hover:border-ink/30 transition-colors"
      >
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
          <IconTrash v-if="deletingId !== item.id" />
          <IconSpinner v-else />
        </button>
      </div>
    </div>
  </Drawer>

  <!-- Reuses ConfirmDialog — renders via Teleport outside the Drawer -->
  <ConfirmDialog
    :open="confirmId !== null"
    title="Delete listing?"
    description="This listing will be permanently removed and cannot be recovered."
    confirm-label="Delete"
    danger
    @update:open="confirmId = null"
    @confirm="confirmDelete"
  />
</template>
