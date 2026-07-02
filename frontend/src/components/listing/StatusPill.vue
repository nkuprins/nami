<script setup lang="ts">
import { computed } from 'vue';
import { ListingSummary } from '../../types/listingItem';
import { isNew } from '../../utils/utils';
import { useI18n } from 'vue-i18n';

const props = defineProps<{ property: ListingSummary }>();
const { t } = useI18n();
const status = computed(() => isNew(props.property.postedAt));
</script>

<template>
  <span
    v-if="status"
    class="inline-flex items-center gap-1.5 h-6 pl-1.5 pr-2.5 rounded-sm bg-bg/95 backdrop-blur micro-label text-ink!"
  >
    <span class="size-1.5 rounded-full bg-success" aria-hidden="true" />
    {{ t('listing.newListing') }}
  </span>
</template>
