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
import type { License } from '@gravitee/gamma-modules-sdk/types';
import { act, render, screen } from '@testing-library/react';
import type { ComponentType } from 'react';
import { MemoryRouter, useLocation, useNavigationType } from 'react-router-dom';

import { AppRoutes } from './AppRoutes';
import { ROUTES } from '../config/routes';
import { useEnvironmentDictionaries } from '../features/dictionaries/hooks/useEnvironmentDictionaries';
import { useEnvironmentMetadata } from '../features/metadata/hooks/useEnvironmentMetadata';
import { ApimApiError } from '../shared/api/apimClient';
import { useEnvironmentPermissionsReady } from '../shared/hooks/useEnvironmentPermissions';
import { markNavItemDenied, resetDeniedNavItemsForEnvironment } from '../shared/nav/deniedNavItems';
import { notify } from '../shared/notify/notify';

jest.mock('./PlatformToaster', () => ({
    PlatformToaster: () => <div data-testid="platform-toaster" />,
}));

const mockUseModuleRouting = jest.fn(() => ({
    activeNavKey: 'applications',
    navigateToKey: jest.fn(),
    rootPath: '/platform',
}));

jest.mock('@gravitee/gamma-modules-sdk/routing', () => ({
    useModuleRouting: () => mockUseModuleRouting(),
}));

const mockUseLayoutConfig = jest.fn();
const mockUseHasPermission = jest.fn().mockReturnValue(true);
const mockUseHasFeature = jest.fn().mockReturnValue(true);
const mockUseHasEnvironmentPermission = jest.fn().mockReturnValue(true);

// The permission store is an external store, not React state: nav visibility only stays correct if it
// re-reads on every version bump. A stub with a no-op `subscribe` and a constant `getSnapshot` makes a
// frozen memo indistinguishable from a reactive one, so the fake keeps real listeners and a real version.
const mockPermissionListeners = new Set<() => void>();
let mockPermissionStoreVersion = 0;

const mockUseEnvironmentPermissionGrant = jest.fn<boolean | undefined, [string[]]>();

// Same reasoning as the permission store above: the license arrives from the host and can land after the
// first render, so the fake keeps real listeners. The holder is read back by reference — a `getSnapshot`
// returning a fresh object literal per call trips React's "result of getSnapshot should be cached" loop.
const mockLicenseListeners = new Set<() => void>();
let mockLicense: License | null = null;

const ENTITLED_LICENSE: License = { tier: 'enterprise', packs: [], features: [], isExpired: false };
const OSS_LICENSE: License = { tier: 'oss', packs: [], features: [], isExpired: false };
const EXPIRED_LICENSE: License = { tier: 'enterprise', packs: [], features: [], isExpired: true };

const mockUseConsoleSettings = jest.fn();

let deniedNavItemResetCount = 0;

function emitPermissionChange() {
    mockPermissionStoreVersion += 1;
    mockPermissionListeners.forEach(listener => listener());
}

function mockSetLicense(license: License | null) {
    mockLicense = license;
    mockLicenseListeners.forEach(listener => listener());
}

jest.mock('@gravitee/graphene-core', () => {
    const actual = jest.requireActual('@gravitee/graphene-core') as object;
    return {
        ...actual,
        useLayoutConfig: (config: unknown, deps: unknown) => mockUseLayoutConfig(config, deps),
        ContextSidebar: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
        ContextToggleButton: () => null,
        SidebarNavigation: () => null,
        buildLinearBreadcrumbs: () => [],
    };
});

jest.mock('../shared/console-settings', () => ({
    ConsoleSettingsProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    useConsoleSettings: () => mockUseConsoleSettings(),
}));

jest.mock('../shared/hooks/useEnvironmentPermissions', () => ({
    useEnvironmentPermissions: jest.fn(),
    useEnvironmentPermissionsReady: jest.fn().mockReturnValue(true),
    useHasEnvironmentPermission: (anyOf: string[]) => mockUseHasEnvironmentPermission(anyOf),
    useEnvironmentPermissionGrant: (anyOf: string[]) => mockUseEnvironmentPermissionGrant(anyOf),
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: () => ({ id: 'env-1' }),
    useHasPermission: (options: unknown) => mockUseHasPermission(options),
    useHasFeature: (feature: unknown) => mockUseHasFeature(feature),
    permissionService: {
        hasAnyOf: (required?: string[]) => mockUseHasPermission({ anyOf: required ?? [] }),
        subscribe: (listener: () => void) => {
            mockPermissionListeners.add(listener);
            return () => mockPermissionListeners.delete(listener);
        },
        getSnapshot: () => mockPermissionStoreVersion,
    },
    licenseService: {
        setLicense: mockSetLicense,
        getLicense: () => mockLicense,
        subscribe: (listener: () => void) => {
            mockLicenseListeners.add(listener);
            return () => mockLicenseListeners.delete(listener);
        },
        getSnapshot: () => mockLicense,
    },
}));

jest.mock('../features/dictionaries/hooks/useEnvironmentDictionaries');
jest.mock('../features/metadata/hooks/useEnvironmentMetadata');

const mockUseEnvironmentDictionaries = jest.mocked(useEnvironmentDictionaries);
const mockUseEnvironmentMetadata = jest.mocked(useEnvironmentMetadata);
const mockUseEnvironmentPermissionsReady = jest.mocked(useEnvironmentPermissionsReady);

function denyPermissions(...denied: string[]) {
    mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.some(permission => denied.includes(permission)));
}

/** Grants exactly `allowed` and nothing else, to pin the landing item down to a single nav entry. */
function grantOnlyPermissions(...allowed: string[]) {
    mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => anyOf.some(permission => allowed.includes(permission)));
}

jest.mock('../pages/ApplicationsPage', () => ({
    ApplicationsPage: () => <div data-testid="applications-page" />,
}));

jest.mock('../pages/FederationPage', () => ({
    FederationPage: () => <div data-testid="federation-page" />,
}));

jest.mock('../pages/UsersPage', () => ({
    UsersPage: () => <div data-testid="users-page" />,
}));

