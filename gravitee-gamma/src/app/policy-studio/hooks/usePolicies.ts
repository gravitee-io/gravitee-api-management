import { useState, useEffect } from 'react';
import type { PolicyPlugin } from '../types';
import { apiFetch } from '../api-client';

interface UsePoliciesResult {
  readonly policies: PolicyPlugin[];
  readonly loading: boolean;
  readonly error: Error | null;
}

export function usePolicies(): UsePoliciesResult {
  const [policies, setPolicies] = useState<PolicyPlugin[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let cancelled = false;

    apiFetch<PolicyPlugin[]>('/org/v2/plugins/policies')
      .then((data) => {
        if (!cancelled) setPolicies(Array.isArray(data) ? data : []);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err : new Error(String(err)));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => { cancelled = true; };
  }, []);

  return { policies, loading, error };
}
