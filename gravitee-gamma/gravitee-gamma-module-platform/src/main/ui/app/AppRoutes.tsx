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
import { permissionService, useEnvironment, useHasFeature, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import {
    buildLinearBreadcrumbs,
    ContextSidebar,
    ContextToggleButton,
    type NavGroup,
    type NavItem,
    SidebarGroup,
    SidebarGroupContent,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarNavigation,
    useLayoutConfig,
} from '@gravitee/graphene-core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createContext, type ReactElement, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { Navigate, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom';

import { PlatformToaster } from './PlatformToaster';
import { APPLICATION_NAV_GROUPS, flattenApplicationDetailNavItems } from '../config/applicationDetailNavigation';
import { applicationDetailTabElement } from '../config/applicationDetailPages';
import {
    filterNavSections,
    findNavSectionKey,
    firstNavItemKey,
    lockNavItem,
    NAV_SECTIONS,
    platformPrimaryNavItems,
    type PlatformNavSection,
} from '../config/navigation';
import { isNavItemVisible, landingNavItemKey, modulePathFor, NO_ACCESS_ROUTE_KEY, pageGuardForNavItem } from '../config/navVisibility';
import { PLATFORM_ROUTE_CONFIG } from '../config/routes';
import { AlertsLayout } from '../features/alerts/components/AlertsLayout';
import { AlertFormPage } from '../features/alerts/pages/AlertFormPage';
import { AlertsActivityPage } from '../features/alerts/pages/AlertsActivityPage';
import { ALERT_ENGINE_FEATURE } from '../features/alerts/utils/alertPermissions';
import { ApplicationDetailIndexRedirect, ApplicationDetailLayout } from '../features/applications/components/detail';
import { APIM_AUDIT_TRAIL_FEATURE } from '../features/audit-logs/license/auditTrailLicense';
import { useEnvironmentDictionaries } from '../features/dictionaries/hooks/useEnvironmentDictionaries';
import { GatewayInstanceDetailLayout } from '../features/gateway-instances/components/GatewayInstanceDetailLayout';
import { useEnvironmentMetadata } from '../features/metadata/hooks/useEnvironmentMetadata';
import { ORGANIZATION_ROLE_UPDATE_PERMISSION } from '../features/roles/utils/rolePermissionConstants';
import { SecurityPlanTypesPage } from '../features/security-plan-types/SecurityPlanTypesPage';
import { usePermissionServiceSnapshot } from '../features/shared/hooks/usePermissionServiceSnapshot';
import { SharedPolicyGroupDetailLayout } from '../features/shared-policy-groups/components/SharedPolicyGroupDetailLayout';
import { ENVIRONMENT_SHARED_POLICY_GROUP_READ_PERMISSION } from '../features/shared-policy-groups/utils/sharedPolicyGroupPermissions';
import { AccessManagementPage } from '../pages/AccessManagementPage';
import { AlertsPage } from '../pages/AlertsPage';
import { ApplicationDetailSubscriptionPage } from '../pages/ApplicationDetailSubscriptionPage';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { AuthenticationPage } from '../pages/AuthenticationPage';
import { CorsSettingsPage } from '../pages/CorsSettingsPage';
import { CreateIdentityProviderPage } from '../pages/CreateIdentityProviderPage';
import { DictionariesPage } from '../pages/DictionariesPage';
import { DictionaryDetailPage } from '../pages/DictionaryDetailPage';
import { EditIdentityProviderPage } from '../pages/EditIdentityProviderPage';
import { EntrypointsAndShardingTagsPage } from '../pages/EntrypointsAndShardingTagsPage';
import { EnvAuditLogsPage } from '../pages/EnvAuditLogsPage';
import { EnvironmentNotificationSettingsPage } from '../pages/EnvironmentNotificationSettingsPage';
import { GatewayInstanceEnvironmentPage } from '../pages/GatewayInstanceEnvironmentPage';
import { GatewayInstanceMonitoringPage } from '../pages/GatewayInstanceMonitoringPage';
import { GatewayInstancesPage } from '../pages/GatewayInstancesPage';
import { GroupDetailPage } from '../pages/GroupDetailPage';
import { GroupsPage } from '../pages/GroupsPage';
import { ManagementAndSchedulersPage } from '../pages/ManagementAndSchedulersPage';
import { MetadataPage } from '../pages/MetadataPage';
import { NotificationTemplateDetailPage } from '../pages/NotificationTemplateDetailPage';
import { NotificationTemplatesPage } from '../pages/NotificationTemplatesPage';
import { OrganizationPolicyStudioPage } from '../pages/OrganizationPolicyStudioPage';
import { OrgAuditLogsPage } from '../pages/OrgAuditLogsPage';
import { PlatformNoAccessPage } from '../pages/PlatformNoAccessPage';
import { RegisterApplicationPage } from '../pages/RegisterApplicationPage';
import { RoleFormPage } from '../pages/RoleFormPage';
import { RoleMembersPage } from '../pages/RoleMembersPage';
import { RolesPage } from '../pages/RolesPage';
import { SharedPolicyGroupHistoryPage } from '../pages/SharedPolicyGroupHistoryPage';
import { SharedPolicyGroupsPage } from '../pages/SharedPolicyGroupsPage';
import { SharedPolicyGroupStudioPage } from '../pages/SharedPolicyGroupStudioPage';
import { SmtpSettingsPage } from '../pages/SmtpSettingsPage';
import { TenantsPage } from '../pages/TenantsPage';
import { UserDetailPage } from '../pages/UserDetailPage';
import { UsersPage } from '../pages/UsersPage';
import { retryTransientRequest } from '../shared/api/queryRetry';
import { ConsoleSettingsProvider } from '../shared/console-settings';
import {
    useEnvironmentPermissionGrant,
    useEnvironmentPermissions,
    useEnvironmentPermissionsReady,
} from '../shared/hooks/useEnvironmentPermissions';
import { resetDeniedNavItemsForEnvironment, useDeniedNavItemKeys } from '../shared/nav/deniedNavItems';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

const queryClient = new QueryClient({
    defaultOptions: {
        queries: { retry: retryTransientRequest },
        mutations: { retry: retryTransientRequest },
    },
});

const APPLICATION_DETAIL_TABS = flattenApplicationDetailNavItems(APPLICATION_NAV_GROUPS);
const EMPTY_NAV_GROUPS: NavGroup[] = [];
const ALERT_ENGINE_NAV_ITEMS: readonly string[] = ['alerts'];
const AUDIT_TRAIL_NAV_ITEMS: readonly string[] = ['organization-audit', 'environment-audit'];

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
    alsoAnyOf,
    unauthorizedTo,
    children,
}: Readonly<{
    permission?: string;
    anyOf?: readonly string[];
    alsoAnyOf?: readonly string[];
    unauthorizedTo?: string;
    children: ReactElement;
}>) {
    const required = resolveRequiredPermissions(permission, anyOf);
    const extraAnyOf = alsoAnyOf ?? [];
    const permissionsReady = useEnvironmentPermissionsReady();
    const canAccessPrimary = useHasPermission({ anyOf: [...required] });
    const canAccessExtra = useHasPermission({ anyOf: [...extraAnyOf] });
    if (!permissionsReady) return null;
    if (required.length === 0 && extraAnyOf.length === 0) {
        if (unauthorizedTo) return <Navigate to={unauthorizedTo} replace />;
        return <UnauthorizedRedirect />;
    }
    if (!canAccessPrimary || (extraAnyOf.length > 0 && !canAccessExtra)) {
        if (unauthorizedTo) return <Navigate to={unauthorizedTo} replace />;
        return <UnauthorizedRedirect />;
    }
    return children;
}

