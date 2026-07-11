<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import EmptyState from '../ui/EmptyState.vue';
import ConfirmDialog from '../ui/ConfirmDialog.vue';
import PropertyGroupCard from './PropertyGroupCard.vue';
import IconClose from '../icons/IconClose.vue';
import IconArrowLeft from '../icons/IconArrowLeft.vue';
import {
  getMyListings,
  deleteListing,
  deleteProperty,
  renewListing,
} from '../../api/listingsApi';
import type { ListingSummary } from '../../types/listingItem';
import { useLocaleRoute } from '../../composables/useLocaleRoute';

const ERROR_DISPLAY_MS = 3000;

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

function close() {
  emit('update:open', false);
}

const items = ref<ListingSummary[]>([]);

const { t } = useI18n();
const { localePath, localePush } = useLocaleRoute();

const loading = ref(false);
const error = ref(false);

const groups = computed(() => {
  const map = new Map<string, ListingSummary[]>();
  for (const item of items.value) {
    const arr = map.get(item.propertyId) ?? [];
    arr.push(item);
    map.set(item.propertyId, arr);
  }
  return [...map.entries()].map(([propertyId, listings]) => ({
    propertyId,
    listings,
  }));
});

async function load() {
  if (!props.open) return;
  loading.value = true;
  error.value = false;
  try {
    items.value = await getMyListings();
  } catch {
    error.value = true;
    items.value = [];
  }
  loading.value = false;
}

// Delete a single listing (property + sibling listings survive)
const confirmListingId = ref<string | null>(null);
const deletingId = ref<string | null>(null);
const deleteError = ref(false);

function requestDeleteListing(id: string) {
  confirmListingId.value = id;
}

async function confirmDeleteListing() {
  const id = confirmListingId.value;
  if (!id) return;
  confirmListingId.value = null;
  deletingId.value = id;
  try {
    await deleteListing(id);
    items.value = items.value.filter((item) => item.id !== id);
  } catch {
    deleteError.value = true;
    setTimeout(() => (deleteError.value = false), ERROR_DISPLAY_MS);
  } finally {
    deletingId.value = null;
  }
}

// Delete the whole property (cascades to all its listings)
const confirmPropertyId = ref<string | null>(null);
const deletingPropertyId = ref<string | null>(null);
const deletePropertyError = ref(false);

function requestDeleteProperty(propertyId: string) {
  confirmPropertyId.value = propertyId;
}

async function confirmDeleteProperty() {
  const propertyId = confirmPropertyId.value;
  if (!propertyId) return;
  confirmPropertyId.value = null;
  deletingPropertyId.value = propertyId;
  try {
    await deleteProperty(propertyId);
    items.value = items.value.filter((item) => item.propertyId !== propertyId);
  } catch {
    deletePropertyError.value = true;
    setTimeout(() => (deletePropertyError.value = false), ERROR_DISPLAY_MS);
  } finally {
    deletingPropertyId.value = null;
  }
}

// Renew a single listing
const renewId = ref<string | null>(null);
const renewMonths = ref(3);
const renewingId = ref<string | null>(null);
const renewError = ref(false);

function requestRenew(id: string) {
  renewMonths.value = 3;
  renewId.value = id;
}

async function confirmRenew() {
  const id = renewId.value;
  if (!id) return;
  renewId.value = null;
  renewingId.value = id;
  try {
    const updated = await renewListing(id, renewMonths.value);
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

// Add a second/third listing type to an existing property
function goAddListing(propertyId: string) {
  close();
  localePush(`/property/${propertyId}/add-listing`);
}

watch(() => props.open, load);

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') close();
}

watch(
  () => props.open,
  (val) => {
    if (val) {
      document.addEventListener('keydown', onKey);
      document.body.style.overflow = 'hidden';
    } else {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    }
  }
);

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey);
  document.body.style.overflow = '';
});
</script>

