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
import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { authzApiService } from '../api/authz-api.service';
import type { EngineSchemaJson } from '../api/authz-api.types';
import { authzQueryKeys } from '../api/query-keys';
import { engineSchemaToParsed, type ParsedSchema } from '../engine-schema';

export interface UseParsedSchemaResult {
    readonly parsed: ParsedSchema;
    readonly isLoading: boolean;
    readonly error: string | undefined;
}

export function useParsedSchema(environmentId: string): UseParsedSchemaResult {
    const query = useQuery<EngineSchemaJson>({
        queryKey: authzQueryKeys.parsedSchema(environmentId),
        queryFn: () => authzApiService.getParsedSchema(environmentId),
        enabled: Boolean(environmentId),
        staleTime: 30_000,
    });

    const parsed = useMemo(() => engineSchemaToParsed(query.data), [query.data]);

    return {
        parsed,
        isLoading: query.isLoading,
        error: query.error instanceof Error ? query.error.message : query.error ? String(query.error) : undefined,
    };
}
