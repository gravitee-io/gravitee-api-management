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
import { useQuery } from '@tanstack/react-query';
import { authzApiService } from '../api/authz-api.service';

export interface UseSchemaValidationResult {
    readonly errors: readonly string[];
    readonly validating: boolean;
}

const EMPTY_SCHEMA_ERROR = 'Schema must not be empty.';

export function useSchemaValidation(environmentId: string, schemaText: string, enabled: boolean): UseSchemaValidationResult {
    const [debounced, setDebounced] = useState(schemaText);

    useEffect(() => {
        const timer = setTimeout(() => setDebounced(schemaText), 300);
        return () => clearTimeout(timer);
    }, [schemaText]);

    const isEmpty = debounced.trim() === '';

    const query = useQuery({
        queryKey: ['authz', 'schema-validate', environmentId, debounced],
        queryFn: () => authzApiService.validateSchema(environmentId, debounced),
        enabled: enabled && Boolean(environmentId) && !isEmpty,
        staleTime: 5_000,
        retry: false,
    });

    if (!enabled) return { errors: [], validating: false };
    if (isEmpty) return { errors: [EMPTY_SCHEMA_ERROR], validating: false };

    return {
        errors: query.data && !query.data.valid ? query.data.errors : [],
        validating: query.isFetching,
    };
}
