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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import { listEnvironmentRoles, sendEnvironmentBroadcast } from '../services/environmentBroadcasts';
import type { BroadcastPayload, BroadcastRecipientOption } from '../types';
import { mapEnvironmentRolesToRecipients } from '../utils/mapEnvironmentRoles';
import { environmentBroadcastKeys } from '../utils/queryKeys';

export function useEnvironmentRoles() {
    const rolesQuery = useQuery({
        queryKey: environmentBroadcastKeys.roles(),
        queryFn: listEnvironmentRoles,
        staleTime: 5 * 60_000,
    });

    const recipientOptions = useMemo<BroadcastRecipientOption[]>(
        () => mapEnvironmentRolesToRecipients(rolesQuery.data ?? []),
        [rolesQuery.data],
    );

    return {
        recipientOptions,
        isLoading: rolesQuery.isLoading,
        isError: rolesQuery.isError,
    };
}

export function useSendEnvironmentBroadcast() {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (payload: BroadcastPayload) => {
            if (!envId) {
                return Promise.reject(new Error('Environment is not available.'));
            }
            return sendEnvironmentBroadcast(envId, payload);
        },
    });
}
