<script setup lang="ts">
import { ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import Drawer from '../ui/Drawer.vue';
import EmptyState from '../ui/EmptyState.vue';
import ConfirmDialog from '../ui/ConfirmDialog.vue';
import PropertyListItem from './PropertyListItem.vue';
import {
  getMyProperties,
  deleteProperty,
  renewProperty,
} from '../../api/propertiesApi';
import type { PropertySummary } from '../../types/propertyItem';
import { resolveTitle } from '../../types/propertyItem';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import IconTrash from '../icons/IconTrash.vue';
import IconEdit from '../icons/IconEdit.vue';
import IconRefresh from '../icons/IconRefresh.vue';
import IconSpinner from '../icons/IconSpinner.vue';

const ERROR_DISPLAY_MS = 3000;

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const items = ref<PropertySummary[]>([]);

const { t } = useI18n();
const { locale, localePath } = useLocaleRoute();

const loading = ref(false);
const error = ref(false);
const confirmId = ref<string | null>(null);
const deletingId = ref<string | null>(null);
const deleteError = ref(false);

const renewId = ref<string | null>(null);
const renewMonths = ref(3);
const renewingId = ref<string | null>(null);
const renewError = ref(false);

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
    setTimeout(() => (deleteError.value = false), ERROR_DISPLAY_MS);
  } finally {
    deletingId.value = null;
  }
}

function requestRenew(event: MouseEvent, id: string) {
  event.preventDefault();
  event.stopPropagation();
  renewMonths.value = 3;
  renewId.value = id;
}

async function confirmRenew() {
  const id = renewId.value;
  if (!id) return;
  renewId.value = null;
  renewingId.value = id;
  try {
    const updated = await renewProperty(id, renewMonths.value);
    items.value = items.value.map((item) =>
      item.id === id ? { ...item, expiresAt: updated.expiresAt } : item
    );
  } catch {
    renewError.value = true;
    setTimeout(() => (renewError.value = false), ERROR_DISPLAY_MS);
  } finally {
    renewingId.value = null;
  }
}

function isExpired(item: PropertySummary): boolean {
  return !!item.expiresAt && new Date(item.expiresAt) < new Date();
}

function formatExpiry(item: PropertySummary): string {
  if (!item.expiresAt) return '';
  const date = new Date(item.expiresAt).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
  return t('drawers.expiresOn', { date });
}

watch(() => props.open, load);
</script>

<template>
  <Drawer
    :open="open"
    :title="t('drawers.myProperties')"
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
      <p class="text-sm text-ink-2">{{ t('drawers.failedToLoad') }}</p>
    </div>

    <EmptyState
      v-else-if="items.length === 0"
      :message="t('drawers.noPropertiesYet')"
    >
      <template #action>
        <RouterLink
          :to="localePath('/add-property')"
          class="text-sm text-ink underline underline-offset-2"
          @click="emit('update:open', false)"
        >
          {{ t('drawers.addPropertyLink') }}
        </RouterLink>
      </template>
    </EmptyState>

    <div v-else class="flex flex-col gap-3">
      <Transition name="fade">
        <p v-if="deleteError" class="text-xs text-warn text-center py-1">
          {{ t('drawers.failedToDelete') }}
        </p>
      </Transition>
      <Transition name="fade">
        <p v-if="renewError" class="text-xs text-warn text-center py-1">
          {{ t('drawers.renewError') }}
        </p>
      </Transition>

      <PropertyListItem
        v-for="item in items"
        :key="item.id"
        :id="item.id"
        :title="resolveTitle(item, locale)"
        :district="item.district"
        :city="item.city"
        :price="item.price"
        :type="item.type"
        :photo="item.photo"
        @navigate="emit('update:open', false)"
      >
        <template #subtitle>
          <span
            v-if="isExpired(item)"
            class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-warn/10 text-warn"
          >
            {{ t('drawers.expired') }}
          </span>
          <p v-else-if="item.expiresAt" class="text-xs text-ink-2 truncate">
            {{ formatExpiry(item) }}
          </p>
        </template>

        <template #footer>
          <div class="grid grid-cols-3 divide-x divide-line">
            <button
              class="flex flex-col items-center gap-1.5 py-2.5 text-ink-3 hover:text-ink hover:bg-surface transition-colors disabled:opacity-40"
              :disabled="renewingId === item.id"
              @click="requestRenew($event, item.id)"
            >
              <IconSpinner v-if="renewingId === item.id" class="size-4" />
              <IconRefresh v-else class="size-4" />
              <span class="text-xs font-medium">{{ t('drawers.renew') }}</span>
            </button>
            <RouterLink
              :to="localePath(`/property/${item.id}/edit`)"
              class="flex flex-col items-center gap-1.5 py-2.5 text-ink-3 hover:text-ink hover:bg-surface transition-colors"
              @click.stop="emit('update:open', false)"
            >
              <IconEdit class="size-4" />
              <span class="text-xs font-medium">{{ t('drawers.edit') }}</span>
            </RouterLink>
            <button
              class="flex flex-col items-center gap-1.5 py-2.5 text-ink-3 hover:text-warn hover:bg-warn/5 transition-colors disabled:opacity-40"
              :disabled="deletingId === item.id"
              @click="requestDelete($event, item.id)"
            >
              <IconSpinner v-if="deletingId === item.id" class="size-4" />
              <IconTrash v-else class="size-4" />
              <span class="text-xs font-medium">{{ t('drawers.delete') }}</span>
            </button>
          </div>
        </template>
      </PropertyListItem>
    </div>
  </Drawer>

  <ConfirmDialog
    :open="confirmId !== null"
    :title="t('drawers.deleteListing')"
    :description="t('drawers.deleteListingDesc')"
    :confirm-label="t('drawers.delete')"
    danger
    @update:open="confirmId = null"
    @confirm="confirmDelete"
  />

  <ConfirmDialog
    :open="renewId !== null"
    :title="t('drawers.renewListing')"
    :description="t('drawers.renewListingDesc')"
    :confirm-label="t('drawers.renewConfirm')"
    @update:open="renewId = null"
    @confirm="confirmRenew"
  >
    <select
      v-model.number="renewMonths"
      class="mt-2 h-10 w-full rounded-lg border border-line bg-bg px-3 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-accent-2"
    >
      <option v-for="n in 6" :key="n" :value="n">
        {{ n }} {{ t('addProperty.months') }}
      </option>
    </select>
  </ConfirmDialog>
</template>
