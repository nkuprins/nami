export type PropertyType = 'buy' | 'rent' | 'new-project';
export type PropertyKind = 'apartment' | 'house';
export type Feature =
    | 'balcony'
    | 'parking'
    | 'elevator'
    | 'furnished'
    | 'pets'
    | 'new-building';

export interface PropertyItem {
    id: string;
    type: PropertyType;
    propertyKind: PropertyKind;
    title: string;
    description: string;
    price: number;
    rooms: number;
    m2: number;
    landM2?: number;
    floor?: number;
    totalFloors?: number;
    yearBuilt?: number;
    features: Feature[];
    district: string;
    city: string;
    address: string;
    coords: { lat: number; lng: number };
    photos: string[];
    postedAt: string;
    completion?: 'ready' | 'not-ready';
}