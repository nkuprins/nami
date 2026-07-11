<script setup lang="ts">
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import FormField from '../../../components/ui/FormField.vue';
import DescriptionField from './DescriptionField.vue';
import type { ListingFieldsForm } from '../composables/formTypes';
import {
  detectEnFieldWarning,
  detectLvFieldWarning,
} from '../../../utils/languageDetect';

type Lang = 'lv' | 'en' | 'ru';

const form = defineModel<ListingFieldsForm>('form', { required: true });
const props = defineProps<{
  fieldError: (field: string) => string | undefined;
}>();

const { t } = useI18n();
const activeLang = ref<Lang>('lv');

const hasLv = computed(
  () => form.value.titleLv.trim() || form.value.descriptionLv.trim()
);
const hasEn = computed(
  () => form.value.titleEn.trim() || form.value.descriptionEn.trim()
);
const hasRu = computed(
  () => form.value.titleRu.trim() || form.value.descriptionRu.trim()
);

const titleError = computed(() => props.fieldError('title'));
const descriptionError = computed(() => props.fieldError('description'));

const lvTitleWarning = computed(() => detectLvFieldWarning(form.value.titleLv));
const enTitleWarning = computed(() => detectEnFieldWarning(form.value.titleEn));
const lvDescWarning = computed(() =>
  detectLvFieldWarning(form.value.descriptionLv)
);
const enDescWarning = computed(() =>
  detectEnFieldWarning(form.value.descriptionEn)
);

function tabLabel(lang: Lang): string {
  if (lang === 'lv') return t('addListing.langTabLv');
  if (lang === 'en') return t('addListing.langTabEn');
  return t('addListing.langTabRu');
}

function hasContent(lang: Lang): boolean {
  if (lang === 'lv') return Boolean(hasLv.value);
  if (lang === 'en') return Boolean(hasEn.value);
  return Boolean(hasRu.value);
}

function warningMessage(w: 'latvianInEn' | 'separator' | null): string {
  if (w === 'latvianInEn') return t('addListing.langWarningLatvianInEn');
  if (w === 'separator') return t('addListing.langWarningSeparator');
  return '';
}
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.basicInfo') }}
    </h2>

    <!-- Language tabs -->
    <div>
      <div class="flex gap-1 mb-3">
        <button
          v-for="lang in ['lv', 'en', 'ru'] as const"
          :key="lang"
          type="button"
          class="flex items-center gap-1.5 px-4 h-8 rounded-full text-sm font-medium border transition-colors"
          :class="
            activeLang === lang
              ? 'bg-ink text-bg border-ink'
              : 'border-line-2 text-ink-2 hover:border-ink-3 hover:text-ink'
          "
          @click="activeLang = lang"
        >
          {{ tabLabel(lang) }}
          <span
            v-if="hasContent(lang)"
            class="size-1.5 rounded-full"
            :class="activeLang === lang ? 'bg-bg' : 'bg-ink'"
          />
        </button>
      </div>

      <p
        v-if="titleError || descriptionError"
        class="text-xs text-warn mb-2"
      >
        {{ titleError || descriptionError }}
      </p>
      <p v-else class="text-xs text-ink-3 mb-2">
        {{ t('addListing.atLeastOneLang') }}
      </p>

      <!-- Latvian fields -->
      <div v-show="activeLang === 'lv'" class="flex flex-col gap-3">
        <FormField
          id="ap-title-lv"
          :label="t('addListing.titleLabel')"
          v-model="form.titleLv"
          :placeholder="t('addListing.titlePlaceholder')"
          required
        />
        <p v-if="lvTitleWarning" class="text-xs text-ink-2 -mt-2">
          {{ warningMessage(lvTitleWarning) }}
        </p>

        <DescriptionField
          id="ap-desc-lv"
          v-model="form.descriptionLv"
          :warning="lvDescWarning ? warningMessage(lvDescWarning) : undefined"
        />
      </div>

      <!-- English fields -->
      <div v-show="activeLang === 'en'" class="flex flex-col gap-3">
        <FormField
          id="ap-title-en"
          :label="t('addListing.titleLabel')"
          v-model="form.titleEn"
          :placeholder="t('addListing.titlePlaceholder')"
          required
        />
        <p v-if="enTitleWarning" class="text-xs text-ink-2 -mt-2">
          {{ warningMessage(enTitleWarning) }}
        </p>

        <DescriptionField
          id="ap-desc-en"
          v-model="form.descriptionEn"
          :warning="enDescWarning ? warningMessage(enDescWarning) : undefined"
        />
      </div>

      <!-- Russian fields -->
      <div v-show="activeLang === 'ru'" class="flex flex-col gap-3">
        <FormField
          id="ap-title-ru"
          :label="t('addListing.titleLabel')"
          v-model="form.titleRu"
          :placeholder="t('addListing.titlePlaceholder')"
          required
        />

        <DescriptionField id="ap-desc-ru" v-model="form.descriptionRu" />
      </div>
    </div>
  </section>
</template>
