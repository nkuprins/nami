<script setup lang="ts">
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import FormField from '../../../components/ui/FormField.vue';
import type { PropertyFormState } from '../composables/usePropertyForm';
import {
  detectEnFieldWarning,
  detectLvFieldWarning,
} from '../../../utils/languageDetect';

const props = defineProps<{
  form: PropertyFormState;
  fieldError: (field: string) => string | undefined;
}>();

const { t } = useI18n();
const activeLang = ref<'lv' | 'en'>('lv');

const hasLv = computed(
  () => props.form.titleLv.trim() || props.form.descriptionLv.trim()
);
const hasEn = computed(
  () => props.form.titleEn.trim() || props.form.descriptionEn.trim()
);

const titleError = computed(() => props.fieldError('title'));

const lvTitleWarning = computed(() => detectLvFieldWarning(props.form.titleLv));
const enTitleWarning = computed(() => detectEnFieldWarning(props.form.titleEn));
const lvDescWarning = computed(() =>
  detectLvFieldWarning(props.form.descriptionLv)
);
const enDescWarning = computed(() =>
  detectEnFieldWarning(props.form.descriptionEn)
);

function warningMessage(w: 'latvianInEn' | 'separator' | null): string {
  if (w === 'latvianInEn') return t('addProperty.langWarningLatvianInEn');
  if (w === 'separator') return t('addProperty.langWarningSeparator');
  return '';
}
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addProperty.basicInfo') }}
    </h2>

    <!-- Language tabs -->
    <div>
      <div class="flex gap-1 mb-3">
        <button
          v-for="lang in ['lv', 'en'] as const"
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
          {{
            lang === 'lv'
              ? t('addProperty.langTabLv')
              : t('addProperty.langTabEn')
          }}
          <span
            v-if="lang === 'lv' ? hasLv : hasEn"
            class="size-1.5 rounded-full"
            :class="activeLang === lang ? 'bg-bg' : 'bg-ink'"
          />
        </button>
      </div>

      <p v-if="titleError" class="text-xs text-red-500 mb-2">
        {{ titleError }}
      </p>
      <p v-else class="text-xs text-ink-3 mb-2">
        {{ t('addProperty.atLeastOneLang') }}
      </p>

      <!-- Latvian fields -->
      <div v-show="activeLang === 'lv'" class="flex flex-col gap-3">
        <FormField
          id="ap-title-lv"
          :label="t('addProperty.titleLabel')"
          v-model="form.titleLv"
          :placeholder="t('addProperty.titlePlaceholder')"
        />
        <p v-if="lvTitleWarning" class="text-xs text-amber-600 -mt-2">
          {{ warningMessage(lvTitleWarning) }}
        </p>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-desc-lv">
            {{ t('addProperty.descriptionLabel') }}
          </label>
          <textarea
            id="ap-desc-lv"
            v-model="form.descriptionLv"
            rows="4"
            :placeholder="t('addProperty.descriptionPlaceholder')"
            class="px-3 py-2.5 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 resize-none focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
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
          :label="t('addProperty.titleLabel')"
          v-model="form.titleEn"
          :placeholder="t('addProperty.titlePlaceholder')"
        />
        <p v-if="enTitleWarning" class="text-xs text-amber-600 -mt-2">
          {{ warningMessage(enTitleWarning) }}
        </p>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-desc-en">
            {{ t('addProperty.descriptionLabel') }}
          </label>
          <textarea
            id="ap-desc-en"
            v-model="form.descriptionEn"
            rows="4"
            :placeholder="t('addProperty.descriptionPlaceholder')"
            class="px-3 py-2.5 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 resize-none focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
          />
          <p v-if="enDescWarning" class="text-xs text-amber-600">
            {{ warningMessage(enDescWarning) }}
          </p>
        </div>
      </div>
    </div>
  </section>
</template>