jest.mock('../pages/UserDetailPage', () => ({
    UserDetailPage: () => <div data-testid="user-detail-page" />,
}));

jest.mock('../pages/GroupsPage', () => ({
    GroupsPage: () => <div data-testid="groups-page" />,
}));

jest.mock('../pages/GroupDetailPage', () => ({
    GroupDetailPage: () => <div data-testid="group-detail-page" />,
}));

jest.mock('../pages/RolesPage', () => ({
    RolesPage: () => <div data-testid="roles-page" />,
}));

jest.mock('../pages/RoleFormPage', () => ({
    RoleFormPage: () => <div data-testid="role-form-page" />,
}));

jest.mock('../pages/RoleMembersPage', () => ({
    RoleMembersPage: () => <div data-testid="role-members-page" />,
}));

jest.mock('../pages/SharedPolicyGroupsPage', () => ({
    SharedPolicyGroupsPage: () => <div data-testid="shared-policy-groups-page" />,
}));

jest.mock('../features/shared-policy-groups/components/SharedPolicyGroupDetailLayout', () => {
    const { Outlet } = jest.requireActual<{ Outlet: ComponentType }>('react-router-dom');
    return {
        SharedPolicyGroupDetailLayout: () => (
            <div data-testid="shared-policy-group-detail-layout">
                <Outlet />
            </div>
        ),
    };
});

jest.mock('../pages/SharedPolicyGroupStudioPage', () => ({
    SharedPolicyGroupStudioPage: () => <div data-testid="shared-policy-group-studio-page" />,
}));

jest.mock('../pages/SharedPolicyGroupHistoryPage', () => ({
    SharedPolicyGroupHistoryPage: () => <div data-testid="shared-policy-group-history-page" />,
}));

jest.mock('../pages/AccessManagementPage', () => ({
    AccessManagementPage: () => <div data-testid="access-management-page" />,
}));

jest.mock('../pages/AuthenticationPage', () => ({
    AuthenticationPage: () => <div data-testid="authentication-page" />,
}));

jest.mock('../pages/CreateIdentityProviderPage', () => ({
    CreateIdentityProviderPage: () => <div data-testid="create-identity-provider-page" />,
}));

jest.mock('../pages/EditIdentityProviderPage', () => ({
    EditIdentityProviderPage: () => <div data-testid="edit-identity-provider-page" />,
}));

jest.mock('../pages/RegisterApplicationPage', () => ({
    RegisterApplicationPage: () => <div data-testid="register-application-page" />,
}));

jest.mock('../pages/OrganizationPolicyStudioPage', () => ({
    OrganizationPolicyStudioPage: () => <div data-testid="organization-policy-studio-page" />,
}));

jest.mock('../pages/TenantsPage', () => ({
    TenantsPage: () => <div data-testid="tenants-page" />,
}));

jest.mock('../pages/ManagementAndSchedulersPage', () => ({
    ManagementAndSchedulersPage: () => <div data-testid="management-and-schedulers-page" />,
}));

jest.mock('../pages/CorsSettingsPage', () => ({
    CorsSettingsPage: () => <div data-testid="cors-settings-page" />,
}));

jest.mock('../pages/SmtpSettingsPage', () => ({
    SmtpSettingsPage: () => <div data-testid="smtp-settings-page" />,
}));

jest.mock('../pages/NotificationTemplatesPage', () => ({
    NotificationTemplatesPage: () => <div data-testid="notification-templates-page" />,
}));

jest.mock('../pages/NotificationTemplateDetailPage', () => ({
    NotificationTemplateDetailPage: () => <div data-testid="notification-template-detail-page" />,
}));

jest.mock('../pages/PlatformNoAccessPage', () => ({
    PlatformNoAccessPage: () => <div data-testid="platform-no-access-page" />,
}));

jest.mock('../pages/EntrypointsAndShardingTagsPage', () => ({
    EntrypointsAndShardingTagsPage: () => <div data-testid="entrypoints-page" />,
}));

jest.mock('../features/security-plan-types/SecurityPlanTypesPage', () => ({
    SecurityPlanTypesPage: () => <div data-testid="security-plan-types-page" />,
}));

jest.mock('../pages/GatewayInstancesPage', () => ({
    GatewayInstancesPage: () => <div data-testid="gateways-page" />,
}));

jest.mock('../features/applications/components/detail', () => ({
    ApplicationDetailLayout: () => <div data-testid="application-detail-layout" />,
    ApplicationDetailIndexRedirect: () => null,
}));

jest.mock('../pages/ApplicationDetailSubscriptionPage', () => ({
    ApplicationDetailSubscriptionPage: () => null,
}));

jest.mock('../pages/AlertsPage', () => ({
    AlertsPage: () => <div data-testid="alerts-page" />,
}));

jest.mock('../pages/OrgAuditLogsPage', () => ({
    OrgAuditLogsPage: () => <div data-testid="org-audit-logs-page" />,
}));

jest.mock('../pages/EnvAuditLogsPage', () => ({
    EnvAuditLogsPage: () => <div data-testid="env-audit-logs-page" />,
}));

jest.mock('../features/alerts/pages/AlertsActivityPage', () => ({
    AlertsActivityPage: () => <div data-testid="alerts-activity-page" />,
}));

jest.mock('../features/alerts/pages/AlertFormPage', () => ({
    AlertFormPage: () => <div data-testid="alert-form-page" />,
}));

jest.mock('../pages/EnvironmentNotificationSettingsPage', () => ({
    EnvironmentNotificationSettingsPage: () => <div data-testid="environment-notification-settings-page" />,
}));

function LocationProbe() {
    return <div data-testid="location">{useLocation().pathname}</div>;
}

function NavigationTypeProbe() {
    return <div data-testid="navigation-type">{useNavigationType()}</div>;
}

function renderFederationUrl() {
    render(
        <MemoryRouter initialEntries={['/federation']}>
            <AppRoutes />
            <LocationProbe />
            <NavigationTypeProbe />
        </MemoryRouter>,
    );
}

function renderPlatform(path = '/applications') {
    render(
        <MemoryRouter initialEntries={[path]}>
            <AppRoutes />
        </MemoryRouter>,
    );
}

