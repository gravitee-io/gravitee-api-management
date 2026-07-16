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
import { Route, Routes } from 'react-router-dom';

import { PortalEditPage } from '../features/portals/pages/PortalEditPage';
import { PortalFirstPageRedirect } from '../features/portals/pages/PortalFirstPageRedirect';
import { PortalsDashboardPage } from '../features/portals/pages/PortalsDashboardPage';
import { PortalViewPage } from '../features/portals/pages/PortalViewPage';
import { PortalAuthRoutePage } from '../features/consumer-auth/pages/PortalAuthRoutePage';
import { PortalTenantDetailPage } from '../features/tenants/pages/PortalTenantDetailPage';
import { PortalTenantsPage } from '../features/tenants/pages/PortalTenantsPage';
import { GlobalPortalTenantsPage } from '../features/tenants/pages/GlobalPortalTenantsPage';
import { NotFoundPage } from '../shared/components/NotFoundPage';

export function App() {
    return (
        <Routes>
            <Route path="/" element={<PortalsDashboardPage />} />
            <Route path="/tenants" element={<GlobalPortalTenantsPage />} />
            <Route path="/portals/:portalId/tenants/:tenantId" element={<PortalTenantDetailPage />} />
            <Route path="/portals/:portalId/tenants" element={<PortalTenantsPage />} />
            <Route path="/portals/:id/edit/:slug" element={<PortalEditPage />} />
            <Route path="/portals/:id/edit" element={<PortalFirstPageRedirect mode="edit" />} />
            <Route path="/portals/:id/login" element={<PortalAuthRoutePage variant="login" />} />
            <Route path="/portals/:id/signup" element={<PortalAuthRoutePage variant="signup" />} />
            <Route path="/portals/:id/invite/:token" element={<PortalAuthRoutePage variant="invite" />} />
            <Route path="/portals/:id/:slug" element={<PortalViewPage />} />
            <Route path="/portals/:id" element={<PortalFirstPageRedirect mode="view" />} />
            <Route
                path="*"
                element={
                    <NotFoundPage
                        homePath="/"
                        homeLabel="Back to dashboards"
                        className="min-h-screen"
                    />
                }
            />
        </Routes>
    );
}
