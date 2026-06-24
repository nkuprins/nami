<script setup lang="ts">
import { ref } from 'vue';
import FormField from '../../../components/ui/FormField.vue';
import type { PropertyFormState } from '../composables/usePropertyForm';

defineProps<{
  form: PropertyFormState;
  photos: Array<{ preview: string }>;
  fieldError: (field: string) => string | undefined;
}>();

defineEmits<{
  (e: 'addFiles', event: Event): void;
  (e: 'removePhoto', index: number): void;
}>();

const fileInputRef = ref<HTMLInputElement | null>(null);
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      Photos
    </h2>
    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        Photos <span class="text-red-500">*</span>
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
        class="self-start h-9 px-4 rounded-full text-sm font-medium border transition-colors"
        :class="
          fieldError('photos')
            ? 'border-red-400 text-red-500'
            : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
        "
        @click="fileInputRef?.click()"
      >
        + Add photos
      </button>

      <div v-if="photos.length" class="grid grid-cols-3 gap-2 mt-1">
        <div v-for="(entry, i) in photos" :key="i" class="relative">
          <img
            :src="entry.preview"
            class="w-full aspect-square object-cover rounded-lg"
            :alt="`Photo ${i + 1}`"
          />
          <button
            type="button"
            class="absolute top-1 right-1 size-5 rounded-full bg-ink/70 text-bg text-xs flex items-center justify-center hover:bg-ink transition-colors"
            @click="$emit('removePhoto', i)"
          >
            ✕
          </button>
        </div>
      </div>

      <p v-if="fieldError('photos')" class="text-xs text-red-500">
        {{ fieldError('photos') }}
      </p>

      <div class="mt-2">
        <FormField
          id="ap-video-url"
          label="Video tour URL"
          v-model="form.videoUrl"
          type="url"
          placeholder="https://www.youtube.com/watch?v=..."
        />
        <p class="text-xs text-ink-3 mt-1.5">
          Optional. Paste a link to your video tour.
        </p>
      </div>
    </div>
  </section>
</template>
