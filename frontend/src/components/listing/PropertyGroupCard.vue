<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import type { ListingSummary, ListingType } from '../../types/listingItem';
import { resolveTitle, compatibleListingTypes } from '../../types/listingItem';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import { usePropertyLabels } from '../../composables/usePropertyLabels';
import { formatPrice } from '../../utils/format';
import Popover from '../ui/Popover.vue';
import IconTrash from '../icons/IconTrash.vue';
import IconEdit from '../icons/IconEdit.vue';
import IconRefresh from '../icons/IconRefresh.vue';
import IconSpinner from '../icons/IconSpinner.vue';
import IconMore from '../icons/IconMore.vue';

const props = defineProps<{
  listings: ListingSummary[];
  renewingId: string | null;
  deletingId: string | null;
}>();

const emit = defineEmits<{
  navigate: [];
  renew: [id: string];
  'delete-listing': [id: string];
  'add-listing': [propertyId: string];
  'delete-property': [propertyId: string];
}>();

const { t } = useI18n();
const { locale, localePath } = useLocaleRoute();
const { typeLabel } = usePropertyLabels();

const representative = computed(
  () => props.listings.find((l) => l.type === 'buy') ?? props.listings[0]
);
const title = computed(() => resolveTitle(representative.value, locale.value));
const propertyId = computed(() => representative.value.propertyId);

const missingTypes = computed<ListingType[]>(() =>
  compatibleListingTypes(props.listings.map((l) => l.type))
);

function isExpired(item: ListingSummary): boolean {
  return !!item.expiresAt && new Date(item.expiresAt) < new Date();
}

function formatExpiry(item: ListingSummary): string {
  if (!item.expiresAt) return '';
  const date = new Date(item.expiresAt).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
  return t('drawers.expiresOn', { date });
}

// Property-level "more" menu (Delete property)
const propertyMenuOpen = ref(false);
const propertyMenuAnchor = ref<HTMLButtonElement | null>(null);

function requestDeleteProperty() {
  propertyMenuOpen.value = false;
  emit('delete-property', propertyId.value);
}

// Per-listing "more" menu (Renew / Delete this listing) — only one open at a time,
// matching every other dropdown/popover pattern in the app.
const openRowMenuId = ref<string | null>(null);
const rowMenuAnchors = ref<Record<string, HTMLElement | null>>({});

function setRowAnchor(id: string, el: HTMLElement | null) {
  rowMenuAnchors.value[id] = el;
}
function toggleRowMenu(id: string) {
  openRowMenuId.value = openRowMenuId.value === id ? null : id;
}
function requestRenew(id: string) {
  openRowMenuId.value = null;
  emit('renew', id);
}
function requestDeleteListing(id: string) {
  openRowMenuId.value = null;
  emit('delete-listing', id);
}
</script>

