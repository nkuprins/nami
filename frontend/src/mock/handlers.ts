import {http, HttpResponse} from 'msw';
import {mockListings} from './listings';
import {DISTRICTS} from '../data/locations';

const PAGE_SIZE = 12;
const nameToSlug = new Map(DISTRICTS.map(d => [d.name, d.slug]));

const dtoCatalog = mockListings.map(item => ({
    ...item,
    district: nameToSlug.get(item.district) ?? item.district.toLowerCase(),
}));

// In-memory mock auth state
let mockUser: { id: string; name: string; email: string; emailVerified: boolean } | null = null;

type SortKey = 'newest' | 'price-asc' | 'price-desc' | 'price-per-m2-asc' | 'm2-desc';

function applyFilters(params: URLSearchParams) {
    let items = [...dtoCatalog];

    const type = params.get('type');
    if (type) items = items.filter(i => i.type === type);

    const locs = params.getAll('loc');
    if (locs.length) items = items.filter(i => locs.includes(i.district));

    const priceMin = params.get('priceMin');
    const priceMax = params.get('priceMax');
    if (priceMin) items = items.filter(i => i.price >= Number(priceMin));
    if (priceMax) items = items.filter(i => i.price <= Number(priceMax));

    const rooms = params.getAll('rooms').map(Number);
    if (rooms.length) items = items.filter(i => rooms.includes(i.rooms));

    const m2Min = params.get('m2Min');
    const m2Max = params.get('m2Max');
    if (m2Min) items = items.filter(i => i.m2 >= Number(m2Min));
    if (m2Max) items = items.filter(i => i.m2 <= Number(m2Max));

    const floorMin = params.get('floorMin');
    const floorMax = params.get('floorMax');
    if (floorMin) items = items.filter(i => i.floor != null && i.floor >= Number(floorMin));
    if (floorMax) items = items.filter(i => i.floor != null && i.floor <= Number(floorMax));

    if (params.get('notGround') === 'true') items = items.filter(i => i.floor == null || i.floor > 1);
    if (params.get('notTop') === 'true')
        items = items.filter(i => i.floor == null || i.totalFloors == null || i.floor < i.totalFloors);

    const yearMin = params.get('yearMin');
    const yearMax = params.get('yearMax');
    if (yearMin) items = items.filter(i => i.yearBuilt != null && i.yearBuilt >= Number(yearMin));
    if (yearMax) items = items.filter(i => i.yearBuilt != null && i.yearBuilt <= Number(yearMax));

    const features = params.getAll('features');
    if (features.length) items = items.filter(i => features.every(f => (i.features as string[]).includes(f)));

    const completion = params.get('completion');
    if (completion) items = items.filter(i => i.completion === completion);

    const sort = (params.get('sort') ?? 'newest') as SortKey;
    const sorters: Record<SortKey, (a: (typeof dtoCatalog)[0], b: (typeof dtoCatalog)[0]) => number> = {
        newest: (a, b) => b.postedAt.localeCompare(a.postedAt),
        'price-asc': (a, b) => a.price - b.price,
        'price-desc': (a, b) => b.price - a.price,
        'price-per-m2-asc': (a, b) => a.price / a.m2 - b.price / b.m2,
        'm2-desc': (a, b) => b.m2 - a.m2,
    };
    items.sort(sorters[sort] ?? sorters.newest);

    const total = items.length;
    const page = Number(params.get('page') ?? '1');
    return {items: items.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE), total};
}

export const handlers = [
    // --- AUTH ENDPOINTS ---
    http.get('/api/auth/me', () => {
        return mockUser
            ? HttpResponse.json(mockUser)
            : HttpResponse.json({message: 'Not authenticated'}, {status: 401});
    }),

    http.post('/api/auth/login', async ({request}) => {
        try {
            const {email, password} = (await request.json()) as any;
            if (password?.length >= 1) {
                mockUser = {id: 'mock-user-1', name: email.split('@')[0], email, emailVerified: true};
                return HttpResponse.json(mockUser);
            }
        } catch (e) { /* fallback empty body */
        }
        return HttpResponse.json({message: 'Invalid credentials'}, {status: 401});
    }),

    http.post('/api/auth/register', () => new HttpResponse(null, {status: 201})),

    http.post('/api/auth/logout', () => {
        mockUser = null;
        return new HttpResponse(null, {status: 200});
    }),

    http.post('/api/auth/refresh', () => new HttpResponse(null, {status: 401})),

    // Grouping simple POST paths together
    ...['forgot-password', 'reset-password', 'verify-email', 'resend-verification'].map(path =>
        http.post(`/api/auth/${path}`, () => new HttpResponse(null, {status: 200}))
    ),

    // --- PROPERTIES ENDPOINTS ---
    http.get('/api/properties', ({request}) => {
        const url = new URL(request.url);
        return HttpResponse.json(applyFilters(url.searchParams));
    }),

    http.get('/api/properties/mine', () => {
        return HttpResponse.json(mockUser ? dtoCatalog.slice(0, 2) : []);
    }),

    // Look how clean parametric routes look instead of regex!
    http.get('/api/properties/:id', ({params}) => {
        const item = dtoCatalog.find(i => i.id === params.id);
        return item ? HttpResponse.json(item) : new HttpResponse(null, {status: 404});
    }),

    http.post('/api/properties', async ({request}) => {
        const body = (await request.json()) as any;
        return HttpResponse.json(
            {...body, id: `mock-${Date.now()}`, postedAt: new Date().toISOString()},
            {status: 201}
        );
    }),

    // --- UPLOADS ENDPOINTS ---
    http.post('/api/uploads/presign', async ({request}) => {
        const {filenames = []} = (await request.json()) as { filenames: string[] };
        const presignedData = filenames.map((_name, i) => ({
            uploadUrl: `/mock-upload/${i}`,
            fileUrl: `https://picsum.photos/seed/upload-${Date.now()}-${i}/1200/900`,
        }));
        return HttpResponse.json(presignedData);
    }),

    http.all('/mock-upload/*', () => new HttpResponse(null, {status: 200})),

    http.all('/api/*', ({request}) => {
        console.error(`[mock] Frontend called an unhandled URL: ${request.url}`);
        return HttpResponse.json({message: `Mock endpoint missing for ${request.url}`}, {status: 404});
    }),
];