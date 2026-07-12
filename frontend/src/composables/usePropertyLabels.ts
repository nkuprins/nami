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
  KNOWN_SEWAGE,
  KNOWN_VENTILATION,
  type ListingType,
  type PropertyKind,
  type PropertyCompletion,
  type Feature,
  type HeatingType,
  type EnergyClass,
  type BathroomLayout,
  type SewageType,
  type VentilationType,
} from '../types/listingItem';
import IconBalcony from '../components/icons/IconBalcony.vue';
import IconParking from '../components/icons/IconParking.vue';
import IconElevator from '../components/icons/IconElevator.vue';
import IconSofa from '../components/icons/IconSofa.vue';
import IconPawPrint from '../components/icons/IconPawPrint.vue';
import IconNewBuilding from '../components/icons/IconNewBuilding.vue';
import IconStairsDown from '../components/icons/IconStairsDown.vue';
import IconRenovated from '../components/icons/IconRenovated.vue';
import IconAirConditioning from '../components/icons/IconAirConditioning.vue';
import IconTerrace from '../components/icons/IconTerrace.vue';
import IconSauna from '../components/icons/IconSauna.vue';
import IconFireplace from '../components/icons/IconFireplace.vue';
import IconUnderfloorHeating from '../components/icons/IconUnderfloorHeating.vue';
import IconMeter from '../components/icons/IconMeter.vue';
import IconStorage from '../components/icons/IconStorage.vue';
import IconCloset from '../components/icons/IconCloset.vue';
import IconPool from '../components/icons/IconPool.vue';
import IconBathtub from '../components/icons/IconBathtub.vue';
import IconShower from '../components/icons/IconShower.vue';
import IconWashingMachine from '../components/icons/IconWashingMachine.vue';
import IconBoiler from '../components/icons/IconBoiler.vue';

export type FeatureCategory = 'comfort' | 'building';

// EU energy-certificate scale (A greenest → G red), muted into the app's warm
// palette and kept light enough that ink text stays legible on every band — so
// the whole scale uses one text colour instead of flipping black/white.
const ENERGY_CLASS_COLOR: Record<EnergyClass, string> = {
  A: '#6eae7b',
  B: '#93c07c',
  C: '#c2cb77',
  D: '#e7ce6e',
  E: '#e3ac6b',
  F: '#db9068',
  G: '#d07a72',
};

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
  renovated: { icon: IconRenovated, category: 'building' },
  air_conditioning: { icon: IconAirConditioning, category: 'comfort' },
  terrace: { icon: IconTerrace, category: 'comfort' },
  sauna: { icon: IconSauna, category: 'comfort' },
  fireplace: { icon: IconFireplace, category: 'comfort' },
  underfloor_heating: { icon: IconUnderfloorHeating, category: 'comfort' },
  individual_meters: { icon: IconMeter, category: 'building' },
  storage_room: { icon: IconStorage, category: 'building' },
  walk_in_closet: { icon: IconCloset, category: 'comfort' },
  pool: { icon: IconPool, category: 'comfort' },
  bathtub: { icon: IconBathtub, category: 'comfort' },
  shower: { icon: IconShower, category: 'comfort' },
  washing_machine: { icon: IconWashingMachine, category: 'comfort' },
  boiler: { icon: IconBoiler, category: 'building' },
  glazed_balcony: { icon: IconBalcony, category: 'comfort' },
  french_balcony: { icon: IconBalcony, category: 'comfort' },
  loggia: { icon: IconBalcony, category: 'comfort' },
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
    KNOWN_HEATING.map((id) => ({
      id,
      label: t(`heating.${id}`),
      hint: t(`heating.${id}Hint`),
    }))
  );

  const energyClassOptions = computed(() =>
    KNOWN_ENERGY_CLASS.map((id) => ({
      id,
      label: t(`energyClass.${id}`),
      color: ENERGY_CLASS_COLOR[id],
    }))
  );

  const bathroomLayoutOptions = computed(() =>
    KNOWN_BATHROOM_LAYOUT.map((id) => ({
      id,
      label: t(`bathroomLayout.${id}`),
    }))
  );

  const sewageOptions = computed(() =>
    KNOWN_SEWAGE.map((id) => ({ id, label: t(`sewage.${id}`) }))
  );

  const ventilationOptions = computed(() =>
    KNOWN_VENTILATION.map((id) => ({ id, label: t(`ventilation.${id}`) }))
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

  function sewageLabel(id: SewageType): string {
    return t(`sewage.${id}`);
  }

  function ventilationLabel(id: VentilationType): string {
    return t(`ventilation.${id}`);
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
    sewageOptions,
    ventilationOptions,
    typeLabel,
    kindLabel,
    completionLabel,
    featureLabel,
    featureIcon,
    featureCategory,
    heatingLabel,
    energyClassLabel,
    bathroomLayoutLabel,
    sewageLabel,
    ventilationLabel,
  };
}
