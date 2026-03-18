import { useState, useCallback } from 'react';
import { apiFetch } from '../api-client';

const schemaCache = new Map<string, object>();

interface UsePolicySchemaResult {
  readonly schema: object | null;
  readonly loading: boolean;
  readonly error: Error | null;
  readonly fetchSchema: (policyId: string) => void;
}

export function usePolicySchema(): UsePolicySchemaResult {
  const [schema, setSchema] = useState<object | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchSchema = useCallback((policyId: string) => {
    const cached = schemaCache.get(policyId);
    if (cached) {
      setSchema(cached);
      setLoading(false);
      setError(null);
      return;
    }

    setLoading(true);
    setError(null);

    apiFetch<object>(`/org/v2/plugins/policies/${policyId}/schema`)
      .then((data) => {
        schemaCache.set(policyId, data);
        setSchema(data);
      })
      .catch((err) => {
        setError(err instanceof Error ? err : new Error(String(err)));
        setSchema(null);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return { schema, loading, error, fetchSchema };
}
