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
import {
  resolveTitle,
  type CadastreComparison,
  type PendingReview,
} from '../../types/listingItem';
import { useLocaleRoute } from '../../composables/useLocaleRoute';

// Internal moderation tool, not part of the public i18n'd site — labels are
// plain English rather than translation keys.

const { locale } = useLocaleRoute();

const items = ref<PendingReview[]>([]);

// Declared-vs-official rows to show under a held listing — only aspects the
// cadastre actually has a record for (so an unlinked parcel shows nothing).
function comparisonRows(c: CadastreComparison) {
  const num = (v: number | null, unit = '') => (v == null ? '—' : `${v}${unit}`);
  const rows: { label: string; declared: string; official: string; mismatch: boolean }[] = [];
  if (c.officialYear != null)
    rows.push({ label: 'Year', declared: num(c.declaredYear), official: num(c.officialYear), mismatch: c.yearMismatch });
  if (c.officialArea != null)
    rows.push({ label: 'Area', declared: num(c.declaredArea, ' m²'), official: num(c.officialArea, ' m²'), mismatch: c.areaMismatch });
  if (c.officialLandM2 != null)
    rows.push({ label: 'Land area', declared: num(c.declaredLandM2, ' m²'), official: num(c.officialLandM2, ' m²'), mismatch: c.landAreaMismatch });
  if (c.officialLandUse != null)
    rows.push({ label: 'Land use', declared: c.declaredLandUse ?? '—', official: c.officialLandUse, mismatch: c.landUseMismatch });
  return rows;
}
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
    items.value = items.value.filter((item) => item.listing.id !== id);
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
    items.value = items.value.filter((item) => item.listing.id !== id);
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
      <li v-for="item in items" :key="item.listing.id">
        <ListingListItem
          :id="item.listing.id"
          :title="resolveTitle(item.listing, locale) || 'Untitled listing'"
          :district="item.listing.location.district"
          :city="item.listing.location.city"
          :price="item.listing.price.amount"
          :type="item.listing.type"
          :photo="item.listing.photo ?? undefined"
        />

        <dl
          v-if="comparisonRows(item.cadastre).length"
          class="mt-2 rounded-lg border border-line-2 bg-surface px-3 py-2 flex flex-col gap-1"
        >
          <div
            v-for="row in comparisonRows(item.cadastre)"
            :key="row.label"
            class="flex items-center gap-2 text-sm"
          >
            <dt class="w-20 shrink-0 text-ink-3">{{ row.label }}</dt>
            <dd class="flex items-center gap-1.5">
              <span :class="row.mismatch ? 'text-warn font-medium' : 'text-ink'">
                {{ row.declared }}
              </span>
              <span class="text-ink-3">declared ·</span>
              <span class="text-ink">{{ row.official }}</span>
              <span class="text-ink-3">official</span>
              <span>{{ row.mismatch ? '⚠️' : '✓' }}</span>
            </dd>
          </div>
        </dl>

        <div class="flex gap-2 mt-2">
          <button
            type="button"
            :disabled="busyId === item.listing.id"
            class="h-10 flex-1 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50"
            @click="approve(item.listing.id)"
          >
            Approve
          </button>
          <button
            type="button"
            :disabled="busyId === item.listing.id"
            class="h-10 flex-1 rounded-full border border-line-2 text-sm font-medium text-ink-2 hover:text-ink hover:bg-surface hover:border-ink-3 transition-colors disabled:opacity-50"
            @click="requestReject(item.listing.id)"
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
