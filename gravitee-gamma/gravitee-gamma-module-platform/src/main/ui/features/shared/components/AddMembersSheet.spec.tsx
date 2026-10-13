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
import { useQuery } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AddMembersSheet } from './AddMembersSheet';
import type { SearchableUser } from '../../../shared/types/userSearch';

jest.mock('@tanstack/react-query', () => ({
    ...jest.requireActual('@tanstack/react-query'),
    useQuery: jest.fn(),
}));

const mockUseQuery = jest.mocked(useQuery);

const DESCRIPTION = 'Search for users by name or email and add them to this application.';
const SEARCH_PLACEHOLDER = 'Search a user by name or email…';
const ALICE: SearchableUser = { id: 'user-alice', reference: 'ref-alice', displayName: 'Alice Martin', email: 'alice@example.com' };
const BOB: SearchableUser = { id: 'user-bob', reference: 'ref-bob', displayName: 'Bob Reader', email: 'bob@example.com' };

type UserEvent = ReturnType<typeof userEvent.setup>;

function renderSheet(open = true, roles = ['USER'], defaultRole?: string) {
    const onClose = jest.fn();
    const onAdd = jest.fn();
    const sheet = (isOpen: boolean) => (
        <AddMembersSheet
            open={isOpen}
            description={DESCRIPTION}
            roles={roles}
            defaultRole={defaultRole}
            existingMembers={[]}
            onClose={onClose}
            onAdd={onAdd}
            isAdding={false}
        />
    );
    const { rerender } = render(sheet(open));
    return { onClose, onAdd, setOpen: (isOpen: boolean) => rerender(sheet(isOpen)) };
}

function searchResultsAre(users: SearchableUser[]) {
    mockUseQuery.mockReturnValue({ data: users, isFetching: false } as ReturnType<typeof useQuery>);
}

async function searchFor(user: UserEvent, query: string) {
    await user.type(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), query);
}

async function selectUser(user: UserEvent, query: string, displayName: string) {
    await searchFor(user, query);
    await user.click(await screen.findByRole('button', { name: new RegExp(displayName) }));
}

async function chooseRole(user: UserEvent, roleName: string) {
    await user.click(screen.getByRole('combobox'));
    await user.click(await screen.findByRole('option', { name: roleName }));
}

describe('AddMembersSheet', () => {
    beforeAll(() => {
        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
        Element.prototype.releasePointerCapture = jest.fn();
        Element.prototype.scrollIntoView = jest.fn();
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    beforeEach(() => {
        mockUseQuery.mockReturnValue({ data: [], isFetching: false } as ReturnType<typeof useQuery>);
    });

    it('does not show sheet content when closed', () => {
        renderSheet(false);
        expect(screen.queryByText(DESCRIPTION)).toBeNull();
    });

    it('shows the title and the description it is given', () => {
        renderSheet(true);
        expect(screen.getByRole('heading', { name: 'Add Members' })).toBeInTheDocument();
        expect(screen.getByText(DESCRIPTION)).toBeInTheDocument();
    });

    it('invokes onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet(true);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('adds every selected user except the ones whose chip was removed', async () => {
        searchResultsAre([ALICE, BOB]);
        const user = userEvent.setup();
        const { onAdd } = renderSheet(true);

        await selectUser(user, 'Al', ALICE.displayName);
        await selectUser(user, 'Bo', BOB.displayName);

        expect(screen.getByText('2 users selected')).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Add 2 members' }));
        expect(onAdd).toHaveBeenNthCalledWith(1, [ALICE, BOB], 'USER');

        await user.click(screen.getByRole('button', { name: `Remove ${ALICE.displayName}` }));

        expect(screen.getByText('1 user selected')).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Add member' }));
        expect(onAdd).toHaveBeenCalledTimes(2);
        expect(onAdd).toHaveBeenNthCalledWith(2, [BOB], 'USER');
    });

    it('adds the selected users with the first role when no role is chosen', async () => {
        searchResultsAre([ALICE]);
        const user = userEvent.setup();
        const { onAdd } = renderSheet(true, ['OWNER', 'USER']);

        await selectUser(user, 'Al', ALICE.displayName);
        await user.click(screen.getByRole('button', { name: 'Add member' }));

        expect(onAdd).toHaveBeenCalledTimes(1);
        expect(onAdd).toHaveBeenCalledWith([ALICE], 'OWNER');
    });

    it('adds the selected users with the role the user picked rather than the default role', async () => {
        searchResultsAre([ALICE]);
        const user = userEvent.setup();
        const { onAdd } = renderSheet(true, ['OWNER', 'USER'], 'USER');

        await selectUser(user, 'Al', ALICE.displayName);
        await chooseRole(user, 'OWNER');
        await user.click(screen.getByRole('button', { name: 'Add member' }));

        expect(onAdd).toHaveBeenCalledTimes(1);
        expect(onAdd).toHaveBeenCalledWith([ALICE], 'OWNER');
    });

    it('tells the user when the search matches nobody', async () => {
        const user = userEvent.setup();
        renderSheet(true);

        await searchFor(user, 'Al');

        expect(await screen.findByText('No users found.')).toBeInTheDocument();
    });

    it('clears the search, selection and role when the sheet is closed and reopened', async () => {
        searchResultsAre([ALICE]);
        const user = userEvent.setup();
        const { onAdd, setOpen } = renderSheet(true, ['OWNER', 'USER']);
        await selectUser(user, 'Al', ALICE.displayName);
        await chooseRole(user, 'USER');
        await searchFor(user, 'Al');

        setOpen(false);
        setOpen(true);

        expect(screen.queryByText('1 user selected')).toBeNull();
        expect(screen.getByPlaceholderText(SEARCH_PLACEHOLDER)).toHaveValue('');
        expect(screen.getByRole('button', { name: 'Add member' })).toBeDisabled();

        await selectUser(user, 'Al', ALICE.displayName);
        await user.click(screen.getByRole('button', { name: 'Add member' }));
        expect(onAdd).toHaveBeenCalledWith([ALICE], 'OWNER');
    });
});
