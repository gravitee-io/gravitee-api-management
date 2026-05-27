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
import { useQuery } from '@tanstack/react-query';
import { ApiError } from '../api/authz-api-client';
import { authzApiService } from '../api/authz-api.service';
import type { SchemaResponse } from '../api/authz-api.types';
import { authzQueryKeys } from '../api/query-keys';

export interface UseSchemaResult {
    readonly schema: SchemaResponse | null;
    readonly notFound: boolean;
    readonly isLoading: boolean;
    readonly error: string | undefined;
}

const NOT_FOUND = Symbol('schema-not-found');
type SchemaQueryResult = SchemaResponse | typeof NOT_FOUND;

export function useSchema(environmentId: string): UseSchemaResult {
    const query = useQuery<SchemaQueryResult>({
        queryKey: authzQueryKeys.schema(environmentId),
        queryFn: async () => {
            try {
                return await authzApiService.getSchema(environmentId);
            } catch (e) {
                // 404 is a legitimate "no schema yet" state, not a transport error.
                if (e instanceof ApiError && e.status === 404) return NOT_FOUND;
                throw e;
            }
        },
        enabled: Boolean(environmentId),
        staleTime: 30_000,
    });

    const notFound = query.data === NOT_FOUND;
    const schema = !notFound && query.data ? (query.data as SchemaResponse) : null;

    return {
        schema,
        notFound,
        isLoading: query.isLoading,
        error: query.error instanceof Error ? query.error.message : query.error ? String(query.error) : undefined,
    };
}
