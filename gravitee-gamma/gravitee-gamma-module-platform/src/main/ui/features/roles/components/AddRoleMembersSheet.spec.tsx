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
jest.mock('@tanstack/react-query', () => ({
    ...jest.requireActual('@tanstack/react-query'),
    useQuery: jest.fn(),
}));

import { useQuery } from '@tanstack/react-query';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ComponentProps } from 'react';

import { AddRoleMembersSheet } from './AddRoleMembersSheet';
import { installFormActionTestEnvironment } from '../../../shared/testing/formAction';
import type { RoleMembershipListItem } from '../types/role';
import { ROLE_SEARCH_DEBOUNCE_MS } from '../utils/paginationConstants';

const mockUseQuery = jest.mocked(useQuery);

let restoreTestEnvironment: () => void;

beforeAll(() => {
    restoreTestEnvironment = installFormActionTestEnvironment();
});

afterAll(() => {
    restoreTestEnvironment();
});

const SEARCH_RESULTS = [
    { id: 'user-1', reference: 'USER', displayName: 'Jane Doe', email: 'jane@company.com' },
    { id: 'user-2', reference: 'USER', displayName: 'John Smith', email: 'john@company.com' },
];

function renderSheet(props: Partial<ComponentProps<typeof AddRoleMembersSheet>> = {}) {
    const onClose = jest.fn();
    const onAdd = jest.fn();
    render(<AddRoleMembersSheet open existingMembers={[]} onClose={onClose} onAdd={onAdd} isAdding={false} {...props} />);
    return { onClose, onAdd };
}

/** Types into the search field, then advances the debounce timer so the (mocked) query results render. */
async function searchFor(user: ReturnType<typeof userEvent.setup>, query: string) {
    await user.type(screen.getByPlaceholderText('Search a user by name or email…'), query);
    await act(async () => {
        jest.advanceTimersByTime(ROLE_SEARCH_DEBOUNCE_MS);
    });
}

describe('AddRoleMembersSheet', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        mockUseQuery.mockReturnValue({ data: SEARCH_RESULTS, isFetching: false } as ReturnType<typeof useQuery>);
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('shows the sheet title when open', () => {
        renderSheet();
        expect(screen.getByRole('heading', { name: 'Add Members' })).toBeInTheDocument();
    });

    it('cancels without adding anyone', async () => {
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        const { onClose, onAdd } = renderSheet();

        await user.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(onClose).toHaveBeenCalled();
        expect(onAdd).not.toHaveBeenCalled();
    });

    it('selects a searched user and adds them', async () => {
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        const { onAdd } = renderSheet();

        await searchFor(user, 'ja');
        await user.click(screen.getByText('Jane Doe'));

        expect(screen.getByText('1 user selected')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Add a member' }));

        expect(onAdd).toHaveBeenCalledWith([SEARCH_RESULTS[0]]);
    });

    // Classic's gio-users-selector searches from the first character; matches that instead of requiring 2+.
    it('searches from a single character', async () => {
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        renderSheet();

        await searchFor(user, 'j');

        expect(screen.getByText('Jane Doe')).toBeInTheDocument();
        expect(screen.getByText('John Smith')).toBeInTheDocument();
    });

    it('excludes users who are already members of this role', async () => {
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        const existingMembers: RoleMembershipListItem[] = [{ id: 'user-1', displayName: 'Jane Doe' }];
        renderSheet({ existingMembers });

        await searchFor(user, 'ja');

        expect(screen.queryByText('Jane Doe')).not.toBeInTheDocument();
        expect(screen.getByText('John Smith')).toBeInTheDocument();
    });

    it('disables Add a member until someone is selected', async () => {
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        renderSheet();

        expect(screen.getByRole('button', { name: 'Add a member' })).toBeDisabled();

        await searchFor(user, 'ja');
        await user.click(screen.getByText('Jane Doe'));

        expect(screen.getByRole('button', { name: 'Add a member' })).toBeEnabled();
    });
});
