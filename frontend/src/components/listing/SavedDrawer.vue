<script setup lang="ts">
import { ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import Drawer from '../ui/Drawer.vue';
import EmptyState from '../ui/EmptyState.vue';
import ListingListItem from './ListingListItem.vue';
import IconHeart from '../icons/IconHeart.vue';
import { useSavedStore } from '../../stores/savedStore';
import { getListing } from '../../api/listingsApi';
import type { ListingDetail } from '../../types/listingItem';
import { resolveTitle } from '../../types/listingItem';
import { useLocaleRoute } from '../../composables/useLocaleRoute';

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const { t } = useI18n();
const { locale, localePath } = useLocaleRoute();
const savedStore = useSavedStore();
const items = ref<ListingDetail[]>([]);
const loading = ref(false);

async function loadSaved() {
  if (!props.open) return;
  if (savedStore.ids.length === 0) {
    items.value = [];
    return;
  }

  loading.value = true;
  try {
    const results = await Promise.all(
      savedStore.ids.map((id) => getListing(id))
    );
    items.value = results.filter((p): p is ListingDetail => p !== undefined);
  } catch {
    items.value = [];
  }
  loading.value = false;
}

watch(() => props.open, loadSaved);
</script>

<template>
  <Drawer
    :open="open"
    :title="t('drawers.savedProperties')"
    @update:open="emit('update:open', $event)"
  >
    <div v-if="loading" class="flex flex-col gap-3">
      <div
        v-for="i in savedStore.ids.length"
        :key="i"
        class="h-20 rounded-xl bg-surface animate-pulse"
      />
    </div>

    <EmptyState
      v-else-if="items.length === 0"
      message="No saved properties yet."
    >
      <template #icon>
        <span class="size-10 text-ink-3">
          <IconHeart :filled="false" />
        </span>
      </template>
      <template #action>
        <RouterLink
          :to="localePath('/')"
          class="text-sm text-ink underline underline-offset-2"
          @click="emit('update:open', false)"
        >
          {{ t('drawers.browseListings') }}
        </RouterLink>
      </template>
    </EmptyState>

    <div v-else class="flex flex-col gap-3">
      <ListingListItem
        v-for="item in items"
        :key="item.id"
        :id="item.id"
        :title="resolveTitle(item, locale)"
        :district="item.location.district"
        :city="item.location.city"
        :price="item.price.amount"
        :type="item.type"
        :photo="item.media.photos?.[0]"
        @navigate="emit('update:open', false)"
      >
        <template #action>
          <button
            type="button"
            class="shrink-0 w-10 grid place-items-center text-accent-2 hover:text-ink-2 transition-colors"
            @click="savedStore.toggle(item.id)"
          >
            <span class="size-5"
              ><IconHeart :filled="savedStore.isSaved(item.id)"
            /></span>
          </button>
        </template>
      </ListingListItem>
    </div>
  </Drawer>
</template>
