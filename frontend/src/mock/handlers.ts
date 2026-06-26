import { http, HttpResponse } from 'msw';
import { mockListings } from './properties';
import { logger } from '../utils/logger';
import { cityByName, districtSlugByName } from '../data/locations';
import { PAGE_SIZE } from '../types/filter';

const MOCK_OWNER_ID = 'mock-user-1';

const registeredNames = new Map<string, string>();

const dtoCatalog = mockListings.map((item, i) => ({
  ...item,
  ownerId: i < 2 ? MOCK_OWNER_ID : `other-user-${i}`,
  district:
    districtSlugByName.get(item.district) ?? item.district.toLowerCase(),
  city: cityByName.get(item.city) ?? item.city.toLowerCase(),
}));

function toListItem(item: (typeof dtoCatalog)[0]) {
  const {
    descriptionLv,
    descriptionEn,
    descriptionRu,
    coords,
    phones,
    videoUrl,
    photos,
    ...rest
  } = item;
  return { ...rest, photo: photos[0] ?? null };
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
  | 'newest'
  | 'price-asc'
  | 'price-desc'
  | 'price-per-m2-asc'
  | 'm2-desc';

function applyFilters(params: URLSearchParams) {
  let items = [...dtoCatalog];

  const type = params.get('type');
  if (type) items = items.filter((i) => i.type === type);

  const locs = params.getAll('loc');
  if (locs.length) {
    items = items.filter((i) =>
      locs.some((loc) => {
        const colon = loc.indexOf(':');
        if (colon === -1) return false;
        return (
          i.city === loc.slice(0, colon) && i.district === loc.slice(colon + 1)
        );
      })
    );
  }

  const priceMin = params.get('priceMin');
  const priceMax = params.get('priceMax');
  if (priceMin) items = items.filter((i) => i.price >= Number(priceMin));
  if (priceMax) items = items.filter((i) => i.price <= Number(priceMax));

  const rooms = params.getAll('rooms').map(Number);
  if (rooms.length) items = items.filter((i) => rooms.includes(i.rooms));

  const m2Min = params.get('m2Min');
  const m2Max = params.get('m2Max');
  if (m2Min) items = items.filter((i) => i.m2 >= Number(m2Min));
  if (m2Max) items = items.filter((i) => i.m2 <= Number(m2Max));

  const floorMin = params.get('floorMin');
  const floorMax = params.get('floorMax');
  if (floorMin)
    items = items.filter((i) => i.floor != null && i.floor >= Number(floorMin));
  if (floorMax)
    items = items.filter((i) => i.floor != null && i.floor <= Number(floorMax));

  if (params.get('notGround') === 'true')
    items = items.filter((i) => i.floor == null || i.floor > 1);
  if (params.get('notTop') === 'true')
    items = items.filter(
      (i) => i.floor == null || i.totalFloors == null || i.floor < i.totalFloors
    );

  const yearMin = params.get('yearMin');
  const yearMax = params.get('yearMax');
  if (yearMin)
    items = items.filter(
      (i) => i.yearBuilt != null && i.yearBuilt >= Number(yearMin)
    );
  if (yearMax)
    items = items.filter(
      (i) => i.yearBuilt != null && i.yearBuilt <= Number(yearMax)
    );

  const features = params.getAll('features');
  if (features.length)
    items = items.filter((i) =>
      features.every((f) => (i.features as string[]).includes(f))
    );

  const completion = params.get('completion');
  if (completion) items = items.filter((i) => i.completion === completion);

  const sort = (params.get('sort') ?? 'newest') as SortKey;
  const sorters: Record<
    SortKey,
    (a: (typeof dtoCatalog)[0], b: (typeof dtoCatalog)[0]) => number
  > = {
    newest: (a, b) => b.postedAt.localeCompare(a.postedAt),
    'price-asc': (a, b) => a.price - b.price,
    'price-desc': (a, b) => b.price - a.price,
    'price-per-m2-asc': (a, b) => a.price / a.m2 - b.price / b.m2,
    'm2-desc': (a, b) => b.m2 - a.m2,
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

  // --- PROPERTIES ENDPOINTS ---
  http.get('/api/properties', ({ request }) => {
    const url = new URL(request.url);
    return HttpResponse.json(applyFilters(url.searchParams));
  }),

  http.delete('/api/properties/:id', ({ params }) => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    const index = dtoCatalog.findIndex((i) => i.id === params.id);
    if (index === -1) return new HttpResponse(null, { status: 404 });
    dtoCatalog.splice(index, 1);
    return new HttpResponse(null, { status: 204 });
  }),

  http.get('/api/properties/mine', () => {
    return HttpResponse.json(
      mockUser ? dtoCatalog.slice(0, 2).map(toListItem) : []
    );
  }),

  // Look how clean parametric routes look instead of regex!
  http.get('/api/properties/:id', ({ params }) => {
    const item = dtoCatalog.find((i) => i.id === params.id);
    return item
      ? HttpResponse.json(item)
      : new HttpResponse(null, { status: 404 });
  }),

  http.post('/api/properties', async ({ request }) => {
    const body = (await request.json()) as any;
    const created = {
      ...body,
      id: `mock-${Date.now()}`,
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

  http.post('/api/saved/:propertyId', ({ params }) => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    mockSavedIds.add(params.propertyId as string);
    return new HttpResponse(null, { status: 204 });
  }),

  http.delete('/api/saved/:propertyId', ({ params }) => {
    if (!mockUser) return new HttpResponse(null, { status: 401 });
    mockSavedIds.delete(params.propertyId as string);
    return new HttpResponse(null, { status: 204 });
  }),

  http.all('/api/*', ({ request }) => {
    logger.error(`[mock] Frontend called an unhandled URL: ${request.url}`);
    return HttpResponse.json(
      { message: `Mock endpoint missing for ${request.url}` },
      { status: 404 }
    );
  }),
];
