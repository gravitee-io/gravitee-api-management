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
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useState } from 'react';
import { authzApiService, DEFAULT_PER_PAGE, type PolicyListParams } from '../api/authz-api.service';
import type { PagedResponse, PolicyRequest, PolicyResponse, PolicyStatus, PolicyType } from '../api/authz-api.types';
import { authzQueryKeys } from '../api/query-keys';

export interface UsePoliciesOptions {
    readonly type?: PolicyType;
    readonly status?: PolicyStatus;
    readonly initialPerPage?: number;
}

export interface UsePoliciesResult {
    readonly data: PagedResponse<PolicyResponse> | null;
    readonly isLoading: boolean;
    readonly error: string | undefined;
    readonly page: number;
    readonly perPage: number;
    readonly setPage: (page: number) => void;
    readonly setPerPage: (perPage: number) => void;
    readonly create: (request: PolicyRequest) => Promise<PolicyResponse>;
    readonly update: (id: string, request: PolicyRequest) => Promise<PolicyResponse>;
    readonly remove: (id: string) => Promise<void>;
    readonly isCreating: boolean;
    readonly isUpdating: boolean;
    readonly isRemoving: boolean;
    readonly reload: () => void;
}

export function usePolicies(environmentId: string, options: UsePoliciesOptions = {}): UsePoliciesResult {
    const { type, status, initialPerPage = DEFAULT_PER_PAGE } = options;
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(initialPerPage);
    const [lastInitialPerPage, setLastInitialPerPage] = useState(initialPerPage);

    // Adjust perPage during render (React recommended pattern) rather than a
    // secondary useEffect, avoiding the extra render cycle that the effect causes.
    if (initialPerPage !== lastInitialPerPage) {
        setLastInitialPerPage(initialPerPage);
        setPerPage(initialPerPage);
    }

    const queryClient = useQueryClient();

    const params: PolicyListParams = { page, perPage, type, status };
    const query = useQuery({
        queryKey: authzQueryKeys.policies.page(environmentId, page, perPage, type, status),
        queryFn: () => authzApiService.listPolicies(environmentId, params),
        enabled: Boolean(environmentId),
        staleTime: 30_000,
        placeholderData: keepPreviousData,
    });

    const invalidate = useCallback(
        () => queryClient.invalidateQueries({ queryKey: authzQueryKeys.policies.all(environmentId) }),
        [environmentId, queryClient],
    );

    const reload = useCallback(() => void invalidate(), [invalidate]);

    const createMutation = useMutation({
        mutationFn: (request: PolicyRequest) => authzApiService.createPolicy(environmentId, request),
        onSuccess: () => void invalidate(),
    });

    const updateMutation = useMutation({
        mutationFn: ({ id, request }: { id: string; request: PolicyRequest }) => authzApiService.updatePolicy(environmentId, id, request),
        onSuccess: () => void invalidate(),
    });

    const removeMutation = useMutation({
        mutationFn: (id: string) => authzApiService.deletePolicy(environmentId, id),
        onSuccess: () => void invalidate(),
    });

    const create = useCallback((request: PolicyRequest) => createMutation.mutateAsync(request), [createMutation]);
    const update = useCallback((id: string, request: PolicyRequest) => updateMutation.mutateAsync({ id, request }), [updateMutation]);
    const remove = useCallback((id: string) => removeMutation.mutateAsync(id), [removeMutation]);

    return {
        data: query.data ?? null,
        isLoading: query.isLoading,
        error: query.error instanceof Error ? query.error.message : query.error ? String(query.error) : undefined,
        page,
        perPage,
        setPage,
        setPerPage,
        create,
        update,
        remove,
        isCreating: createMutation.isPending,
        isUpdating: updateMutation.isPending,
        isRemoving: removeMutation.isPending,
        reload,
    };
}
