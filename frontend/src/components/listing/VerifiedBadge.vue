<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import IconCheck from '../icons/IconCheck.vue';

// Shown when a listing's posted figures matched the VZD cadastre. Presentational:
// call sites gate it with `v-if="listing.cadastreVerified"`.
const { t } = useI18n();
</script>

<template>
  <span
    class="inline-flex items-center gap-1 h-6 pl-1.5 pr-2.5 rounded-sm bg-bg/95 backdrop-blur micro-label text-ink!"
    :title="t('listing.verifiedHint')"
  >
    <span class="size-3.5 text-success" aria-hidden="true"><IconCheck /></span>
    {{ t('listing.verified') }}
  </span>
</template>
