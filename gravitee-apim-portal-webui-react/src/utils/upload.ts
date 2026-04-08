/**
 * Converts a local file to a base64 data URL.
 * For the PoC, images are stored inline in the JSON document.
 * In production, this should upload to a backend/CDN and return the URL.
 */
export function uploadFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(new Error(`Failed to read file: ${file.name}`));
    reader.readAsDataURL(file);
  });
}