type NavGroupProps = { label?: string; items: { key: string; access?: string }[] };

type LayoutConfig = {
    navigation?: { props?: { items?: { key: string; title: string }[]; onItemSelect?: (key: string) => void } };
    contextSidebar?: {
        props?: {
            groups?: NavGroupProps[];
            onItemSelect?: (key: string) => void;
            children?: { props?: { groups?: NavGroupProps[]; onItemSelect?: (key: string) => void } };
        };
    };
    viewMode?: 'global' | 'context';
    breadcrumbs?: unknown;
};

function sectionLayout(): LayoutConfig | undefined {
    return layoutConfigs().findLast(config => 'breadcrumbs' in config);
}

function layoutConfigs(): LayoutConfig[] {
    return mockUseLayoutConfig.mock.calls.map(call => call[0] as LayoutConfig);
}

function primaryNav() {
    return layoutConfigs().findLast(config => config.navigation)?.navigation;
}

function contextSidebar() {
    return layoutConfigs().findLast(config => config.contextSidebar)?.contextSidebar;
}

function contextNav() {
    const sidebar = contextSidebar();
    return sidebar?.props?.children ?? sidebar;
}

function contextGroups(): NavGroupProps[] {
    return contextNav()?.props?.groups ?? [];
}

function visibleNavKeys(): string[] {
    return contextGroups().flatMap(group => group.items.map(item => item.key));
}

function navGroupItemKeys(groupLabel: string): string[] {
    return (
        contextGroups()
            .find(group => group.label === groupLabel)
            ?.items.map(item => item.key) ?? []
    );
}

function navItemAccess(key: string): string | undefined {
    return contextGroups()
        .flatMap(group => group.items)
        .find(item => item.key === key)?.access;
}

function primaryNavKeys(): string[] {
    return primaryNav()?.props?.items?.map(item => item.key) ?? [];
}

