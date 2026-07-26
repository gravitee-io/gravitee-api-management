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
    createConsoleDocGrant,
    deleteConsoleDocGrant,
    getAllConsoleDocGrants,
    getConsoleDocGrantsByScopeId,
    seedConsoleDocGrantsIfEmpty,
    setConsoleDocGrantRole,
} from '../storage/console-doc-grants.storage';
import type { ConsoleDocGrant, ConsoleDocGrantInput, ConsoleDocRole } from '../types/permissions.types';

/** Pass a `scopeId` to scope the authoring grants to a single asset, as the APIM module screen does. */
export function useConsoleDocGrants(scopeId?: string) {
    const [grants, setGrants] = useState<ConsoleDocGrant[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        setLoading(true);
        try {
            await seedConsoleDocGrantsIfEmpty();
            setGrants(scopeId ? await getConsoleDocGrantsByScopeId(scopeId) : await getAllConsoleDocGrants());
        } finally {
            setLoading(false);
        }
    }, [scopeId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const addGrant = useCallback(
        async (input: ConsoleDocGrantInput) => {
            const created = await createConsoleDocGrant(input);
            await refresh();
            return created;
        },
        [refresh],
    );

    const setRole = useCallback(
        async (grantId: string, role: ConsoleDocRole) => {
            await setConsoleDocGrantRole(grantId, role);
            await refresh();
        },
        [refresh],
    );

    const removeGrant = useCallback(
        async (grantId: string) => {
            await deleteConsoleDocGrant(grantId);
            await refresh();
        },
        [refresh],
    );

    return { grants, loading, refresh, addGrant, setRole, removeGrant };
}
