/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useCallback, useEffect, useState } from 'react';

import { getPortalTenant, savePortalTenant } from '../storage/portal-tenants.storage';
import { getMembersByTenantId } from '../storage/portal-tenant-members.storage';
import type { PortalTenant, PortalTenantMember } from '../types/portal-tenant.types';

export function usePortalTenant(tenantId: string | undefined) {
    const [tenant, setTenant] = useState<PortalTenant | undefined>();
    const [members, setMembers] = useState<PortalTenantMember[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        if (!tenantId) {
            setTenant(undefined);
            setMembers([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            const [storedTenant, storedMembers] = await Promise.all([
                getPortalTenant(tenantId),
                getMembersByTenantId(tenantId),
            ]);
            setTenant(storedTenant);
            setMembers(storedMembers);
        } finally {
            setLoading(false);
        }
    }, [tenantId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const updateTenant = useCallback(
        async (patch: Partial<PortalTenant>) => {
            if (!tenant) {
                return;
            }

            const updated: PortalTenant = {
                ...tenant,
                ...patch,
                updatedAt: new Date().toISOString(),
            };
            await savePortalTenant(updated);
            setTenant(updated);
            return updated;
        },
        [tenant],
    );

    return {
        tenant,
        members,
        loading,
        refresh,
        updateTenant,
        setMembers,
    };
}
