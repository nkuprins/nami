import { http, HttpResponse } from 'msw';
import { mockListings } from './listings';
import { logger } from '../utils/logger';
import { cityByName, districtSlugByName } from '../data/locations';
import { PAGE_SIZE } from '../types/filter';

const MOCK_OWNER_ID = 'mock-user-1';
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

// In-memory mock auth state
let mockUser: {
  id: string;
  name: string;
  email: string;
  emailVerified: boolean;
} | null = null;

const mockSavedIds = new Set<string>();

type SortKey =
  'newest' | 'price-asc' | 'price-desc' | 'price-per-m2-asc' | 'm2-desc';

function applyFilters(params: URLSearchParams) {
  let items = [...dtoCatalog];

  const type = params.get('type');
  if (type) {
    items = items.filter((i) => i.type === type);
  }

  const locs = params.getAll('loc');
  if (locs.length) {
    items = items.filter((i) =>
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

  const priceMin = params.get('priceMin');
  const priceMax = params.get('priceMax');
  if (priceMin) items = items.filter((i) => i.price.amount >= Number(priceMin));
  if (priceMax) items = items.filter((i) => i.price.amount <= Number(priceMax));

  const rooms = params.getAll('rooms').map(Number);
  if (rooms.length)
    items = items.filter((i) => rooms.includes(i.details.rooms));

  const bedrooms = params.getAll('bedrooms').map(Number);
  if (bedrooms.length)
    items = items.filter(
      (i) => i.details.bedrooms != null && bedrooms.includes(i.details.bedrooms)
    );

  const bathrooms = params.getAll('bathrooms').map(Number);
  if (bathrooms.length)
    items = items.filter(
      (i) =>
        i.details.bathrooms != null && bathrooms.includes(i.details.bathrooms)
    );

  const m2Min = params.get('m2Min');
  const m2Max = params.get('m2Max');
  if (m2Min) items = items.filter((i) => i.details.m2 >= Number(m2Min));
  if (m2Max) items = items.filter((i) => i.details.m2 <= Number(m2Max));

  const floorMin = params.get('floorMin');
  const floorMax = params.get('floorMax');
  if (floorMin)
    items = items.filter(
      (i) => i.details.floor != null && i.details.floor >= Number(floorMin)
    );
  if (floorMax)
    items = items.filter(
      (i) => i.details.floor != null && i.details.floor <= Number(floorMax)
    );

  if (params.get('notGround') === 'true')
    items = items.filter((i) => i.details.floor == null || i.details.floor > 1);
  if (params.get('notTop') === 'true')
    items = items.filter(
      (i) =>
        i.details.floor == null ||
        i.details.totalFloors == null ||
        i.details.floor < i.details.totalFloors
    );

  const yearMin = params.get('yearMin');
  const yearMax = params.get('yearMax');
  if (yearMin)
    items = items.filter(
      (i) =>
        i.details.yearBuilt != null && i.details.yearBuilt >= Number(yearMin)
    );
  if (yearMax)
    items = items.filter(
      (i) =>
        i.details.yearBuilt != null && i.details.yearBuilt <= Number(yearMax)
    );

  const heating = params.getAll('heating');
  if (heating.length)
    items = items.filter(
      (i) => i.details.heating != null && heating.includes(i.details.heating)
    );

  const energyClass = params.getAll('energyClass');
  if (energyClass.length)
    items = items.filter(
      (i) =>
        i.details.energyClass != null &&
        energyClass.includes(i.details.energyClass)
    );

  const features = params.getAll('features');
  if (features.length)
    items = items.filter((i) =>
      features.every((f) => (i.features as string[] | null)?.includes(f))
    );

  const completion = params.get('completion');
  if (completion) items = items.filter((i) => i.completion === completion);

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
        };
        return HttpResponse.json(mockUser);
      }
    } catch (e) {
      /* fallback empty body */
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
    const item = dtoCatalog.find((i) => i.id === params.id);
    return item
      ? HttpResponse.json(item)
      : new HttpResponse(null, { status: 404 });
  }),

  // Create a property + its first listing.
  http.post('/api/properties', async ({ request }) => {
    const body = (await request.json()) as any;
    const propertyId = `prop-mock-${Date.now()}`;
    const created = {
      ...body,
      id: `mock-${Date.now()}`,
      propertyId,
      ownerId: mockUser?.id ?? MOCK_OWNER_ID,
      postedAt: new Date().toISOString(),
    };
    dtoCatalog.unshift(created);
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
