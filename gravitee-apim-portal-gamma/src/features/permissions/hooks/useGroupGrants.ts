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

import {
    createPortalAccessGrant,
    deletePortalAccessGrant,
    getGrantsByGroupId,
    setNavigationOverride,
    updatePortalAccessGrant,
} from '../storage/portal-access-grants.storage';
import type {
    PortalAccessGrant,
    PortalAccessGrantInput,
    PortalAccessGrantPatch,
    PortalAccessLevel,
} from '../types/permissions.types';

export type OverrideSelection = PortalAccessLevel | 'NONE' | 'INHERIT';

export function useGroupGrants(groupId: string | undefined, tenantId: string | undefined) {
    const [grants, setGrants] = useState<PortalAccessGrant[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        if (!groupId) {
            setGrants([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            setGrants(await getGrantsByGroupId(groupId));
        } finally {
            setLoading(false);
        }
    }, [groupId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const addGrant = useCallback(
        async (input: Omit<PortalAccessGrantInput, 'groupId' | 'tenantId'>) => {
            if (!groupId || !tenantId) {
                return undefined;
            }

            const created = await createPortalAccessGrant({ ...input, groupId, tenantId });
            await refresh();
            return created;
        },
        [groupId, refresh, tenantId],
    );

    const updateGrant = useCallback(
        async (grantId: string, patch: PortalAccessGrantPatch) => {
            const updated = await updatePortalAccessGrant(grantId, patch);
            await refresh();
            return updated;
        },
        [refresh],
    );

    const removeGrant = useCallback(
        async (grantId: string) => {
            await deletePortalAccessGrant(grantId);
            await refresh();
        },
        [refresh],
    );

    const setOverride = useCallback(
        async (grantId: string, navigationItemId: string, portalId: string, access: OverrideSelection) => {
            await setNavigationOverride(grantId, { navigationItemId, portalId, access });
            await refresh();
        },
        [refresh],
    );

    return { grants, loading, refresh, addGrant, updateGrant, removeGrant, setOverride };
}
