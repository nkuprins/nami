import { LOCATION_MAP } from './rawLocations';

type Slug = string;
type Name = string;

export const cityBySlug = new Map<Slug, Name>();
export const cityByName = new Map<Name, Slug>();
export const districtNameBySlug = new Map<Slug, Name>();
export const districtSlugByName = new Map<Name, Slug>();

const LATVIAN_MAP: Record<string, string> = {
  ā: 'a',
  č: 'c',
  ē: 'e',
  ģ: 'g',
  ī: 'i',
  ķ: 'k',
  ļ: 'l',
  ņ: 'n',
  š: 's',
  ū: 'u',
  ž: 'z',
};

export function slugify(text: string): string {
  return text
    .trim()
    .toLowerCase()
    .replace(/[āčēģīķļņšūž]/g, (char) => LATVIAN_MAP[char])
    .replace(/\s+/g, '-');
}

console.time('⏱️ START Locations Engine Initialized');

for (const [cityName, districts] of LOCATION_MAP) {
  const citySlug = slugify(cityName);
  cityBySlug.set(citySlug, cityName);
  cityByName.set(cityName, citySlug);

  for (const districtName of districts) {
    const districtSlug = slugify(districtName);
    districtNameBySlug.set(districtSlug, districtName);
    districtSlugByName.set(districtName, districtSlug);
  }
}

console.timeEnd('⏱️ STOP Locations Engine Initialized');
