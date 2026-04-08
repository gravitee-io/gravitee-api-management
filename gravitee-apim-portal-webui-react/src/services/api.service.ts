import type { ApisResponse } from '../entities/api';
import { apiFetch } from './http';

const BASE_URL = '/portal/environments/DEFAULT';

export interface ApiSearchParams {
  page?: number;
  size?: number;
  q?: string;
  category?: string;
}

export async function searchApis({ page = 1, size = 9, q = '', category = '' }: ApiSearchParams = {}): Promise<ApisResponse> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    q,
    view: 'PORTAL',
  });

  if (category && category !== 'all') {
    params.set('category', category);
  }

  const response = await apiFetch(`${BASE_URL}/apis/_search?${params.toString()}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch APIs: ${response.status}`);
  }

  return response.json() as Promise<ApisResponse>;
}