<template>
  <Teleport to="body">
    <Transition name="scrim">
      <div
        v-if="open"
        class="hidden md:block fixed inset-0 z-[45] bg-ink/45 backdrop-blur-sm"
        aria-hidden="true"
      />
    </Transition>

    <Transition name="pop">
      <div
        v-if="open"
        class="fixed inset-0 z-[45] flex flex-col md:items-center md:justify-center md:p-6"
        @click="close"
      >
        <div
          class="flex flex-col w-full h-full md:h-auto md:max-w-3xl md:max-h-[85vh] md:rounded-2xl md:shadow-lift md:border md:border-line bg-bg overflow-hidden"
          @click.stop
        >
          <header
            class="flex items-center gap-3 px-4 md:px-6 h-14 md:h-16 border-b border-line shrink-0"
          >
            <button
              type="button"
              class="md:hidden focus-ring size-9 -ml-1 grid place-items-center text-ink-2 hover:text-ink"
              :aria-label="t('listing.back')"
              @click="close"
            >
              <span class="size-5"><IconArrowLeft /></span>
            </button>
            <div class="flex-1 md:flex-none">
              <p class="micro-label">{{ t('drawers.myProperties') }}</p>
              <p class="hidden md:block text-xs text-ink-3 mt-0.5">
                {{ t('drawers.escToClose') }}
              </p>
            </div>
            <button
              type="button"
              class="hidden md:grid ml-auto focus-ring size-10 -mr-2 place-items-center text-ink-2 hover:text-ink"
              :aria-label="t('drawers.close')"
              @click="close"
            >
              <span class="size-5"><IconClose /></span>
            </button>
          </header>

          <div class="flex-1 overflow-y-auto px-4 md:px-6 py-4 md:py-6">
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
              v-else-if="groups.length === 0"
              :message="t('drawers.noPropertiesYet')"
            >
              <template #action>
                <RouterLink
                  :to="localePath('/add-listing')"
                  class="text-sm text-ink underline underline-offset-2"
                  @click="close"
                >
                  {{ t('drawers.addPropertyLink') }}
                </RouterLink>
              </template>
            </EmptyState>

            <div v-else class="flex flex-col gap-4">
              <Transition name="fade">
                <p
                  v-if="deleteError"
                  class="text-xs text-warn text-center py-1"
                >
                  {{ t('drawers.failedToDelete') }}
                </p>
              </Transition>
              <Transition name="fade">
                <p
                  v-if="deletePropertyError"
                  class="text-xs text-warn text-center py-1"
                >
                  {{ t('drawers.failedToDelete') }}
                </p>
              </Transition>
              <Transition name="fade">
                <p v-if="renewError" class="text-xs text-warn text-center py-1">
                  {{ t('drawers.renewError') }}
                </p>
              </Transition>

              <PropertyGroupCard
                v-for="group in groups"
                :key="group.propertyId"
                :listings="group.listings"
                :renewing-id="renewingId"
                :deleting-id="deletingId"
                @navigate="close"
                @renew="requestRenew"
                @delete-listing="requestDeleteListing"
                @add-listing="goAddListing"
                @delete-property="requestDeleteProperty"
              />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <ConfirmDialog
    :open="confirmListingId !== null"
    :title="t('drawers.deleteListing')"
    :description="t('drawers.deleteListingDesc')"
    :confirm-label="t('drawers.delete')"
    danger
    @update:open="confirmListingId = null"
    @confirm="confirmDeleteListing"
  />

  <ConfirmDialog
    :open="confirmPropertyId !== null"
    :title="t('drawers.deletePropertyTitle')"
    :description="t('drawers.deletePropertyDesc')"
    :confirm-label="t('drawers.deletePropertyAction')"
    danger
    @update:open="confirmPropertyId = null"
    @confirm="confirmDeleteProperty"
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
      :aria-label="t('addListing.listingDuration')"
      class="mt-2 h-10 w-full rounded-lg border border-line-2 bg-bg px-3 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink"
    >
      <option v-for="n in 6" :key="n" :value="n">
        {{ n }} {{ t('addListing.months') }}
      </option>
    </select>
  </ConfirmDialog>
</template>