// Redirects out of a locked page go through UnauthorizedRedirect rather than a hardcoded
// "../applications": applications is itself guarded now, so a fixed target can bounce the user
// straight back here. UnauthorizedRedirect resolves the landing key, which excludes locked items.
function RequireAlertEngineLicense({ children }: { readonly children: ReactElement }) {
    const hasFeature = useHasFeature(ALERT_ENGINE_FEATURE);
    if (!hasFeature) {
        return <UnauthorizedRedirect />;
    }
    return children;
}

// Shared Policy Groups narrows on this module's own permission cache, so a permission revoked
// mid-session closes the page the same way it already disappears from the nav link. It only narrows
// when the cache actually holds an answer; "no answer" defers to the store-backed NavPermissionGuard
// wrapping this route, which is the same source the sidebar and landing key read.
function SharedPolicyGroupPageGuard({ children }: Readonly<{ children: ReactElement }>) {
    const permissionsReady = useEnvironmentPermissionsReady();
    const grant = useEnvironmentPermissionGrant([ENVIRONMENT_SHARED_POLICY_GROUP_READ_PERMISSION]);
    if (!permissionsReady) return null;
    if (grant === false) return <UnauthorizedRedirect />;
    return children;
}

function NavPermissionGuard({
    itemKey,
    unauthorizedTo,
    children,
}: Readonly<{ itemKey: string; unauthorizedTo?: string; children: ReactElement }>) {
    const { anyOf, alsoAnyOf } = pageGuardForNavItem(itemKey);
    const deniedNavItemKeys = useDeniedNavItemKeys();
    // A live 403 outranks the permission map, which can still grant the item from a scope this module
    // does not own. Without this the page renders, 403s, and redirects on every visit.
    if (deniedNavItemKeys.has(itemKey)) {
        return <UnauthorizedRedirect />;
    }
    return (
        <PermissionPageGuard anyOf={anyOf} alsoAnyOf={alsoAnyOf} unauthorizedTo={unauthorizedTo}>
            {children}
        </PermissionPageGuard>
    );
}

