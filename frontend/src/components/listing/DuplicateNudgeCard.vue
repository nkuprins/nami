<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import type { ListingSummary } from '../../types/listingItem';
import type { DuplicateMatchKind } from '../../views/ListingForm/composables/useDuplicatePropertyNudge';
import IconBuilding from '../icons/IconBuilding.vue';

const props = defineProps<{
  matchKind: DuplicateMatchKind;
  match: ListingSummary | null;
  acknowledged: boolean;
}>();

defineEmits<{ 'add-listing': []; 'acknowledge-fuzzy': [] }>();

const { t } = useI18n();
const { localePath } = useLocaleRoute();
</script>

<template>
  <Transition name="pop">
    <div
      v-if="matchKind !== 'none' && match && !acknowledged"
      class="rounded-xl border p-4 flex flex-col gap-3"
      :class="
        matchKind === 'exact'
          ? 'border-warn/40 bg-warn/5'
          : 'border-accent-2/30 bg-accent/5'
      "
    >
      <div class="flex items-start gap-2.5">
        <span class="mt-0.5 size-4 shrink-0 text-ink-2"><IconBuilding /></span>
        <div class="flex flex-col gap-1">
          <p class="text-sm font-semibold text-ink">
            {{
              matchKind === 'fuzzy'
                ? t('addListing.duplicateNudgeFuzzyTitle')
                : t('addListing.duplicateNudgeTitle')
            }}
          </p>
          <p class="text-sm text-ink-2 leading-relaxed">
            {{
              matchKind === 'fuzzy'
                ? t('addListing.duplicateNudgeFuzzy', {
                    address: match.location.address,
                  })
                : t('addListing.duplicateNudge', {
                    address: match.location.address,
                  })
            }}
          </p>
        </div>
      </div>
      <div class="flex flex-wrap items-center gap-2 pl-[1.625rem]">
        <RouterLink
          :to="localePath(`/listing/${match.id}`)"
          class="focus-ring h-9 px-4 rounded-full bg-ink text-bg text-sm font-medium flex items-center justify-center hover:bg-accent-2 transition-colors"
        >
          {{ t('addListing.duplicateNudgeView') }}
        </RouterLink>
        <button
          type="button"
          class="focus-ring h-9 px-4 rounded-full border border-ink/30 text-sm text-ink hover:bg-ink hover:text-bg hover:border-ink transition-colors"
          @click="$emit('add-listing')"
        >
          {{ t('addListing.duplicateNudgeAddListing') }}
        </button>
        <button
          v-if="matchKind === 'fuzzy'"
          type="button"
          class="focus-ring h-9 px-4 rounded-full border border-dashed border-ink-3 text-sm text-ink-2 hover:border-ink hover:text-ink transition-colors"
          @click="$emit('acknowledge-fuzzy')"
        >
          {{ t('addListing.duplicateNudgeFuzzyContinue') }}
        </button>
      </div>
    </div>
  </Transition>
</template>
