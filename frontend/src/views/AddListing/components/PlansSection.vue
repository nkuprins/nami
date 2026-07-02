<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import IconChevron from '../../../components/icons/IconChevron.vue';

const { t } = useI18n();

defineProps<{
  plans: Array<{ preview: string } | { url: string }>;
}>();

const emit = defineEmits<{
  (e: 'addFiles', event: Event): void;
  (e: 'removePlan', index: number): void;
  (e: 'move', from: number, to: number): void;
}>();

function thumbSrc(entry: { preview: string } | { url: string }): string {
  return 'preview' in entry ? entry.preview : entry.url;
}

const fileInputRef = ref<HTMLInputElement | null>(null);
const dragIndex = ref<number | null>(null);

function onDrop(i: number) {
  if (dragIndex.value !== null) emit('move', dragIndex.value, i);
  dragIndex.value = null;
}
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.plansSection') }}
    </h2>
    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.plansSection') }}
      </p>

      <input
        ref="fileInputRef"
        type="file"
        accept="image/*"
        multiple
        class="hidden"
        @change="$emit('addFiles', $event)"
      />
      <button
        type="button"
        class="self-start h-9 px-4 rounded-full text-sm font-medium border border-line text-ink-2 hover:border-ink/40 hover:text-ink transition-colors"
        @click="fileInputRef?.click()"
      >
        {{ t('addListing.addPlans') }}
      </button>

      <p v-if="plans.length" class="text-xs text-ink-3 mt-1">
        {{ t('addListing.reorderHint') }}
      </p>

      <div v-if="plans.length" class="grid grid-cols-3 gap-2 mt-1">
        <div
          v-for="(entry, i) in plans"
          :key="i"
          draggable="true"
          class="relative"
          @dragstart="dragIndex = i"
          @dragover.prevent
          @drop="onDrop(i)"
        >
          <img
            :src="thumbSrc(entry)"
            class="w-full aspect-square object-cover rounded-lg"
            :alt="`Plan ${i + 1}`"
          />
          <button
            type="button"
            class="absolute top-1 right-1 size-5 rounded-full bg-ink/70 text-bg text-xs flex items-center justify-center hover:bg-ink transition-colors"
            @click="$emit('removePlan', i)"
          >
            ✕
          </button>
          <div class="absolute bottom-1 left-1 right-1 flex justify-between">
            <button
              type="button"
              class="size-5 rounded-full bg-ink/70 text-bg flex items-center justify-center hover:bg-ink transition-colors disabled:opacity-30"
              :disabled="i === 0"
              :aria-label="t('addListing.moveLeft')"
              @click="$emit('move', i, i - 1)"
            >
              <IconChevron dir="left" class="size-3" />
            </button>
            <button
              type="button"
              class="size-5 rounded-full bg-ink/70 text-bg flex items-center justify-center hover:bg-ink transition-colors disabled:opacity-30"
              :disabled="i === plans.length - 1"
              :aria-label="t('addListing.moveRight')"
              @click="$emit('move', i, i + 1)"
            >
              <IconChevron dir="right" class="size-3" />
            </button>
          </div>
        </div>
      </div>

      <p class="text-xs text-ink-3 mt-0.5">
        {{ t('addListing.plansHint') }}
      </p>
    </div>
  </section>
</template>
