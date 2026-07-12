<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import type { ListingSummary } from '../../types/listingItem';
import { resolveTitle } from '../../types/listingItem';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import { usePropertyLabels } from '../../composables/usePropertyLabels';
import { formatPrice, joinPlace } from '../../utils/format';
import Popover from '../ui/Popover.vue';
import IconTrash from '../icons/IconTrash.vue';
import IconEdit from '../icons/IconEdit.vue';
import IconRefresh from '../icons/IconRefresh.vue';
import IconSpinner from '../icons/IconSpinner.vue';
import IconMore from '../icons/IconMore.vue';
import IconBuilding from '../icons/IconBuilding.vue';
import IconChevron from '../icons/IconChevron.vue';
import IconPlus from '../icons/IconPlus.vue';

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

// A property is now just a shared address; each listing carries its own title.
// So the group's identity is the address, and every row resolves its own title.
const representative = computed(
  () => props.listings.find((l) => l.type === 'buy') ?? props.listings[0]
);
const propertyId = computed(() => representative.value.propertyId);
const location = computed(() => representative.value.location);
const addressLine = computed(
  () =>
    location.value.address ||
    joinPlace(location.value.district, location.value.city, ', ')
);

function listingTitle(item: ListingSummary): string {
  return resolveTitle(item, locale.value) || t('drawers.untitledListing');
}

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
  <div class="rounded-xl shadow-soft bg-bg overflow-hidden">
    <!-- Group identity = the shared physical address, not any one listing -->
    <div class="flex items-start gap-2.5 px-4 pt-4 pb-3.5 bg-surface/40">
      <span class="mt-0.5 size-4 shrink-0 text-ink-3"><IconBuilding /></span>
      <div class="min-w-0 flex-1">
        <p
          class="text-[0.9375rem] md:text-base font-medium leading-tight text-ink truncate"
        >
          {{ addressLine }}
        </p>
        <p class="text-xs text-ink-2 mt-1 truncate">
          {{ joinPlace(location.district, location.city) }}
        </p>
      </div>

      <button
        ref="propertyMenuAnchor"
        type="button"
        class="focus-ring shrink-0 self-center -mr-1 size-8 grid place-items-center rounded-full text-ink-3 hover:text-ink hover:bg-line/60 transition-colors"
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
          :to="
            localePath(`/listing/${representative.id}/edit?section=location`)
          "
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

    <!-- Each row is a self-contained listing, linking to its own public page -->
    <ul class="divide-y divide-line border-t border-line">
      <li v-for="item in listings" :key="item.id" class="flex items-stretch">
        <RouterLink
          :to="localePath(`/listing/${item.id}`)"
          class="group focus-ring flex items-center gap-3 min-w-0 flex-1 pl-4 pr-2 py-3 transition-colors hover:bg-surface/60"
          @click="emit('navigate')"
        >
          <div
            class="shrink-0 size-14 md:size-16 rounded-lg overflow-hidden bg-surface"
          >
            <img
              v-if="item.photo"
              :src="item.photo"
              :alt="listingTitle(item)"
              class="w-full h-full object-cover"
            />
            <span
              v-else
              class="size-full grid place-items-center text-ink-3"
              aria-hidden="true"
            >
              <IconBuilding class="size-5" />
            </span>
          </div>

          <div class="min-w-0 flex-1">
            <p
              class="flex flex-wrap items-center gap-x-2 gap-y-0.5 font-mono text-[0.6875rem] uppercase tracking-[0.14em]"
            >
              <span class="text-ink-2">{{ typeLabel(item.type) }}</span>
              <span v-if="isExpired(item)" class="text-warn font-medium">
                {{ t('drawers.expired') }}
              </span>
              <span v-else-if="item.expiresAt" class="text-ink-3">
                {{ formatExpiry(item) }}
              </span>
            </p>
            <p
              class="text-sm text-ink truncate mt-1 group-hover:text-accent-2 transition-colors"
            >
              {{ listingTitle(item) }}
            </p>
            <p class="display-price text-base md:text-lg text-ink mt-0.5">
              {{ formatPrice(item.price.amount, item.type) }}
            </p>
          </div>

          <span
            class="shrink-0 size-4 self-center text-ink-3 opacity-0 -translate-x-1 group-hover:opacity-100 group-hover:translate-x-0 transition-all"
            aria-hidden="true"
          >
            <IconChevron dir="right" />
          </span>
        </RouterLink>

        <div class="flex items-center gap-1 shrink-0 pr-3 pl-1">
          <RouterLink
            :to="localePath(`/listing/${item.id}/edit`)"
            class="focus-ring h-8 px-3 inline-flex items-center gap-1.5 rounded-full border border-line text-xs font-medium text-ink hover:bg-surface transition-colors"
            @click="emit('navigate')"
          >
            <IconEdit class="size-3.5" />
            <span class="hidden sm:inline">{{ t('drawers.edit') }}</span>
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
            :width="272"
            @update:open="(v) => !v && (openRowMenuId = null)"
          >
            <div class="flex flex-col gap-0.5">
              <button
                type="button"
                class="focus-ring w-full text-left px-3 py-2 rounded-md hover:bg-surface transition-colors flex items-center gap-2 whitespace-nowrap disabled:opacity-40"
                :disabled="renewingId === item.id"
                @click="requestRenew(item.id)"
              >
                <IconSpinner
                  v-if="renewingId === item.id"
                  class="size-4 shrink-0"
                />
                <IconRefresh v-else class="size-4 shrink-0" />
                {{ t('drawers.renew') }}
              </button>
              <button
                type="button"
                class="focus-ring w-full text-left px-3 py-2 rounded-md hover:bg-warn/5 text-warn transition-colors flex items-center gap-2 whitespace-nowrap disabled:opacity-40"
                :disabled="deletingId === item.id"
                @click="requestDeleteListing(item.id)"
              >
                <IconSpinner
                  v-if="deletingId === item.id"
                  class="size-4 shrink-0"
                />
                <IconTrash v-else class="size-4 shrink-0" />
                {{ t('drawers.deleteThisListing') }}
              </button>
            </div>
          </Popover>
        </div>
      </li>
    </ul>

    <div class="p-3 border-t border-line">
      <button
        type="button"
        class="group focus-ring w-full h-10 inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg border border-line-2 text-xs font-semibold text-ink-2 hover:text-accent-2 hover:bg-accent/5 hover:border-accent/40 transition-colors"
        @click="emit('add-listing', propertyId)"
      >
        <IconPlus class="size-[18px] text-ink-3 group-hover:text-accent-2 transition-colors" />
        {{ t('drawers.addAnotherListing') }}
      </button>
    </div>
  </div>
</template>
