const XSRF_HEADER = 'X-Xsrf-Token';

let xsrfToken: string | null = null;
let tokenInitialized = false;
let tokenPromise: Promise<void> | null = null;

async function fetchXsrfToken(): Promise<void> {
  const response = await fetch('/portal/environments/DEFAULT/', {
    method: 'GET',
    headers: { 'X-Requested-With': 'XMLHttpRequest' },
    credentials: 'include',
  });

  const token = response.headers.get(XSRF_HEADER);
  if (token) {
    xsrfToken = token;
  }
  tokenInitialized = true;
}

async function ensureXsrfToken(): Promise<void> {
  if (tokenInitialized) return;
  if (!tokenPromise) {
    tokenPromise = fetchXsrfToken();
  }
  await tokenPromise;
}

export async function apiFetch(url: string, init: RequestInit = {}): Promise<Response> {
  await ensureXsrfToken();

  const headers = new Headers(init.headers);
  headers.set('X-Requested-With', 'XMLHttpRequest');
  if (xsrfToken) {
    headers.set(XSRF_HEADER, xsrfToken);
  }

  const response = await fetch(url, {
    ...init,
    headers,
    credentials: 'include',
  });

  const newToken = response.headers.get(XSRF_HEADER);
  if (newToken) {
    xsrfToken = newToken;
  }

  return response;
}
