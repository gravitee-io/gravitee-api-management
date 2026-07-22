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
import { Navigate, Route, Routes } from 'react-router-dom';

import { DomainsPage } from '../features/module-config/pages/DomainsPage';
import { GoogleAnalyticsPage } from '../features/module-config/pages/GoogleAnalyticsPage';
import { IdentityProvidersPage } from '../features/module-config/pages/IdentityProvidersPage';
import { LogsPage } from '../features/module-config/pages/LogsPage';
import { ModuleDashboardsPage } from '../features/module-config/pages/ModuleDashboardsPage';
import { TemplatesPage } from '../features/module-config/pages/TemplatesPage';
import { ThirdPartyAppsPage } from '../features/module-config/pages/ThirdPartyAppsPage';
import { WebhooksPage } from '../features/module-config/pages/WebhooksPage';
import { PortalDetailLayout } from '../features/portals/components/detail/PortalDetailLayout';
import { PortalEditPage } from '../features/portals/pages/PortalEditPage';
import { PortalFirstPageRedirect } from '../features/portals/pages/PortalFirstPageRedirect';
import { PortalsDashboardPage } from '../features/portals/pages/PortalsDashboardPage';
import { PortalViewPage } from '../features/portals/pages/PortalViewPage';
import { PortalAuthRoutePage } from '../features/consumer-auth/pages/PortalAuthRoutePage';
import { LegacyPortalTenantsRedirect } from '../features/tenants/pages/LegacyPortalTenantsRedirect';
import { PortalTenantDetailPage } from '../features/tenants/pages/PortalTenantDetailPage';
import { PortalTenantsPage } from '../features/tenants/pages/PortalTenantsPage';
import { CategoriesPage } from '../features/settings/pages/CategoriesPage';
import { IdpConfigurationPage } from '../features/settings/pages/IdpConfigurationPage';
import { PortalGeneralSettingsPage } from '../features/settings/pages/PortalGeneralSettingsPage';
import { PortalSettingsComingSoonPage } from '../features/settings/pages/PortalSettingsComingSoonPage';
import { SubscriptionFormDetailPage } from '../features/settings/pages/SubscriptionFormDetailPage';
import { SubscriptionFormListPage } from '../features/settings/pages/SubscriptionFormListPage';
import { WorkflowDetailPage } from '../features/settings/pages/WorkflowDetailPage';
import { WorkflowsPage } from '../features/settings/pages/WorkflowsPage';
import { NotFoundPage } from '../shared/components/NotFoundPage';

export function App() {
    return (
        <Routes>
            <Route path="/" element={<PortalsDashboardPage />} />
            <Route path="/identity-providers" element={<IdentityProvidersPage />} />
            <Route path="/domains" element={<DomainsPage />} />
            <Route path="/templates" element={<TemplatesPage />} />
            <Route path="/google-analytics" element={<GoogleAnalyticsPage />} />
            <Route path="/webhooks" element={<WebhooksPage />} />
            <Route path="/third-party-apps" element={<ThirdPartyAppsPage />} />
            <Route path="/dashboards" element={<ModuleDashboardsPage />} />
            <Route path="/logs" element={<LogsPage />} />
            <Route path="/portals/:portalId/settings" element={<PortalDetailLayout />}>
                <Route index element={<Navigate to="general" replace />} />
                <Route path="general" element={<PortalGeneralSettingsPage />} />
                <Route path="categories" element={<CategoriesPage />} />
                <Route
                    path="subscription-forms/:formId"
                    element={<SubscriptionFormDetailPage />}
                />
                <Route path="subscription-forms" element={<SubscriptionFormListPage />} />
                <Route path="workflows/:workflowId" element={<WorkflowDetailPage />} />
                <Route path="workflows" element={<WorkflowsPage />} />
                <Route path="idp" element={<IdpConfigurationPage />} />
                <Route path="tenants/:tenantId" element={<PortalTenantDetailPage />} />
                <Route path="tenants" element={<PortalTenantsPage />} />
                <Route path=":section" element={<PortalSettingsComingSoonPage />} />
            </Route>
            <Route path="/portals/:portalId/tenants/:tenantId" element={<LegacyPortalTenantsRedirect />} />
            <Route path="/portals/:portalId/tenants" element={<LegacyPortalTenantsRedirect />} />
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
