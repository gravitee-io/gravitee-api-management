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

jest.mock('../pages/SharedPolicyGroupDetailPage', () => ({
    SharedPolicyGroupDetailPage: () => <div data-testid="shared-policy-group-detail-page" />,
}));

jest.mock('../pages/RegisterApplicationPage', () => ({
    RegisterApplicationPage: () => <div data-testid="register-application-page" />,
}));

jest.mock('../features/applications/components/detail', () => ({
    ApplicationDetailLayout: () => <div data-testid="application-detail-layout" />,
    ApplicationDetailIndexRedirect: () => null,
}));

jest.mock('../pages/ApplicationDetailSubscriptionPage', () => ({
    ApplicationDetailSubscriptionPage: () => null,
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

    it('routes to the shared policy group detail page under the platform module', () => {
        render(
            <MemoryRouter initialEntries={['/shared-policy-groups/spg-1']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('shared-policy-group-detail-page')).not.toBeNull();
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
});
