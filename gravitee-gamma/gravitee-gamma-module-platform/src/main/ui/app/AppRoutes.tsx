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
import { type ReactElement, useMemo } from 'react';
import { Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom';

import { PlatformToaster } from './PlatformToaster';
import { APPLICATION_NAV_GROUPS, flattenApplicationDetailNavItems } from '../config/applicationDetailNavigation';
import { applicationDetailTabElement } from '../config/applicationDetailPages';
import { NAV_GROUPS } from '../config/navigation';
import { PLATFORM_ROUTE_CONFIG } from '../config/routes';
import { ApplicationDetailIndexRedirect, ApplicationDetailLayout } from '../features/applications/components/detail';
import { useEnvironmentDictionaries } from '../features/dictionaries/hooks/useEnvironmentDictionaries';
import { useEnvironmentMetadata } from '../features/metadata/hooks/useEnvironmentMetadata';
import { SecurityPlanTypesPage } from '../features/security-plan-types/SecurityPlanTypesPage';
import { ORGANIZATION_USER_ACCESS_PERMISSIONS } from '../features/users/utils/userPermissions';
import { AccessManagementPage } from '../pages/AccessManagementPage';
import { ApplicationDetailSubscriptionPage } from '../pages/ApplicationDetailSubscriptionPage';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { DictionariesPage } from '../pages/DictionariesPage';
import { DictionaryDetailPage } from '../pages/DictionaryDetailPage';
import { EntrypointsAndShardingTagsPage } from '../pages/EntrypointsAndShardingTagsPage';
import { MetadataPage } from '../pages/MetadataPage';
import { RegisterApplicationPage } from '../pages/RegisterApplicationPage';
import { UsersPage } from '../pages/UsersPage';
import { retryTransientRequest } from '../shared/api/queryRetry';
import { ConsoleSettingsProvider } from '../shared/console-settings';
import { useHasPermission } from '../shared/gamma-modules-sdk';
import { useEnvironmentPermissions, useEnvironmentPermissionsReady } from '../shared/hooks/useEnvironmentPermissions';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

const queryClient = new QueryClient({
    defaultOptions: {
        queries: { retry: retryTransientRequest },
        mutations: { retry: retryTransientRequest },
    },
});

const APPLICATION_DETAIL_TABS = flattenApplicationDetailNavItems(APPLICATION_NAV_GROUPS);

function resolveRequiredPermissions(permission?: string, anyOf?: readonly string[]): readonly string[] {
    if (anyOf) {
        return anyOf;
    }
    if (permission) {
        return [permission];
    }
    return [];
}

function PermissionPageGuard({
    permission,
    anyOf,
    unauthorizedTo = 'applications',
    children,
}: Readonly<{ permission?: string; anyOf?: readonly string[]; unauthorizedTo?: string; children: ReactElement }>) {
    const required = resolveRequiredPermissions(permission, anyOf);
    const permissionsReady = useEnvironmentPermissionsReady();
    const canAccess = useHasPermission({ anyOf: [...required] });
    if (!permissionsReady) return null;
    if (!canAccess) return <Navigate to={unauthorizedTo} replace />;
    return children;
}

function isNavItemVisible(
    itemKey: string,
    permissionsReady: boolean,
    canReadMetadata: boolean,
    canReadDictionaries: boolean,
    canAccessUsers: boolean,
    canReadEntrypoints: boolean,
): boolean {
    if (itemKey === 'users') {
        return !permissionsReady || canAccessUsers;
    }
    if (itemKey === 'metadata') {
        return !permissionsReady || canReadMetadata;
    }
    if (itemKey === 'dictionaries') {
        return !permissionsReady || canReadDictionaries;
    }
    if (itemKey === 'entrypoints-and-sharding-tags') {
        return !permissionsReady || canReadEntrypoints;
    }
    return true;
}

function EntrypointsGuard() {
    const permissionsReady = useEnvironmentPermissionsReady();
    const canRead = useHasPermission({ anyOf: ['environment-entrypoint-r', 'organization-entrypoint-r'] });
    if (!permissionsReady) return null;
    if (!canRead) return <Navigate to="applications" replace />;
    return <EntrypointsAndShardingTagsPage />;
}

function ModuleLayout() {
    useEnvironmentPermissions();

    const permissionsReady = useEnvironmentPermissionsReady();

    // Only probe the resource once the permission map already says "yes" — that's the only case where a
    // 403 here would disagree with it. Probing unconditionally would hit both list APIs on every platform
    // page for every user, including those who already correctly lack access.
    const canReadMetadataPermission = useHasPermission({ anyOf: ['environment-metadata-r'] });
    const metadataQuery = useEnvironmentMetadata({ enabled: canReadMetadataPermission });
    const canReadMetadata = canReadMetadataPermission && !isForbiddenApiError(metadataQuery.isError, metadataQuery.error);

    const canReadDictionariesPermission = useHasPermission({ anyOf: ['environment-dictionary-r'] });
    const dictionariesQuery = useEnvironmentDictionaries({ enabled: canReadDictionariesPermission });
    const canReadDictionaries = canReadDictionariesPermission && !isForbiddenApiError(dictionariesQuery.isError, dictionariesQuery.error);

    const canAccessUsers = useHasPermission({ anyOf: [...ORGANIZATION_USER_ACCESS_PERMISSIONS] });
    const canReadEntrypoints = useHasPermission({ anyOf: ['environment-entrypoint-r', 'organization-entrypoint-r'] });

    const navigate = useNavigate();
    const { activeNavKey, navigateToKey } = useModuleRouting(PLATFORM_ROUTE_CONFIG);

    const visibleNavGroups = useMemo(
        () =>
            NAV_GROUPS.map(group => ({
                ...group,
                items: group.items.filter(item =>
                    isNavItemVisible(item.key, permissionsReady, canReadMetadata, canReadDictionaries, canAccessUsers, canReadEntrypoints),
                ),
            })).filter(group => group.items.length > 0),
        [permissionsReady, canReadMetadata, canReadDictionaries, canAccessUsers, canReadEntrypoints],
    );

    const breadcrumbs = useMemo(
        () => buildLinearBreadcrumbs(navigate, [{ label: PLATFORM_ROUTE_CONFIG.routes[activeNavKey].label }]),
        [activeNavKey, navigate],
    );

    useLayoutConfig(
        {
            navigation: <SidebarNavigation groups={visibleNavGroups} activeItemKey={activeNavKey} onItemSelect={navigateToKey} />,
            breadcrumbs,
        },
        [activeNavKey, breadcrumbs, navigateToKey, visibleNavGroups],
    );

    return <Outlet />;
}

/** Route tree for this module: mounted under the host router when federated, or under the local dev root for standalone. */
export function AppRoutes() {
    return (
        <QueryClientProvider client={queryClient}>
            <ConsoleSettingsProvider>
                <PlatformToaster />
                <Routes>
                    <Route element={<ModuleLayout />}>
                        <Route index element={<Navigate to="applications" replace />} />
                        <Route path="applications">
                            <Route index element={<ApplicationsPage />} />
                            <Route path="new" element={<RegisterApplicationPage />} />
                            <Route path=":applicationId" element={<ApplicationDetailLayout />}>
                                <Route index element={<ApplicationDetailIndexRedirect />} />
                                {APPLICATION_DETAIL_TABS.map(tab => (
                                    <Route key={tab.path} path={tab.path} element={applicationDetailTabElement(tab.path, tab.label)} />
                                ))}
                                <Route path="subscriptions/:subscriptionId" element={<ApplicationDetailSubscriptionPage />} />
                                <Route path="*" element={<ApplicationDetailIndexRedirect />} />
                            </Route>
                        </Route>
                        <Route path="access-management" element={<AccessManagementPage />} />
                        <Route
                            path="users"
                            element={
                                <PermissionPageGuard anyOf={ORGANIZATION_USER_ACCESS_PERMISSIONS}>
                                    <UsersPage />
                                </PermissionPageGuard>
                            }
                        />
                        <Route
                            path="metadata"
                            element={
                                <PermissionPageGuard permission="environment-metadata-r">
                                    <MetadataPage />
                                </PermissionPageGuard>
                            }
                        />
                        <Route path="dictionaries">
                            <Route
                                index
                                element={
                                    <PermissionPageGuard permission="environment-dictionary-r" unauthorizedTo="../applications">
                                        <DictionariesPage />
                                    </PermissionPageGuard>
                                }
                            />
                            <Route
                                path=":dictionaryId"
                                element={
                                    <PermissionPageGuard
                                        anyOf={[
                                            'environment-dictionary-c',
                                            'environment-dictionary-r',
                                            'environment-dictionary-u',
                                            'environment-dictionary-d',
                                        ]}
                                        unauthorizedTo="../../applications"
                                    >
                                        <DictionaryDetailPage />
                                    </PermissionPageGuard>
                                }
                            />
                        </Route>
                        <Route path="entrypoints-and-sharding-tags" element={<EntrypointsGuard />} />
                        <Route path="security-plan-types" element={<SecurityPlanTypesPage />} />
                    </Route>
                </Routes>
            </ConsoleSettingsProvider>
        </QueryClientProvider>
    );
}
