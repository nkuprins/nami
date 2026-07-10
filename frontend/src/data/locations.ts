import { LOCATION_MAP } from './rawLocations';

export const cityBySlug = new Map<string, string>();
export const cityByName = new Map<string, string>();
export const districtNameBySlug = new Map<string, string>();
export const districtSlugByName = new Map<string, string>();

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
