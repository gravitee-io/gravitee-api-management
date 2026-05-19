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
import { Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom';

import { NAV_GROUPS } from '../config/navigation';
import { APIM_ROUTE_CONFIG } from '../config/routes';
import { ApiProductDetailLayout, ApiProductIndexRedirect } from '../features/api-products/components';
import { ApiDetailIndexRedirect, ApiDetailLayout } from '../features/apis/components/detail/ApiDetailLayout';
import { AlertFormPage } from '../pages/AlertFormPage';
import { AnalyticsPage } from '../pages/AnalyticsPage';
import { ApiAlertsPage } from '../pages/ApiAlertsPage';
import { ApiBroadcastsPage } from '../pages/ApiBroadcastsPage';
import { ApiConsumerDetailPage } from '../pages/ApiConsumerDetailPage';
import { ApiConsumersPage } from '../pages/ApiConsumersPage';
import { ApiCorsPage } from '../pages/ApiCorsPage';
import { ApiDetailOverviewPage } from '../pages/ApiDetailOverviewPage';
import { ApiDetailPlaceholderPage } from '../pages/ApiDetailPlaceholderPage';
import { ApiEntrypointsPage } from '../pages/ApiEntrypointsPage';
import { ApiFailoverPage } from '../pages/ApiFailoverPage';
import { ApiGeneralPage } from '../pages/ApiGeneralPage';
import { ApiNotificationsPage } from '../pages/ApiNotificationsPage';
import { ApiPlanFormPage } from '../pages/ApiPlanFormPage';
import { ApiPlansPage } from '../pages/ApiPlansPage';
import { ApiProductApisPage } from '../pages/ApiProductApisPage';
import { ApiProductConsumerDetailPage } from '../pages/ApiProductConsumerDetailPage';
import { ApiProductConsumersPage } from '../pages/ApiProductConsumersPage';
import { ApiProductGeneralPage } from '../pages/ApiProductGeneralPage';
import { ApiProductOverviewPage } from '../pages/ApiProductOverviewPage';
import { ApiProductPlanFormPage } from '../pages/ApiProductPlanFormPage';
import { ApiProductPlansPage } from '../pages/ApiProductPlansPage';
import { ApiProductsPage } from '../pages/ApiProductsPage';
import { ApiProductUserPermissionsPage } from '../pages/ApiProductUserPermissionsPage';
import { ApiPropertiesPage } from '../pages/ApiPropertiesPage';
import { ApisPage } from '../pages/ApisPage';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { AuditLogsPage } from '../pages/AuditLogsPage';
import { CreateApiProductPage } from '../pages/CreateApiProductPage';
import { CreateApiProxyPage } from '../pages/CreateApiProxyPage';
import { DashboardPage } from '../pages/DashboardPage';
import { DeploymentConfigurationPage } from '../pages/DeploymentConfigurationPage';
import { DeploymentHistoryPage } from '../pages/DeploymentHistoryPage';
import { ScratchWizardPage } from '../pages/ScratchWizardPage';
import { SettingsPage } from '../pages/SettingsPage';
import { TemplateWizardPage } from '../pages/TemplateWizardPage';
import { UserPermissionsPage } from '../pages/UserPermissionsPage';

const queryClient = new QueryClient();

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
                        <Route path=":apiId" element={<ApiDetailLayout />}>
                            <Route index element={<ApiDetailIndexRedirect />} />
                            <Route path="overview" element={<ApiDetailOverviewPage />} />
                            <Route path="general" element={<ApiGeneralPage />} />
                            <Route path="properties" element={<ApiPropertiesPage />} />
                            <Route path="resources" element={<ApiDetailPlaceholderPage title="Resources" />} />
                            <Route path="notifications" element={<ApiNotificationsPage />} />
                            <Route path="api-score" element={<ApiDetailPlaceholderPage title="API Score" />} />
                            <Route path="response-templates" element={<ApiDetailPlaceholderPage title="Response Templates" />} />
                            <Route path="entrypoints" element={<ApiEntrypointsPage />} />
                            <Route path="cors" element={<ApiCorsPage />} />
                            <Route path="endpoints">
                                <Route index element={<Navigate to="list" replace />} />
                                <Route path="list" element={<ApiDetailPlaceholderPage title="Endpoints" />} />
                                <Route path="failover" element={<ApiFailoverPage />} />
                                <Route
                                    path="health-check-dashboard"
                                    element={<ApiDetailPlaceholderPage title="Health Check Dashboard" />}
                                />
                            </Route>
                            <Route path="reporter-settings" element={<ApiDetailPlaceholderPage title="Reporter Settings" />} />
                            <Route path="policy-studio" element={<ApiDetailPlaceholderPage title="Policy Studio" />} />
                            <Route path="documentation" element={<ApiDetailPlaceholderPage title="Documentation" />} />
                            <Route path="plans">
                                <Route index element={<ApiPlansPage />} />
                                <Route path="new/:securityType" element={<ApiPlanFormPage />} />
                                <Route path=":planId" element={<ApiPlanFormPage />} />
                            </Route>
                            <Route path="consumers">
                                <Route index element={<ApiConsumersPage />} />
                                <Route path=":subscriptionId" element={<ApiConsumerDetailPage />} />
                            </Route>
                            <Route path="broadcasts" element={<ApiBroadcastsPage />} />
                            <Route path="authorization" element={<ApiDetailPlaceholderPage title="Authorization" />} />
                            <Route path="user-permissions" element={<UserPermissionsPage />} />
                            <Route path="alerts">
                                <Route index element={<ApiAlertsPage />} />
                                <Route path="new" element={<AlertFormPage />} />
                                <Route path=":alertId" element={<AlertFormPage />} />
                            </Route>
                            <Route path="audit-logs" element={<AuditLogsPage />} />
                            <Route path="deployment">
                                <Route index element={<Navigate to="configuration" replace />} />
                                <Route path="configuration" element={<DeploymentConfigurationPage />} />
                                <Route path="history" element={<DeploymentHistoryPage />} />
                            </Route>
                            <Route path="*" element={<Navigate to="overview" replace />} />
                        </Route>
                    </Route>
                    <Route path="api-products">
                        <Route index element={<ApiProductsPage />} />
                        <Route path="new" element={<CreateApiProductPage />} />
                        <Route path=":productId" element={<ApiProductDetailLayout />}>
                            <Route index element={<ApiProductIndexRedirect />} />
                            <Route path="overview" element={<ApiProductOverviewPage />} />
                            <Route path="general" element={<ApiProductGeneralPage />} />
                            <Route path="apis" element={<ApiProductApisPage />} />
                            <Route path="plans">
                                <Route index element={<ApiProductPlansPage />} />
                                <Route path="new/:securityType" element={<ApiProductPlanFormPage />} />
                                <Route path=":planId" element={<ApiProductPlanFormPage />} />
                            </Route>
                            <Route path="consumers">
                                <Route index element={<ApiProductConsumersPage />} />
                                <Route path=":subscriptionId" element={<ApiProductConsumerDetailPage />} />
                            </Route>
                            <Route path="user-permissions" element={<ApiProductUserPermissionsPage />} />
                            <Route path="*" element={<Navigate to="overview" replace />} />
                        </Route>
                    </Route>
                    <Route path="applications" element={<ApplicationsPage />} />
                    <Route path="analytics" element={<AnalyticsPage />} />
                    <Route path="settings" element={<SettingsPage />} />
                </Route>
            </Routes>
        </QueryClientProvider>
    );
}
