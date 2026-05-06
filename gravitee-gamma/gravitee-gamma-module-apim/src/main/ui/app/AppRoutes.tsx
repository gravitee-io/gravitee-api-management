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
import { buildLinearBreadcrumbs, SidebarNavigation, useLayoutConfig } from '@gravitee/graphene-core';
import { useMemo } from 'react';
import { Outlet, Route, Routes, useNavigate } from 'react-router-dom';

import { NAV_GROUPS } from '../config/navigation';
import { APIM_ROUTE_CONFIG } from '../config/routes';
import { AnalyticsPage } from '../pages/AnalyticsPage';
import { ApiProductsPage } from '../pages/ApiProductsPage';
import { ApisPage } from '../pages/ApisPage';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { DashboardPage } from '../pages/DashboardPage';
import { SettingsPage } from '../pages/SettingsPage';

function ModuleLayout() {
    const navigate = useNavigate();
    const { activeNavKey, navigateToKey, rootPath } = useModuleRouting(APIM_ROUTE_CONFIG);

    const breadcrumbs = useMemo(
        () => buildLinearBreadcrumbs(navigate, [{ label: 'APIM', to: rootPath }, { label: APIM_ROUTE_CONFIG.routes[activeNavKey].label }]),
        [activeNavKey, navigate, rootPath],
    );

    const isSettings = activeNavKey === 'settings';

    useLayoutConfig(
        {
            navigation: <SidebarNavigation groups={NAV_GROUPS} activeItemKey={activeNavKey} onItemSelect={navigateToKey} />,
            breadcrumbs,
            ...(isSettings
                ? { viewMode: 'context' as const }
                : {
                      viewMode: 'global' as const,
                      contextSidebar: null,
                      leading: null,
                  }),
        },
        [activeNavKey, breadcrumbs, navigateToKey, isSettings],
    );

    return <Outlet />;
}

/** Route tree for this module: mounted under the host router when federated, or under the local dev root for standalone. */
export function AppRoutes() {
    return (
        <Routes>
            <Route element={<ModuleLayout />}>
                <Route index element={<DashboardPage />} />
                <Route path="dashboard" element={<DashboardPage />} />
                <Route path="apis" element={<ApisPage />} />
                <Route path="api-products" element={<ApiProductsPage />} />
                <Route path="applications" element={<ApplicationsPage />} />
                <Route path="analytics" element={<AnalyticsPage />} />
                <Route path="settings" element={<SettingsPage />} />
            </Route>
        </Routes>
    );
}
