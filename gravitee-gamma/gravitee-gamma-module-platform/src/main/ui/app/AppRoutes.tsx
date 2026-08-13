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
import {
    buildLinearBreadcrumbs,
    ContextSidebar,
    ContextToggleButton,
    type NavItem,
    SidebarGroup,
    SidebarGroupContent,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    useLayoutConfig,
} from '@gravitee/graphene-core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createContext, type ReactElement, useCallback, useContext, useMemo, useState } from 'react';
import { Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom';

import { PlatformToaster } from './PlatformToaster';
import { APPLICATION_NAV_GROUPS, flattenApplicationDetailNavItems } from '../config/applicationDetailNavigation';
import { applicationDetailTabElement } from '../config/applicationDetailPages';
import {
    filterNavSections,
    findNavSectionKey,
    firstNavItemKey,
    NAV_SECTIONS,
    platformPrimaryNavItems,
    type PlatformNavSection,
} from '../config/navigation';
import { PLATFORM_ROUTE_CONFIG } from '../config/routes';
import { ENVIRONMENT_ALERT_READ_PERMISSION } from '../features/alerts/utils/alertPermissions';
import { ApplicationDetailIndexRedirect, ApplicationDetailLayout } from '../features/applications/components/detail';
import { useEnvironmentDictionaries } from '../features/dictionaries/hooks/useEnvironmentDictionaries';
import { GatewayInstanceDetailLayout } from '../features/gateway-instances/components/GatewayInstanceDetailLayout';
import { ENVIRONMENT_GROUP_READ_PERMISSION } from '../features/groups/utils/groupPermissions';
import { useEnvironmentMetadata } from '../features/metadata/hooks/useEnvironmentMetadata';
import { SecurityPlanTypesPage } from '../features/security-plan-types/SecurityPlanTypesPage';
import { ORGANIZATION_USER_ACCESS_PERMISSIONS } from '../features/users/utils/userPermissions';
import { AccessManagementPage } from '../pages/AccessManagementPage';
import { AlertsPage } from '../pages/AlertsPage';
import { ApplicationDetailSubscriptionPage } from '../pages/ApplicationDetailSubscriptionPage';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { DictionariesPage } from '../pages/DictionariesPage';
import { DictionaryDetailPage } from '../pages/DictionaryDetailPage';
import { EntrypointsAndShardingTagsPage } from '../pages/EntrypointsAndShardingTagsPage';
import { GatewayInstanceEnvironmentPage } from '../pages/GatewayInstanceEnvironmentPage';
import { GatewayInstanceMonitoringPage } from '../pages/GatewayInstanceMonitoringPage';
import { GatewayInstancesPage } from '../pages/GatewayInstancesPage';
import { GroupDetailPage } from '../pages/GroupDetailPage';
import { GroupsPage } from '../pages/GroupsPage';
import { MetadataPage } from '../pages/MetadataPage';
import { RegisterApplicationPage } from '../pages/RegisterApplicationPage';
import { UserDetailPage } from '../pages/UserDetailPage';
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
    canReadGateways: boolean,
    canReadEntrypoints: boolean,
    canReadGroups: boolean,
    canReadAlerts: boolean,
): boolean {
    if (itemKey === 'users') {
        return !permissionsReady || canAccessUsers;
    }
    if (itemKey === 'user-groups') {
        return !permissionsReady || canReadGroups;
    }
    if (itemKey === 'metadata') {
        return !permissionsReady || canReadMetadata;
    }
    if (itemKey === 'dictionaries') {
        return !permissionsReady || canReadDictionaries;
    }
    if (itemKey === 'gateways') {
        return !permissionsReady || canReadGateways;
    }
    if (itemKey === 'entrypoints-and-sharding-tags') {
        return !permissionsReady || canReadEntrypoints;
    }
    if (itemKey === 'alerts') {
        return !permissionsReady || canReadAlerts;
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

function PlatformPrimaryNavigation({
    items,
    activeItemKey,
    onItemSelect,
}: Readonly<{ items: NavItem[]; activeItemKey?: string; onItemSelect: (key: string) => void }>) {
    return (
        <SidebarGroup>
            <SidebarGroupContent>
                <SidebarMenu>
                    {items.map(item => {
                        const Icon = item.icon;
                        return (
                            <SidebarMenuItem key={item.key}>
                                <SidebarMenuButton
                                    isActive={item.key === activeItemKey}
                                    tooltip={item.title}
                                    onClick={() => onItemSelect(item.key)}
                                >
                                    {Icon ? <Icon /> : null}
                                    <span>{item.title}</span>
                                </SidebarMenuButton>
                            </SidebarMenuItem>
                        );
                    })}
                </SidebarMenu>
            </SidebarGroupContent>
        </SidebarGroup>
    );
}

interface PlatformNavContextValue {
    readonly activeNavKey: string;
    readonly activeSection: PlatformNavSection | undefined;
    readonly navigateToKey: (key: string) => void;
    readonly contextExpanded: boolean;
    readonly toggleContext: () => void;
}

const PlatformNavContext = createContext<PlatformNavContextValue | null>(null);

function usePlatformNavContext(): PlatformNavContextValue {
    const value = useContext(PlatformNavContext);
    if (!value) {
        throw new Error('PlatformSectionLayout must render inside ModuleLayout');
    }
    return value;
}

function ModuleLayout() {
    useEnvironmentPermissions();

    const permissionsReady = useEnvironmentPermissionsReady();
    const [contextExpanded, setContextExpanded] = useState(true);
    const toggleContext = useCallback(() => setContextExpanded(expanded => !expanded), []);

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
    const canReadGateways = useHasPermission({ anyOf: ['environment-instance-r'] });
    const canReadEntrypoints = useHasPermission({ anyOf: ['environment-entrypoint-r', 'organization-entrypoint-r'] });
    const canReadGroups = useHasPermission({ anyOf: [ENVIRONMENT_GROUP_READ_PERMISSION] });
    const canReadAlerts = useHasPermission({ anyOf: [ENVIRONMENT_ALERT_READ_PERMISSION] });

    const { activeNavKey, navigateToKey } = useModuleRouting(PLATFORM_ROUTE_CONFIG);

    const visibleNavSections = useMemo(
        () =>
            filterNavSections(NAV_SECTIONS, itemKey =>
                isNavItemVisible(
                    itemKey,
                    permissionsReady,
                    canReadMetadata,
                    canReadDictionaries,
                    canAccessUsers,
                    canReadGateways,
                    canReadEntrypoints,
                    canReadGroups,
                    canReadAlerts,
                ),
            ),
        [
            permissionsReady,
            canReadMetadata,
            canReadDictionaries,
            canAccessUsers,
            canReadGateways,
            canReadEntrypoints,
            canReadGroups,
            canReadAlerts,
        ],
    );

    const activeSectionKey = findNavSectionKey(visibleNavSections, activeNavKey) ?? visibleNavSections[0]?.key;
    const activeSection = visibleNavSections.find(section => section.key === activeSectionKey);

    const handleSectionSelect = useCallback(
        (key: string) => {
            const section = visibleNavSections.find(candidate => candidate.key === key);
            if (!section) {
                return;
            }
            // Compare against the section that owns the current page, not activeSectionKey.
            // activeSectionKey falls back to the first visible section, which would no-op a click
            // onto that section when the current page is not in any visible section.
            if (findNavSectionKey(visibleNavSections, activeNavKey) === section.key) {
                return;
            }
            const firstKey = firstNavItemKey(section);
            if (firstKey) {
                navigateToKey(firstKey);
            }
        },
        [activeNavKey, navigateToKey, visibleNavSections],
    );

    useLayoutConfig(
        {
            navigation: (
                <PlatformPrimaryNavigation
                    items={platformPrimaryNavItems(visibleNavSections)}
                    activeItemKey={activeSectionKey}
                    onItemSelect={handleSectionSelect}
                />
            ),
        },
        [activeSectionKey, handleSectionSelect, visibleNavSections],
    );

    const navContext = useMemo(
        () => ({ activeNavKey, activeSection, navigateToKey, contextExpanded, toggleContext }),
        [activeNavKey, activeSection, navigateToKey, contextExpanded, toggleContext],
    );

    return (
        <PlatformNavContext.Provider value={navContext}>
            <Outlet />
        </PlatformNavContext.Provider>
    );
}

function PlatformSectionLayout() {
    const { activeNavKey, activeSection, navigateToKey, contextExpanded, toggleContext } = usePlatformNavContext();
    const navigate = useNavigate();
    const breadcrumbs = useMemo(
        () => buildLinearBreadcrumbs(navigate, [{ label: PLATFORM_ROUTE_CONFIG.routes[activeNavKey].label }]),
        [activeNavKey, navigate],
    );

    useLayoutConfig(
        {
            viewMode: 'context',
            contextExpanded,
            contextSidebar: (
                <ContextSidebar groups={activeSection?.groups ?? []} activeItemKey={activeNavKey} onItemSelect={navigateToKey} />
            ),
            leading: <ContextToggleButton expanded={contextExpanded} onToggle={toggleContext} />,
            breadcrumbs,
        },
        [activeNavKey, activeSection, breadcrumbs, contextExpanded, navigateToKey, toggleContext],
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
                        <Route element={<PlatformSectionLayout />}>
                            <Route index element={<Navigate to="applications" replace />} />
                            <Route path="applications" element={<ApplicationsPage />} />
                            <Route path="applications/new" element={<RegisterApplicationPage />} />
                            <Route path="access-management" element={<AccessManagementPage />} />
                            <Route path="users">
                                <Route
                                    index
                                    element={
                                        <PermissionPageGuard anyOf={ORGANIZATION_USER_ACCESS_PERMISSIONS}>
                                            <UsersPage />
                                        </PermissionPageGuard>
                                    }
                                />
                                <Route
                                    path=":userId"
                                    element={
                                        <PermissionPageGuard anyOf={ORGANIZATION_USER_ACCESS_PERMISSIONS}>
                                            <UserDetailPage />
                                        </PermissionPageGuard>
                                    }
                                />
                            </Route>
                            <Route path="user-groups">
                                <Route
                                    index
                                    element={
                                        <PermissionPageGuard
                                            permission={ENVIRONMENT_GROUP_READ_PERMISSION}
                                            unauthorizedTo="../applications"
                                        >
                                            <GroupsPage />
                                        </PermissionPageGuard>
                                    }
                                />
                                <Route
                                    path=":groupId"
                                    element={
                                        <PermissionPageGuard
                                            permission={ENVIRONMENT_GROUP_READ_PERMISSION}
                                            unauthorizedTo="../../applications"
                                        >
                                            <GroupDetailPage />
                                        </PermissionPageGuard>
                                    }
                                />
                            </Route>
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
                            <Route path="gateways">
                                <Route
                                    index
                                    element={
                                        <PermissionPageGuard permission="environment-instance-r" unauthorizedTo="../applications">
                                            <GatewayInstancesPage />
                                        </PermissionPageGuard>
                                    }
                                />
                                <Route
                                    path=":instanceId"
                                    element={
                                        <PermissionPageGuard permission="environment-instance-r" unauthorizedTo="../../applications">
                                            <GatewayInstanceDetailLayout />
                                        </PermissionPageGuard>
                                    }
                                >
                                    <Route index element={<Navigate to="environment" replace />} />
                                    <Route path="environment" element={<GatewayInstanceEnvironmentPage />} />
                                    <Route path="monitoring" element={<GatewayInstanceMonitoringPage />} />
                                </Route>
                            </Route>
                            <Route path="entrypoints-and-sharding-tags" element={<EntrypointsGuard />} />
                            <Route path="security-plan-types" element={<SecurityPlanTypesPage />} />
                            <Route
                                path="alerts"
                                element={
                                    <PermissionPageGuard permission={ENVIRONMENT_ALERT_READ_PERMISSION} unauthorizedTo="../applications">
                                        <AlertsPage />
                                    </PermissionPageGuard>
                                }
                            />
                        </Route>
                        <Route path="applications/:applicationId" element={<ApplicationDetailLayout />}>
                            <Route index element={<ApplicationDetailIndexRedirect />} />
                            {APPLICATION_DETAIL_TABS.map(tab => (
                                <Route key={tab.path} path={tab.path} element={applicationDetailTabElement(tab.path, tab.label)} />
                            ))}
                            <Route path="subscriptions/:subscriptionId" element={<ApplicationDetailSubscriptionPage />} />
                            <Route path="*" element={<ApplicationDetailIndexRedirect />} />
                        </Route>
                    </Route>
                </Routes>
            </ConsoleSettingsProvider>
        </QueryClientProvider>
    );
}
