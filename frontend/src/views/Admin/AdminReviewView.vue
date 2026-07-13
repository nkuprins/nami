<script setup lang="ts">
import { onMounted, ref } from 'vue';
import ListingListItem from '../../components/listing/ListingListItem.vue';
import EmptyState from '../../components/ui/EmptyState.vue';
import ConfirmDialog from '../../components/ui/ConfirmDialog.vue';
import IconSpinner from '../../components/icons/IconSpinner.vue';
import {
  getPendingListings,
  approveListing,
  rejectListing,
} from '../../api/adminApi';
import { resolveTitle, type ListingSummary } from '../../types/listingItem';
import { useLocaleRoute } from '../../composables/useLocaleRoute';

// Internal moderation tool, not part of the public i18n'd site — labels are
// plain English rather than translation keys.

const { locale } = useLocaleRoute();

const items = ref<ListingSummary[]>([]);
const loading = ref(false);
const error = ref(false);
const busyId = ref<string | null>(null);
const rejectTargetId = ref<string | null>(null);

async function load() {
  loading.value = true;
  error.value = false;
  try {
    items.value = await getPendingListings();
  } catch {
    error.value = true;
  }
  loading.value = false;
}

onMounted(load);

async function approve(id: string) {
  busyId.value = id;
  try {
    await approveListing(id);
    items.value = items.value.filter((item) => item.id !== id);
  } finally {
    busyId.value = null;
  }
}

function requestReject(id: string) {
  rejectTargetId.value = id;
}

async function confirmReject() {
  const id = rejectTargetId.value;
  if (!id) return;
  rejectTargetId.value = null;
  busyId.value = id;
  try {
    await rejectListing(id);
    items.value = items.value.filter((item) => item.id !== id);
  } finally {
    busyId.value = null;
  }
}
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <h1 class="text-xl font-semibold text-ink mb-1">Listings pending review</h1>
    <p class="text-sm text-ink-2 mb-6">
      Held because the posted area or build year didn't match the VZD
      cadastre register.
    </p>

    <div v-if="loading" class="flex justify-center py-16">
      <span class="size-6 text-ink-3"><IconSpinner /></span>
    </div>

    <EmptyState v-else-if="error" message="Failed to load pending listings." />
    <EmptyState v-else-if="items.length === 0" message="Nothing to review." />

    <ul v-else class="flex flex-col gap-3">
      <li v-for="item in items" :key="item.id">
        <ListingListItem
          :id="item.id"
          :title="resolveTitle(item, locale) || 'Untitled listing'"
          :district="item.location.district"
          :city="item.location.city"
          :price="item.price.amount"
          :type="item.type"
          :photo="item.photo ?? undefined"
        />
        <div class="flex gap-2 mt-2">
          <button
            type="button"
            :disabled="busyId === item.id"
            class="h-10 flex-1 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50"
            @click="approve(item.id)"
          >
            Approve
          </button>
          <button
            type="button"
            :disabled="busyId === item.id"
            class="h-10 flex-1 rounded-full border border-line-2 text-sm font-medium text-ink-2 hover:text-ink hover:bg-surface hover:border-ink-3 transition-colors disabled:opacity-50"
            @click="requestReject(item.id)"
          >
            Reject
          </button>
        </div>
      </li>
    </ul>

    <ConfirmDialog
      :open="rejectTargetId !== null"
      title="Reject this listing?"
      description="The listing will be deactivated. The owner can fix it and renew the listing to be re-checked."
      confirm-label="Reject"
      danger
      @update:open="(v) => !v && (rejectTargetId = null)"
      @confirm="confirmReject"
    />
  </div>
</template>
