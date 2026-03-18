import { useState, useEffect, useCallback } from 'react';
import type { ApiV4 } from '../types';
import { apiFetch } from '../api-client';

interface UseApiResult {
  readonly api: ApiV4 | null;
  readonly loading: boolean;
  readonly error: Error | null;
  readonly saveApi: (api: ApiV4) => Promise<void>;
}

export function useApi(apiId: string): UseApiResult {
  const [api, setApi] = useState<ApiV4 | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!apiId) {
      setError(new Error('Missing API ID'));
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    apiFetch<ApiV4>(`/v2/apis/${apiId}`)
      .then((data) => {
        if (!cancelled) setApi(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err : new Error(String(err)));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => { cancelled = true; };
  }, [apiId]);

  const saveApi = useCallback(async (updatedApi: ApiV4) => {
    const result = await apiFetch<ApiV4>(`/v2/apis/${apiId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updatedApi),
    });
    setApi(result);
  }, [apiId]);

  return { api, loading, error, saveApi };
}
