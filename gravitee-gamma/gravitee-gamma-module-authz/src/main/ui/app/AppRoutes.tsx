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
import { SidebarNavigation, Toaster, useLayoutConfig } from '@gravitee/graphene-core';
import { useCallback, useMemo } from 'react';
import { Navigate, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { NAV_GROUPS } from '../config/navigation';
import { ROUTES, navigateToNavKey, resolveModulePath } from '../config/routes';
import { buildLinearBreadcrumbs } from '../lib/buildLinearBreadcrumbs';
import { ModuleErrorBoundary } from './ModuleErrorBoundary';
import { ApisPage } from './features/policy-management/pages/ApisPage';
import { CustomPoliciesPage } from './features/policy-management/pages/CustomPoliciesPage';
import { LlmsPage } from './features/policy-management/pages/LlmsPage';
import { McpsPage } from './features/policy-management/pages/McpsPage';

function ModuleLayout() {
    const location = useLocation();
    const navigate = useNavigate();

    const { modulePrefix, activeNavKey } = useMemo(() => resolveModulePath(location.pathname), [location.pathname]);

    const handleNavSelect = useCallback(
        (key: string) => {
            navigateToNavKey(navigate, modulePrefix, key, location.pathname);
        },
        [navigate, modulePrefix, location.pathname],
    );

    const breadcrumbs = useMemo(() => {
        const pageLabel = ROUTES[activeNavKey].label;
        const segments = location.pathname.split('/').filter(Boolean);
        let hostPrefix = '';
        if (segments[0] === 'environments' && segments.length >= 3) {
            hostPrefix = `/${segments[0]}/${segments[1]}/${segments[2]}`;
        } else if (modulePrefix) {
            hostPrefix = `/${modulePrefix}`;
        }
        const rootPath = hostPrefix ? `${hostPrefix}/mcps` : '/mcps';
        return buildLinearBreadcrumbs(navigate, [{ label: 'Authorization', to: rootPath }, { label: pageLabel }]);
    }, [activeNavKey, navigate, modulePrefix, location.pathname]);

    useLayoutConfig(
        {
            navigation: <SidebarNavigation groups={NAV_GROUPS} activeItemKey={activeNavKey} onItemSelect={handleNavSelect} />,
            breadcrumbs,
        },
        [activeNavKey, breadcrumbs, handleNavSelect],
    );

    return <Outlet />;
}

export function AppRoutes() {
    return (
        <ModuleErrorBoundary>
            <Routes>
                <Route element={<ModuleLayout />}>
                    <Route index element={<Navigate to="mcps" replace />} />
                    <Route path="dashboard" element={<Navigate to="../mcps" replace />} />
                    <Route path="mcps" element={<McpsPage />} />
                    <Route path="llms" element={<LlmsPage />} />
                    <Route path="apis" element={<ApisPage />} />
                    <Route path="custom-policies" element={<CustomPoliciesPage />} />
                </Route>
            </Routes>
            <Toaster position="top-right" richColors closeButton />
        </ModuleErrorBoundary>
    );
}
