<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue';
import IconPhone from '../../../components/icons/IconPhone.vue';
import IconChevron from '../../../components/icons/IconChevron.vue';

defineProps<{
  price: string;
  pricePerM2: string;
  phones: string[] | undefined;
  phoneRevealed: boolean;
}>();

const emit = defineEmits<{
  'reveal-phone': [];
}>();

const phonePopoverOpen = ref(false);
const phonePopoverEl = ref<HTMLElement | null>(null);

function onClickOutsidePhone(e: MouseEvent) {
  if (
    phonePopoverEl.value &&
    !phonePopoverEl.value.contains(e.target as Node)
  ) {
    phonePopoverOpen.value = false;
  }
}

watch(phonePopoverOpen, (open) => {
  if (open) {
    document.addEventListener('mousedown', onClickOutsidePhone, true);
  } else {
    document.removeEventListener('mousedown', onClickOutsidePhone, true);
  }
});

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onClickOutsidePhone, true);
});
</script>

<template>
  <div
    class="lg:hidden fixed bottom-0 left-0 right-0 z-30 bg-bg/95 backdrop-blur border-t border-line px-4 py-3"
  >
    <div class="flex items-center justify-between gap-4 relative">
      <div>
        <p class="display-price text-lg text-ink">{{ price }}</p>
        <p class="text-xs text-ink-2 tabular">{{ pricePerM2 }}</p>
      </div>

      <div
        v-if="phoneRevealed && phones?.length"
        ref="phonePopoverEl"
        class="relative"
      >
        <div class="flex items-stretch bg-ink rounded-lg overflow-hidden">
          <a
            :href="`tel:${phones[0].replace(/\s/g, '')}`"
            class="flex items-center gap-1.5 px-5 py-2.5 text-cream text-sm font-medium hover:opacity-90 transition-opacity"
          >
            <span class="size-4 shrink-0"><IconPhone /></span>
            {{ phones[0] }}
          </a>
          <button
            v-if="phones.length > 1"
            class="flex items-center px-2.5 border-l border-cream/20 text-cream cursor-pointer hover:bg-white/10 transition-colors"
            @click="phonePopoverOpen = !phonePopoverOpen"
            aria-label="More phone numbers"
          >
            <span class="size-4"
              ><IconChevron :dir="phonePopoverOpen ? 'down' : 'up'"
            /></span>
          </button>
        </div>

        <Transition name="fade">
          <div
            v-if="phonePopoverOpen"
            class="absolute bottom-full right-0 mb-2 w-56 bg-bg border border-line rounded-xl shadow-lift p-2"
          >
            <a
              v-for="(phone, i) in phones"
              :key="i"
              :href="`tel:${phone.replace(/\s/g, '')}`"
              class="flex items-center gap-2 px-3 py-2.5 text-sm text-ink font-medium rounded-lg hover:bg-surface transition-colors"
            >
              <span class="size-4 shrink-0"><IconPhone /></span>
              {{ phone }}
            </a>
          </div>
        </Transition>
      </div>

      <!-- Not yet revealed -->
      <button
        v-else-if="phones?.length"
        class="flex items-center gap-1.5 px-5 py-2.5 bg-ink text-cream text-sm font-medium rounded-lg cursor-pointer hover:opacity-90 transition-opacity"
        @click="emit('reveal-phone')"
      >
        <span class="size-4 shrink-0"><IconPhone /></span>
        Show number
      </button>
    </div>
  </div>
  <div class="lg:hidden h-20" />
</template>
