import type { ListingSummary } from './listingItem';

// One map pin = one physical property (an address). It carries every matching
// listing at that address (cheapest first) so the popup can show all of them —
// a property with several offers ("same house") shows them side by side, and no
// listing kept by the filter is ever hidden.
export interface MapPin {
  propertyId: string;
  lat: number;
  lng: number;
  listings: ListingSummary[];
}
