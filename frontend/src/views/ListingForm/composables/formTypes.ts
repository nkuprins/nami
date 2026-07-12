import type {
  BathroomLayout,
  EnergyClass,
  Feature,
  HeatingType,
  ListingType,
  PropertyCompletion,
  PropertyKind,
  SewageType,
  VentilationType,
} from '../../../types/listingItem';

// The physical/media fields of a listing, shared by the create form
// (AddListingView), the listing edit form (EditListingView) and the
// add-another-listing form (AddListingToPropertyView). Only the create form
// also collects `address`/`coords` (the shared location); the listing-scoped
// forms leave them untouched.
export interface PropertyFieldsForm {
  propertyKind: PropertyKind;
  address: string;
  rooms: string;
  bedrooms: string;
  bathrooms: string;
  bathroomLayout: BathroomLayout | '';
  m2: string;
  landM2: string;
  floor: string;
  totalFloors: string;
  yearBuilt: string;
  heating: HeatingType | '';
  energyClass: EnergyClass | '';
  maintenanceCost: string;
  sewage: SewageType | '';
  ventilation: VentilationType | '';
  features: Feature[];
  videoUrl: string;
  coords: { lat: number; lng: number } | null;
}

// Fields belonging to a single listing, shared by the create form
// (AddListingView) and the listing-only edit form (EditListingView).
export interface ListingFieldsForm {
  type: ListingType | '';
  alsoRent: boolean;
  titleLv: string;
  titleEn: string;
  titleRu: string;
  descriptionLv: string;
  descriptionEn: string;
  descriptionRu: string;
  price: string;
  vatIncluded: boolean;
  rentPrice: string;
  rentVatIncluded: boolean;
  rentDurationMonths: number;
  durationMonths: number;
  completion: PropertyCompletion | '';
  phones: string[];
}

// A full self-contained listing form: physical/media + terms. Used by the create
// form, the listing edit form and the add-another-listing form.
export type ListingFormState = PropertyFieldsForm & ListingFieldsForm;
