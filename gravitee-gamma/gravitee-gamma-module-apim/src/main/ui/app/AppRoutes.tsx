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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useMemo } from 'react';
import { Outlet, Route, Routes, useNavigate } from 'react-router-dom';

const queryClient = new QueryClient();

import { NAV_GROUPS } from '../config/navigation';
import { APIM_ROUTE_CONFIG } from '../config/routes';
import { AnalyticsPage } from '../pages/AnalyticsPage';
import { ApiProductsPage } from '../pages/ApiProductsPage';
import { ApisPage } from '../pages/ApisPage';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { CreateApiProxyPage } from '../pages/CreateApiProxyPage';
import { DashboardPage } from '../pages/DashboardPage';
import { ScratchWizardPage } from '../pages/ScratchWizardPage';
import { SettingsPage } from '../pages/SettingsPage';
import { TemplateWizardPage } from '../pages/TemplateWizardPage';

function ModuleLayout() {
    const navigate = useNavigate();
    const { activeNavKey, navigateToKey, rootPath } = useModuleRouting(APIM_ROUTE_CONFIG);

    const breadcrumbs = useMemo(
        () => buildLinearBreadcrumbs(navigate, [{ label: 'APIM', to: rootPath }, { label: APIM_ROUTE_CONFIG.routes[activeNavKey].label }]),
        [activeNavKey, navigate, rootPath],
    );

    useLayoutConfig(
        {
            navigation: <SidebarNavigation groups={NAV_GROUPS} activeItemKey={activeNavKey} onItemSelect={navigateToKey} />,
            breadcrumbs,
        },
        [activeNavKey, breadcrumbs, navigateToKey],
    );

    return <Outlet />;
}

/** Route tree for this module: mounted under the host router when federated, or under the local dev root for standalone. */
export function AppRoutes() {
    return (
        <QueryClientProvider client={queryClient}>
            <Routes>
                <Route element={<ModuleLayout />}>
                    <Route index element={<DashboardPage />} />
                    <Route path="dashboard" element={<DashboardPage />} />
                    <Route path="apis">
                        <Route index element={<ApisPage />} />
                        <Route path="new">
                            <Route index element={<CreateApiProxyPage />} />
                            <Route path="scratch" element={<ScratchWizardPage />} />
                            <Route path="template/:id" element={<TemplateWizardPage />} />
                        </Route>
                    </Route>
                    <Route path="api-products" element={<ApiProductsPage />} />
                    <Route path="applications" element={<ApplicationsPage />} />
                    <Route path="analytics" element={<AnalyticsPage />} />
                    <Route path="settings" element={<SettingsPage />} />
                </Route>
            </Routes>
        </QueryClientProvider>
    );
}
