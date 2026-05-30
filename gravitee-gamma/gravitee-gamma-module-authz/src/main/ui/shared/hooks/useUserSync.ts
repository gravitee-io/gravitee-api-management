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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { ApiError } from '../api/authz-api-client';
import { authzApiService } from '../api/authz-api.service';
import type { AmSyncStartResponse, AmSyncStatusResponse } from '../api/authz-api.types';
import { authzQueryKeys } from '../api/query-keys';

export interface UseUserSyncResult {
    /** Latest known sync status, or null when none has run yet (backend 404). */
    readonly status: AmSyncStatusResponse | null;
    readonly isLoadingStatus: boolean;
    readonly statusError: string | undefined;
    readonly start: () => Promise<AmSyncStartResponse>;
    readonly isStarting: boolean;
    readonly startError: string | undefined;
}

export function useUserSync(environmentId: string): UseUserSyncResult {
    const queryClient = useQueryClient();

    const statusQuery = useQuery({
        queryKey: authzQueryKeys.userSync(environmentId),
        // No sync has ever run for this org → backend answers 404. Treat that as a
        // null status rather than an error so the UI shows an idle state.
        queryFn: async () => {
            try {
                return await authzApiService.getUserSyncStatus(environmentId);
            } catch (err) {
                if (err instanceof ApiError && err.status === 404) {
                    return null;
                }
                throw err;
            }
        },
        enabled: Boolean(environmentId),
        // Poll while a sync is in flight; stop once it settles.
        refetchInterval: query => (query.state.data?.status === 'PENDING' ? 2000 : false),
    });

    const startMutation = useMutation({
        mutationFn: () => authzApiService.startUserSync(environmentId),
        // Refresh the status card after both a successful start and a 409 (a sync was
        // already running) so it picks up the in-flight job either way.
        onSettled: () => void queryClient.invalidateQueries({ queryKey: authzQueryKeys.userSync(environmentId) }),
    });

    const start = useCallback(() => startMutation.mutateAsync(), [startMutation]);

    // A 409 just means a sync is already running — not a failure worth surfacing as an
    // error; the polling status card already reflects the in-flight job.
    const startErr = startMutation.error;
    const startError = startErr instanceof ApiError && startErr.status === 409 ? undefined : startErr instanceof Error ? startErr.message : undefined;

    return {
        status: statusQuery.data ?? null,
        isLoadingStatus: statusQuery.isLoading,
        statusError: statusQuery.error instanceof Error ? statusQuery.error.message : undefined,
        start,
        isStarting: startMutation.isPending,
        startError,
    };
}
