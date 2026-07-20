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
    getPortal,
    updatePortalSettings,
    type PortalSettingsPatch,
} from '../../portals/storage/portals.storage';
import type { DeveloperPortal } from '../../portals/types';

export function usePortal(portalId: string | undefined) {
    const [portal, setPortal] = useState<DeveloperPortal | undefined>();
    const [loading, setLoading] = useState(true);
    const [missing, setMissing] = useState(false);

    const refresh = useCallback(async () => {
        if (!portalId) {
            setPortal(undefined);
            setMissing(true);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            const loaded = await getPortal(portalId);
            setPortal(loaded);
            setMissing(!loaded);
        } finally {
            setLoading(false);
        }
    }, [portalId]);

    const updateSettings = useCallback(
        async (patch: PortalSettingsPatch) => {
            if (!portalId) {
                return undefined;
            }

            const updated = await updatePortalSettings(portalId, patch);
            if (updated) {
                setPortal(updated);
            }
            return updated;
        },
        [portalId],
    );

    useEffect(() => {
        void refresh();
    }, [refresh]);

    return { portal, loading, missing, refresh, updateSettings };
}
