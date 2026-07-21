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
    createTransversalIdentityProvider,
    deleteTransversalIdentityProvider,
    listTransversalIdentityProviders,
    setTransversalIdentityProviderEnabled,
    updateTransversalIdentityProvider,
} from '../storage/transversal-identity-providers.storage';
import type {
    TransversalIdentityProvider,
    TransversalIdentityProviderInput,
    TransversalIdentityProviderPatch,
} from '../types';

export function useTransversalIdentityProviders() {
    const [providers, setProviders] = useState<TransversalIdentityProvider[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        setLoading(true);
        try {
            setProviders(await listTransversalIdentityProviders());
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const addProvider = useCallback(
        async (input: TransversalIdentityProviderInput) => {
            const created = await createTransversalIdentityProvider(input);
            await refresh();
            return created;
        },
        [refresh],
    );

    const updateProvider = useCallback(
        async (id: string, patch: TransversalIdentityProviderPatch) => {
            const updated = await updateTransversalIdentityProvider(id, patch);
            await refresh();
            return updated;
        },
        [refresh],
    );

    const removeProvider = useCallback(
        async (id: string) => {
            await deleteTransversalIdentityProvider(id);
            await refresh();
        },
        [refresh],
    );

    const toggleEnabled = useCallback(
        async (id: string, enabled: boolean) => {
            await setTransversalIdentityProviderEnabled(id, enabled);
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