describe('AppRoutes', () => {
    beforeEach(() => {
        mockUseLayoutConfig.mockClear();
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'applications',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockReset().mockReturnValue(true);
        mockUseHasFeature.mockReset().mockReturnValue(true);
        mockUseHasEnvironmentPermission.mockReset().mockReturnValue(true);
        mockUseEnvironmentPermissionGrant.mockReset().mockReturnValue(true);
        mockUseEnvironmentPermissionsReady.mockReturnValue(true);
        mockPermissionListeners.clear();
        mockPermissionStoreVersion = 0;
        mockLicenseListeners.clear();
        // Null on both stores is what the host reports before it pushes anything: entitlement stays
        // unknown until a license lands, and null settings are ConsoleSettingsProvider's pre-fetch value.
        mockLicense = null;
        mockUseConsoleSettings.mockReset().mockReturnValue(null);
        // The denial store is module state; a switch to an unused environment id clears it.
        resetDeniedNavItemsForEnvironment(`reset-${(deniedNavItemResetCount += 1)}`);
        mockUseEnvironmentDictionaries.mockReturnValue({
            data: [],
            isLoading: false,
            isError: false,
            error: null,
        } as unknown as ReturnType<typeof useEnvironmentDictionaries>);
        mockUseEnvironmentMetadata.mockReturnValue({
            data: [],
            isLoading: false,
            isError: false,
            error: null,
        } as unknown as ReturnType<typeof useEnvironmentMetadata>);
    });

    it('mounts PlatformToaster for module-wide toast feedback', () => {
        renderPlatform();

        expect(screen.getByTestId('platform-toaster')).not.toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('routes to the Users page under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/users']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('users-page')).not.toBeNull();
    });

    it('routes to the Groups page under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/groups']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('groups-page')).not.toBeNull();
    });

    it('routes to the Group detail page under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/groups/group-1']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('group-detail-page')).not.toBeNull();
    });

    it('routes to the Roles page under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/roles']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('roles-page')).not.toBeNull();
    });

    it('routes to the Role form page for create and edit', () => {
        render(
            <MemoryRouter initialEntries={['/roles/API']}>
                <AppRoutes />
            </MemoryRouter>,
        );
        expect(screen.getByTestId('role-form-page')).not.toBeNull();
    });

    it('routes to the Role members page', () => {
        render(
            <MemoryRouter initialEntries={['/roles/ORGANIZATION/ADMIN/members']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('role-members-page')).not.toBeNull();
    });

    it('routes a direct Federation URL visit to the Federation page without redirecting', () => {
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        mockSetLicense(ENTITLED_LICENSE);

        render(
            <MemoryRouter initialEntries={['/federation']}>
                <AppRoutes />
                <LocationProbe />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('federation-page')).not.toBeNull();
        expect(screen.getByTestId('location').textContent).toBe('/federation');
    });

    it('shows the Federation nav item next to Applications when the gate passes with no integrations configured', () => {
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        mockSetLicense(ENTITLED_LICENSE);

        renderPlatform();

        expect(navGroupItemKeys('APIs & Assets')).toEqual([
            'applications',
            'federation',
            'metadata',
            'dictionaries',
            'shared-policy-groups',
        ]);
        expect(navItemAccess('federation')).toBeUndefined();
    });

    it('hides the Federation nav item when Federation is not enabled for the organization', () => {
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: false } });
        mockSetLicense(ENTITLED_LICENSE);

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('federation');
    });

    it('hides the Federation nav item when console settings carry no federation flag at all', () => {
        mockUseConsoleSettings.mockReturnValue({});
        mockSetLicense(ENTITLED_LICENSE);

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('federation');
    });

    it('hides the Federation nav item when the user lacks environment-integration-r', () => {
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        mockSetLicense(ENTITLED_LICENSE);
        denyPermissions('environment-integration-r');

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('federation');
    });

    it.each([
        ['an oss-tier license', OSS_LICENSE],
        ['an expired license', EXPIRED_LICENSE],
        ['no license reported yet', null],
    ])('hides the Federation nav item entirely, not as a locked item, with %s', (_case, license) => {
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        mockSetLicense(license);

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('federation');
    });

    it('redirects a direct Federation URL visit to Applications when the user lacks environment-integration-r', () => {
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        mockSetLicense(ENTITLED_LICENSE);
        denyPermissions('environment-integration-r');

        renderFederationUrl();

        expect(screen.queryByTestId('federation-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
        expect(screen.getByTestId('location').textContent).toBe('/applications');
        expect(screen.getByTestId('navigation-type').textContent).toBe('REPLACE');
    });

    it.each([
        ['Federation is not enabled for the organization', { federation: { enabled: false } }, ENTITLED_LICENSE],
        ['the installed license tier is oss', { federation: { enabled: true } }, OSS_LICENSE],
        ['the license has expired', { federation: { enabled: true } }, EXPIRED_LICENSE],
        ['no license has been reported yet', { federation: { enabled: true } }, null],
    ])('redirects a direct Federation URL visit to Applications when %s', (_case, consoleSettings, license) => {
        mockUseConsoleSettings.mockReturnValue(consoleSettings);
        mockSetLicense(license);

        renderFederationUrl();

        expect(screen.queryByTestId('federation-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
        expect(screen.getByTestId('location').textContent).toBe('/applications');
        expect(screen.getByTestId('navigation-type').textContent).toBe('REPLACE');
    });

    it('redirects a refused Federation visit to the landing item when the user cannot see Applications', () => {
        grantOnlyPermissions('environment-integration-r', 'organization-settings-r', 'organization-tenant-r');
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: false } });
        mockSetLicense(ENTITLED_LICENSE);

        renderFederationUrl();

        expect(screen.queryByTestId('federation-page')).toBeNull();
        expect(screen.getByTestId('tenants-page')).not.toBeNull();
        expect(screen.getByTestId('location').textContent).toBe('/tenants');
    });

    it('lands on the Federation page from the platform index when Federation is the only visible item', () => {
        grantOnlyPermissions('environment-integration-r');
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        mockSetLicense(ENTITLED_LICENSE);

        render(
            <MemoryRouter initialEntries={['/']}>
                <AppRoutes />
                <LocationProbe />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('location').textContent).toBe('/federation');
        expect(screen.getByTestId('federation-page')).not.toBeNull();
        expect(screen.queryByTestId('platform-no-access-page')).toBeNull();
    });

    it('warns once while Federation is enabled and no license has been reported', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });

        renderPlatform();

        expect(warn).toHaveBeenCalledTimes(1);
        warn.mockRestore();
    });

    it('does not repeat the unreported-license warning across a re-render', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        renderPlatform();

        act(() => {
            emitPermissionChange();
        });

        expect(warn).toHaveBeenCalledTimes(1);
        warn.mockRestore();
    });

    it('does not warn a second time when the refused visit is to the Federation route itself', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });

        renderFederationUrl();

        expect(screen.queryByTestId('federation-page')).toBeNull();
        expect(warn).toHaveBeenCalledTimes(1);
        warn.mockRestore();
    });

    it.each([
        ['the license tier is oss', { federation: { enabled: true } }, OSS_LICENSE],
        ['the license has expired', { federation: { enabled: true } }, EXPIRED_LICENSE],
        ['Federation is not enabled', { federation: { enabled: false } }, null],
    ])('stays quiet when %s, which answers the entitlement question', (_case, consoleSettings, license) => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        mockUseConsoleSettings.mockReturnValue(consoleSettings);
        mockSetLicense(license);

        renderPlatform();

        expect(warn).not.toHaveBeenCalled();
        warn.mockRestore();
    });

    it('lands on the Federation page when the Federation nav item is selected', () => {
        const navigateToKey = jest.fn();
        mockUseModuleRouting.mockReturnValue({ activeNavKey: 'applications', navigateToKey, rootPath: '/platform' });
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        mockSetLicense(ENTITLED_LICENSE);
        renderPlatform();
        expect(visibleNavKeys()).toContain('federation');

        contextNav()?.props?.onItemSelect?.('federation');

        expect(navigateToKey).toHaveBeenCalledWith('federation');
        renderPlatform(`/${ROUTES.federation.path}`);
        expect(screen.getByTestId('federation-page')).not.toBeNull();
    });

    it('surfaces no error notification while the Federation gate runs without environment-integration-r', () => {
        const notifyError = jest.spyOn(notify, 'error').mockImplementation(() => undefined);
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        mockSetLicense(ENTITLED_LICENSE);
        denyPermissions('environment-integration-r');

        renderPlatform();
        renderFederationUrl();

        expect(notifyError).not.toHaveBeenCalled();
        notifyError.mockRestore();
    });

    it('requests no integrations endpoint while deciding whether Federation is available', () => {
        const fetchSpy = jest.spyOn(global, 'fetch').mockResolvedValue(new Response('[]'));
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        mockSetLicense(ENTITLED_LICENSE);
        denyPermissions('environment-integration-r');

        renderPlatform();
        renderFederationUrl();

        const requestedUrls = fetchSpy.mock.calls.map(([input]) => String(input));
        expect(requestedUrls.filter(url => /integration/i.test(url))).toEqual([]);
        fetchSpy.mockRestore();
    });

    it('adds the Federation nav item when the license lands after the first render', () => {
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        renderPlatform();
        expect(visibleNavKeys()).not.toContain('federation');

        act(() => {
            mockSetLicense(ENTITLED_LICENSE);
        });

        expect(visibleNavKeys()).toContain('federation');
    });

    it('redirects off the Federation page when the entitlement lapses while it is open', () => {
        mockUseConsoleSettings.mockReturnValue({ federation: { enabled: true } });
        mockSetLicense(ENTITLED_LICENSE);
        renderFederationUrl();
        expect(screen.getByTestId('federation-page')).not.toBeNull();

        act(() => {
            mockSetLicense(EXPIRED_LICENSE);
        });

        expect(screen.queryByTestId('federation-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
        expect(screen.getByTestId('location').textContent).toBe('/applications');
    });

    it('shows Organization, Environment, and Team in the primary sidebar', () => {
        renderPlatform();

        expect(primaryNavKeys()).toEqual(['organization', 'environment', 'team']);
        expect(primaryNav()?.props?.items?.map(item => item.title)).toEqual(['Organization', 'Environment', 'Team']);
    });

    it('does not show a General primary nav item', () => {
        renderPlatform();

        expect(primaryNavKeys()).not.toContain('general');
    });

    it('shows Applications in the Environment context sidebar', () => {
        renderPlatform();

        expect(visibleNavKeys()).toContain('applications');
        expect(visibleNavKeys()).toContain('metadata');
        expect(visibleNavKeys()).toContain('dictionaries');
        expect(visibleNavKeys()).not.toContain('users');
    });

    it('shows the User Groups nav item when the user has read permission', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'groups',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/groups');

        expect(visibleNavKeys()).toContain('groups');
    });

    it('hides the Groups nav item when the user lacks environment-group-r', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'groups',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-group-r'));

        renderPlatform('/groups');

        expect(visibleNavKeys()).not.toContain('groups');
    });

    it('shows the Roles nav item when the user has read permission', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'roles',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/roles');

        expect(visibleNavKeys()).toContain('roles');
    });

    it('hides the Roles nav item when the user lacks organization-role-r', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'roles',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-role-r'));

        renderPlatform('/roles');

        expect(visibleNavKeys()).not.toContain('roles');
    });

    it('shows the Dictionaries nav item when the user has read permission', () => {
        renderPlatform();

        expect(visibleNavKeys()).toContain('dictionaries');
    });

    it('hides the Dictionaries nav item when the user lacks environment-dictionary-r', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-dictionary-r'));

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('dictionaries');
    });

    it('hides the Dictionaries nav item on a fresh load when the permissions map is wrong and the resource 403s', () => {
        // The permissions map says the user CAN read dictionaries, but the actual list call always 403s
        // (e.g. a stale backend permission cache) — the nav must not trust the map alone.
        mockUseEnvironmentDictionaries.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        } as unknown as ReturnType<typeof useEnvironmentDictionaries>);

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('dictionaries');
    });

    it('shows the Metadata nav item when the user has read permission', () => {
        renderPlatform();

        expect(visibleNavKeys()).toContain('metadata');
    });

    it('hides the Metadata nav item when the user lacks environment-metadata-r', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-metadata-r'));

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('metadata');
    });

    it('hides the Metadata nav item on a fresh load when the permissions map is wrong and the resource 403s', () => {
        mockUseEnvironmentMetadata.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        } as unknown as ReturnType<typeof useEnvironmentMetadata>);

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('metadata');
    });

    it('routes to the Shared Policy Groups page under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/shared-policy-groups']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('shared-policy-groups-page')).not.toBeNull();
    });

    it('routes to the shared policy group studio tab by default under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/shared-policy-groups/spg-1']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('shared-policy-group-detail-layout')).not.toBeNull();
        expect(screen.getByTestId('shared-policy-group-studio-page')).not.toBeNull();
    });

    it('redirects the removed overview route to the studio', () => {
        render(
            <MemoryRouter initialEntries={['/shared-policy-groups/spg-1/overview']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('shared-policy-group-studio-page')).not.toBeNull();
    });

    it('routes to Shared Policy Group version history from the overflow action', () => {
        render(
            <MemoryRouter initialEntries={['/shared-policy-groups/spg-1/history']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('shared-policy-group-history-page')).not.toBeNull();
    });

    it('redirects unknown shared policy group detail routes to the studio tab', () => {
        render(
            <MemoryRouter initialEntries={['/shared-policy-groups/spg-1/unknown']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('shared-policy-group-studio-page')).not.toBeNull();
    });

    it('redirects away from Shared Policy Groups when the user lacks environment-shared_policy_group-r', () => {
        mockUseEnvironmentPermissionGrant.mockImplementation((anyOf: string[]) => !anyOf.includes('environment-shared_policy_group-r'));

        render(
            <MemoryRouter initialEntries={['/shared-policy-groups']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.queryByTestId('shared-policy-groups-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('shows the Shared Policy Groups nav item when the user has read permission', () => {
        renderPlatform();

        expect(visibleNavKeys()).toContain('shared-policy-groups');
    });

    // permissionService is an external store: a 403 patch or a host reload bumps it without changing any
    // React state the sidebar memo depends on, so the memo has to track the store's version explicitly.
    it('recomputes the sidebar when the permission store changes after the first render', () => {
        renderPlatform('/applications');
        expect(visibleNavKeys()).toContain('gateways');

        denyPermissions('environment-instance-r');
        act(() => {
            emitPermissionChange();
        });

        expect(visibleNavKeys()).not.toContain('gateways');
    });

    it('re-adds a nav item when the permission store grants it after the first render', () => {
        denyPermissions('environment-instance-r');
        renderPlatform('/applications');
        expect(visibleNavKeys()).not.toContain('gateways');

        mockUseHasPermission.mockReturnValue(true);
        act(() => {
            emitPermissionChange();
        });

        expect(visibleNavKeys()).toContain('gateways');
    });

    it('hides the Shared Policy Groups nav item when the user lacks environment-shared_policy_group-r', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-shared_policy_group-r'));

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('shared-policy-groups');
    });

    it('routes to the user detail page under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/users/user-1']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('user-detail-page')).not.toBeNull();
    });

    it('navigates to the first visible item when a different primary section is selected', () => {
        const navigateToKey = jest.fn();
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'applications',
            navigateToKey,
            rootPath: '/platform',
        });
        renderPlatform();

        primaryNav()?.props?.onItemSelect?.('team');

        expect(navigateToKey).toHaveBeenCalledWith('users');
    });

    it('does not navigate when the already-active primary section is selected', () => {
        const navigateToKey = jest.fn();
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'applications',
            navigateToKey,
            rootPath: '/platform',
        });
        renderPlatform();

        primaryNav()?.props?.onItemSelect?.('environment');

        expect(navigateToKey).not.toHaveBeenCalled();
    });

    it('navigates context sidebar items by their own keys', () => {
        const navigateToKey = jest.fn();
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'applications',
            navigateToKey,
            rootPath: '/platform',
        });
        renderPlatform();

        contextNav()?.props?.onItemSelect?.('metadata');

        expect(navigateToKey).toHaveBeenCalledWith('metadata');
    });

    it('does not set a platform context sidebar on application detail pages', () => {
        renderPlatform('/applications/app-1');

        expect(screen.getByTestId('application-detail-layout')).not.toBeNull();
        expect(primaryNavKeys()).toEqual(['organization', 'environment', 'team']);
        expect(contextSidebar()).toBeUndefined();
    });

    it('routes to the register application page under the platform module', () => {
        renderPlatform('/applications/new');

        expect(screen.getByTestId('register-application-page')).not.toBeNull();
    });

    it('routes to the Alerts page under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/alerts']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('alerts-page')).not.toBeNull();
        expect(screen.getByRole('link', { name: 'My alerts' })).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Activity' })).not.toBeNull();
    });

    it('routes to the Alerts activity page', () => {
        render(
            <MemoryRouter initialEntries={['/alerts/activity']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('alerts-activity-page')).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Activity' })).not.toBeNull();
    });

    it('does not show My alerts / Activity tabs on the alert form', () => {
        render(
            <MemoryRouter initialEntries={['/alerts/new']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('alert-form-page')).not.toBeNull();
        expect(screen.queryByRole('link', { name: 'My alerts' })).toBeNull();
        expect(screen.queryByRole('link', { name: 'Activity' })).toBeNull();
    });

    it('shows the Alerts nav item when the user has read permission', () => {
        renderPlatform();

        expect(visibleNavKeys()).toContain('alerts');
        expect(navItemAccess('alerts')).toBeUndefined();
    });

    it('locks the Alerts nav item when Alert Engine is unlicensed', () => {
        mockUseHasFeature.mockImplementation((feature: string) => feature !== 'alert-engine');

        renderPlatform();

        expect(visibleNavKeys()).toContain('alerts');
        expect(navItemAccess('alerts')).toBe('locked');
    });

    it('redirects Alerts pages to applications when Alert Engine is unlicensed', () => {
        mockUseHasFeature.mockImplementation((feature: string) => feature !== 'alert-engine');

        render(
            <MemoryRouter initialEntries={['/alerts']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.queryByTestId('alerts-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    // The audit pages answer an unlicensed visit with an upsell dialog whose dismiss leaves the page.
    // Landing on one makes it undismissable: every exit resolves back to the landing key.
    it('does not land on an audit page when the Audit Trail is unlicensed', () => {
        mockUseHasFeature.mockImplementation((feature: string) => feature !== 'apim-audit-trail');
        grantOnlyPermissions('organization-settings-u', 'organization-audit-r');

        renderPlatform('/');

        expect(screen.queryByTestId('org-audit-logs-page')).toBeNull();
    });

    it('hides the Alerts nav item when the user lacks environment-alert-r', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-alert-r'));

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('alerts');
    });

    it('routes to the Notification settings page under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/notification-settings']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('environment-notification-settings-page')).not.toBeNull();
    });

    it('shows the Notification settings nav item when the user has read permission', () => {
        renderPlatform();

        expect(visibleNavKeys()).toContain('notification-settings');
    });

    it('hides the Notification settings nav item when the user lacks environment-notification-r', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-notification-r'));

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('notification-settings');
    });

    it('redirects away from Notification settings when the user lacks environment-notification-r', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-notification-r'));

        render(
            <MemoryRouter initialEntries={['/notification-settings']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.queryByTestId('environment-notification-settings-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    // Alerts stays in the sidebar as a locked item, so it must not also be the landing target: the
    // license redirect would bounce to applications, whose guard would bounce back to the landing key.
    it('does not land on Alerts when Alert Engine is unlicensed, so the license redirect cannot loop', () => {
        mockUseHasFeature.mockImplementation((feature: string) => feature !== 'alert-engine');
        grantOnlyPermissions('environment-alert-r');

        expect(() => renderPlatform('/')).not.toThrow();

        expect(screen.queryByTestId('alerts-page')).toBeNull();
        expect(screen.getByTestId('platform-no-access-page')).not.toBeNull();
    });

    it('does not loop when the permission store grants Shared Policy Groups but the module cache denies it', () => {
        grantOnlyPermissions('environment-shared_policy_group-r');
        mockUseEnvironmentPermissionGrant.mockReturnValue(false);

        expect(() => renderPlatform('/')).not.toThrow();

        expect(screen.queryByTestId('shared-policy-groups-page')).toBeNull();
    });

    it('defers to the permission store when the module permission cache has no data', () => {
        mockUseEnvironmentPermissionGrant.mockReturnValue(undefined);

        renderPlatform('/shared-policy-groups');

        expect(screen.getByTestId('shared-policy-groups-page')).not.toBeNull();
    });

    it('routes to the Tenants page under the platform module', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'tenants',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/tenants');

        expect(screen.getByTestId('tenants-page')).not.toBeNull();
    });

    it('shows the Tenants nav item when the user has read permission', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'tenants',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/tenants');

        expect(visibleNavKeys()).toContain('tenants');
        expect(visibleNavKeys()).toContain('entrypoints-and-sharding-tags');
    });

    it('hides the Tenants nav item when the user lacks tenant read permission', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'tenants',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(
            ({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-tenant-r') && !anyOf.includes('environment-tenant-r'),
        );

        renderPlatform('/tenants');

        expect(visibleNavKeys()).not.toContain('tenants');
    });

    it('redirects away from Tenants when the user lacks tenant read permission', () => {
        mockUseHasPermission.mockImplementation(
            ({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-tenant-r') && !anyOf.includes('environment-tenant-r'),
        );

        renderPlatform('/tenants');

        expect(screen.queryByTestId('tenants-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('routes to the organization Policy Studio under the platform module', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'policy-studio',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/policy-studio');

        expect(screen.getByTestId('organization-policy-studio-page')).not.toBeNull();
        expect(visibleNavKeys()).toContain('policy-studio');
    });

    // The organization GET accepts READ, CREATE, DELETE and UPDATE, so every acl that can reach the
    // endpoint must also reach the page.
    it.each(['organization-policies-r', 'organization-policies-c', 'organization-policies-d', 'organization-policies-u'])(
        'opens the organization Policy Studio for a user who only holds %s',
        heldPermission => {
            mockUseModuleRouting.mockReturnValue({
                activeNavKey: 'policy-studio',
                navigateToKey: jest.fn(),
                rootPath: '/platform',
            });
            mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => {
                const platformPolicyChecks = anyOf.filter(permission => permission.startsWith('organization-policies-'));
                return platformPolicyChecks.length === 0 || platformPolicyChecks.includes(heldPermission);
            });

            renderPlatform('/policy-studio');

            expect(screen.getByTestId('organization-policy-studio-page')).not.toBeNull();
            expect(visibleNavKeys()).toContain('policy-studio');
        },
    );

    it('hides the organization Policy Studio from a user who holds no platform policy permission', () => {
        mockUseHasPermission.mockImplementation(
            ({ anyOf }: { anyOf: string[] }) => !anyOf.some(permission => permission.startsWith('organization-policies-')),
        );

        renderPlatform('/policy-studio');

        expect(screen.queryByTestId('organization-policy-studio-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
        expect(visibleNavKeys()).not.toContain('policy-studio');
    });

    it('routes to the Organization Audit page under the platform module', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'organization-audit',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/organization-audit');

        expect(screen.getByTestId('org-audit-logs-page')).not.toBeNull();
    });

    it('shows the Organization Audit nav item when the user has audit read permission', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'organization-audit',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/organization-audit');

        expect(visibleNavKeys()).toContain('organization-audit');
    });

    it('hides the Organization Audit nav item when the user lacks audit read permission', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'organization-audit',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-audit-r'));

        renderPlatform('/organization-audit');

        expect(visibleNavKeys()).not.toContain('organization-audit');
    });

    it('redirects away from Organization Audit when the user lacks audit read permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-audit-r'));

        renderPlatform('/organization-audit');

        expect(screen.queryByTestId('org-audit-logs-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('routes to the Environment Audit page under the platform module', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'environment-audit',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/environment-audit');

        expect(screen.getByTestId('env-audit-logs-page')).not.toBeNull();
    });

    it('shows the Environment Audit nav item when the user has audit read permission', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'environment-audit',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/environment-audit');

        expect(visibleNavKeys()).toContain('environment-audit');
    });

    it('hides the Environment Audit nav item when the user lacks audit read permission', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'environment-audit',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-audit-r'));

        renderPlatform('/environment-audit');

        expect(visibleNavKeys()).not.toContain('environment-audit');
    });

    it('redirects away from Environment Audit when the user lacks audit read permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-audit-r'));

        renderPlatform('/environment-audit');

        expect(screen.queryByTestId('env-audit-logs-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('hides the Organization Audit nav item for a user holding only environment audit read', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'environment-audit',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-audit-r'));

        renderPlatform('/environment-audit');

        expect(visibleNavKeys()).not.toContain('organization-audit');
        expect(visibleNavKeys()).toContain('environment-audit');
    });

    it('hides the Environment Audit nav item for a user holding only organization audit read', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'organization-audit',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-audit-r'));

        renderPlatform('/organization-audit');

        expect(visibleNavKeys()).not.toContain('environment-audit');
        expect(visibleNavKeys()).toContain('organization-audit');
    });

    it('redirects away from Organization Audit for a user holding only environment audit read', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-audit-r'));

        renderPlatform('/organization-audit');

        expect(screen.queryByTestId('org-audit-logs-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('redirects away from Environment Audit for a user holding only organization audit read', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-audit-r'));

        renderPlatform('/environment-audit');

        expect(screen.queryByTestId('env-audit-logs-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('routes to organization console settings pages', () => {
        renderPlatform('/management-and-schedulers');
        expect(screen.getByTestId('management-and-schedulers-page')).not.toBeNull();
        renderPlatform('/cors');
        expect(screen.getByTestId('cors-settings-page')).not.toBeNull();
        renderPlatform('/smtp');
        expect(screen.getByTestId('smtp-settings-page')).not.toBeNull();
    });

    it('routes to organization notification templates', () => {
        renderPlatform('/templates');
        expect(screen.getByTestId('notification-templates-page')).not.toBeNull();
        renderPlatform('/templates/API/API_STARTED');
        expect(screen.getByTestId('notification-template-detail-page')).not.toBeNull();
    });

    it('hides Templates without organization-notification_templates-r', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'smtp',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(
            ({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-notification_templates-r'),
        );
        renderPlatform('/smtp');

        expect(visibleNavKeys()).not.toContain('templates');
        expect(visibleNavKeys()).toContain('smtp');
    });

    it('redirects away from Templates without organization-notification_templates-r', () => {
        mockUseHasPermission.mockImplementation(
            ({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-notification_templates-r'),
        );

        renderPlatform('/templates');

        expect(screen.queryByTestId('notification-templates-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('shows Management & Schedulers, CORS, SMTP, and Templates when the user can read org settings', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'management-and-schedulers',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/management-and-schedulers');

        expect(visibleNavKeys()).toEqual(
            expect.arrayContaining(['authentication', 'management-and-schedulers', 'cors', 'smtp', 'templates']),
        );
        expect(visibleNavKeys()).not.toContain('access-management');
    });

    it('hides organization console settings nav items without organization-settings-r', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'management-and-schedulers',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        denyPermissions('organization-settings-r', 'organization-settings-u');
        renderPlatform('/applications');

        expect(visibleNavKeys()).not.toContain('management-and-schedulers');
        expect(visibleNavKeys()).not.toContain('cors');
        expect(visibleNavKeys()).not.toContain('smtp');
        expect(visibleNavKeys()).not.toContain('templates');
        expect(visibleNavKeys()).toContain('access-management');
        expect(visibleNavKeys()).not.toContain('tenants');
        expect(visibleNavKeys()).not.toContain('users');
        expect(visibleNavKeys()).not.toContain('policy-studio');
    });

    it('hides Management, CORS, and SMTP when the user has organization-settings-u without -r', () => {
        denyPermissions('organization-settings-r');
        renderPlatform('/access-management');

        expect(visibleNavKeys()).not.toContain('management-and-schedulers');
        expect(visibleNavKeys()).not.toContain('cors');
        expect(visibleNavKeys()).not.toContain('smtp');
        renderPlatform('/cors');
        expect(screen.queryByTestId('cors-settings-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('routes to Authentication and Create Identity Provider', () => {
        renderPlatform('/authentication');
        expect(screen.getByTestId('authentication-page')).not.toBeNull();
        renderPlatform('/authentication/new');
        expect(screen.getByTestId('create-identity-provider-page')).not.toBeNull();
    });

    it('routes to Edit Identity Provider', () => {
        renderPlatform('/authentication/google-idp');
        expect(screen.getByTestId('edit-identity-provider-page')).not.toBeNull();
    });

    it('hides Authentication without organization-identity_provider-r', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'access-management',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-identity_provider-r'));
        renderPlatform('/access-management');

        expect(visibleNavKeys()).not.toContain('authentication');
        expect(visibleNavKeys()).toContain('access-management');
    });

    it('redirects Create Identity Provider to the list without organization-identity_provider-c', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-identity_provider-c'));
        renderPlatform('/authentication/new');
        expect(screen.queryByTestId('create-identity-provider-page')).toBeNull();
        expect(screen.getByTestId('authentication-page')).not.toBeNull();
    });

    it('redirects Edit Identity Provider to applications without organization-identity_provider-r', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-identity_provider-r'));
        renderPlatform('/authentication/google-idp');
        expect(screen.queryByTestId('edit-identity-provider-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('hides Applications without environment-application-r', () => {
        denyPermissions('environment-application-r');
        renderPlatform();

        expect(visibleNavKeys()).not.toContain('applications');
    });

    it('does not render Applications for a pasted URL without environment-application-r', () => {
        denyPermissions('environment-application-r');
        renderPlatform('/applications');

        expect(screen.queryByTestId('applications-page')).toBeNull();
        expect(screen.getByTestId('tenants-page')).not.toBeNull();
    });

    it('does not render Alerts for a pasted URL without environment-alert-r', () => {
        denyPermissions('environment-alert-r');
        renderPlatform('/alerts');

        expect(screen.queryByTestId('alerts-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('does not render Gateways for a pasted URL without environment-instance-r', () => {
        denyPermissions('environment-instance-r');
        renderPlatform('/gateways');

        expect(screen.queryByTestId('gateways-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('does not render Security Plan Types for a pasted URL without environment-settings-r', () => {
        denyPermissions('environment-settings-r');
        renderPlatform('/security-plan-types');

        expect(screen.queryByTestId('security-plan-types-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('hides Security Plan Types without environment-settings-r', () => {
        denyPermissions('environment-settings-r');
        renderPlatform();

        expect(visibleNavKeys()).not.toContain('security-plan-types');
    });

    it('renders Access Management from a pasted URL without organization-settings', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'access-management',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        denyPermissions('organization-settings-r', 'organization-settings-u');
        renderPlatform('/access-management');

        expect(screen.getByTestId('access-management-page')).not.toBeNull();
        expect(visibleNavKeys()).toContain('access-management');
    });

    it('does not render Access Management without environment-am_configuration-r', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'access-management',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        denyPermissions('environment-am_configuration-r');
        renderPlatform('/access-management');

        expect(screen.queryByTestId('access-management-page')).toBeNull();
        expect(visibleNavKeys()).not.toContain('access-management');
    });

    it('shows a no-access page when the user has no Platform menus', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPlatform('/alerts');

        expect(screen.queryByTestId('alerts-page')).toBeNull();
        expect(screen.getByTestId('platform-no-access-page')).not.toBeNull();
    });

    // Tenants is granted by the organization scope, which the 403 strip cannot rewrite. Without the
    // denial it stays visible and stays the landing key, so the redirect out of it lands back on it.
    it('drops an org-scoped item from the sidebar and the landing key once a 403 denies it', () => {
        grantOnlyPermissions('organization-settings-r', 'organization-tenant-r');
        renderPlatform('/');

        expect(visibleNavKeys()).toContain('tenants');
        expect(screen.getByTestId('tenants-page')).not.toBeNull();

        act(() => markNavItemDenied('tenants'));

        expect(visibleNavKeys()).not.toContain('tenants');
        expect(screen.queryByTestId('tenants-page')).toBeNull();
        expect(screen.getByTestId('management-and-schedulers-page')).not.toBeNull();
    });

    it('does not reserve a context sidebar when no Platform menus are visible', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPlatform('/no-access');

        expect(sectionLayout()?.contextSidebar).toBeUndefined();
        expect(sectionLayout()?.viewMode).toBeUndefined();
    });

    it('keeps no-access when that URL is opened and nothing is visible', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPlatform('/no-access');

        expect(screen.getByTestId('platform-no-access-page')).not.toBeNull();
    });

    it('leaves a leftover no-access URL when the user has Applications', () => {
        renderPlatform('/no-access');

        expect(screen.queryByTestId('platform-no-access-page')).toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('opens Applications from the platform index when the user can read them', () => {
        renderPlatform('/');

        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('hides all nav items until environment permissions are ready', () => {
        mockUseEnvironmentPermissionsReady.mockReturnValue(false);
        renderPlatform();

        expect(visibleNavKeys()).toEqual([]);
        expect(primaryNavKeys()).toEqual([]);
    });

    it('keeps a stable empty groups list for the section layout while nothing is visible', () => {
        mockUseEnvironmentPermissionsReady.mockReturnValue(false);
        const view = render(
            <MemoryRouter initialEntries={['/applications']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        const groupsAfterFirst = mockUseLayoutConfig.mock.calls
            .filter(call => call[0] !== undefined && typeof call[0] === 'object' && call[0] !== null && 'breadcrumbs' in call[0])
            .at(-1)?.[1] as unknown[];

        view.rerender(
            <MemoryRouter initialEntries={['/applications']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        const groupsAfterRerender = mockUseLayoutConfig.mock.calls
            .filter(call => call[0] !== undefined && typeof call[0] === 'object' && call[0] !== null && 'breadcrumbs' in call[0])
            .at(-1)?.[1] as unknown[];

        expect(groupsAfterFirst?.[3]).toBe(groupsAfterRerender?.[3]);
    });
});
