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
    createPortalIdentityProvider,
    deletePortalIdentityProvider,
    getIdentityProvidersByPortalId,
    setIdentityProviderEnabled,
    updatePortalIdentityProvider,
} from '../storage/portal-identity-providers.storage';
import type {
    PortalIdentityProvider,
    PortalIdentityProviderInput,
    PortalIdentityProviderPatch,
} from '../types';

export function usePortalIdentityProviders(portalId: string | undefined) {
    const [providers, setProviders] = useState<PortalIdentityProvider[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        if (!portalId) {
            setProviders([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            setProviders(await getIdentityProvidersByPortalId(portalId));
        } finally {
            setLoading(false);
        }
    }, [portalId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const addProvider = useCallback(
        async (input: PortalIdentityProviderInput) => {
            if (!portalId) {
                return undefined;
            }
            const created = await createPortalIdentityProvider(portalId, input);
            await refresh();
            return created;
        },
        [portalId, refresh],
    );

    const updateProvider = useCallback(
        async (providerId: string, patch: PortalIdentityProviderPatch) => {
            const updated = await updatePortalIdentityProvider(providerId, patch);
            await refresh();
            return updated;
        },
        [refresh],
    );

    const removeProvider = useCallback(
        async (providerId: string) => {
            await deletePortalIdentityProvider(providerId);
            await refresh();
        },
        [refresh],
    );

    const toggleEnabled = useCallback(
        async (providerId: string, enabled: boolean) => {
            await setIdentityProviderEnabled(providerId, enabled);
            await refresh();
        },
        [refresh],
    );

    return {
        providers,
        loading,
        refresh,
        addProvider,
        updateProvider,
        removeProvider,
        toggleEnabled,
    };
}
