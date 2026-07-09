<script setup lang="ts">
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { renderMarkdown } from '../../../utils/renderMarkdown';

defineProps<{
  id: string;
  warning?: string;
}>();
const model = defineModel<string>({ required: true });

const { t } = useI18n();
const preview = ref(false);

const rendered = computed(() =>
  model.value.trim() ? renderMarkdown(model.value) : ''
);
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <div class="flex items-center justify-between">
      <label class="text-sm font-medium text-ink" :for="id">
        {{ t('addListing.descriptionLabel') }}
        <span class="text-red-500">*</span>
      </label>
      <div class="flex gap-1">
        <button
          type="button"
          class="px-2.5 h-6 rounded-full text-xs font-medium border transition-colors"
          :class="
            !preview
              ? 'bg-ink text-bg border-ink'
              : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
          "
          @click="preview = false"
        >
          {{ t('addListing.descriptionWrite') }}
        </button>
        <button
          type="button"
          class="px-2.5 h-6 rounded-full text-xs font-medium border transition-colors"
          :class="
            preview
              ? 'bg-ink text-bg border-ink'
              : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
          "
          @click="preview = true"
        >
          {{ t('addListing.descriptionPreview') }}
        </button>
      </div>
    </div>

    <textarea
      v-show="!preview"
      :id="id"
      v-model="model"
      rows="14"
      :placeholder="t('addListing.descriptionPlaceholder')"
      class="px-3 py-2.5 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 resize-y min-h-80 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
    />
    <div
      v-show="preview"
      class="px-3 py-2.5 rounded-lg border border-line bg-bg min-h-80 overflow-auto"
    >
      <div
        v-if="rendered"
        class="text-sm text-ink-2 leading-relaxed prose-description"
        v-html="rendered"
      />
      <p v-else class="text-sm text-ink-3 italic">
        {{ t('addListing.descriptionPreviewEmpty') }}
      </p>
    </div>

    <p v-if="warning" class="text-xs text-amber-600">
      {{ warning }}
    </p>
  </div>
</template>
