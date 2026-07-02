<script setup lang="ts">
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import FormField from '../../../components/ui/FormField.vue';
import type { ListingFieldsForm } from '../composables/formTypes';
import {
  detectEnFieldWarning,
  detectLvFieldWarning,
} from '../../../utils/languageDetect';

const props = defineProps<{
  form: ListingFieldsForm;
  fieldError: (field: string) => string | undefined;
}>();

const { t } = useI18n();
const activeLang = ref<'lv' | 'en' | 'ru'>('lv');

const hasLv = computed(
  () => props.form.titleLv.trim() || props.form.descriptionLv.trim()
);
const hasEn = computed(
  () => props.form.titleEn.trim() || props.form.descriptionEn.trim()
);
const hasRu = computed(
  () => props.form.titleRu.trim() || props.form.descriptionRu.trim()
);

const titleError = computed(() => props.fieldError('title'));
const descriptionError = computed(() => props.fieldError('description'));

const lvTitleWarning = computed(() => detectLvFieldWarning(props.form.titleLv));
const enTitleWarning = computed(() => detectEnFieldWarning(props.form.titleEn));
const lvDescWarning = computed(() =>
  detectLvFieldWarning(props.form.descriptionLv)
);
const enDescWarning = computed(() =>
  detectEnFieldWarning(props.form.descriptionEn)
);

function tabLabel(lang: 'lv' | 'en' | 'ru'): string {
  if (lang === 'lv') return t('addListing.langTabLv');
  if (lang === 'en') return t('addListing.langTabEn');
  return t('addListing.langTabRu');
}

function hasContent(lang: 'lv' | 'en' | 'ru'): boolean {
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
              : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
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
        class="text-xs text-red-500 mb-2"
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
        <p v-if="lvTitleWarning" class="text-xs text-amber-600 -mt-2">
          {{ warningMessage(lvTitleWarning) }}
        </p>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-desc-lv">
            {{ t('addListing.descriptionLabel') }}
            <span class="text-red-500">*</span>
          </label>
          <textarea
            id="ap-desc-lv"
            v-model="form.descriptionLv"
            rows="14"
            :placeholder="t('addListing.descriptionPlaceholder')"
            class="px-3 py-2.5 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 resize-y min-h-80 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
          />
          <p v-if="lvDescWarning" class="text-xs text-amber-600">
            {{ warningMessage(lvDescWarning) }}
          </p>
        </div>
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
        <p v-if="enTitleWarning" class="text-xs text-amber-600 -mt-2">
          {{ warningMessage(enTitleWarning) }}
        </p>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-desc-en">
            {{ t('addListing.descriptionLabel') }}
            <span class="text-red-500">*</span>
          </label>
          <textarea
            id="ap-desc-en"
            v-model="form.descriptionEn"
            rows="14"
            :placeholder="t('addListing.descriptionPlaceholder')"
            class="px-3 py-2.5 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 resize-y min-h-80 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
          />
          <p v-if="enDescWarning" class="text-xs text-amber-600">
            {{ warningMessage(enDescWarning) }}
          </p>
        </div>
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

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-desc-ru">
            {{ t('addListing.descriptionLabel') }}
            <span class="text-red-500">*</span>
          </label>
          <textarea
            id="ap-desc-ru"
            v-model="form.descriptionRu"
            rows="14"
            :placeholder="t('addListing.descriptionPlaceholder')"
            class="px-3 py-2.5 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 resize-y min-h-80 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
          />
        </div>
      </div>
    </div>
  </section>
</template>
