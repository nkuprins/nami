import { computed, type Component } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  KNOWN_TYPES,
  KNOWN_KINDS,
  KNOWN_COMPLETION,
  KNOWN_FEATURES,
  KNOWN_HEATING,
  KNOWN_ENERGY_CLASS,
  KNOWN_BATHROOM_LAYOUT,
  type ListingType,
  type PropertyKind,
  type PropertyCompletion,
  type Feature,
  type HeatingType,
  type EnergyClass,
  type BathroomLayout,
} from '../types/listingItem';
import IconBalcony from '../components/icons/IconBalcony.vue';
import IconParking from '../components/icons/IconParking.vue';
import IconElevator from '../components/icons/IconElevator.vue';
import IconSofa from '../components/icons/IconSofa.vue';
import IconPawPrint from '../components/icons/IconPawPrint.vue';
import IconNewBuilding from '../components/icons/IconNewBuilding.vue';
import IconStairsDown from '../components/icons/IconStairsDown.vue';

export type FeatureCategory = 'comfort' | 'building';

const FEATURE_META: Record<
  Feature,
  { icon: Component; category: FeatureCategory }
> = {
  balcony: { icon: IconBalcony, category: 'comfort' },
  furnished: { icon: IconSofa, category: 'comfort' },
  pets: { icon: IconPawPrint, category: 'comfort' },
  parking: { icon: IconParking, category: 'building' },
  elevator: { icon: IconElevator, category: 'building' },
  new_building: { icon: IconNewBuilding, category: 'building' },
  basement: { icon: IconStairsDown, category: 'building' },
};

export function usePropertyLabels() {
  const { t } = useI18n();

  const typeOptions = computed(() =>
    KNOWN_TYPES.map((id) => ({ id, label: t(`types.${id}`) }))
  );

  const categoryOptions = computed(() =>
    KNOWN_TYPES.map((id: ListingType) => ({
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
      icon: FEATURE_META[id].icon,
      category: FEATURE_META[id].category,
    }))
  );

  const heatingOptions = computed(() =>
    KNOWN_HEATING.map((id) => ({ id, label: t(`heating.${id}`) }))
  );

  const energyClassOptions = computed(() =>
    KNOWN_ENERGY_CLASS.map((id) => ({ id, label: t(`energyClass.${id}`) }))
  );

  const bathroomLayoutOptions = computed(() =>
    KNOWN_BATHROOM_LAYOUT.map((id) => ({
      id,
      label: t(`bathroomLayout.${id}`),
    }))
  );

  function typeLabel(id: ListingType): string {
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

  function featureIcon(id: Feature): Component {
    return FEATURE_META[id].icon;
  }

  function featureCategory(id: Feature): FeatureCategory {
    return FEATURE_META[id].category;
  }

  function heatingLabel(id: HeatingType): string {
    return t(`heating.${id}`);
  }

  function energyClassLabel(id: EnergyClass): string {
    return t(`energyClass.${id}`);
  }

  function bathroomLayoutLabel(id: BathroomLayout): string {
    return t(`bathroomLayout.${id}`);
  }

  return {
    typeOptions,
    categoryOptions,
    kindOptions,
    completionOptions,
    featureOptions,
    heatingOptions,
    energyClassOptions,
    bathroomLayoutOptions,
    typeLabel,
    kindLabel,
    completionLabel,
    featureLabel,
    featureIcon,
    featureCategory,
    heatingLabel,
    energyClassLabel,
    bathroomLayoutLabel,
  };
}
