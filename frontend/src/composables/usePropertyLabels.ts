import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  KNOWN_TYPES,
  KNOWN_KINDS,
  KNOWN_COMPLETION,
  KNOWN_FEATURES,
  type PropertyType,
  type PropertyKind,
  type PropertyCompletion,
  type Feature,
} from '../types/propertyItem';

export function usePropertyLabels() {
  const { t } = useI18n();

  const typeOptions = computed(() =>
    KNOWN_TYPES.map((id) => ({ id, label: t(`types.${id}`) }))
  );

  const categoryOptions = computed(() =>
    KNOWN_TYPES.map((id: PropertyType) => ({
      id,
      label: t(`categoryTabs.${id}.label`),
      hint: t(`categoryTabs.${id}.hint`),
    }))
  );

  const kindOptions = computed(() =>
    KNOWN_KINDS.map((id) => ({ id, label: t(`kinds.${id}`) }))
  );

  const completionOptions = computed(() =>
    KNOWN_COMPLETION.map((id) => ({ id, label: t(`completion.${id}`) }))
  );

  const featureOptions = computed(() =>
    KNOWN_FEATURES.map((id) => ({
      id,
      label: t(`features.${id}`),
      hint: t(`features.${id}Hint`),
    }))
  );

  function typeLabel(id: PropertyType): string {
    return t(`types.${id}`);
  }

  function kindLabel(id: PropertyKind): string {
    return t(`kinds.${id}`);
  }

  function completionLabel(id: PropertyCompletion): string {
    return t(`completion.${id}`);
  }

  function featureLabel(id: Feature): string {
    return t(`features.${id}`);
  }

  return {
    typeOptions,
    categoryOptions,
    kindOptions,
    completionOptions,
    featureOptions,
    typeLabel,
    kindLabel,
    completionLabel,
    featureLabel,
  };
}
