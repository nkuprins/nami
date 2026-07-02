import type {
  BathroomLayout,
  EnergyClass,
  Feature,
  HeatingType,
  ListingType,
  PropertyCompletion,
  PropertyKind,
} from '../../../types/listingItem';

// Fields belonging to the physical property, shared by the create form
// (AddListingView) and the property-only edit form (EditPropertyView).
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
