import {PropertyItem} from '../types/propertyItem';
import {FilterState} from '../types/filter';
import {districtByName, districtBySlug} from '../data/locations';
import {fetchApi} from './fetchApi';

type PropertyItemDto = Omit<PropertyItem, 'district' | 'city'> & {
    district: string;
    city: string;
};

const citySlugMap: Record<string, string> = {
    Rīga: 'riga',
    Jūrmala: 'jurmala',
    Sigulda: 'sigulda',
};

function resolveLocation(slug: string): { district: string; city: string } {
    const entry = districtBySlug.get(slug);
    return entry
        ? {district: entry.name, city: entry.city}
        : {district: slug, city: slug};
}

function mapDto(dto: PropertyItemDto): PropertyItem {
    const {district, city: _city, ...rest} = dto;
    const loc = resolveLocation(district);
    return {...rest, district: loc.district, city: loc.city};
}

function buildParams(f: FilterState): URLSearchParams {
    const p = new URLSearchParams();
    p.set('type', f.type);
    f.loc.forEach((s) => p.append('loc', s));
    if (f.priceMin != null) p.set('priceMin', String(f.priceMin));
    if (f.priceMax != null) p.set('priceMax', String(f.priceMax));
    f.rooms.forEach((r) => p.append('rooms', String(r)));
    if (f.m2Min != null) p.set('m2Min', String(f.m2Min));
    if (f.m2Max != null) p.set('m2Max', String(f.m2Max));
    if (f.floorMin != null) p.set('floorMin', String(f.floorMin));
    if (f.floorMax != null) p.set('floorMax', String(f.floorMax));
    if (f.notGround) p.set('notGround', 'true');
    if (f.notTop) p.set('notTop', 'true');
    if (f.yearMin != null) p.set('yearMin', String(f.yearMin));
    if (f.yearMax != null) p.set('yearMax', String(f.yearMax));
    f.features.forEach((ft) => p.append('features', ft));
    if (f.completion) p.set('completion', f.completion);
    p.set('sort', f.sort);
    p.set('page', String(f.page));
    return p;
}

export async function listProperties(
    f: FilterState
): Promise<{ items: PropertyItem[]; total: number }> {
    const res = await fetchApi(`/api/properties?${buildParams(f)}`);
    if (!res.ok) throw new Error(`listProperties: ${res.status}`);
    const data = await res.json();
    return {items: data.items.map(mapDto), total: data.total};
}

export async function countProperties(f: FilterState): Promise<number> {
    const params = buildParams(f);
    params.set('page', '1');
    const res = await fetchApi(`/api/properties?${params}`);
    if (!res.ok) throw new Error(`countProperties: ${res.status}`);
    return (await res.json()).total;
}

export async function getMyProperties(): Promise<PropertyItem[]> {
    const res = await fetchApi(`/api/properties/mine`);
    if (!res.ok) throw new Error(`getMyProperties: ${res.status}`);
    return (await res.json()).map(mapDto);
}

export async function getProperty(
    id: string
): Promise<PropertyItem | undefined> {
    const res = await fetch(`/api/properties/${id}`);
    if (res.status === 404) return undefined;
    if (!res.ok) throw new Error(`getProperty: ${res.status}`);
    return mapDto(await res.json());
}

export async function requestPresignedUrls(
    filenames: string[]
): Promise<{ uploadUrl: string; fileUrl: string }[]> {
    const res = await fetchApi(`/api/uploads/presign`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({filenames}),
    });
    if (!res.ok) throw new Error(`requestPresignedUrls: ${res.status}`);
    return res.json();
}

export async function uploadFilesToS3(
    files: File[],
    slots: { uploadUrl: string; fileUrl: string }[]
): Promise<string[]> {
    return Promise.all(
        files.map((file, i) =>
            fetch(slots[i].uploadUrl, {
                method: 'PUT',
                body: file,
                headers: {'Content-Type': file.type},
            }).then(() => slots[i].fileUrl)
        )
    );
}

export async function addProperty(
    data: Omit<PropertyItem, 'id' | 'postedAt'>
): Promise<PropertyItem> {
    const districtEntry = districtByName.get(data.district);
    const districtSlug = districtEntry?.slug ?? data.district;
    const citySlug = districtEntry
        ? (citySlugMap[districtEntry.city] ?? districtEntry.city.toLowerCase())
        : data.city.toLowerCase();

    const res = await fetchApi(`/api/properties`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({...data, district: districtSlug, city: citySlug}),
    });
    if (!res.ok) throw new Error(`addProperty: ${res.status}`);
    return mapDto(await res.json());
}
