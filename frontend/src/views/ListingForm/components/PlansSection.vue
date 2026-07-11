<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import ReorderableMediaGrid from './ReorderableMediaGrid.vue';

const { t } = useI18n();

defineProps<{
  plans: Array<{ preview: string } | { url: string }>;
}>();

defineEmits<{
  (e: 'addFiles', event: Event): void;
  (e: 'removePlan', index: number): void;
  (e: 'move', from: number, to: number): void;
}>();

const fileInputRef = ref<HTMLInputElement | null>(null);
</script>

<template>
  <section class="flex flex-col gap-4">
    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.plansSection') }}
      </p>

      <input
        ref="fileInputRef"
        type="file"
        accept="image/jpeg,image/png"
        multiple
        class="hidden"
        :aria-label="t('addListing.addPlans')"
        @change="$emit('addFiles', $event)"
      />
      <button
        type="button"
        class="self-start h-9 px-4 rounded-full text-sm font-medium border border-line-2 text-ink-2 hover:border-ink-3 hover:text-ink transition-colors"
        @click="fileInputRef?.click()"
      >
        {{ t('addListing.addPlans') }}
      </button>

      <p v-if="plans.length" class="text-xs text-ink-3 mt-1">
        {{ t('addListing.reorderHint') }}
      </p>

      <ReorderableMediaGrid
        v-if="plans.length"
        :items="plans"
        :alt-text="(i) => `Plan ${i + 1}`"
        @remove="$emit('removePlan', $event)"
        @move="(from, to) => $emit('move', from, to)"
      />

      <p class="text-xs text-ink-3 mt-0.5">
        {{ t('addListing.plansHint') }}
      </p>
    </div>
  </section>
</template>
