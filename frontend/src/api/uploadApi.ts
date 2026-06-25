import { fetchApi } from './fetchApi';

export async function requestPresignedUrls(
  filenames: string[]
): Promise<{ uploadUrl: string; fileUrl: string }[]> {
  const res = await fetchApi(`/api/uploads/presign`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ filenames }),
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
        headers: { 'Content-Type': file.type },
        credentials: 'omit',
      }).then(() => slots[i].fileUrl)
    )
  );
}
