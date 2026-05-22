/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useEffect, useState } from 'react';
import { ApiError } from '../api/authz-api-client';
import { authzApiService } from '../api/authz-api.service';
import type { SchemaResponse } from '../api/authz-api.types';

export interface UseSchemaResult {
    readonly schema: SchemaResponse | null;
    readonly notFound: boolean;
    readonly isLoading: boolean;
    readonly error: string | undefined;
}

export function useSchema(environmentId: string): UseSchemaResult {
    const [schema, setSchema] = useState<SchemaResponse | null>(null);
    const [notFound, setNotFound] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | undefined>(undefined);

    useEffect(() => {
        let cancelled = false;
        setIsLoading(true);
        setError(undefined);

        authzApiService
            .getSchema(environmentId)
            .then(res => {
                if (cancelled) return;
                setSchema(res);
                setNotFound(false);
            })
            .catch(e => {
                if (cancelled) return;
                if (e instanceof ApiError && e.status === 404) {
                    setSchema(null);
                    setNotFound(true);
                } else {
                    setError(e instanceof Error ? e.message : 'Failed to load schema');
                }
            })
            .finally(() => {
                if (!cancelled) setIsLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [environmentId]);

    return { schema, notFound, isLoading, error };
}
