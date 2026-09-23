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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';

import { getEnvironmentPortalConfiguration } from '../../settings/services/portalSettings';
import { portalSettingsKeys } from '../utils/queryKeys';

/**
 * Broadcast by the platform module right after it saves the environment's portal settings, so this
 * module can drop its own cached copy of the flags. Mirrors `PORTAL_SETTINGS_CHANGED_EVENT` in
 * `gravitee-gamma-module-platform`'s API Review settings page; the two run as separate federated
 * modules and cannot import from each other.
 */
export const PORTAL_SETTINGS_CHANGED_EVENT = 'gamma:portal-settings-changed';

/**
 * GET /portal: the environment feature flags Classic keeps on `Constants.env.settings` (API Score, API Review).
 *
 * These flags are owned by the platform module's settings page, which runs as a separate federated module
 * with its OWN QueryClient. Its `invalidateQueries` after a save therefore cannot reach this cache, whatever
 * the query key is called, so freshness is kept three ways: the settings page broadcasts
 * `PORTAL_SETTINGS_CHANGED_EVENT` when it saves, screens re-read the flags when they mount, and a save made
 * in another tab is picked up when this one is focused again.
 */
export function useEnvironmentPortalConfiguration() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    useEffect(() => {
        const dropCachedFlags = () => void queryClient.invalidateQueries({ queryKey: portalSettingsKeys.all });
        window.addEventListener(PORTAL_SETTINGS_CHANGED_EVENT, dropCachedFlags);
        return () => window.removeEventListener(PORTAL_SETTINGS_CHANGED_EVENT, dropCachedFlags);
    }, [queryClient]);

    return useQuery({
        queryKey: portalSettingsKeys.portalConfig(env?.id ?? ''),
        queryFn: () => getEnvironmentPortalConfiguration(env!.id),
        enabled: Boolean(env?.id),
        staleTime: 60_000,
        refetchOnMount: 'always',
        refetchOnWindowFocus: true,
    });
}
