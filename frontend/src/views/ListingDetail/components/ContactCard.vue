<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import IconPhone from '../../../components/icons/IconPhone.vue';
import { formatPhone } from '../../../utils/utils';

const { t } = useI18n();

defineProps<{
  phones?: string[];
  phoneRevealed: boolean;
}>();

defineEmits<{ (e: 'reveal-phone'): void }>();

const VISIBLE_DIGITS = 7;

function maskPhone(phone: string): string {
  const formatted = formatPhone(phone);
  let digitCount = 0;
  return formatted.replace(/\d/g, (d) => {
    digitCount++;
    return digitCount > VISIBLE_DIGITS ? 'X' : d;
  });
}
</script>

<template>
  <div v-if="phones?.length">
    <div class="flex flex-col gap-2">
      <template v-if="phoneRevealed">
        <a
          v-for="(phone, i) in phones"
          :key="i"
          :href="`tel:${formatPhone(phone).replace(/\s/g, '')}`"
          class="w-full flex items-center justify-center gap-1.5 py-2.5 bg-transparent text-ink text-sm font-medium rounded-lg border border-line-2 cursor-pointer hover:bg-surface hover:border-ink-3 transition-colors"
        >
          <span class="size-4 shrink-0"><IconPhone /></span>
          {{ formatPhone(phone) }}
        </a>
      </template>
      <template v-else>
        <button
          v-for="(phone, i) in phones"
          :key="i"
          class="w-full flex items-center justify-center gap-1.5 py-2.5 bg-transparent text-ink text-sm font-medium rounded-lg border border-line-2 cursor-pointer hover:bg-surface hover:border-ink-3 transition-colors"
          @click="$emit('reveal-phone')"
        >
          <span class="size-4 shrink-0"><IconPhone /></span>
          {{ maskPhone(phone) }}
          <span class="text-xs text-ink-3 ml-1">{{ t('listing.show') }}</span>
        </button>
      </template>
    </div>
  </div>
</template>
