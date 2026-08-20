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

import { useMutation, useQueryClient } from '@tanstack/react-query';

import { createTenant, deleteTenant, updateTenant } from '../services/tenants';
import type { NewTenantPayload, UpdateTenantPayload } from '../types/tenant';
import { tenantKeys } from '../utils/queryKeys';

async function invalidateTenantCaches(queryClient: ReturnType<typeof useQueryClient>) {
    await queryClient.invalidateQueries({ queryKey: tenantKeys.all });
}

export function useCreateTenant() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: NewTenantPayload) => createTenant(payload),
        onSuccess: async () => {
            await invalidateTenantCaches(queryClient);
        },
    });
}

export function useUpdateTenant() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: UpdateTenantPayload) => updateTenant(payload),
        onSuccess: async () => {
            await invalidateTenantCaches(queryClient);
        },
    });
}

export function useDeleteTenant() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (tenantKey: string) => deleteTenant(tenantKey),
        onSuccess: async () => {
            await invalidateTenantCaches(queryClient);
        },
    });
}
