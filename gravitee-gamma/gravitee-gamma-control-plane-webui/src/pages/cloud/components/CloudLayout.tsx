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

import { CLOUD_NAV_GROUPS } from '../cloud-navigation';
import { cloudNavPath, isCloudNavKey, resolveCloudRoute } from '../cloud-routes';
import { useEnvHrid } from '../../../features/environment/environment.utils';

export function CloudLayout() {
    const navigate = useNavigate();
    const envHrid = useEnvHrid();
    const { pathname } = useLocation();

    const { activeNavKey, breadcrumbSegments } = useMemo(() => resolveCloudRoute(pathname, envHrid), [pathname, envHrid]);

    const handleNavSelect = useCallback(
        (key: string) => {
            if (isCloudNavKey(key)) {
                navigate(cloudNavPath(key, envHrid));
            }
        },
        [navigate, envHrid],
    );

    const breadcrumbs = useMemo(() => buildLinearBreadcrumbs(navigate, [...breadcrumbSegments]), [breadcrumbSegments, navigate]);

    const isSettings = pathname.includes('/cloud/settings');

    useLayoutConfig(
        {
            navigation: <SidebarNavigation groups={CLOUD_NAV_GROUPS} activeItemKey={activeNavKey} onItemSelect={handleNavSelect} />,
            breadcrumbs,
            contentVariant: 'default',
            ...(isSettings
                ? {}
                : {
                      viewMode: 'global',
                      contextSidebar: undefined,
                      leading: undefined,
                      contextExpanded: undefined,
                  }),
        },
        [activeNavKey, breadcrumbs, handleNavSelect, isSettings],
    );

    return <Outlet />;
}
