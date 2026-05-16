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

import { listApplicationRoles, sendBroadcast } from '../../../services/apis/broadcasts';
import type { BroadcastPayload, RecipientOption } from '../types/broadcast';
import { apiBroadcastKeys } from '../utils/queryKeys';

/**
 * Fetches APPLICATION-scoped roles and builds the recipients dropdown options.
 * API_SUBSCRIBERS is always prepended as the first option.
 */
export function useApplicationRoles() {
    const rolesQuery = useQuery({
        queryKey: apiBroadcastKeys.applicationRoles(),
        queryFn: listApplicationRoles,
        staleTime: 5 * 60_000,
    });

    const recipientOptions = useMemo<RecipientOption[]>(() => {
        const sorted = [...(rolesQuery.data ?? [])].sort((a, b) => a.name.localeCompare(b.name));
        return [
            { name: 'API_SUBSCRIBERS', displayName: 'API subscribers' },
            ...sorted.map(role => ({
                name: role.name,
                displayName: `Members with the ${role.name} role on applications subscribed to this API`,
            })),
        ];
    }, [rolesQuery.data]);

    return {
        recipientOptions,
        isLoading: rolesQuery.isLoading,
        isError: rolesQuery.isError,
    };
}

export function useSendBroadcast(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (payload: BroadcastPayload) => sendBroadcast(envId, apiId, payload),
    });
}