<template>
  <div class="rounded-xl border border-line overflow-hidden">
    <div class="flex items-center gap-1 p-3">
      <RouterLink
        :to="localePath(`/listing/${representative.id}`)"
        class="flex gap-3 min-w-0 flex-1 -m-1 p-1 rounded-lg hover:bg-surface transition-colors"
        @click="emit('navigate')"
      >
        <div
          class="shrink-0 size-16 md:size-20 rounded-lg overflow-hidden bg-surface"
        >
          <img
            v-if="representative.photo"
            :src="representative.photo"
            :alt="title"
            class="w-full h-full object-cover"
          />
        </div>
        <div class="min-w-0 flex-1 self-center">
          <p class="text-sm md:text-base font-semibold text-ink truncate">
            {{ title }}
          </p>
          <p class="text-xs text-ink-2 mt-0.5 truncate">
            {{ representative.location.district }},
            {{ representative.location.city }}
          </p>
        </div>
      </RouterLink>

      <button
        ref="propertyMenuAnchor"
        type="button"
        class="focus-ring shrink-0 size-8 grid place-items-center rounded-full text-ink-3 hover:text-ink hover:bg-surface transition-colors"
        :aria-label="t('drawers.moreActions')"
        @click="propertyMenuOpen = !propertyMenuOpen"
      >
        <IconMore class="size-4" />
      </button>
      <Popover
        :open="propertyMenuOpen"
        :anchor-el="propertyMenuAnchor"
        :title="t('drawers.moreActions')"
        align="end"
        :width="220"
        @update:open="(v) => (propertyMenuOpen = v)"
      >
        <RouterLink
          :to="localePath(`/property/${propertyId}/edit`)"
          class="focus-ring w-full text-left px-3 py-2 rounded-md hover:bg-surface transition-colors flex items-center gap-2"
          @click="
            () => {
              propertyMenuOpen = false;
              emit('navigate');
            }
          "
        >
          <IconEdit class="size-4" /> {{ t('drawers.editPropertyAction') }}
        </RouterLink>
        <button
          type="button"
          class="focus-ring w-full text-left px-3 py-2 rounded-md hover:bg-warn/5 text-warn transition-colors flex items-center gap-2"
          @click="requestDeleteProperty"
        >
          <IconTrash class="size-4" /> {{ t('drawers.deletePropertyAction') }}
        </button>
      </Popover>
    </div>

    <p class="micro-label text-ink-3! px-3 pb-1.5">
      {{ t('drawers.listedAs') }}
    </p>

    <div class="divide-y divide-line border-t border-line">
      <div
        v-for="item in listings"
        :key="item.id"
        class="flex items-center gap-2 px-3 py-2.5"
      >
        <div class="min-w-0 flex-1">
          <div class="flex items-center gap-1.5 flex-wrap">
            <span
              class="micro-label px-2 py-0.5 rounded-full border bg-surface border-line text-ink-2!"
            >
              {{ typeLabel(item.type) }}
            </span>
            <span
              v-if="item.expiresAt"
              class="micro-label px-2 py-0.5 rounded-full border"
              :class="
                isExpired(item)
                  ? 'bg-warn/10 border-warn/20 text-warn!'
                  : 'bg-surface border-line text-ink-2!'
              "
            >
              {{ isExpired(item) ? t('drawers.expired') : formatExpiry(item) }}
            </span>
          </div>
          <p class="text-sm font-semibold text-ink mt-1.5">
            {{ formatPrice(item.price.amount, item.type) }}
          </p>
        </div>
        <div class="flex items-center gap-1.5 shrink-0">
          <RouterLink
            :to="localePath(`/listing/${item.id}/edit`)"
            class="focus-ring h-8 px-3 inline-flex items-center gap-1.5 rounded-full border border-line text-xs font-medium text-ink hover:bg-surface transition-colors"
            @click="emit('navigate')"
          >
            <IconEdit class="size-3.5" /> {{ t('drawers.edit') }}
          </RouterLink>

          <button
            :ref="(el) => setRowAnchor(item.id, el as HTMLElement | null)"
            type="button"
            class="focus-ring size-8 grid place-items-center rounded-full text-ink-3 hover:text-ink hover:bg-surface transition-colors"
            :aria-label="t('drawers.moreActions')"
            @click="toggleRowMenu(item.id)"
          >
            <IconMore class="size-4" />
          </button>
          <Popover
            :open="openRowMenuId === item.id"
            :anchor-el="rowMenuAnchors[item.id] ?? null"
            :title="t('drawers.moreActions')"
            align="end"
            :width="200"
            @update:open="(v) => !v && (openRowMenuId = null)"
          >
            <div class="flex flex-col gap-0.5">
              <button
                type="button"
                class="focus-ring w-full text-left px-3 py-2 rounded-md hover:bg-surface transition-colors flex items-center gap-2 disabled:opacity-40"
                :disabled="renewingId === item.id"
                @click="requestRenew(item.id)"
              >
                <IconSpinner v-if="renewingId === item.id" class="size-4" />
                <IconRefresh v-else class="size-4" />
                {{ t('drawers.renew') }}
              </button>
              <button
                type="button"
                class="focus-ring w-full text-left px-3 py-2 rounded-md hover:bg-warn/5 text-warn transition-colors flex items-center gap-2 disabled:opacity-40"
                :disabled="deletingId === item.id"
                @click="requestDeleteListing(item.id)"
              >
                <IconSpinner v-if="deletingId === item.id" class="size-4" />
                <IconTrash v-else class="size-4" />
                {{ t('drawers.deleteThisListing') }}
              </button>
            </div>
          </Popover>
        </div>
      </div>
    </div>

    <div v-if="missingTypes.length" class="px-3 py-2.5 border-t border-line">
      <button
        type="button"
        class="focus-ring h-9 px-4 inline-flex items-center gap-1.5 rounded-full border border-line-2 text-xs font-semibold text-ink hover:bg-surface hover:border-ink-3 transition-colors"
        @click="emit('add-listing', propertyId)"
      >
        <span class="text-base leading-none">+</span>
        {{ t('drawers.alsoListFor', { type: typeLabel(missingTypes[0]) }) }}
      </button>
    </div>
  </div>
</template>
