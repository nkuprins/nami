export type City = string;
export type District = string;

export interface Location {
    city: City;
    district: District;
}

export const LOCATION_MAP: ReadonlyMap<City, District[]> = new Map([
    ['Rīga', ['Centrs', 'Āgenskalns']],
    ['Jūrmala', ['Asari']],
]);
