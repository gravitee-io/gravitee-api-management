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
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import type { NavGroup } from '@gravitee/graphene-core';
import { useCallback, useMemo } from 'react';

import { APIM_ROUTE_CONFIG, isRouteKey } from '../config/routes';

/** Side menu items link to the page their selection navigates to, so they can be opened in a new tab. */
export function useModuleNavigation(groups: readonly NavGroup[]): { navGroups: NavGroup[]; handleNavSelect: (key: string) => void } {
    const { hrefForKey, navigateToKey } = useModuleRouting(APIM_ROUTE_CONFIG);

    const navGroups = useMemo(
        () =>
            groups.map(group => ({
                ...group,
                items: group.items.map(item => (isRouteKey(item.key) ? { ...item, href: hrefForKey(item.key) } : item)),
            })),
        [groups, hrefForKey],
    );

    const handleNavSelect = useCallback(
        (key: string) => {
            if (isRouteKey(key)) navigateToKey(key);
        },
        [navigateToKey],
    );

    return { navGroups, handleNavSelect };
}
