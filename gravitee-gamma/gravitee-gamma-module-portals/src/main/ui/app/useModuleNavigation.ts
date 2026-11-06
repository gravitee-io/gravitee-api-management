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
import type { ModuleRoutingResult } from '@gravitee/gamma-modules-sdk/routing';
import type { NavGroup } from '@gravitee/graphene-core';
import { useCallback, useMemo } from 'react';

import { NAV_GROUPS } from '../config/navigation';
import { isRouteKey, type RouteKey } from '../config/routes';

export function useModuleNavigation({ hrefForKey, navigateToKey }: Pick<ModuleRoutingResult<RouteKey>, 'hrefForKey' | 'navigateToKey'>): {
    navGroups: NavGroup[];
    handleNavSelect: (key: string) => void;
} {
    const navGroups = useMemo(
        () =>
            NAV_GROUPS.map(group => ({
                ...group,
                items: group.items.map(item => (isRouteKey(item.key) ? { ...item, href: hrefForKey(item.key) } : item)),
            })),
        [hrefForKey],
    );
    const handleNavSelect = useCallback(
        (key: string) => {
            if (isRouteKey(key)) navigateToKey(key);
        },
        [navigateToKey],
    );
    return { navGroups, handleNavSelect };
}
