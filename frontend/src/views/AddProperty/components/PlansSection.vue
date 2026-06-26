<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

defineProps<{
  plans: Array<{ preview: string }>;
}>();

defineEmits<{
  (e: 'addFiles', event: Event): void;
  (e: 'removePlan', index: number): void;
}>();

const fileInputRef = ref<HTMLInputElement | null>(null);
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addProperty.plansSection') }}
    </h2>
    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addProperty.plansSection') }}
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
        {{ t('addProperty.addPlans') }}
      </button>

      <div v-if="plans.length" class="grid grid-cols-3 gap-2 mt-1">
        <div v-for="(entry, i) in plans" :key="i" class="relative">
          <img
            :src="entry.preview"
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
        </div>
      </div>

      <p class="text-xs text-ink-3 mt-0.5">
        {{ t('addProperty.plansHint') }}
      </p>
    </div>
  </section>
</template>
