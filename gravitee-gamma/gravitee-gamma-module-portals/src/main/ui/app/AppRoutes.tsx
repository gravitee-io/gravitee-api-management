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
import { SidebarNavigation, useLayoutConfig } from '@gravitee/graphene-core';
import { QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';

import { useModuleBreadcrumbs } from './useModuleBreadcrumbs';
import { useModuleNavigation } from './useModuleNavigation';
import { getActiveNavKey, PORTALS_ROUTE_CONFIG, ROUTES } from '../config/routes';
import { PortalOverviewPlaceholderPage } from '../features/portals/pages/PortalOverviewPlaceholderPage';
import { PortalsDashboardPage } from '../features/portals/pages/PortalsDashboardPage';
import { PortalsListPlaceholderPage } from '../features/portals/pages/PortalsListPlaceholderPage';
import { createPortalsQueryClient } from '../shared/api/queryClient';

function ModuleLayout() {
    const routing = useModuleRouting(PORTALS_ROUTE_CONFIG);
    const { pathname } = useLocation();
    const activeNavKey = getActiveNavKey(pathname, routing.pathForKey('dashboard'));
    const { navGroups, handleNavSelect } = useModuleNavigation(routing);
    const breadcrumbs = useModuleBreadcrumbs(activeNavKey, routing);

    useLayoutConfig(
        {
            navigation: <SidebarNavigation groups={navGroups} activeItemKey={activeNavKey} onItemSelect={handleNavSelect} />,
            breadcrumbs,
        },
        [activeNavKey, breadcrumbs, navGroups, handleNavSelect],
    );

    return <Outlet />;
}

export function AppRoutes() {
    const [queryClient] = useState(createPortalsQueryClient);

    return (
        <QueryClientProvider client={queryClient}>
            <Routes>
                <Route element={<ModuleLayout />}>
                    <Route index element={<PortalsDashboardPage />} />
                    <Route path={ROUTES.portals.path} element={<PortalsListPlaceholderPage />} />
                    <Route path=":portalId">
                        <Route index element={<Navigate to="overview" replace />} />
                        <Route path="overview" element={<PortalOverviewPlaceholderPage />} />
                        <Route path="*" element={<Navigate to="../overview" replace />} />
                    </Route>
                </Route>
            </Routes>
        </QueryClientProvider>
    );
}
