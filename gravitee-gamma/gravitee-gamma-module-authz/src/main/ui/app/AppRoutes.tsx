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
import { SidebarNavigation, buildLinearBreadcrumbs, useLayoutConfig } from '@gravitee/graphene-core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useMemo } from 'react';
import { Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom';
import { NAV_GROUPS } from '../config/navigation';
import { AUTHZ_ROUTE_CONFIG, ROUTES, type RouteKey } from '../config/routes';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { ApisPage } from '../features/policy-management/pages/ApisPage';
import { CustomPoliciesPage } from '../features/policy-management/pages/CustomPoliciesPage';
import { McpsPage } from '../features/policy-management/pages/McpsPage';
import { ModelsPage } from '../features/policy-management/pages/ModelsPage';
import { ActionsPage } from '../features/policy-structure/ActionsPage';
import { EntitiesPage } from '../features/policy-structure/EntitiesPage';
import { ModuleErrorBoundary } from './ModuleErrorBoundary';

const queryClient = new QueryClient();

function ModuleLayout() {
    const navigate = useNavigate();
    const { activeNavKey, navigateToKey, rootPath } = useModuleRouting(AUTHZ_ROUTE_CONFIG);

    const breadcrumbs = useMemo(
        () => buildLinearBreadcrumbs(navigate, [{ label: 'Authorization', to: rootPath }, { label: ROUTES[activeNavKey].label }]),
        [activeNavKey, navigate, rootPath],
    );

    useLayoutConfig(
        {
            navigation: (
                <SidebarNavigation groups={NAV_GROUPS} activeItemKey={activeNavKey} onItemSelect={key => navigateToKey(key as RouteKey)} />
            ),
            breadcrumbs,
        },
        [activeNavKey, breadcrumbs, navigateToKey],
    );

    return <Outlet />;
}

export function AppRoutes() {
    return (
        <QueryClientProvider client={queryClient}>
            <ModuleErrorBoundary>
                <Routes>
                    <Route element={<ModuleLayout />}>
                        <Route index element={<Navigate to="dashboard" replace />} />
                        <Route path="dashboard" element={<DashboardPage />} />
                        <Route path="mcps" element={<McpsPage />} />
                        <Route path="models" element={<ModelsPage />} />
                        <Route path="apis" element={<ApisPage />} />
                        <Route path="custom-policies" element={<CustomPoliciesPage />} />
                        <Route path="entities" element={<EntitiesPage />} />
                        <Route path="actions" element={<ActionsPage />} />
                    </Route>
                </Routes>
            </ModuleErrorBoundary>
        </QueryClientProvider>
    );
}
