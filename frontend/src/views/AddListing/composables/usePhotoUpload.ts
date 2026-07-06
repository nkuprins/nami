import { onUnmounted, ref } from 'vue';

export type PhotoEntry =
  | { kind: 'existing'; url: string }
  | { kind: 'new'; file: File; preview: string };

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png']);

export function usePhotoUpload(max = 30) {
  const photos = ref<PhotoEntry[]>([]);

  function seed(urls: string[]) {
    photos.value.forEach((p) => {
      if (p.kind === 'new') URL.revokeObjectURL(p.preview);
    });
    photos.value = urls.map((url) => ({ kind: 'existing', url }));
  }

  function addFiles(e: Event) {
    const input = e.target as HTMLInputElement;
    for (const file of Array.from(input.files ?? [])) {
      if (photos.value.length >= max) break;
      if (!ALLOWED_TYPES.has(file.type)) continue;
      if (file.size > MAX_FILE_SIZE) continue;
      if (
        photos.value.some((p) => p.kind === 'new' && p.file.name === file.name)
      )
        continue;
      photos.value.push({
        kind: 'new',
        file,
        preview: URL.createObjectURL(file),
      });
    }
    input.value = '';
  }

  function remove(i: number) {
    const entry = photos.value[i];
    if (entry.kind === 'new') URL.revokeObjectURL(entry.preview);
    photos.value.splice(i, 1);
  }

  function move(from: number, to: number) {
    if (from === to || to < 0 || to >= photos.value.length) return;
    const [entry] = photos.value.splice(from, 1);
    photos.value.splice(to, 0, entry);
  }

  async function buildFinalUrls(
    uploadNew: (files: File[]) => Promise<string[]>
  ): Promise<string[]> {
    const newFiles = photos.value.filter(
      (p): p is Extract<PhotoEntry, { kind: 'new' }> => p.kind === 'new'
    );
    const uploaded = newFiles.length
      ? await uploadNew(newFiles.map((p) => p.file))
      : [];
    let i = 0;
    return photos.value.map((p) =>
      p.kind === 'existing' ? p.url : uploaded[i++]
    );
  }

  onUnmounted(() => {
    photos.value.forEach((p) => {
      if (p.kind === 'new') URL.revokeObjectURL(p.preview);
    });
  });

  return { photos, seed, addFiles, remove, move, buildFinalUrls };
}
