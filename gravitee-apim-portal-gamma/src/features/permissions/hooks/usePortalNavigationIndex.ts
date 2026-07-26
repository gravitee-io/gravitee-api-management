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
import { useCallback, useEffect, useMemo, useState } from 'react';

import { getNavItems } from '../../portals/storage/navigation-items.storage';
import { getAllPortals } from '../../portals/storage/portals.storage';
import type { PortalNavigationItem } from '../../portals/types/navigation-item.types';

/**
 * Navigation items across every portal. Grants are environment-level, so the same asset can appear
 * in several portals and each occurrence carries its own overridable subtree.
 */
export function usePortalNavigationIndex() {
    const [items, setItems] = useState<PortalNavigationItem[]>([]);
    const [portals, setPortals] = useState<{ id: string; name: string }[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        setLoading(true);
        try {
            const allPortals = await getAllPortals();
            const navItemsByPortal = await Promise.all(allPortals.map(portal => getNavItems(portal.id)));
            setPortals(allPortals.map(portal => ({ id: portal.id, name: portal.name })));
            setItems(navItemsByPortal.flat());
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const portalNameById = useMemo(
        () => new Map(portals.map(portal => [portal.id, portal.name])),
        [portals],
    );

    return { items, portals, portalNameById, loading, refresh };
}
