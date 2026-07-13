import { http, HttpResponse } from 'msw';
import { mockListings } from './listings';
import { logger } from '../utils/logger';
import { cityByName, districtSlugByName } from '../data/locations';
import { PAGE_SIZE, ROOM_COUNT_MAX } from '../types/filter';

const MOCK_OWNER_ID = 'mock-user-1';

// Mirrors backend normalization: fold diacritics, lowercase, alnum-only words.
function normalizeMockAddress(text: string): string {
  return text
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

// Streets are scoped like the real register mirror: a Rīga district only sees
// Rīga streets; the grouped "Ādaži & Carnikava" city sees its village's
// streets plus a rural named house. Unknown cities fall back to a generic set
// so the picker still works everywhere in mock mode.
interface MockStreet {
  kind: 'street' | 'house';
  code: number;
  name: string;
  territory: string;
  city: string; // city slug the entry belongs to
  district?: string; // district slug, when scoped narrower than the city
  lat?: number;
  lng?: number;
}

const RIGA_STREET_NAMES = [
  'Brīvības iela',
  'Eksporta iela',
  'Elizabetes iela',
  'Krišjāņa Barona iela',
  'Aleksandra Čaka iela',
  'Krišjāņa Valdemāra iela',
  'Lāčplēša iela',
  'Dzirnavu iela',
  'Ģertrūdes iela',
  'Tērbatas iela',
  'Matīsa iela',
  'Avotu iela',
  'Skolas iela',
  'Stabu iela',
  'Maskavas iela',
];

const GENERIC_STREET_NAMES = [
  'Skolas iela',
  'Dārza iela',
  'Liepu iela',
  'Parka iela',
  'Jūras iela',
];

const MOCK_STREETS: MockStreet[] = [
  ...RIGA_STREET_NAMES.map((name, i) => ({
    kind: 'street' as const,
    code: 5000 + i,
    name,
    territory: 'Rīga',
    city: 'riga',
  })),
  {
    kind: 'street',
    code: 5900,
    name: 'Skolas iela',
    territory: 'Kalngale',
    city: 'adazi-&-carnikava',
    district: 'kalngale',
  },
  {
    kind: 'house',
    code: 6100,
    name: 'Vecvagari',
    territory: 'Kalngale',
    city: 'adazi-&-carnikava',
    district: 'kalngale',
    lat: 57.083,
    lng: 24.273,
  },
  ...GENERIC_STREET_NAMES.map((name, i) => ({
    kind: 'street' as const,
    code: 5950 + i,
    name,
    territory: '',
    city: '*',
  })),
];

function mockStreetsFor(citySlug: string, districtSlug: string): MockStreet[] {
  const scoped = MOCK_STREETS.filter(
    (s) =>
      s.city === citySlug && (!s.district || s.district === districtSlug)
  );
  return scoped.length > 0
    ? scoped
    : MOCK_STREETS.filter((s) => s.city === '*');
}

// Mirrors the backend's match: prefix or word-boundary prefix ("barona" finds
// "Krišjāņa Barona iela").
function matchesAddressQuery(name: string, q: string): boolean {
  if (!q) return true;
  const norm = normalizeMockAddress(name);
  return norm.startsWith(q) || norm.includes(` ${q}`);
}

const MOCK_BUILDINGS = MOCK_STREETS.filter((s) => s.kind === 'street').flatMap(
  (street) =>
    ['1', '2', '3', '5', '8', '10', '12', '12 k-1', '14A', '21'].map(
      (name, i) => ({
        streetCode: street.code,
        code: street.code * 100 + i,
        name,
        lat: 56.9496 + (street.code % 7) * 0.002 + i * 0.0004,
        lng: 24.1052 + (street.code % 5) * 0.002 + i * 0.0004,
      })
    )
);
// Properties owned by the mock user: two multi-listing properties (buy+rent
// pairs) plus one single-listing property, to exercise the grouping UI.
const MOCK_OWNED_PROPERTY_IDS = new Set(['prop-001', 'prop-009', 'prop-005']);

const registeredNames = new Map<string, string>();

const dtoCatalog = mockListings.map((item) => ({
  ...item,
  ownerId: MOCK_OWNED_PROPERTY_IDS.has(item.propertyId)
    ? MOCK_OWNER_ID
    : `other-user-${item.propertyId}`,
  location: {
    ...item.location,
    district:
      districtSlugByName.get(item.location.district) ??
      item.location.district.toLowerCase(),
    city:
      cityByName.get(item.location.city) ?? item.location.city.toLowerCase(),
  },
}));

type CatalogItem = (typeof dtoCatalog)[0];

function toListItem(item: CatalogItem) {
  const { media, phones, translations, location, details, ...rest } = item;
  return {
    ...rest,
    translations: Object.fromEntries(
      Object.entries(translations).map(([locale, t]) => [
        locale,
        { title: t!.title },
      ])
    ),
    location: { ...location, coords: null },
    details: {
      rooms: details.rooms,
      m2: details.m2,
      landM2: details.landM2,
      floor: details.floor,
      totalFloors: details.totalFloors,
      yearBuilt: details.yearBuilt,
    },
    photo: media.photos?.[0] ?? null,
  };
}

// VITE_MOCK_ADMIN=true grants the mock session the admin role, so /admin is
// reachable in mock mode without a real backend/role to promote.
const MOCK_IS_ADMIN = import.meta.env.VITE_MOCK_ADMIN === 'true';

// In-memory mock auth state. VITE_MOCK_AUTO_LOGIN seeds an already-signed-in
// user (owning MOCK_OWNED_PROPERTY_IDS) so E2E/Playwright runs skip the login
// form entirely — see mock/index.ts for the matching session-cookie bypass.
let mockUser: {
  id: string;
  name: string;
  email: string;
  emailVerified: boolean;
  admin: boolean;
} | null =
  import.meta.env.VITE_MOCK_AUTO_LOGIN === 'true'
    ? {
        id: MOCK_OWNER_ID,
        name: 'Jānis Bērziņš',
        email: 'janis.berzins@gmail.com',
        emailVerified: true,
        admin: MOCK_IS_ADMIN,
      }
    : null;

const mockPendingListingIds = new Set(
  dtoCatalog.slice(0, 2).map((item) => item.id)
);

// Mirrors backend CadastreQueryService's tolerances against one fixed mock
// "official" record, so the PENDING_REVIEW path can be exercised in mock
// mode without a real VZD ingest. Only applies once an address has resolved
// to a building (arBuildingCode set, same fail-open-to-ACTIVE rule as the
// backend); apartment area is only checked when an apartment number is given.
const MOCK_OFFICIAL_YEAR_BUILT = 1985;
const MOCK_OFFICIAL_AREA_M2 = 52.8;
const YEAR_MISMATCH_TOLERANCE_YEARS = 5;
const AREA_MISMATCH_RATIO = 0.15;

function isCadastreMismatch(body: any): boolean {
  if (!body.location?.arBuildingCode) return false;
  const yearBuilt = body.details?.yearBuilt;
  if (
    yearBuilt != null &&
    Math.abs(yearBuilt - MOCK_OFFICIAL_YEAR_BUILT) >
      YEAR_MISMATCH_TOLERANCE_YEARS
  )
    return true;
  const m2 = body.details?.m2;
  if (body.location?.apartment && m2 != null) {
    const ratio = Math.abs(m2 - MOCK_OFFICIAL_AREA_M2) / MOCK_OFFICIAL_AREA_M2;
    if (ratio > AREA_MISMATCH_RATIO) return true;
  }
  return false;
}

const mockSavedIds = new Set<string>();

type SortKey =
  'newest' | 'price-asc' | 'price-desc' | 'price-per-m2-asc' | 'm2-desc';

function filterByType(items: CatalogItem[], params: URLSearchParams) {
  const type = params.get('type');
  if (!type) return items;
  return items.filter((i) => i.type === type);
}

function filterByKind(items: CatalogItem[], params: URLSearchParams) {
  const kind = params.get('kind');
  if (!kind) return items;
  return items.filter((i) => i.propertyKind === kind);
}

function filterByLocation(items: CatalogItem[], params: URLSearchParams) {
  const locs = params.getAll('loc');
  if (!locs.length) return items;
  return items.filter((i) =>
    locs.some((loc) => {
      const colon = loc.indexOf(':');
      if (colon === -1) return false;
      return (
        i.location.city === loc.slice(0, colon) &&
        i.location.district === loc.slice(colon + 1)
      );
    })
  );
}

function filterByPriceRange(items: CatalogItem[], params: URLSearchParams) {
  const priceMin = params.get('priceMin');
  const priceMax = params.get('priceMax');
  let result = items;
  if (priceMin)
    result = result.filter((i) => i.price.amount >= Number(priceMin));
  if (priceMax)
    result = result.filter((i) => i.price.amount <= Number(priceMax));
  return result;
}

// Matches the backend's count filter: an exact match on each selected value,
// with the top bucket (ROOM_COUNT_MAX) meaning "that many or more".
function countMatches(count: number | null | undefined, selected: number[]) {
  if (count == null) return false;
  return selected.some((v) =>
    v >= ROOM_COUNT_MAX ? count >= ROOM_COUNT_MAX : count === v
  );
}

function filterByRoomCounts(items: CatalogItem[], params: URLSearchParams) {
  const rooms = params.getAll('rooms').map(Number);
  const bedrooms = params.getAll('bedrooms').map(Number);
  const bathrooms = params.getAll('bathrooms').map(Number);
  let result = items;
  if (rooms.length)
    result = result.filter((i) => countMatches(i.details.rooms, rooms));
  if (bedrooms.length)
    result = result.filter((i) => countMatches(i.details.bedrooms, bedrooms));
  if (bathrooms.length)
    result = result.filter((i) => countMatches(i.details.bathrooms, bathrooms));
  return result;
}

function filterBySize(items: CatalogItem[], params: URLSearchParams) {
  const m2Min = params.get('m2Min');
  const m2Max = params.get('m2Max');
  const landM2Min = params.get('landM2Min');
  const landM2Max = params.get('landM2Max');
  let result = items;
  if (m2Min) result = result.filter((i) => i.details.m2 >= Number(m2Min));
  if (m2Max) result = result.filter((i) => i.details.m2 <= Number(m2Max));
  // Land area only exists on houses; a null-land listing fails either bound.
  if (landM2Min)
    result = result.filter(
      (i) => i.details.landM2 != null && i.details.landM2 >= Number(landM2Min)
    );
  if (landM2Max)
    result = result.filter(
      (i) => i.details.landM2 != null && i.details.landM2 <= Number(landM2Max)
    );
  return result;
}

function filterByFloor(items: CatalogItem[], params: URLSearchParams) {
  const floorMin = params.get('floorMin');
  const floorMax = params.get('floorMax');
  let result = items;
  if (floorMin)
    result = result.filter(
      (i) => i.details.floor != null && i.details.floor >= Number(floorMin)
    );
  if (floorMax)
    result = result.filter(
      (i) => i.details.floor != null && i.details.floor <= Number(floorMax)
    );
  if (params.get('notGround') === 'true')
    result = result.filter(
      (i) => i.details.floor == null || i.details.floor !== 1
    );
  if (params.get('notTop') === 'true')
    result = result.filter(
      (i) =>
        i.details.floor == null ||
        i.details.totalFloors == null ||
        i.details.floor < i.details.totalFloors
    );
  return result;
}

function filterByYear(items: CatalogItem[], params: URLSearchParams) {
  const yearMin = params.get('yearMin');
  const yearMax = params.get('yearMax');
  let result = items;
  if (yearMin)
    result = result.filter(
      (i) =>
        i.details.yearBuilt != null && i.details.yearBuilt >= Number(yearMin)
    );
  if (yearMax)
    result = result.filter(
      (i) =>
        i.details.yearBuilt != null && i.details.yearBuilt <= Number(yearMax)
    );
  return result;
}

function filterByHeating(items: CatalogItem[], params: URLSearchParams) {
  const heating = params.getAll('heating');
  if (!heating.length) return items;
  return items.filter(
    (i) => i.details.heating != null && heating.includes(i.details.heating)
  );
}

function filterByEnergyClass(items: CatalogItem[], params: URLSearchParams) {
  const energyClass = params.getAll('energyClass');
  if (!energyClass.length) return items;
  return items.filter(
    (i) =>
      i.details.energyClass != null &&
      energyClass.includes(i.details.energyClass)
  );
}

function filterBySewage(items: CatalogItem[], params: URLSearchParams) {
  const sewage = params.getAll('sewage');
  if (!sewage.length) return items;
  return items.filter(
    (i) => i.details.sewage != null && sewage.includes(i.details.sewage)
  );
}

function filterByVentilation(items: CatalogItem[], params: URLSearchParams) {
  const ventilation = params.getAll('ventilation');
  if (!ventilation.length) return items;
  return items.filter(
    (i) =>
      i.details.ventilation != null &&
      ventilation.includes(i.details.ventilation)
  );
}

function filterByRoof(items: CatalogItem[], params: URLSearchParams) {
  const roof = params.getAll('roof');
  if (!roof.length) return items;
  return items.filter(
    (i) => i.details.roof != null && roof.includes(i.details.roof)
  );
}

function filterByFeatures(items: CatalogItem[], params: URLSearchParams) {
  const features = params.getAll('features');
  if (!features.length) return items;
  return items.filter((i) =>
    features.every((f) => (i.features as string[] | null)?.includes(f))
  );
}

// ANY-of set filter: keeps a listing that has at least one of the selected
// values — mirrors the backend's EXISTS-without-having semantics for the new
// attribute sets (ventilation systems, communications, stove, security, extras,
// parking). Distinct from filterByFeatures, which requires ALL selected.
function filterByAnyOfSet(
  items: CatalogItem[],
  params: URLSearchParams,
  key: 'ventilationSystems' | 'communications' | 'stove' | 'security' | 'extras' | 'parking'
) {
  const selected = params.getAll(key);
  if (!selected.length) return items;
  return items.filter((i) =>
    selected.some((v) => (i[key] as string[] | null)?.includes(v))
  );
}

function filterByCompletion(items: CatalogItem[], params: URLSearchParams) {
  const completion = params.get('completion');
  if (!completion) return items;
  return items.filter((i) => i.completion === completion);
}

function filterByMaintenanceCost(items: CatalogItem[], params: URLSearchParams) {
  const max = params.get('maintenanceCostMax');
  if (!max) return items;
  return items.filter(
    (i) =>
      i.details.maintenanceCost != null &&
      i.details.maintenanceCost <= Number(max)
  );
}

function filterByBathroomLayout(items: CatalogItem[], params: URLSearchParams) {
  const layout = params.get('bathroomLayout');
  if (!layout) return items;
  return items.filter((i) => i.details.bathroomLayout === layout);
}

function filterByVat(items: CatalogItem[], params: URLSearchParams) {
  if (params.get('vatIncluded') !== 'true') return items;
  return items.filter((i) => i.price.vatIncluded === true);
}

function applyFilters(params: URLSearchParams) {
  let items = dtoCatalog.filter((i) => i.status === 'active');
  items = filterByType(items, params);
  items = filterByKind(items, params);
  items = filterByLocation(items, params);
  items = filterByPriceRange(items, params);
  items = filterByRoomCounts(items, params);
  items = filterBySize(items, params);
  items = filterByFloor(items, params);
  items = filterByYear(items, params);
  items = filterByHeating(items, params);
  items = filterByEnergyClass(items, params);
  items = filterBySewage(items, params);
  items = filterByVentilation(items, params);
  items = filterByRoof(items, params);
  items = filterByFeatures(items, params);
  items = filterByAnyOfSet(items, params, 'ventilationSystems');
  items = filterByAnyOfSet(items, params, 'communications');
  items = filterByAnyOfSet(items, params, 'stove');
  items = filterByAnyOfSet(items, params, 'security');
  items = filterByAnyOfSet(items, params, 'extras');
  items = filterByAnyOfSet(items, params, 'parking');
  items = filterByCompletion(items, params);
  items = filterByMaintenanceCost(items, params);
  items = filterByBathroomLayout(items, params);
  items = filterByVat(items, params);

  const sort = (params.get('sort') ?? 'newest') as SortKey;
  const sorters: Record<SortKey, (a: CatalogItem, b: CatalogItem) => number> = {
    newest: (a, b) => b.postedAt.localeCompare(a.postedAt),
    'price-asc': (a, b) => a.price.amount - b.price.amount,
    'price-desc': (a, b) => b.price.amount - a.price.amount,
    'price-per-m2-asc': (a, b) =>
      a.price.amount / a.details.m2 - b.price.amount / b.details.m2,
    'm2-desc': (a, b) => b.details.m2 - a.details.m2,
  };
  items.sort(sorters[sort] ?? sorters.newest);

  const total = items.length;
  const page = Number(params.get('page') ?? '1');
  return {
    items: items
      .slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
      .map(toListItem),
    total,
  };
}

export const handlers = [
  // --- AUTH ENDPOINTS ---
  http.get('/api/auth/me', () => {
    return mockUser
      ? HttpResponse.json(mockUser)
      : HttpResponse.json({ message: 'Not authenticated' }, { status: 401 });
  }),

  http.post('/api/auth/login', async ({ request }) => {
    try {
      const { email, password } = (await request.json()) as any;
      if (password?.length >= 1) {
        mockUser = {
          id: 'mock-user-1',
          name: registeredNames.get(email.toLowerCase()) ?? email.split('@')[0],
          email,
          emailVerified: true,
          admin: MOCK_IS_ADMIN,
        };
        return HttpResponse.json(mockUser);
      }
    } catch (e) {
      logger.warn('[mock] Failed to parse login request body:', e);
    }
    return HttpResponse.json(
      { message: 'Invalid credentials' },
      { status: 401 }
    );
  }),

  http.post('/api/auth/register', async ({ request }) => {
    try {
      const { name, email } = (await request.json()) as any;
      if (name && email) registeredNames.set(email.toLowerCase(), name);
    } catch {}
    return new HttpResponse(null, { status: 201 });
  }),

  http.post('/api/auth/logout', () => {
    mockUser = null;
    return new HttpResponse(null, { status: 200 });
  }),

  http.post('/api/auth/refresh', () => new HttpResponse(null, { status: 401 })),

  // Grouping simple POST paths together
  ...[
    'forgot-password',
    'reset-password',
    'verify-email',
    'resend-verification',
  ].map((path) =>
    http.post(
      `/api/auth/${path}`,
      () => new HttpResponse(null, { status: 200 })
    )
  ),

  // --- ADDRESS REGISTER ENDPOINTS ---
  // A tiny register mirror: the same streets for any district, plus one rural
  // named house, so the strict street/house pickers work offline.
  http.get('/api/address/streets', ({ request }) => {
    const url = new URL(request.url);
    const city = url.searchParams.get('city') ?? '';
    const district = url.searchParams.get('district') ?? '';
    const q = normalizeMockAddress(url.searchParams.get('q') ?? '');
    const matches = mockStreetsFor(city, district)
      .filter((s) => matchesAddressQuery(s.name, q))
      .slice(0, 20)
      .map(({ city: _city, district: _district, ...option }) => option);
    return HttpResponse.json(matches);
  }),

  http.get('/api/address/buildings', ({ request }) => {
    const url = new URL(request.url);
    const streetCode = Number(url.searchParams.get('streetCode'));
    const q = normalizeMockAddress(url.searchParams.get('q') ?? '');
    const matches = MOCK_BUILDINGS.filter(
      (b) =>
        b.streetCode === streetCode &&
        normalizeMockAddress(b.name).startsWith(q)
    ).map(({ streetCode: _ignored, ...b }) => b);
    return HttpResponse.json(matches);
  }),

  // --- PROPERTIES / LISTINGS ENDPOINTS ---
  http.get('/api/properties', ({ request }) => {
    const url = new URL(request.url);
    return HttpResponse.json(applyFilters(url.searchParams));
  }),

  // Delete a single listing — property and sibling listings survive.
  http.delete('/api/properties/:id', ({ params }) => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    const index = dtoCatalog.findIndex((i) => i.id === params.id);
    if (index === -1) return new HttpResponse(null, { status: 404 });
    dtoCatalog.splice(index, 1);
    return new HttpResponse(null, { status: 204 });
  }),

  http.get('/api/properties/counts', ({ request }) => {
    const type = new URL(request.url).searchParams.get('type');
    let items = dtoCatalog.filter((i) => i.status === 'active');
    if (type) items = items.filter((i) => i.type === type);
    return HttpResponse.json({
      apartment: items.filter((i) => i.propertyKind === 'apartment').length,
      house: items.filter((i) => i.propertyKind === 'house').length,
    });
  }),

  http.get('/api/properties/mine', () => {
    if (!mockUser) return HttpResponse.json([]);
    const owned = dtoCatalog.filter((i) => i.ownerId === mockUser!.id);
    const now = new Date();
    const soonExpiry = new Date(
      now.getTime() + 10 * 24 * 60 * 60 * 1000
    ).toISOString();
    const pastExpiry = new Date(
      now.getTime() - 2 * 24 * 60 * 60 * 1000
    ).toISOString();
    return HttpResponse.json(
      owned.map((item, i) => ({
        ...toListItem(item),
        expiresAt: i % 3 === 0 ? pastExpiry : soonExpiry,
      }))
    );
  }),

  // --- ADMIN ENDPOINTS ---
  http.get('/api/admin/listings/pending', () => {
    const pending = dtoCatalog.filter((i) => mockPendingListingIds.has(i.id));
    return HttpResponse.json(pending.map(toListItem));
  }),

  http.post('/api/admin/listings/:id/approve', ({ params }) => {
    mockPendingListingIds.delete(params.id as string);
    const item = dtoCatalog.find((i) => i.id === params.id);
    if (item) item.status = 'active';
    return new HttpResponse(null, { status: 200 });
  }),

  http.post('/api/admin/listings/:id/reject', ({ params }) => {
    mockPendingListingIds.delete(params.id as string);
    const item = dtoCatalog.find((i) => i.id === params.id);
    if (item) item.status = 'inactive';
    return new HttpResponse(null, { status: 200 });
  }),

  http.get('/api/admin/listings/:id', ({ params }) => {
    const item = dtoCatalog.find((i) => i.id === params.id);
    return item
      ? HttpResponse.json(item)
      : new HttpResponse(null, { status: 404 });
  }),

  http.post('/api/admin/listings/:id/suspend', ({ params }) => {
    const item = dtoCatalog.find((i) => i.id === params.id);
    if (!item) return new HttpResponse(null, { status: 404 });
    item.status = 'inactive';
    return new HttpResponse(null, { status: 200 });
  }),

  http.post('/api/admin/listings/:id/reactivate', ({ params }) => {
    const item = dtoCatalog.find((i) => i.id === params.id);
    if (!item) return new HttpResponse(null, { status: 404 });
    item.status = 'active';
    return new HttpResponse(null, { status: 200 });
  }),

  http.post('/api/properties/:id/renew', async ({ params, request }) => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    const item = dtoCatalog.find((i) => i.id === params.id);
    if (!item) return new HttpResponse(null, { status: 404 });
    const { durationMonths = 3 } = (await request.json()) as {
      durationMonths?: number;
    };
    const newExpiry = new Date();
    newExpiry.setMonth(newExpiry.getMonth() + durationMonths);
    return HttpResponse.json({ ...item, expiresAt: newExpiry.toISOString() });
  }),

  // Add a second/third listing type to an existing property.
  http.post(
    '/api/properties/:propertyId/listings',
    async ({ params, request }) => {
      if (!mockUser) return new HttpResponse(null, { status: 401 });
      const siblings = dtoCatalog.filter(
        (i) => i.propertyId === params.propertyId
      );
      if (!siblings.length) return new HttpResponse(null, { status: 404 });
      const body = (await request.json()) as any;
      if (siblings.some((s) => s.type === body.type)) {
        return HttpResponse.json(
          {
            message: 'A listing of this type already exists for this property',
          },
          { status: 409 }
        );
      }
      const representative = siblings[0];
      const created: CatalogItem = {
        ...representative,
        id: `mock-${Date.now()}`,
        type: body.type,
        price: body.price,
        translations: body.translations,
        phones: body.phones ?? null,
        completion: body.completion,
        postedAt: new Date().toISOString(),
        expiresAt: undefined,
      };
      dtoCatalog.unshift(created);
      return HttpResponse.json(created, { status: 201 });
    }
  ),

  // Delete the property and cascade to all of its listings.
  http.delete('/api/properties/:propertyId/listings', ({ params }) => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    const before = dtoCatalog.length;
    for (let i = dtoCatalog.length - 1; i >= 0; i--) {
      if (dtoCatalog[i].propertyId === params.propertyId)
        dtoCatalog.splice(i, 1);
    }
    if (dtoCatalog.length === before)
      return new HttpResponse(null, { status: 404 });
    return new HttpResponse(null, { status: 204 });
  }),

  // Get the property's own fields (shared across every listing on it).
  http.get('/api/properties/:propertyId/property', ({ params }) => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    const item = dtoCatalog.find((i) => i.propertyId === params.propertyId);
    if (!item) return new HttpResponse(null, { status: 404 });
    if (item.ownerId !== mockUser.id)
      return new HttpResponse(null, { status: 403 });
    return HttpResponse.json({
      id: item.propertyId,
      ownerId: item.ownerId,
      propertyKind: item.propertyKind,
      details: item.details,
      media: item.media,
      features: item.features,
      location: item.location,
    });
  }),

  // Update the property's own fields — applies to every listing sharing it.
  http.put(
    '/api/properties/:propertyId/property',
    async ({ params, request }) => {
      if (!mockUser) return new HttpResponse(null, { status: 401 });
      const siblings = dtoCatalog.filter(
        (i) => i.propertyId === params.propertyId
      );
      if (!siblings.length) return new HttpResponse(null, { status: 404 });
      if (siblings[0].ownerId !== mockUser.id)
        return new HttpResponse(null, { status: 403 });
      const body = (await request.json()) as any;
      siblings.forEach((item) => {
        item.propertyKind = body.propertyKind;
        item.details = body.details;
        item.media = body.media;
        item.features = body.features;
        item.location = body.location;
      });
      return HttpResponse.json({
        id: params.propertyId,
        ownerId: siblings[0].ownerId,
        propertyKind: body.propertyKind,
        details: body.details,
        media: body.media,
        features: body.features,
        location: body.location,
      });
    }
  ),

  // Look how clean parametric routes look instead of regex!
  http.get('/api/properties/:id', ({ params }) => {
    const item = dtoCatalog.find(
      (i) => i.id === params.id && i.status === 'active'
    );
    return item
      ? HttpResponse.json(item)
      : new HttpResponse(null, { status: 404 });
  }),

  // Create a property + its first listing.
  http.post('/api/properties', async ({ request }) => {
    const body = (await request.json()) as any;
    const propertyId = `prop-mock-${Date.now()}`;
    const mismatch = isCadastreMismatch(body);
    const created = {
      ...body,
      id: `mock-${Date.now()}`,
      propertyId,
      ownerId: mockUser?.id ?? MOCK_OWNER_ID,
      postedAt: new Date().toISOString(),
      status: mismatch ? 'pending_review' : 'active',
    };
    dtoCatalog.unshift(created);
    logger.debug('[mock] cadastre mismatch check', {
      arBuildingCode: body.location?.arBuildingCode,
      apartment: body.location?.apartment,
      yearBuilt: body.details?.yearBuilt,
      m2: body.details?.m2,
      mismatch,
    });
    if (mismatch) mockPendingListingIds.add(created.id);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.put('/api/properties/:id', async ({ params, request }) => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    const index = dtoCatalog.findIndex((i) => i.id === params.id);
    if (index === -1) return new HttpResponse(null, { status: 404 });
    const item = dtoCatalog[index];
    if (item.ownerId !== mockUser.id)
      return new HttpResponse(null, { status: 403 });
    const body = (await request.json()) as any;
    Object.assign(item, body);
    return HttpResponse.json(item);
  }),

  // --- UPLOADS ENDPOINTS ---
  http.post('/api/uploads/presign', async ({ request }) => {
    const { filenames = [] } = (await request.json()) as {
      filenames: string[];
    };
    const presignedData = filenames.map((_name, i) => ({
      uploadUrl: `/mock-upload/${i}`,
      fileUrl: `https://picsum.photos/seed/upload-${Date.now()}-${i}/1200/900`,
    }));
    return HttpResponse.json(presignedData);
  }),

  http.all('/mock-upload/*', () => new HttpResponse(null, { status: 200 })),

  // --- SAVED ENDPOINTS ---
  http.get('/api/saved', () =>
    mockUser
      ? HttpResponse.json([...mockSavedIds])
      : new HttpResponse(null, { status: 401 })
  ),

  http.post('/api/saved/:listingId', ({ params }) => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    mockSavedIds.add(params.listingId as string);
    return new HttpResponse(null, { status: 204 });
  }),

  http.delete('/api/saved/:listingId', ({ params }) => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    mockSavedIds.delete(params.listingId as string);
    return new HttpResponse(null, { status: 204 });
  }),

  http.get('/api/auth/export', () => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    return HttpResponse.json({
      user: mockUser,
      exportedAt: new Date().toISOString(),
    });
  }),

  http.all('/api/*', ({ request }) => {
    logger.error(`[mock] Frontend called an unhandled URL: ${request.url}`);
    return HttpResponse.json(
      { message: `Mock endpoint missing for ${request.url}` },
      { status: 404 }
    );
  }),
];
