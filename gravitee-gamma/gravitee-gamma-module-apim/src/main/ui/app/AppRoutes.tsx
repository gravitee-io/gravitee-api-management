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
import '@gravitee/gamma-lib-observability/styles';
import '@gravitee/graphene-charts/lineage/styles.css';
import { CapabilityProvider, type DashboardCapabilities } from '@gravitee/gamma-lib-observability';
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { buildModuleNavPath, resolveModulePath } from '@gravitee/gamma-modules-sdk/routing';
import { buildLinearBreadcrumbs, SidebarNavigation, useLayoutConfig } from '@gravitee/graphene-core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';
import { Navigate, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom';

import { ApimToaster } from './ApimToaster';
import { OnboardingProvider, OnboardingTourHost } from './onboarding';
import { NAV_GROUPS } from '../config/navigation';
import { observability } from '../config/observability';
import { APIM_ROUTE_CONFIG, getActiveNavKey, ROUTES, type RouteKey } from '../config/routes';
import { ApiProductDetailLayout, ApiProductIndexRedirect } from '../features/api-products/components';
import { ApiProductsPage } from '../features/api-products/pages/ApiProductsPage';
import { CreateApiProductPage } from '../features/api-products/pages/CreateApiProductPage';
import { ApiProductOverviewPage } from '../features/api-products/pages/detail/ApiProductOverviewPage';
import { ApiProductApisPage } from '../features/api-products/pages/detail/apis/ApiProductApisPage';
import { ApiProductConsumerDetailPage } from '../features/api-products/pages/detail/consumers/ApiProductConsumerDetailPage';
import { ApiProductConsumersPage } from '../features/api-products/pages/detail/consumers/ApiProductConsumersPage';
import { ApiProductGeneralPage } from '../features/api-products/pages/detail/general/ApiProductGeneralPage';
import { ApiProductPlanFormPage } from '../features/api-products/pages/detail/plans/ApiProductPlanFormPage';
import { ApiProductPlansPage } from '../features/api-products/pages/detail/plans/ApiProductPlansPage';
import { ApiProductUserPermissionsPage } from '../features/api-products/pages/detail/user-permissions/ApiProductUserPermissionsPage';
import { ApiDetailIndexRedirect, ApiDetailLayout } from '../features/apis/components/detail/ApiDetailLayout';
import { ApisPage } from '../features/apis/pages/ApisPage';
import { CreateApiProxyPage } from '../features/apis/pages/CreateApiProxyPage';
import { AlertFormPage } from '../features/apis/pages/detail/alerts/AlertFormPage';
import { ApiAlertsPage } from '../features/apis/pages/detail/alerts/ApiAlertsPage';
import { ApiDetailOverviewPage } from '../features/apis/pages/detail/ApiDetailOverviewPage';
import { ApiDetailPlaceholderPage } from '../features/apis/pages/detail/ApiDetailPlaceholderPage';
import { AuditLogsPage } from '../features/apis/pages/detail/audit-logs/AuditLogsPage';
import { ApiBroadcastsPage } from '../features/apis/pages/detail/broadcasts/ApiBroadcastsPage';
import { ApiConsumerDetailPage } from '../features/apis/pages/detail/consumers/ApiConsumerDetailPage';
import { ApiConsumersPage } from '../features/apis/pages/detail/consumers/ApiConsumersPage';
import { ApiCorsPage } from '../features/apis/pages/detail/cors/ApiCorsPage';
import { DeploymentConfigurationPage } from '../features/apis/pages/detail/deployment/DeploymentConfigurationPage';
import { DeploymentHistoryPage } from '../features/apis/pages/detail/deployment/DeploymentHistoryPage';
import { ApiEndpointsPage } from '../features/apis/pages/detail/endpoints/ApiEndpointsPage';
import { ApiHealthCheckDashboardPage } from '../features/apis/pages/detail/endpoints/health-check-dashboard/ApiHealthCheckDashboardPage';
import { ApiEntrypointsPage } from '../features/apis/pages/detail/entrypoints/ApiEntrypointsPage';
import { ApiFailoverPage } from '../features/apis/pages/detail/failover/ApiFailoverPage';
import { ApiGeneralPage } from '../features/apis/pages/detail/general/ApiGeneralPage';
import { ApiNotificationFormPage } from '../features/apis/pages/detail/notifications/ApiNotificationFormPage';
import { ApiNotificationsPage } from '../features/apis/pages/detail/notifications/ApiNotificationsPage';
import { ApiPlanFormPage } from '../features/apis/pages/detail/plans/ApiPlanFormPage';
import { ApiPlansPage } from '../features/apis/pages/detail/plans/ApiPlansPage';
import { ApiPropertiesPage } from '../features/apis/pages/detail/properties/ApiPropertiesPage';
import { ApiDynamicPropertiesPage } from '../features/apis/pages/detail/properties/dynamic/ApiDynamicPropertiesPage';
import { ApiReporterSettingsPage } from '../features/apis/pages/detail/reporter-settings/ApiReporterSettingsPage';
import { ApiResourcesPage } from '../features/apis/pages/detail/resources/ApiResourcesPage';
import { ApiResourceWizardPage } from '../features/apis/pages/detail/resources/ApiResourceWizardPage';
import { UserPermissionsPage } from '../features/apis/pages/detail/user-permissions/UserPermissionsPage';
import { PolicyStudioPage } from '../features/apis/pages/policy-studio/PolicyStudioPage';
import { ScratchWizardPage } from '../features/apis/pages/ScratchWizardPage';
import { TemplateWizardPage } from '../features/apis/pages/TemplateWizardPage';
import { DashboardPage } from '../features/dashboard/pages/DashboardPage';
import { SettingsPage } from '../features/settings/pages/SettingsPage';
import { createTracingApiPaginatedLoader } from '../lib/api/tracing-api-loader';
import { useEnvironmentId } from '../lib/hooks/useEnvironmentId';
import { useObservabilityBaseUrl } from '../lib/hooks/useObservabilityBaseUrl';
import { gammaConsoleHttpOptions } from '../shared/api/apimClient';

const queryClient = new QueryClient();

const PERMISSIVE_CAPABILITIES: DashboardCapabilities = {
    'observability.dashboards.read': true,
    'observability.logs.read': true,
    'observability.traces.read': true,
};

/** Host console env root (`/environments/:hrid`) — used to deep-link log rows to APIs. */
const HOST_ENV_ROOT_RE = /\/environments\/[^/]+/;

/** Host routes mount this module under `/environments/:envHrid/apim/...`. */
const HOST_ENV_PATH_RE = /\/environments\/[^/]+\//;

function buildObserveBreadcrumbItem(segment: { label: string; routeKey?: string }, modulePrefix: string, pathname: string) {
    const to = segment.routeKey ? buildModuleNavPath(modulePrefix, segment.routeKey, pathname) : undefined;
    return { label: segment.label, to };
}

function ModuleLayout() {
    const location = useLocation();
    const navigate = useNavigate();
    const activeNavKey = useMemo(() => getActiveNavKey(location.pathname), [location.pathname]);
    const { modulePrefix } = useMemo(() => resolveModulePath(location.pathname, APIM_ROUTE_CONFIG), [location.pathname]);

    const handleNavSelect = useCallback(
        (key: string) => {
            navigate(buildModuleNavPath(modulePrefix, ROUTES[key as RouteKey].path, location.pathname));
        },
        [navigate, modulePrefix, location.pathname],
    );

    const breadcrumbs = useMemo(() => {
        const observeSegments = observability.breadcrumbSegments(activeNavKey);
        if (observeSegments) {
            return buildLinearBreadcrumbs(navigate, [
                ...observeSegments.map(segment => buildObserveBreadcrumbItem(segment, modulePrefix, location.pathname)),
            ]);
        }
        return buildLinearBreadcrumbs(navigate, [{ label: ROUTES[activeNavKey].label }]);
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

function OnboardingTourHostConnected() {
    const location = useLocation();
    const navigate = useNavigate();
    const { modulePrefix } = useMemo(() => resolveModulePath(location.pathname, APIM_ROUTE_CONFIG), [location.pathname]);

    const handleNavigate = useCallback(
        (navKey: RouteKey) => {
            navigate(buildModuleNavPath(modulePrefix, ROUTES[navKey].path, location.pathname));
        },
        [navigate, modulePrefix, location.pathname],
    );

    return <OnboardingTourHost onNavigate={handleNavigate} />;
}

function ObservabilitySection() {
    const env = useEnvironment();
    const { pathname } = useLocation();
    const environmentId = useEnvironmentId();
    const { baseUrl, isLoading: baseUrlLoading, isError, error } = useObservabilityBaseUrl();
    const loadTracingApis = useMemo(() => createTracingApiPaginatedLoader(environmentId), [environmentId]);
    const consoleBasePath = useMemo(() => HOST_ENV_ROOT_RE.exec(pathname)?.[0] ?? '', [pathname]);

    const awaitingHostEnv = HOST_ENV_PATH_RE.test(pathname) && !env;
    if (isError && error) throw error;
    if (awaitingHostEnv || baseUrlLoading || !baseUrl) return null;

    return (
        <CapabilityProvider capabilities={PERMISSIVE_CAPABILITIES}>
            <observability.Routes
                baseUrl={baseUrl}
                tracesBaseUrl={baseUrl}
                http={gammaConsoleHttpOptions}
                logsDetailBasePath={consoleBasePath}
                tracesDetailBasePath={consoleBasePath}
                loadTracingApis={loadTracingApis}
            />
        </CapabilityProvider>
    );
}

/** Route tree for this module: mounted under the host router when federated, or under the local dev root for standalone. */
export function AppRoutes() {
    return (
        <QueryClientProvider client={queryClient}>
            <OnboardingProvider>
                <ApimToaster />
                <OnboardingTourHostConnected />
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
                                <Route path="properties">
                                    <Route index element={<ApiPropertiesPage />} />
                                    <Route path="dynamic" element={<ApiDynamicPropertiesPage />} />
                                </Route>
                                <Route path="resources">
                                    <Route index element={<ApiResourcesPage />} />
                                    <Route path="new" element={<ApiResourceWizardPage />} />
                                    <Route path=":resourceName/edit" element={<ApiResourceWizardPage />} />
                                </Route>
                                <Route path="notifications">
                                    <Route index element={<ApiNotificationsPage />} />
                                    <Route path="new" element={<ApiNotificationFormPage />} />
                                    <Route path=":notificationKey" element={<ApiNotificationFormPage />} />
                                </Route>
                                <Route path="entrypoints" element={<ApiEntrypointsPage />} />
                                <Route path="cors" element={<ApiCorsPage />} />
                                <Route path="endpoints">
                                    <Route index element={<Navigate to="list" replace />} />
                                    <Route path="list" element={<ApiEndpointsPage />} />
                                    <Route path="failover" element={<ApiFailoverPage />} />
                                    <Route path="health-check-dashboard" element={<ApiHealthCheckDashboardPage />} />
                                </Route>
                                <Route path="reporter-settings" element={<ApiReporterSettingsPage />} />
                                <Route path="policy-studio" element={<PolicyStudioPage />} />
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
                        {/* Analytics is out of scope for now; restore this route (and the dashboard tile) when the feature is ready. */}
                        <Route path="settings" element={<SettingsPage />} />
                        <Route path="observe/*" element={<ObservabilitySection />} />
                    </Route>
                </Routes>
            </OnboardingProvider>
        </QueryClientProvider>
    );
}
