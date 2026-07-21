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
    createPortalDomain,
    deletePortalDomain,
    listPortalDomains,
    updatePortalDomain,
} from '../storage/portal-domains.storage';
import type { PortalDomain, PortalDomainInput, PortalDomainPatch } from '../types';

export function usePortalDomains() {
    const [domains, setDomains] = useState<PortalDomain[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        setLoading(true);
        try {
            setDomains(await listPortalDomains());
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const addDomain = useCallback(
        async (input: PortalDomainInput) => {
            const created = await createPortalDomain(input);
            await refresh();
            return created;
        },
        [refresh],
    );

    const updateDomain = useCallback(
        async (id: string, patch: PortalDomainPatch) => {
            const updated = await updatePortalDomain(id, patch);
            await refresh();
            return updated;
        },
        [refresh],
    );

    const removeDomain = useCallback(
        async (id: string) => {
            await deletePortalDomain(id);
            await refresh();
        },
        [refresh],
    );

    return {
        domains,
        loading,
        refresh,
        addDomain,
        updateDomain,
        removeDomain,
    };
}
