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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { TransferOwnership } from './TransferOwnership';
import { searchUsers } from '../../services/members';
import type { Member } from '../../types/members.types';

jest.mock('../../services/members', () => ({ searchUsers: jest.fn().mockResolvedValue([]) }));

const mockSearchUsers = searchUsers as jest.Mock;

// jsdom ships no ResizeObserver, which the sheet and its select mount.
globalThis.ResizeObserver ??= class {
    observe() {}
    unobserve() {}
    disconnect() {}
};

// ...nor the pointer-capture and scrolling APIs Radix's select calls when it opens.
Element.prototype.hasPointerCapture ??= () => false;
Element.prototype.setPointerCapture ??= () => {};
Element.prototype.releasePointerCapture ??= () => {};
Element.prototype.scrollIntoView ??= () => {};

function renderSheet(members: Member[] = []) {
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <TransferOwnership
                open
                members={members}
                roles={['OWNER', 'USER']}
                onClose={jest.fn()}
                onTransfer={jest.fn()}
                isTransferring={false}
            />
        </QueryClientProvider>,
    );
}

describe('TransferOwnership', () => {
    it('does not show the irreversible-action alert before a new owner is picked', () => {
        renderSheet();

        expect(screen.queryByText('This action is irreversible')).toBeNull();
    });

    it('keeps the alert hidden on the Other User tab until a user is selected', async () => {
        renderSheet();

        await userEvent.setup().click(screen.getByRole('button', { name: 'Other User' }));

        expect(screen.queryByText('This action is irreversible')).toBeNull();
    });

    it('uses the same user-search placeholder as the console', async () => {
        renderSheet();

        await userEvent.setup().click(screen.getByRole('button', { name: 'Other User' }));

        expect(screen.getByPlaceholderText('Search a user by name or email…')).toBeInTheDocument();
    });

    it('shows the alert once an API member is picked', async () => {
        const user = userEvent.setup();
        renderSheet([{ id: 'member-1', displayName: 'Jane Doe', roles: [{ name: 'USER', scope: 'API' }] }]);

        await user.click(screen.getAllByRole('combobox')[0]);
        await user.click(await screen.findByRole('option', { name: 'Jane Doe' }));

        expect(screen.getByText('This action is irreversible')).toBeInTheDocument();
    });

    it('shows the alert once another user is picked from the search results', async () => {
        mockSearchUsers.mockResolvedValue([{ id: 'user-1', reference: 'ref-1', displayName: 'John Smith', email: 'john@example.com' }]);
        const user = userEvent.setup();
        renderSheet();

        await user.click(screen.getByRole('button', { name: 'Other User' }));
        await user.type(screen.getByPlaceholderText('Search a user by name or email…'), 'jo');
        await user.click(await screen.findByText('John Smith'));

        expect(screen.getByText('This action is irreversible')).toBeInTheDocument();
    });
});