function UnauthorizedRedirect() {
    const location = useLocation();
    const { landingNavKey } = usePlatformNavContext();
    return <Navigate to={modulePathFor(location.pathname, landingNavKey ?? NO_ACCESS_ROUTE_KEY)} replace />;
}

function PlatformLandingOrNoAccess() {
    const location = useLocation();
    const { landingNavKey, permissionsReady } = usePlatformNavContext();
    if (!permissionsReady) return null;
    if (!landingNavKey) return <PlatformNoAccessPage />;
    return <Navigate to={modulePathFor(location.pathname, landingNavKey)} replace />;
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
    readonly permissionsReady: boolean;
    readonly landingNavKey: string | undefined;
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
    const permissionVersion = usePermissionServiceSnapshot();
    const env = useEnvironment();
    const deniedNavItemKeys = useDeniedNavItemKeys();

    useEffect(() => {
        resetDeniedNavItemsForEnvironment(env?.id);
    }, [env?.id]);

    const permissionsReady = useEnvironmentPermissionsReady();
    const [contextExpanded, setContextExpanded] = useState(true);
    const toggleContext = useCallback(() => setContextExpanded(expanded => !expanded), []);

    // Only probe the resource once the permission map already says "yes" — that's the only case where a
    // 403 here would disagree with it. Probing unconditionally would hit both list APIs on every platform
    // page for every user, including those who already correctly lack access.
    const canReadMetadataPermission = useHasPermission({ anyOf: ['environment-metadata-r'] });
    const metadataQuery = useEnvironmentMetadata({ enabled: canReadMetadataPermission });
    const metadataForbidden = isForbiddenApiError(metadataQuery.isError, metadataQuery.error);

    const canReadDictionariesPermission = useHasPermission({ anyOf: ['environment-dictionary-r'] });
    const dictionariesQuery = useEnvironmentDictionaries({ enabled: canReadDictionariesPermission });
    const dictionariesForbidden = isForbiddenApiError(dictionariesQuery.isError, dictionariesQuery.error);

    const { activeNavKey, navigateToKey } = useModuleRouting(PLATFORM_ROUTE_CONFIG);
    const hasAlertEngine = useHasFeature(ALERT_ENGINE_FEATURE);
    const hasAuditTrail = useHasFeature(APIM_AUDIT_TRAIL_FEATURE);

    // Unlicensed pages redirect away or open an upsell dialog, so landing on one bounces the user
    // straight back out. Alerts redirects; the audit pages show the dialog and cannot be dismissed.
    const lockedItemKeys = useMemo(
        () => [...(hasAlertEngine ? [] : ALERT_ENGINE_NAV_ITEMS), ...(hasAuditTrail ? [] : AUDIT_TRAIL_NAV_ITEMS)],
        [hasAlertEngine, hasAuditTrail],
    );

    // permissionService is an external store, so re-reading it has to be keyed on its version:
    // a 403 patch or a host reload changes the answers without changing any React state below.
    const hasPermission = useCallback((permission: string): boolean => permissionService.hasAnyOf([permission]), [permissionVersion]);

    const navVisibility = useMemo(
        () => ({
            permissionsReady,
            has: hasPermission,
            metadataForbidden,
            dictionariesForbidden,
            lockedItemKeys,
            deniedItemKeys: deniedNavItemKeys,
        }),
        [deniedNavItemKeys, dictionariesForbidden, hasPermission, lockedItemKeys, metadataForbidden, permissionsReady],
    );

    const visibleNavSections = useMemo(
        () =>
            lockNavItem(
                filterNavSections(NAV_SECTIONS, itemKey => isNavItemVisible(itemKey, navVisibility)),
                'alerts',
                !hasAlertEngine,
            ),
        [hasAlertEngine, navVisibility],
    );
    const landingNavKey = landingNavItemKey(navVisibility);

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
        () => ({
            activeNavKey,
            activeSection,
            navigateToKey,
            contextExpanded,
            toggleContext,
            permissionsReady,
            landingNavKey,
        }),
        [activeNavKey, activeSection, contextExpanded, landingNavKey, navigateToKey, permissionsReady, toggleContext],
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
    const groups = activeSection?.groups ?? EMPTY_NAV_GROUPS;
    const breadcrumbs = useMemo(
        () => buildLinearBreadcrumbs(navigate, [{ label: PLATFORM_ROUTE_CONFIG.routes[activeNavKey]?.label ?? activeNavKey }]),
        [activeNavKey, navigate],
    );

    const hasContextNav = Boolean(activeSection?.groups.some(group => group.items.length > 0));

    useLayoutConfig(
        {
            breadcrumbs,
            ...(hasContextNav && activeSection
                ? {
                      viewMode: 'context' as const,
                      contextExpanded,
                      contextSidebar: (
                          <ContextSidebar>
                              <SidebarNavigation groups={groups} activeItemKey={activeNavKey} onItemSelect={navigateToKey} />
                          </ContextSidebar>
                      ),
                      leading: <ContextToggleButton expanded={contextExpanded} onToggle={toggleContext} />,
                  }
                : {}),
        },
        [activeNavKey, breadcrumbs, contextExpanded, groups, hasContextNav, navigateToKey, toggleContext],
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
                            <Route index element={<PlatformLandingOrNoAccess />} />
                            <Route
                                path="applications"
                                element={
                                    <NavPermissionGuard itemKey="applications">
                                        <ApplicationsPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="applications/new"
                                element={
                                    <PermissionPageGuard permission="environment-application-c">
                                        <RegisterApplicationPage />
                                    </PermissionPageGuard>
                                }
                            />
                            <Route
                                path="access-management"
                                element={
                                    <NavPermissionGuard itemKey="access-management">
                                        <AccessManagementPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="authentication"
                                element={
                                    <NavPermissionGuard itemKey="authentication">
                                        <Outlet />
                                    </NavPermissionGuard>
                                }
                            >
                                <Route index element={<AuthenticationPage />} />
                                <Route
                                    path="new"
                                    element={
                                        <PermissionPageGuard permission="organization-identity_provider-c" unauthorizedTo="..">
                                            <CreateIdentityProviderPage />
                                        </PermissionPageGuard>
                                    }
                                />
                                <Route path=":identityProviderId" element={<EditIdentityProviderPage />} />
                            </Route>
                            <Route
                                path="management-and-schedulers"
                                element={
                                    <NavPermissionGuard itemKey="management-and-schedulers">
                                        <ManagementAndSchedulersPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="cors"
                                element={
                                    <NavPermissionGuard itemKey="cors">
                                        <CorsSettingsPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="smtp"
                                element={
                                    <NavPermissionGuard itemKey="smtp">
                                        <SmtpSettingsPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="templates"
                                element={
                                    <NavPermissionGuard itemKey="templates">
                                        <Outlet />
                                    </NavPermissionGuard>
                                }
                            >
                                <Route index element={<NotificationTemplatesPage />} />
                                <Route path=":scope/:hook" element={<NotificationTemplateDetailPage />} />
                            </Route>
                            <Route path="users">
                                <Route
                                    index
                                    element={
                                        <NavPermissionGuard itemKey="users">
                                            <UsersPage />
                                        </NavPermissionGuard>
                                    }
                                />
                                <Route
                                    path=":userId"
                                    element={
                                        <NavPermissionGuard itemKey="users">
                                            <UserDetailPage />
                                        </NavPermissionGuard>
                                    }
                                />
                            </Route>
                            <Route path="groups">
                                <Route
                                    index
                                    element={
                                        <NavPermissionGuard itemKey="groups">
                                            <GroupsPage />
                                        </NavPermissionGuard>
                                    }
                                />
                                <Route
                                    path=":groupId"
                                    element={
                                        <NavPermissionGuard itemKey="groups">
                                            <GroupDetailPage />
                                        </NavPermissionGuard>
                                    }
                                />
                            </Route>
                            <Route path="roles">
                                <Route
                                    index
                                    element={
                                        <NavPermissionGuard itemKey="roles">
                                            <RolesPage />
                                        </NavPermissionGuard>
                                    }
                                />
                                <Route
                                    path=":roleScope"
                                    element={
                                        <PermissionPageGuard anyOf={[ORGANIZATION_ROLE_UPDATE_PERMISSION]} unauthorizedTo="..">
                                            <RoleFormPage />
                                        </PermissionPageGuard>
                                    }
                                />
                                <Route
                                    path=":roleScope/:roleName"
                                    element={
                                        <PermissionPageGuard anyOf={[ORGANIZATION_ROLE_UPDATE_PERMISSION]} unauthorizedTo="..">
                                            <RoleFormPage />
                                        </PermissionPageGuard>
                                    }
                                />
                                <Route
                                    path=":roleScope/:roleName/members"
                                    element={
                                        <PermissionPageGuard anyOf={[ORGANIZATION_ROLE_UPDATE_PERMISSION]} unauthorizedTo="..">
                                            <RoleMembersPage />
                                        </PermissionPageGuard>
                                    }
                                />
                            </Route>
                            <Route
                                path="metadata"
                                element={
                                    <NavPermissionGuard itemKey="metadata">
                                        <MetadataPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route path="dictionaries">
                                <Route
                                    index
                                    element={
                                        <NavPermissionGuard itemKey="dictionaries">
                                            <DictionariesPage />
                                        </NavPermissionGuard>
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
                                        >
                                            <DictionaryDetailPage />
                                        </PermissionPageGuard>
                                    }
                                />
                            </Route>
                            <Route
                                path="shared-policy-groups"
                                element={
                                    <NavPermissionGuard itemKey="shared-policy-groups">
                                        <Outlet />
                                    </NavPermissionGuard>
                                }
                            >
                                <Route
                                    index
                                    element={
                                        <SharedPolicyGroupPageGuard>
                                            <SharedPolicyGroupsPage />
                                        </SharedPolicyGroupPageGuard>
                                    }
                                />
                                <Route
                                    path=":sharedPolicyGroupId"
                                    element={
                                        <SharedPolicyGroupPageGuard>
                                            <SharedPolicyGroupDetailLayout />
                                        </SharedPolicyGroupPageGuard>
                                    }
                                >
                                    <Route index element={<Navigate to="studio" replace />} />
                                    <Route path="studio" element={<SharedPolicyGroupStudioPage />} />
                                    <Route path="history" element={<SharedPolicyGroupHistoryPage />} />
                                    <Route path="*" element={<Navigate to="../studio" replace />} />
                                </Route>
                            </Route>
                            <Route path="gateways">
                                <Route
                                    index
                                    element={
                                        <NavPermissionGuard itemKey="gateways">
                                            <GatewayInstancesPage />
                                        </NavPermissionGuard>
                                    }
                                />
                                <Route
                                    path=":instanceId"
                                    element={
                                        <NavPermissionGuard itemKey="gateways">
                                            <GatewayInstanceDetailLayout />
                                        </NavPermissionGuard>
                                    }
                                >
                                    <Route index element={<Navigate to="environment" replace />} />
                                    <Route path="environment" element={<GatewayInstanceEnvironmentPage />} />
                                    <Route path="monitoring" element={<GatewayInstanceMonitoringPage />} />
                                </Route>
                            </Route>
                            <Route
                                path="tenants"
                                element={
                                    <NavPermissionGuard itemKey="tenants">
                                        <TenantsPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="entrypoints-and-sharding-tags"
                                element={
                                    <NavPermissionGuard itemKey="entrypoints-and-sharding-tags">
                                        <EntrypointsAndShardingTagsPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="policy-studio"
                                element={
                                    <NavPermissionGuard itemKey="policy-studio">
                                        <OrganizationPolicyStudioPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="security-plan-types"
                                element={
                                    <NavPermissionGuard itemKey="security-plan-types">
                                        <SecurityPlanTypesPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="alerts"
                                element={
                                    <NavPermissionGuard itemKey="alerts">
                                        <RequireAlertEngineLicense>
                                            <Outlet />
                                        </RequireAlertEngineLicense>
                                    </NavPermissionGuard>
                                }
                            >
                                <Route element={<AlertsLayout />}>
                                    <Route index element={<AlertsPage />} />
                                    <Route path="activity" element={<AlertsActivityPage />} />
                                </Route>
                                <Route path="new" element={<AlertFormPage />} />
                                <Route path=":alertId" element={<AlertFormPage />} />
                            </Route>
                            <Route
                                path="notification-settings"
                                element={
                                    <NavPermissionGuard itemKey="notification-settings">
                                        <EnvironmentNotificationSettingsPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="organization-audit"
                                element={
                                    <NavPermissionGuard itemKey="organization-audit">
                                        <OrgAuditLogsPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route
                                path="environment-audit"
                                element={
                                    <NavPermissionGuard itemKey="environment-audit">
                                        <EnvAuditLogsPage />
                                    </NavPermissionGuard>
                                }
                            />
                            <Route path={NO_ACCESS_ROUTE_KEY} element={<PlatformLandingOrNoAccess />} />
                        </Route>
                        <Route
                            path="applications/:applicationId"
                            element={
                                <NavPermissionGuard itemKey="applications">
                                    <ApplicationDetailLayout />
                                </NavPermissionGuard>
                            }
                        >
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
