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
import { SidebarNavigation, useLayoutConfig, buildLinearBreadcrumbs } from '@gravitee/graphene-core';
import React, { useCallback, useMemo } from 'react';
import { Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom';

import { NAV_GROUPS } from '../config/navigation';
import { navigateToNavKey, ROUTES, resolveModulePath } from '../config/routes';
import { DashboardPage } from '../pages/DashboardPage';
import {
    ApiDesignerPage,
    ApiProductsPage,
    ApisPage,
    ApiProxyWizardPage,
    DashboardsPage,
    LineagePage,
    LogsPage,
    MigrationStudioPage,
    SettingsPage,
} from '../pages';

function ModuleLayout() {
    const location = useLocation();
    const navigate = useNavigate();

    const { modulePrefix, activeNavKey } = useMemo(() => resolveModulePath(location.pathname), [location.pathname]);

    const handleNavSelect = useCallback(
        (key: string) => {
            navigateToNavKey(navigate, modulePrefix, key);
        },
        [navigate, modulePrefix],
    );

    const breadcrumbs = useMemo(() => {
        const pageLabel = ROUTES[activeNavKey].label;
        const rootPath = modulePrefix ? `/${modulePrefix}/dashboard` : '/';
        return buildLinearBreadcrumbs(navigate, [{ label: 'APIM', to: rootPath }, { label: pageLabel }]);
    }, [activeNavKey, navigate, modulePrefix]);

    useLayoutConfig(
        {
            navigation: <SidebarNavigation groups={NAV_GROUPS} activeItemKey={activeNavKey} onItemSelect={handleNavSelect} />,
            breadcrumbs,
        },
        [activeNavKey, breadcrumbs, handleNavSelect],
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
                <Route path="api-proxy-wizard" element={<ApiProxyWizardPage />} />
                <Route path="migration-studio" element={<MigrationStudioPage />} />
                <Route path="api-products" element={<ApiProductsPage />} />
                <Route path="api-designer" element={<ApiDesignerPage />} />
                <Route path="dashboards" element={<DashboardsPage />} />
                <Route path="logs" element={<LogsPage />} />
                <Route path="lineage" element={<LineagePage />} />
                <Route path="settings" element={<SettingsPage />} />
            </Route>
        </Routes>
    );
}
