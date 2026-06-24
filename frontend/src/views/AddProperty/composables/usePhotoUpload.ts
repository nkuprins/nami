import { onUnmounted, ref } from 'vue';

export interface PhotoEntry {
  file: File;
  preview: string;
}

export function usePhotoUpload() {
  const photos = ref<PhotoEntry[]>([]);

  function addFiles(e: Event) {
    const input = e.target as HTMLInputElement;
    for (const file of Array.from(input.files ?? [])) {
      if (photos.value.some((p) => p.file.name === file.name)) continue;
      photos.value.push({ file, preview: URL.createObjectURL(file) });
    }
    input.value = '';
  }

  function remove(i: number) {
    URL.revokeObjectURL(photos.value[i].preview);
    photos.value.splice(i, 1);
  }

  onUnmounted(() => {
    photos.value.forEach((p) => URL.revokeObjectURL(p.preview));
  });

  return { photos, addFiles, remove };
}
