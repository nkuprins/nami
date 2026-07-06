<script setup lang="ts">
import { computed, ref } from 'vue';
import PhotoLightBox from '../../../components/listing/PhotoLightBox.vue';
import IconPlayer from '../../../components/icons/IconPlayer.vue';
import { getVideoThumbnailUrl } from '../../../utils/video';
import { mediaVariant, onVariantError } from '../../../utils/mediaVariant';

const props = defineProps<{
  photos: string[];
  alt: string;
  videoUrl?: string;
}>();

const emit = defineEmits<{ 'play-video': [] }>();

const GRID_MAX_VISIBLE = 9;

const videoThumb = computed(() =>
  props.videoUrl ? getVideoThumbnailUrl(props.videoUrl) : ''
);

type Tile =
  | { type: 'photo'; src: string; index: number; overlay?: number }
  | { type: 'video' };

const tiles = computed<Tile[]>(() => {
  const visible = props.photos.slice(0, GRID_MAX_VISIBLE);
  const overflow = props.photos.length - GRID_MAX_VISIBLE;
  const photoTiles: Tile[] = visible.map((src, index) => ({
    type: 'photo',
    src,
    index,
    overlay:
      index === visible.length - 1 && overflow > 0 ? overflow : undefined,
  }));

  if (!props.videoUrl) return photoTiles;

  // Slot the video tile right before the blurred "+N" overlay tile, if present.
  if (overflow > 0) {
    const overlayTile = photoTiles.pop()!;
    return [...photoTiles, { type: 'video' }, overlayTile];
  }
  return [...photoTiles, { type: 'video' }];
});

const lightboxOpen = ref(false);
const lightboxIndex = ref(0);

function open(i: number) {
  lightboxIndex.value = i;
  lightboxOpen.value = true;
}
</script>

<template>
  <div>
    <div class="grid grid-cols-3 md:grid-cols-5 gap-1.5">
      <div
        v-for="(tile, i) in tiles"
        :key="tile.type === 'photo' ? `photo-${tile.index}` : 'video'"
        class="relative overflow-hidden rounded-lg bg-surface group aspect-square"
        :class="tile.type === 'photo' ? 'cursor-zoom-in' : 'cursor-pointer'"
        @click="tile.type === 'photo' ? open(tile.index) : emit('play-video')"
      >
        <template v-if="tile.type === 'photo'">
          <img
            :src="mediaVariant(tile.src, 'thumb')"
            :alt="`${alt} — ${tile.index + 1}`"
            class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.04]"
            loading="lazy"
            @error="(e) => onVariantError(e, tile.src)"
          />
          <div
            v-if="tile.overlay"
            class="absolute inset-0 bg-ink/60 backdrop-blur-sm flex items-center justify-center"
          >
            <span class="text-cream text-xl font-semibold tabular"
              >+{{ tile.overlay }}</span
            >
          </div>
        </template>
        <template v-else>
          <img
            v-if="videoThumb"
            :src="videoThumb"
            :alt="`${alt} — video tour`"
            class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.04]"
            loading="lazy"
          />
          <div
            v-else
            class="absolute inset-0 bg-linear-to-br from-surface via-bg to-cream"
          />
          <div
            class="absolute inset-0 bg-black/25 transition-opacity group-hover:bg-black/30"
          />
          <div class="absolute inset-0 flex items-center justify-center">
            <span
              class="flex size-10 items-center justify-center rounded-full bg-white/90 text-ink shadow-lg transition-transform group-hover:scale-105"
            >
              <IconPlayer />
            </span>
          </div>
        </template>
      </div>
    </div>

    <PhotoLightBox
      v-model:open="lightboxOpen"
      :photos="photos"
      :alt="alt"
      :initial-index="lightboxIndex"
    />
  </div>
</template>
