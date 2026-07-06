<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import FormField from '../../../components/ui/FormField.vue';
import ReorderableMediaGrid from './ReorderableMediaGrid.vue';
import type { PropertyFieldsForm } from '../composables/formTypes';

const { t } = useI18n();

const form = defineModel<PropertyFieldsForm>('form', { required: true });
defineProps<{
  photos: Array<{ preview: string } | { url: string }>;
  fieldError: (field: string) => string | undefined;
}>();

const emit = defineEmits<{
  (e: 'addFiles', event: Event): void;
  (e: 'removePhoto', index: number): void;
  (e: 'move', from: number, to: number): void;
}>();

const fileInputRef = ref<HTMLInputElement | null>(null);
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.photosSection') }}
    </h2>
    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.photosSection') }} <span class="text-red-500">*</span>
      </p>

      <input
        ref="fileInputRef"
        type="file"
        accept="image/jpeg,image/png"
        multiple
        class="hidden"
        :aria-label="t('addListing.addPhotos')"
        @change="$emit('addFiles', $event)"
      />
      <button
        type="button"
        class="self-start h-9 px-4 rounded-full text-sm font-medium border transition-colors"
        :class="
          fieldError('photos')
            ? 'border-red-400 text-red-500'
            : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
        "
        @click="fileInputRef?.click()"
      >
        {{ t('addListing.addPhotos') }}
      </button>

      <p v-if="photos.length" class="text-xs text-ink-3 mt-1">
        {{ t('addListing.reorderHint') }}
      </p>

      <ReorderableMediaGrid
        v-if="photos.length"
        :items="photos"
        :alt-text="(i) => `Uploaded listing preview ${i + 1}`"
        @remove="$emit('removePhoto', $event)"
        @move="(from, to) => $emit('move', from, to)"
      >
        <template #badge="{ index }">
          <span
            v-if="index === 0"
            class="absolute top-1 left-1 px-1.5 py-0.5 rounded-full bg-ink/70 text-bg text-[10px] font-medium"
          >
            {{ t('addListing.coverPhoto') }}
          </span>
        </template>
      </ReorderableMediaGrid>

      <p v-if="fieldError('photos')" class="text-xs text-red-500">
        {{ fieldError('photos') }}
      </p>

      <div class="mt-2">
        <FormField
          id="ap-video-url"
          :label="t('addListing.videoUrl')"
          v-model="form.videoUrl"
          type="url"
          placeholder="https://www.youtube.com/watch?v=..."
        />
        <p class="text-xs text-ink-3 mt-1.5">
          {{ t('addListing.videoUrlHint') }}
        </p>
      </div>
    </div>
  </section>
</template>
