<script setup lang="ts">
import IconPhone from '../../../components/icons/IconPhone.vue';

defineProps<{
  phones?: string[];
  phoneRevealed: boolean;
}>();

defineEmits<{ (e: 'reveal-phone'): void }>();

const VISIBLE_DIGITS = 7;

function maskPhone(phone: string): string {
  return phone.length > VISIBLE_DIGITS
    ? phone.slice(0, VISIBLE_DIGITS) +
        phone.slice(VISIBLE_DIGITS).replace(/\d/g, 'X')
    : phone;
}
</script>

<template>
  <div v-if="phones?.length">
    <div class="flex flex-col gap-2">
      <template v-if="phoneRevealed">
        <a
          v-for="(phone, i) in phones"
          :key="i"
          :href="`tel:${phone.replace(/\s/g, '')}`"
          class="w-full flex items-center justify-center gap-1.5 py-2.5 bg-transparent text-ink text-sm font-medium rounded-lg border border-line cursor-pointer hover:bg-surface transition-colors"
        >
          <span class="size-4 shrink-0"><IconPhone /></span>
          {{ phone }}
        </a>
      </template>
      <template v-else>
        <button
          v-for="(phone, i) in phones"
          :key="i"
          class="w-full flex items-center justify-center gap-1.5 py-2.5 bg-transparent text-ink text-sm font-medium rounded-lg border border-line cursor-pointer hover:bg-surface transition-colors"
          @click="$emit('reveal-phone')"
        >
          <span class="size-4 shrink-0"><IconPhone /></span>
          {{ maskPhone(phone) }}
          <span class="text-xs text-ink-3 ml-1">Show</span>
        </button>
      </template>
    </div>
  </div>
</template>
