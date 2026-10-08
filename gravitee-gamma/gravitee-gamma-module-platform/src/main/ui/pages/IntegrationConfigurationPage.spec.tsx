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

import { dataTableHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { IntegrationConfigurationPage } from './IntegrationConfigurationPage';
import { useIntegrationGroupMembers, type IntegrationGroupMembersView } from '../features/integrations/hooks/useIntegrationGroupMembers';
import { useIntegrationMembers } from '../features/integrations/hooks/useIntegrationMembers';
import { getIntegration } from '../features/integrations/services/integrationDetail';
import { getIntegrationPermissions } from '../features/integrations/services/integrationPermissions';
import type { IntegrationMember } from '../features/integrations/types/integrationMembers';
import {
    INTEGRATION_DEFINITION_DELETE_PERMISSION,
    INTEGRATION_DEFINITION_READ_PERMISSION,
    INTEGRATION_DEFINITION_UPDATE_PERMISSION,
    INTEGRATION_MEMBER_READ_PERMISSION,
} from '../features/integrations/utils/integrationPermissions';
import { ApimApiError } from '../shared/api/apimClient';
import { notify } from '../shared/notify';

jest.mock('../features/integrations/services/integrationDetail', () => ({ getIntegration: jest.fn() }));
jest.mock('../features/integrations/services/integrationPermissions', () => ({ getIntegrationPermissions: jest.fn() }));
jest.mock('../features/integrations/components/IntegrationDangerZone', () => ({
    IntegrationDangerZone: ({ integrationId }: { integrationId: string }) => <section>Danger Zone of {integrationId}</section>,
}));
jest.mock('../features/integrations/hooks/useIntegrationMembers', () => ({ useIntegrationMembers: jest.fn() }));
jest.mock('../features/integrations/hooks/useIntegrationGroupMembers', () => ({ useIntegrationGroupMembers: jest.fn() }));
jest.mock('../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() } }));

const mockGetIntegration = jest.mocked(getIntegration);
const mockGetIntegrationPermissions = jest.mocked(getIntegrationPermissions);
const mockNotifyError = jest.mocked(notify.error);
const mockUseIntegrationMembers = jest.mocked(useIntegrationMembers);
const mockUseIntegrationGroupMembers = jest.mocked(useIntegrationGroupMembers);

function directMembersResult(members: IntegrationMember[]): ReturnType<typeof useIntegrationMembers> {
    return { data: members, isLoading: false, isError: false, error: null } as unknown as ReturnType<typeof useIntegrationMembers>;
}

function groupMembersResult(views: IntegrationGroupMembersView[]): ReturnType<typeof useIntegrationGroupMembers> {
    return { views, isLoading: false };
}

beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn(),
        })),
    });
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

function LocationProbe() {
    return <span data-testid="location">{useLocation().pathname}</span>;
}

function renderPage(initialPath = '/integrations/int-1/configuration') {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialPath]}>
                <LocationProbe />
                <Routes>
                    <Route path="/integrations">
                        <Route index element={<p>Integrations list</p>} />
                        <Route path=":integrationId/configuration" element={<IntegrationConfigurationPage />}>
                            <Route index />
                            <Route path="members" />
                        </Route>
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return queryClient;
}

async function waitForIntegrationAndPermissionsToLoad() {
    await waitFor(() => expect(mockGetIntegration).toHaveBeenCalledWith('DEFAULT', 'int-1'));
    await waitFor(() => expect(mockGetIntegrationPermissions).toHaveBeenCalledWith('DEFAULT', 'int-1'));
    await act(() => new Promise(resolve => setTimeout(resolve, 0)));
}

