<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import type { ListingSummary } from '../../types/listingItem';
import { resolveTitle } from '../../types/listingItem';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import { usePropertyLabels } from '../../composables/usePropertyLabels';
import { formatPrice } from '../../utils/format';
import IconBuilding from '../icons/IconBuilding.vue';
import IconChevron from '../icons/IconChevron.vue';

defineProps<{ listings: ListingSummary[] }>();
defineEmits<{ select: [id: string] }>();

const { t } = useI18n();
const { locale } = useLocaleRoute();
const { typeLabel } = usePropertyLabels();

function listingTitle(item: ListingSummary): string {
  return resolveTitle(item, locale.value) || t('drawers.untitledListing');
}
</script>

<template>
  <div class="flex flex-col gap-4 max-w-2xl">
    <div class="flex flex-col gap-1">
      <h2 class="text-base font-semibold text-ink">
        {{ t('addListing.sourcePickerTitle') }}
      </h2>
      <p class="text-sm text-ink-2 leading-relaxed">
        {{ t('addListing.sourcePickerSubtitle') }}
      </p>
    </div>

    <ul class="flex flex-col gap-2">
      <li v-for="item in listings" :key="item.id">
        <button
          type="button"
          class="group focus-ring w-full flex items-center gap-3 rounded-xl border border-line bg-bg px-3 py-3 text-left transition-colors hover:bg-surface/60 hover:border-ink-3"
          @click="$emit('select', item.id)"
        >
          <div class="shrink-0 size-14 rounded-lg overflow-hidden bg-surface">
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
              class="font-mono text-[0.6875rem] uppercase tracking-[0.14em] text-ink-2"
            >
              {{ typeLabel(item.type) }}
            </p>
            <p
              class="text-sm text-ink truncate mt-1 group-hover:text-accent-2 transition-colors"
            >
              {{ listingTitle(item) }}
            </p>
            <p class="display-price text-base text-ink mt-0.5">
              {{ formatPrice(item.price.amount, item.type) }}
            </p>
          </div>

          <span
            class="shrink-0 size-4 self-center text-ink-3 opacity-0 -translate-x-1 group-hover:opacity-100 group-hover:translate-x-0 transition-all"
            aria-hidden="true"
          >
            <IconChevron dir="right" />
          </span>
        </button>
      </li>
    </ul>
  </div>
</template>
