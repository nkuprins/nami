<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import FormField from '../../../components/ui/FormField.vue';
import IconChevron from '../../../components/icons/IconChevron.vue';
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

      <div v-if="photos.length" class="grid grid-cols-3 gap-2 mt-1">
        <div
          v-for="(entry, i) in photos"
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
            :alt="`Uploaded listing preview ${i + 1}`"
          />
          <span
            v-if="i === 0"
            class="absolute top-1 left-1 px-1.5 py-0.5 rounded-full bg-ink/70 text-bg text-[10px] font-medium"
          >
            {{ t('addListing.coverPhoto') }}
          </span>
          <button
            type="button"
            class="absolute top-1 right-1 size-5 rounded-full bg-ink/70 text-bg text-xs flex items-center justify-center hover:bg-ink transition-colors"
            @click="$emit('removePhoto', i)"
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
              :disabled="i === photos.length - 1"
              :aria-label="t('addListing.moveRight')"
              @click="$emit('move', i, i + 1)"
            >
              <IconChevron dir="right" class="size-3" />
            </button>
          </div>
        </div>
      </div>

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