describe('IntegrationConfigurationPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockGetIntegration.mockResolvedValue({ id: 'int-1', name: 'Old name', provider: 'solace' });
        mockUseIntegrationMembers.mockReturnValue(directMembersResult([]));
        mockUseIntegrationGroupMembers.mockReturnValue(groupMembersResult([]));
    });

    it('lists the integration direct members on the User Permissions tab in the order the members listing returns them', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_MEMBER_READ_PERMISSION]);
        mockUseIntegrationMembers.mockReturnValue(
            directMembersResult([
                { id: 'user-2', displayName: 'Charlie', roles: [{ name: 'USER', scope: 'INTEGRATION' }] },
                { id: 'user-3', displayName: 'Alice', roles: [{ name: 'OWNER', scope: 'INTEGRATION' }] },
                { id: 'user-1', displayName: 'Bob', roles: [{ name: 'USER', scope: 'INTEGRATION' }] },
            ]),
        );

        renderPage('/integrations/int-1/configuration/members');
        await screen.findByRole('table');

        const names = dataTableHarness()
            .getRows()
            .map(row => row.getCellText('Name'));

        expect(names).toEqual([expect.stringContaining('Charlie'), expect.stringContaining('Alice'), expect.stringContaining('Bob')]);
    });

    it("lists each inherited member of an associated group with the member's integration role on the User Permissions tab", async () => {
        mockGetIntegration.mockResolvedValue({ id: 'int-1', name: 'Old name', provider: 'solace', groups: ['grp-a'] });
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_MEMBER_READ_PERMISSION]);
        mockUseIntegrationGroupMembers.mockReturnValue(
            groupMembersResult([
                {
                    group: { id: 'grp-a', name: 'Group A' },
                    status: 'loaded',
                    members: [
                        { id: 'user-1', displayName: 'Alice', roles: { GROUP: 'ADMIN', INTEGRATION: 'OWNER' } },
                        { id: 'user-2', displayName: 'Bob', roles: { GROUP: 'ADMIN', INTEGRATION: 'USER' } },
                    ],
                },
            ]),
        );

        renderPage('/integrations/int-1/configuration/members');
        await screen.findByRole('table');

        const rows = dataTableHarness()
            .getRows()
            .map(row => [row.getCellText('Name'), row.getCellText('Role')]);

        expect(screen.getByText('Group A')).toBeInTheDocument();
        expect(rows).toEqual([
            [expect.stringContaining('Alice'), 'OWNER'],
            [expect.stringContaining('Bob'), 'USER'],
        ]);
    });

    it.each([
        [
            'lists',
            [INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_UPDATE_PERMISSION, INTEGRATION_MEMBER_READ_PERMISSION],
            true,
        ],
        ['does not list', [INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_UPDATE_PERMISSION], false],
    ])('%s a User Permissions tab next to General for a user who can update the integration', async (_case, permissions, listed) => {
        mockGetIntegrationPermissions.mockResolvedValue(permissions);

        renderPage();
        await waitForIntegrationAndPermissionsToLoad();

        expect(screen.getByRole('tab', { name: 'General' })).toBeInTheDocument();
        expect(screen.queryByRole('tab', { name: 'User Permissions' }) !== null).toBe(listed);
    });

    it('lists no General tab, only User Permissions, for a user who can read the integration members but neither update nor delete it', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_MEMBER_READ_PERMISSION]);

        renderPage();

        expect(await screen.findByRole('tab', { name: 'User Permissions' })).toBeInTheDocument();
        expect(screen.queryByRole('tab', { name: 'General' })).toBeNull();
    });

    it.each([
        [
            'User Permissions',
            'Configuration',
            'can update the integration but not read its members',
            '/integrations/int-1/configuration/members',
            [INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_UPDATE_PERMISSION],
            '/integrations/int-1/configuration',
        ],
        [
            'User Permissions',
            'Configuration',
            'can delete the integration but not read its members',
            '/integrations/int-1/configuration/members',
            [INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_DELETE_PERMISSION],
            '/integrations/int-1/configuration',
        ],
        [
            'Configuration',
            'User Permissions',
            'can read the integration members but neither update nor delete it',
            '/integrations/int-1/configuration',
            [INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_MEMBER_READ_PERMISSION],
            '/integrations/int-1/configuration/members',
        ],
    ])('redirects a direct %s visit to the %s URL when the user %s', async (_from, _to, _who, initialPath, permissions, expectedPath) => {
        mockGetIntegrationPermissions.mockResolvedValue(permissions);

        renderPage(initialPath);

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe(expectedPath));
    });

    it('moves between the General and User Permissions URLs as the user switches tabs', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([
            INTEGRATION_DEFINITION_READ_PERMISSION,
            INTEGRATION_DEFINITION_UPDATE_PERMISSION,
            INTEGRATION_MEMBER_READ_PERMISSION,
        ]);
        const user = userEvent.setup();

        renderPage();
        await user.click(await screen.findByRole('tab', { name: 'User Permissions' }));

        expect(screen.getByTestId('location').textContent).toBe('/integrations/int-1/configuration/members');
        expect(screen.getByRole('tab', { name: 'User Permissions' })).toHaveAttribute('aria-selected', 'true');

        await user.click(screen.getByRole('tab', { name: 'General' }));

        expect(screen.getByTestId('location').textContent).toBe('/integrations/int-1/configuration');
        expect(screen.getByRole('tab', { name: 'General' })).toHaveAttribute('aria-selected', 'true');
    });

    it('shows the Name field on the General tab to a user who can update the integration', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_UPDATE_PERMISSION]);

        renderPage();

        expect(await screen.findByRole('textbox', { name: /^Name/ })).toHaveValue('Old name');
    });

    it('shows no Name field on the General tab to a user who can delete but not update the integration', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_DELETE_PERMISSION]);

        renderPage();
        await waitForIntegrationAndPermissionsToLoad();

        expect(screen.getByRole('tabpanel', { name: 'General' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: /^Name/ })).not.toBeInTheDocument();
    });

    it('shows the Danger Zone of the integration on the General tab when the user can delete the integration', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_DELETE_PERMISSION]);

        renderPage();

        expect(screen.getByRole('tab', { name: 'General' })).toBeInTheDocument();
        expect(await screen.findByText('Danger Zone of int-1')).toBeInTheDocument();
        expect(mockGetIntegrationPermissions).toHaveBeenCalledWith('DEFAULT', 'int-1');
    });

    it('shows no Danger Zone on the General tab when the user can update but not delete the integration', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_UPDATE_PERMISSION]);

        renderPage();
        await waitForIntegrationAndPermissionsToLoad();

        expect(screen.getByRole('tab', { name: 'General' })).toBeInTheDocument();
        expect(screen.queryByText(/Danger Zone/)).toBeNull();
    });

    it('redirects to the Integrations list without a toast when the integration is an A2A one', async () => {
        mockGetIntegration.mockResolvedValue({ id: 'int-1', name: 'Agent Bridge', provider: 'A2A' });
        mockGetIntegrationPermissions.mockResolvedValue([
            INTEGRATION_DEFINITION_READ_PERMISSION,
            INTEGRATION_DEFINITION_UPDATE_PERMISSION,
            INTEGRATION_DEFINITION_DELETE_PERMISSION,
        ]);

        renderPage();

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations'));
        expect(screen.getByText('Integrations list')).toBeInTheDocument();
        expect(screen.queryByTestId('integration-configuration-page')).toBeNull();
        expect(screen.queryByRole('textbox', { name: /^Name/ })).toBeNull();
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it('shows only the load failure message, with no Name field or Danger Zone, when the integration fails to load', async () => {
        const failure = new Error('integration unavailable');
        mockGetIntegration.mockRejectedValue(failure);
        mockGetIntegrationPermissions.mockResolvedValue([
            INTEGRATION_DEFINITION_READ_PERMISSION,
            INTEGRATION_DEFINITION_UPDATE_PERMISSION,
            INTEGRATION_DEFINITION_DELETE_PERMISSION,
        ]);

        renderPage();

        expect(await screen.findByText('Integration could not be loaded. Please refresh and try again.')).toBeInTheDocument();
        expect(mockNotifyError).toHaveBeenCalledWith(failure, 'Integration could not be loaded. Please refresh and try again.');
        await waitForIntegrationAndPermissionsToLoad();
        expect(screen.queryByRole('textbox', { name: /^Name/ })).toBeNull();
        expect(screen.queryByText(/Danger Zone/)).toBeNull();
    });

    it('shows the load failure message in place of the Group Inherited Members on the User Permissions tab when the integration fails to load', async () => {
        mockGetIntegration.mockRejectedValue(new Error('integration unavailable'));
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_MEMBER_READ_PERMISSION]);

        renderPage('/integrations/int-1/configuration/members');

        expect(await screen.findByText('Integration could not be loaded. Please refresh and try again.')).toBeInTheDocument();
        await waitForIntegrationAndPermissionsToLoad();
        expect(screen.queryByText('Group Inherited Members')).toBeNull();
    });

    it('keeps the Group Inherited Members on the User Permissions tab when a background refetch of the loaded integration fails', async () => {
        mockGetIntegration.mockResolvedValue({ id: 'int-1', name: 'Old name', provider: 'solace', groups: ['grp-a'] });
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_MEMBER_READ_PERMISSION]);
        const queryClient = renderPage('/integrations/int-1/configuration/members');
        expect(await screen.findByText('Group Inherited Members')).toBeInTheDocument();

        const failure = new Error('integration unavailable');
        mockGetIntegration.mockRejectedValueOnce(failure);
        await act(() => queryClient.refetchQueries());
        await waitFor(() =>
            expect(mockNotifyError).toHaveBeenCalledWith(failure, 'Integration could not be loaded. Please refresh and try again.'),
        );

        expect(screen.getByText('Group Inherited Members')).toBeInTheDocument();
        expect(screen.queryByText('Integration could not be loaded. Please refresh and try again.')).toBeNull();
    });

    it('redirects to the Integrations list without a toast when the integration request is forbidden', async () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        mockGetIntegration.mockRejectedValue(new ApimApiError(403, 'Forbidden'));
        mockGetIntegrationPermissions.mockResolvedValue([
            INTEGRATION_DEFINITION_READ_PERMISSION,
            INTEGRATION_DEFINITION_UPDATE_PERMISSION,
            INTEGRATION_DEFINITION_DELETE_PERMISSION,
        ]);

        renderPage();

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations'));
        expect(screen.getByText('Integrations list')).toBeInTheDocument();
        expect(screen.queryByTestId('integration-configuration-page')).toBeNull();
        expect(mockNotifyError).not.toHaveBeenCalled();
        warn.mockRestore();
    });

    it('shows a loading skeleton and no Name field or Danger Zone while the integration is loading', async () => {
        mockGetIntegration.mockReturnValue(new Promise(() => {}));
        mockGetIntegrationPermissions.mockResolvedValue([
            INTEGRATION_DEFINITION_READ_PERMISSION,
            INTEGRATION_DEFINITION_UPDATE_PERMISSION,
            INTEGRATION_DEFINITION_DELETE_PERMISSION,
        ]);

        renderPage();
        await waitFor(() => expect(mockGetIntegrationPermissions).toHaveBeenCalledWith('DEFAULT', 'int-1'));
        await act(() => new Promise(resolve => setTimeout(resolve, 0)));

        expect(document.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
        expect(screen.queryByRole('textbox', { name: /^Name/ })).toBeNull();
        expect(screen.queryByText(/Danger Zone/)).toBeNull();
    });

    it('shows the direct members but no Group Inherited Members on the User Permissions tab while the integration is loading', async () => {
        mockGetIntegration.mockReturnValue(new Promise(() => {}));
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_MEMBER_READ_PERMISSION]);
        mockUseIntegrationMembers.mockReturnValue(
            directMembersResult([{ id: 'user-1', displayName: 'Alice', roles: [{ name: 'OWNER', scope: 'INTEGRATION' }] }]),
        );

        renderPage('/integrations/int-1/configuration/members');
        await screen.findByRole('table');

        const names = dataTableHarness()
            .getRows()
            .map(row => row.getCellText('Name'));

        expect(names).toEqual([expect.stringContaining('Alice')]);
        expect(screen.queryByText('Group Inherited Members')).toBeNull();
        expect(mockUseIntegrationGroupMembers).not.toHaveBeenCalled();
    });
});
