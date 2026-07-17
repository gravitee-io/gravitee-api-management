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

import { createPortalFromTemplate } from '../storage/create-portal';
import { seedPortalsIfEmpty, deletePortalWithRelatedData } from '../storage/portals.storage';
import type { PortalTemplateId } from '../templates/portal-templates';
import type { DeveloperPortal } from '../types';

async function loadPortals(): Promise<DeveloperPortal[]> {
    return seedPortalsIfEmpty();
}

export function usePortals() {
    const [portals, setPortals] = useState<DeveloperPortal[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        setLoading(true);
        try {
            const loaded = await loadPortals();
            setPortals(loaded);
        } finally {
            setLoading(false);
        }
    }, []);

    const deletePortal = useCallback(
        async (id: string) => {
            await deletePortalWithRelatedData(id);
            await refresh();
        },
        [refresh],
    );

    const createPortal = useCallback(async (templateId: PortalTemplateId) => {
        const portal = await createPortalFromTemplate(templateId);
        const loaded = await loadPortals();
        setPortals(loaded);
        return portal;
    }, []);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    return { portals, loading, refresh, deletePortal, createPortal };
}
