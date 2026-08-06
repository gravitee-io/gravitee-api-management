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
import { buttonHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AddUserGroupSheet } from './AddUserGroupSheet';
import { useEnvironmentGroups, useGroupMembershipRoleCatalog } from '../hooks/useOrganizationUser';

jest.mock('../hooks/useOrganizationUser', () => ({
    useEnvironmentGroups: jest.fn(),
    useGroupMembershipRoleCatalog: jest.fn(),
}));

const mockUseEnvironmentGroups = jest.mocked(useEnvironmentGroups);
const mockUseGroupMembershipRoleCatalog = jest.mocked(useGroupMembershipRoleCatalog);

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
    Element.prototype.scrollIntoView = jest.fn();
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
});

function renderSheet(overrides: Partial<Parameters<typeof AddUserGroupSheet>[0]> = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <AddUserGroupSheet
                open
                environmentId="DEFAULT"
                existingGroupIds={[]}
                onClose={jest.fn()}
                onSubmit={jest.fn()}
                isPending={false}
                {...overrides}
            />
        </QueryClientProvider>,
    );
}

describe('AddUserGroupSheet', () => {
    beforeEach(() => {
        mockUseEnvironmentGroups.mockReturnValue({
            data: {
                data: [
                    { id: 'group-1', name: 'Platform Admins' },
                    { id: 'group-2', name: 'Developers' },
                ],
                pagination: { page: 1, perPage: 100, pageCount: 1, pageItemsCount: 2, totalCount: 2 },
            },
            isLoading: false,
        } as ReturnType<typeof useEnvironmentGroups>);
        mockUseGroupMembershipRoleCatalog.mockReturnValue({
            data: [{ id: 'user', name: 'USER' }],
        } as ReturnType<typeof useGroupMembershipRoleCatalog>);
    });

    it('submits a group membership with at least one role', async () => {
        const user = userEvent.setup();
        const onSubmit = jest.fn();
        renderSheet({ onSubmit });

        await user.click(document.getElementById('add-user-group-id')!);
        await user.click(await screen.findByRole('option', { name: 'Platform Admins' }));
        await user.click(screen.getByRole('checkbox', { name: 'Group admin role' }));
        await buttonHarness({ name: /^Save$/ }).click();

        await waitFor(() => expect(onSubmit).toHaveBeenCalled());
        expect(onSubmit.mock.calls[0]?.[0]).toEqual({
            groupId: 'group-1',
            isGroupAdmin: true,
            apiRole: undefined,
            apiProductRole: undefined,
            applicationRole: undefined,
            integrationRole: undefined,
        });
    });

    it('filters out groups the user already belongs to', async () => {
        const user = userEvent.setup();
        renderSheet({ existingGroupIds: ['group-1'] });

        await user.click(document.getElementById('add-user-group-id')!);
        expect(await screen.findByRole('option', { name: 'Developers' })).toBeTruthy();
        expect(screen.queryByRole('option', { name: 'Platform Admins' })).toBeNull();
    });

    it('shows a message when every group is already added', async () => {
        renderSheet({ existingGroupIds: ['group-1', 'group-2'] });

        expect(await screen.findByText('All groups are already added.')).toBeTruthy();
        expect(screen.queryByRole('button', { name: /^Save$/ })).toBeNull();
    });

    it('keeps save disabled until at least one role is selected', async () => {
        const user = userEvent.setup();
        renderSheet();

        await user.click(document.getElementById('add-user-group-id')!);
        await user.click(await screen.findByRole('option', { name: 'Platform Admins' }));

        expect(screen.getByRole('button', { name: /^Save$/ })).toHaveProperty('disabled', true);
    });
});
