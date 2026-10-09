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

import { type GroupOwnerField, type PrimaryOwnerMode, TransferOwnership } from './TransferOwnership';
import { searchUsers } from '../../services/members';
import type { Group, Member } from '../../types/members.types';

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

function renderSheet({
    members = [],
    groups = [],
    currentPrimaryOwnerId,
    primaryOwnerMode = 'USER',
    groupOwnerField = 'apiPrimaryOwner',
    scopeLabel = 'API',
    onTransfer,
}: {
    members?: Member[];
    groups?: Group[];
    currentPrimaryOwnerId?: string;
    primaryOwnerMode?: PrimaryOwnerMode | string;
    groupOwnerField?: GroupOwnerField;
    scopeLabel?: string;
    onTransfer?: jest.Mock;
} = {}) {
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <TransferOwnership
                open
                members={members}
                roles={['OWNER', 'USER']}
                onClose={jest.fn()}
                onTransfer={onTransfer ?? jest.fn()}
                isTransferring={false}
                groups={groups}
                currentPrimaryOwnerId={currentPrimaryOwnerId}
                primaryOwnerMode={primaryOwnerMode}
                groupOwnerField={groupOwnerField}
                scopeLabel={scopeLabel}
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

        await userEvent.setup().click(screen.getByRole('button', { name: 'Other user' }));

        expect(screen.queryByText('This action is irreversible')).toBeNull();
    });

    it('uses the same user-search placeholder as the console', async () => {
        renderSheet();

        await userEvent.setup().click(screen.getByRole('button', { name: 'Other user' }));

        expect(screen.getByPlaceholderText('Search a user by name or email…')).toBeInTheDocument();
    });

    it('shows the alert once an API member is picked', async () => {
        const user = userEvent.setup();
        renderSheet({ members: [{ id: 'member-1', displayName: 'Jane Doe', roles: [{ name: 'USER', scope: 'API' }] }] });

        await user.click(screen.getAllByRole('combobox')[0]);
        await user.click(await screen.findByRole('option', { name: 'Jane Doe' }));

        expect(screen.getByText('This action is irreversible')).toBeInTheDocument();
    });

    it('shows the alert once another user is picked from the search results', async () => {
        mockSearchUsers.mockResolvedValue([{ id: 'user-1', reference: 'ref-1', displayName: 'John Smith', email: 'john@example.com' }]);
        const user = userEvent.setup();
        renderSheet();

        await user.click(screen.getByRole('button', { name: 'Other user' }));
        await user.type(screen.getByPlaceholderText('Search a user by name or email…'), 'jo');
        await user.click(await screen.findByText('John Smith'));

        expect(screen.getByText('This action is irreversible')).toBeInTheDocument();
    });

    it('hides the primary-owner-group tab in USER mode', () => {
        renderSheet({ primaryOwnerMode: 'USER' });
        expect(screen.queryByRole('button', { name: 'Primary owner group' })).toBeNull();
        expect(screen.getByRole('button', { name: 'API member' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Other user' })).toBeInTheDocument();
    });

    it('shows the primary-owner-group tab in HYBRID mode', () => {
        renderSheet({ primaryOwnerMode: 'HYBRID' });
        expect(screen.getByRole('button', { name: 'Primary owner group' })).toBeInTheDocument();
    });

    it('treats lowercase primaryOwnerMode like classic (toUpperCase)', () => {
        renderSheet({ primaryOwnerMode: 'hybrid' });
        expect(screen.getByRole('button', { name: 'Primary owner group' })).toBeInTheDocument();
    });

    it('shows only the group picker in GROUP mode', () => {
        renderSheet({ primaryOwnerMode: 'GROUP' });
        expect(screen.queryByRole('button', { name: 'API member' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Other user' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Primary owner group' })).toBeNull();
        expect(screen.getByText('Select a primary owner group')).toBeInTheDocument();
    });

    it('warns when no group has a primary-owner member', async () => {
        const user = userEvent.setup();
        renderSheet({
            primaryOwnerMode: 'HYBRID',
            groups: [{ id: 'g1', name: 'Support', apiPrimaryOwner: null }],
        });

        await user.click(screen.getByRole('button', { name: 'Primary owner group' }));

        expect(screen.getByText(/must contain a member with a primary owner API role/i)).toBeInTheDocument();
        expect(screen.getByRole('combobox', { name: /select a primary owner group/i })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Transfer' })).toBeDisabled();
    });

    it('filters API Product groups by apiProductPrimaryOwner, not apiPrimaryOwner', async () => {
        const user = userEvent.setup();
        renderSheet({
            primaryOwnerMode: 'GROUP',
            groupOwnerField: 'apiProductPrimaryOwner',
            scopeLabel: 'API Product',
            groups: [
                { id: 'g-api', name: 'API PO Only', apiPrimaryOwner: 'member-1', apiProductPrimaryOwner: null },
                { id: 'g-product', name: 'Product PO', apiPrimaryOwner: null, apiProductPrimaryOwner: 'member-2' },
            ],
        });

        await user.click(screen.getAllByRole('combobox')[0]);
        expect(await screen.findByRole('option', { name: 'Product PO' })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: 'API PO Only' })).toBeNull();
    });

    it('warns with the API Product role message when no eligible product groups exist', () => {
        renderSheet({
            primaryOwnerMode: 'GROUP',
            groupOwnerField: 'apiProductPrimaryOwner',
            scopeLabel: 'API Product',
            groups: [{ id: 'g1', name: 'Support', apiPrimaryOwner: 'member-1', apiProductPrimaryOwner: null }],
        });

        expect(screen.getByText(/must contain a member with a primary owner API Product role/i)).toBeInTheDocument();
        expect(screen.getByRole('combobox', { name: /select a primary owner group/i })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Transfer' })).toBeDisabled();
    });

    it('only lists groups that carry a primary-owner member, excluding the current owner group', async () => {
        const user = userEvent.setup();
        const onTransfer = jest.fn();
        renderSheet({
            primaryOwnerMode: 'HYBRID',
            currentPrimaryOwnerId: 'g-owner',
            groups: [
                { id: 'g-owner', name: 'Current Owners', apiPrimaryOwner: 'member-1' },
                { id: 'g-support', name: 'Support Team', apiPrimaryOwner: 'member-2' },
                { id: 'g-readonly', name: 'Read Only', apiPrimaryOwner: null },
            ],
            onTransfer,
        });

        await user.click(screen.getByRole('button', { name: 'Primary owner group' }));
        await user.click(screen.getAllByRole('combobox')[0]);
        await user.click(await screen.findByRole('option', { name: 'Support Team' }));
        await user.click(screen.getByRole('button', { name: 'Transfer' }));

        expect(onTransfer).toHaveBeenCalledWith({ userId: 'g-support', userType: 'GROUP', poRole: 'OWNER' });
    });
});
