import { fetchApi } from './fetchApi';

export const savedApi = {
  async getSavedIds(): Promise<string[]> {
    const res = await fetchApi('/api/saved');
    if (!res.ok) return [];
    return res.json();
  },

  async save(id: string): Promise<void> {
    await fetchApi(`/api/saved/${id}`, { method: 'POST' });
  },

  async unsave(id: string): Promise<void> {
    await fetchApi(`/api/saved/${id}`, { method: 'DELETE' });
  },
};
