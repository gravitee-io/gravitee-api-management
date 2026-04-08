import type { Page, PagesResponse } from '../entities/page';
import { apiFetch } from './http';

const BASE_URL = '/portal/environments/DEFAULT';

export async function listPagesByApiId(apiId: string): Promise<Page[]> {
  const response = await apiFetch(`${BASE_URL}/apis/${apiId}/pages?size=-1`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch pages: ${response.status}`);
  }

  const data = (await response.json()) as PagesResponse;
  return data.data ?? [];
}

export async function getPageContent(apiId: string, pageId: string): Promise<Page> {
  const response = await apiFetch(`${BASE_URL}/apis/${apiId}/pages/${pageId}?include=content`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch page content: ${response.status}`);
  }

  return response.json() as Promise<Page>;
}
