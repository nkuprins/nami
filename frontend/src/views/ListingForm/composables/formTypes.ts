import type {
  BathroomLayout,
  Communication,
  EnergyClass,
  Feature,
  HeatingType,
  ListingType,
  ParkingType,
  PropertyCompletion,
  PropertyExtra,
  PropertyKind,
  RoofType,
  SecurityFeature,
  SewageType,
  StoveType,
  VentilationSystem,
  VentilationType,
} from '../../../types/listingItem';
import type { BuildingOption, StreetOption } from '../../../api/addressApi';

// The physical/media fields of a listing, shared by the create form
// (AddListingView), the listing edit form (EditListingView) and the
// add-another-listing form (AddListingToPropertyView). Only the create form
// also collects the address fields (street/building picked from the State
// Address Register, free-typed apartment, coords); the listing-scoped forms
// leave them untouched. `address` is the composed display string — derived
// from the picks in the create form, seeded from the API in the edit form.
export interface PropertyFieldsForm {
  propertyKind: PropertyKind;
  address: string;
  street: StreetOption | null;
  building: BuildingOption | null;
  apartment: string;
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
  roof: RoofType | '';
  features: Feature[];
  ventilationSystems: VentilationSystem[];
  communications: Communication[];
  stove: StoveType[];
  security: SecurityFeature[];
  extras: PropertyExtra[];
  parking: ParkingType[];
  videoUrl: string;
  websiteUrl: string;
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
  phones: PhoneContactForm[];
}

// A phone entry as edited in the form. name/email may be left blank — the
// backend fills them from the account holder when submitted blank.
export interface PhoneContactForm {
  phone: string;
  name: string;
  email: string;
}

// A full self-contained listing form: physical/media + terms. Used by the create
// form, the listing edit form and the add-another-listing form.
export type ListingFormState = PropertyFieldsForm & ListingFieldsForm;
