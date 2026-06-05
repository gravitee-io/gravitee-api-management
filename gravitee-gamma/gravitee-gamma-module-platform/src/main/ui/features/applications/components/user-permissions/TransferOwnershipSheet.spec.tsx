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

import { TransferOwnershipSheet } from './TransferOwnershipSheet';
import type { ApplicationUiMember, SearchableUser } from '../../types/applicationMembers.types';
import { querySheetHeading } from '../test/sheetSpecHelpers';

jest.mock('@tanstack/react-query', () => ({
    ...jest.requireActual('@tanstack/react-query'),
    useQuery: jest.fn(),
}));

const mockUseQuery = jest.mocked(useQuery);

const members: ApplicationUiMember[] = [
    {
        id: 'owner-1',
        displayName: 'Owner',
        roles: [{ name: 'PRIMARY_OWNER', scope: 'APPLICATION' }],
    },
    {
        id: 'member-1',
        displayName: 'Abhishek Kumar',
        email: 'abhishek.kumar@graviteesource.com',
        roles: [{ name: 'USER', scope: 'APPLICATION' }],
    },
];

function renderSheet(open = true) {
    const onClose = jest.fn();
    const onTransfer = jest.fn();
    render(
        <TransferOwnershipSheet
            open={open}
            members={members}
            roles={['PRIMARY_OWNER', 'USER']}
            onClose={onClose}
            onTransfer={onTransfer}
            isTransferring={false}
        />,
    );
    return { onClose, onTransfer };
}

describe('TransferOwnershipSheet', () => {
    beforeEach(() => {
        mockUseQuery.mockReturnValue({ data: [], isFetching: false } as ReturnType<typeof useQuery>);
    });

    it('does not show sheet content when closed', () => {
        renderSheet(false);
        expect(querySheetHeading('Transfer ownership')).toBeNull();
    });

    it('shows sheet title when open', () => {
        renderSheet(true);
        expect(screen.getByRole('heading', { name: 'Transfer ownership' })).not.toBeNull();
    });

    it('invokes onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet(true);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('renders a capped scrollable container when many users match search', () => {
        const manyUsers: SearchableUser[] = Array.from({ length: 20 }, (_, index) => ({
            reference: `ref-${index}`,
            id: `id-${index}`,
            displayName: `User ${index}`,
            email: `user${index}@example.com`,
        }));
        mockUseQuery.mockReturnValue({ data: manyUsers, isFetching: false } as ReturnType<typeof useQuery>);

        renderSheet(true);
        fireEvent.click(screen.getByRole('button', { name: 'Other user' }));
        fireEvent.change(screen.getByPlaceholderText(/Type at least 2 characters/i), { target: { value: 'user' } });

        const results = screen.getByTestId('transfer-ownership-user-search-results');
        expect(results.className).toContain('max-h-48');
        expect(results.className).toContain('overflow-y-auto');
        expect(screen.getAllByRole('button', { name: /User \d+/ }).length).toBe(20);
    });
});
