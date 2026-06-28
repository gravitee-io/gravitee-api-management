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
/** Module Federation entry: dashboard-only routes for Gamma Console embedding. */
import { buildLinearBreadcrumbs, useLayoutConfig } from '@gravitee/graphene-core';
import { useMemo } from 'react';
import { Outlet, Route, Routes, useNavigate } from 'react-router-dom';

import { PortalAppProvider } from './app/PortalAppContext';
import constants from './constants.json';
import { PortalsDashboardPage } from './features/portals/pages/PortalsDashboardPage';

const standaloneEditorBaseUrl = constants.appBasePath ?? '/portal-editor';

function ModuleLayout() {
    const navigate = useNavigate();
    const breadcrumbs = useMemo(() => buildLinearBreadcrumbs(navigate, [{ label: 'Developer Portals' }]), [navigate]);

    useLayoutConfig({ breadcrumbs }, [breadcrumbs]);

    return <Outlet />;
}

export function DashboardRoutes() {
    return (
        <PortalAppProvider embeddedInConsole standaloneEditorBaseUrl={standaloneEditorBaseUrl}>
            <Routes>
                <Route element={<ModuleLayout />}>
                    <Route index element={<PortalsDashboardPage />} />
                    <Route path="*" element={<PortalsDashboardPage />} />
                </Route>
            </Routes>
        </PortalAppProvider>
    );
}

export { DashboardRoutes as default };
