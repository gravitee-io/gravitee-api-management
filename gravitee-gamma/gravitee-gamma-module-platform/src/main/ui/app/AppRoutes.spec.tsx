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
import { render, screen } from '@testing-library/react';
import type { ComponentType } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { AppRoutes } from './AppRoutes';
import { useEnvironmentDictionaries } from '../features/dictionaries/hooks/useEnvironmentDictionaries';
import { useEnvironmentMetadata } from '../features/metadata/hooks/useEnvironmentMetadata';
import { ApimApiError } from '../shared/api/apimClient';

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

jest.mock('@gravitee/graphene-core', () => {
    const actual = jest.requireActual('@gravitee/graphene-core') as object;
    return {
        ...actual,
        useLayoutConfig: (config: unknown, deps: unknown) => mockUseLayoutConfig(config, deps),
        ContextSidebar: () => null,
        ContextToggleButton: () => null,
        buildLinearBreadcrumbs: () => [],
    };
});

jest.mock('../shared/console-settings', () => ({
    ConsoleSettingsProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

jest.mock('../shared/hooks/useEnvironmentPermissions', () => ({
    useEnvironmentPermissions: jest.fn(),
    useEnvironmentPermissionsReady: jest.fn().mockReturnValue(true),
}));

const mockUseHasPermission = jest.fn().mockReturnValue(true);

jest.mock('../shared/gamma-modules-sdk', () => ({
    useHasPermission: (options: unknown) => mockUseHasPermission(options),
}));

jest.mock('../features/dictionaries/hooks/useEnvironmentDictionaries');
jest.mock('../features/metadata/hooks/useEnvironmentMetadata');

const mockUseEnvironmentDictionaries = jest.mocked(useEnvironmentDictionaries);
const mockUseEnvironmentMetadata = jest.mocked(useEnvironmentMetadata);

jest.mock('../pages/ApplicationsPage', () => ({
    ApplicationsPage: () => <div data-testid="applications-page" />,
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

function renderPlatform(path = '/applications') {
    render(
        <MemoryRouter initialEntries={[path]}>
            <AppRoutes />
        </MemoryRouter>,
    );
}

type LayoutConfig = {
    navigation?: { props?: { items?: { key: string; title: string }[]; onItemSelect?: (key: string) => void } };
    contextSidebar?: { props?: { groups?: { items: { key: string }[] }[]; onItemSelect?: (key: string) => void } };
};

function layoutConfigs(): LayoutConfig[] {
    return mockUseLayoutConfig.mock.calls.map(call => call[0] as LayoutConfig);
}

function primaryNav() {
    return layoutConfigs().findLast(config => config.navigation)?.navigation;
}

function contextSidebar() {
    return layoutConfigs().findLast(config => config.contextSidebar)?.contextSidebar;
}

function visibleNavKeys(): string[] {
    const groups = contextSidebar()?.props?.groups ?? [];
    return groups.flatMap(group => group.items.map(item => item.key));
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
            <MemoryRouter initialEntries={['/user-groups']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('groups-page')).not.toBeNull();
    });

    it('routes to the Group detail page under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/user-groups/group-1']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('group-detail-page')).not.toBeNull();
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
            activeNavKey: 'user-groups',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/user-groups');

        expect(visibleNavKeys()).toContain('user-groups');
    });

    it('hides the Groups nav item when the user lacks environment-group-r', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'user-groups',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-group-r'));

        renderPlatform('/user-groups');

        expect(visibleNavKeys()).not.toContain('user-groups');
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
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-shared_policy_group-r'));

        render(
            <MemoryRouter initialEntries={['/shared-policy-groups']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });

    it('shows the Shared Policy Groups nav item when the user has read permission', () => {
        renderPlatform();

        expect(visibleNavKeys()).toContain('shared-policy-groups');
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

        contextSidebar()?.props?.onItemSelect?.('metadata');

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
    });

    it('hides the Alerts nav item when the user lacks environment-alert-r', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-alert-r'));

        renderPlatform();

        expect(visibleNavKeys()).not.toContain('alerts');
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

    // The organization GET refuses READ, so `-r` alone must not open the page: it would 403 on load.
    it('hides the organization Policy Studio from a user who only holds the read permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => {
            const platformPolicyChecks = anyOf.filter(permission => permission.startsWith('organization-policies-'));
            return platformPolicyChecks.length === 0 || platformPolicyChecks.includes('organization-policies-r');
        });

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

    it('shows Management & Schedulers, CORS, and SMTP when the user can read org settings', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'management-and-schedulers',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        renderPlatform('/management-and-schedulers');

        expect(visibleNavKeys()).toEqual(
            expect.arrayContaining(['access-management', 'authentication', 'management-and-schedulers', 'cors', 'smtp']),
        );
        expect(visibleNavKeys()).not.toContain('templates');
    });

    it('hides organization console settings nav items without organization-settings-r', () => {
        mockUseModuleRouting.mockReturnValue({
            activeNavKey: 'access-management',
            navigateToKey: jest.fn(),
            rootPath: '/platform',
        });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('organization-settings-r'));
        renderPlatform('/access-management');

        expect(visibleNavKeys()).not.toContain('management-and-schedulers');
        expect(visibleNavKeys()).not.toContain('cors');
        expect(visibleNavKeys()).not.toContain('smtp');
        expect(visibleNavKeys()).toContain('access-management');
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
});
