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
import { buildLinearBreadcrumbs, SidebarNavigation, useLayoutConfig } from '@gravitee/graphene-core';
import { useCallback, useMemo } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

import { useEnvHrid } from '../../features/environment/environment.utils';
import { NAV_GROUPS } from '../config/navigation';
import { hostNavPath, isHostNavKey, resolveHostRoute } from '../config/routes';

export function RouteLayout() {
    const navigate = useNavigate();
    const envHrid = useEnvHrid();
    const { pathname } = useLocation();

    const { activeNavKey, breadcrumbSegments } = useMemo(() => resolveHostRoute(pathname, envHrid), [pathname, envHrid]);

    const handleNavSelect = useCallback(
        (key: string) => {
            if (isHostNavKey(key)) {
                navigate(hostNavPath(key, envHrid));
            }
        },
        [navigate, envHrid],
    );

    const breadcrumbs = useMemo(() => buildLinearBreadcrumbs(navigate, [...breadcrumbSegments]), [breadcrumbSegments, navigate]);

    useLayoutConfig(
        {
            navigation: <SidebarNavigation groups={NAV_GROUPS} activeItemKey={activeNavKey} onItemSelect={handleNavSelect} />,
            breadcrumbs,
        },
        [activeNavKey, breadcrumbs, handleNavSelect],
    );

    return <Outlet />;
}
