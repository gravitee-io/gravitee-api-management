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
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { IntegrationDirectMembers } from './IntegrationDirectMembers';
import { PlatformToaster } from '../../../app/PlatformToaster';
import { ApimApiError } from '../../../shared/api/apimClient';
import { searchUsers } from '../../../shared/services/userSearch';
import type { SearchableUser } from '../../../shared/types/userSearch';
import {
    addIntegrationMember,
    listIntegrationMembers,
    listIntegrationRoles,
    removeIntegrationMember,
    updateIntegrationMemberRole,
} from '../services/integrationMembers';
import type { AddIntegrationMember, IntegrationMember, IntegrationRole } from '../types/integrationMembers';

jest.mock('../services/integrationMembers', () => ({
    listIntegrationMembers: jest.fn(),
    listIntegrationRoles: jest.fn(),
    addIntegrationMember: jest.fn(),
    updateIntegrationMemberRole: jest.fn(),
    removeIntegrationMember: jest.fn(),
}));
jest.mock('../../../shared/services/userSearch', () => ({ searchUsers: jest.fn() }));

const mockListIntegrationMembers = jest.mocked(listIntegrationMembers);
const mockListIntegrationRoles = jest.mocked(listIntegrationRoles);
const mockAddIntegrationMember = jest.mocked(addIntegrationMember);
const mockUpdateIntegrationMemberRole = jest.mocked(updateIntegrationMemberRole);
const mockRemoveIntegrationMember = jest.mocked(removeIntegrationMember);
const mockSearchUsers = jest.mocked(searchUsers);

const JANE_OWNER: IntegrationMember = {
    id: 'user-jane',
    displayName: 'Jane Owner',
    roles: [{ name: 'PRIMARY_OWNER', scope: 'INTEGRATION' }],
};
const BOB_READER: IntegrationMember = { id: 'user-bob', displayName: 'Bob Reader', roles: [{ name: 'USER', scope: 'INTEGRATION' }] };
const JANE_DOE: IntegrationMember = { id: 'user-jane-doe', displayName: 'Jane Doe', roles: [{ name: 'USER', scope: 'INTEGRATION' }] };
const JANE_OWNER_AND_BOB_READER_ROWS = [
    [expect.stringContaining('Jane Owner'), 'Primary Owner'],
    [expect.stringContaining('Bob Reader'), 'USER'],
];

type UserEvent = ReturnType<typeof userEvent.setup>;

function renderSection({ canCreateMembers = false, canUpdateMembers = false, canDeleteMembers = false } = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <IntegrationDirectMembers
                integrationId="int-1"
                canCreateMembers={canCreateMembers}
                canUpdateMembers={canUpdateMembers}
                canDeleteMembers={canDeleteMembers}
            />
            <PlatformToaster />
        </QueryClientProvider>,
    );
}

async function openAddMembersSheet(user: UserEvent) {
    await user.click(screen.getByRole('button', { name: 'Add members' }));
    return screen.findByRole('dialog');
}

async function searchInSheet(user: UserEvent, sheet: HTMLElement, query: string) {
    await user.type(within(sheet).getByPlaceholderText('Search a user by name or email…'), query);
}

async function addMember(user: UserEvent, query: string, displayName: string, roleName: string) {
    const sheet = await openAddMembersSheet(user);
    await searchInSheet(user, sheet, query);
    await user.click(await within(sheet).findByRole('button', { name: new RegExp(displayName) }));
    await user.click(within(sheet).getByRole('combobox'));
    await user.click(await screen.findByRole('option', { name: roleName }));
    await user.click(within(sheet).getByRole('button', { name: 'Add member' }));
}

function directMemberRows() {
    return dataTableHarness()
        .getRows()
        .map(row => [row.getCellText('Name'), row.getCellText('Role')]);
}

function memberRow(displayName: string) {
    return dataTableHarness().getRow(displayName).getElement();
}

function memberActionsTrigger(displayName: string) {
    return within(memberRow(displayName)).queryByRole('button', { name: 'Member actions' });
}

async function openMemberActions(user: UserEvent, displayName: string) {
    await user.click(within(memberRow(displayName)).getByRole('button', { name: 'Member actions' }));
    return screen.findByRole('menu');
}

async function startEditingRole(user: UserEvent, displayName: string) {
    await openMemberActions(user, displayName);
    await user.click(await screen.findByRole('menuitem', { name: 'Edit role' }));
}

async function openRoleSelector(user: UserEvent, displayName: string) {
    await user.click(within(memberRow(displayName)).getByRole('combobox'));
}

async function selectRole(user: UserEvent, displayName: string, roleName: string) {
    await openRoleSelector(user, displayName);
    await user.click(await screen.findByRole('option', { name: roleName }));
}

function saveButton(displayName: string) {
    return within(memberRow(displayName)).getByRole('button', { name: 'Save' });
}

async function changeRole(user: UserEvent, displayName: string, roleName: string) {
    await startEditingRole(user, displayName);
    await selectRole(user, displayName, roleName);
    await user.click(saveButton(displayName));
}

async function openRemoveMemberDialog(user: UserEvent, displayName: string) {
    await openMemberActions(user, displayName);
    await user.click(await screen.findByRole('menuitem', { name: 'Remove member' }));
    return screen.findByRole('dialog');
}

async function confirmRemoval(user: UserEvent, displayName: string) {
    const dialog = await openRemoveMemberDialog(user, displayName);
    await user.click(within(dialog).getByRole('button', { name: 'Remove' }));
}

// The open confirmation dialog hides the rest of the page from role queries, so rows are read with hidden included.
function isListed(displayName: string) {
    return within(screen.getByRole('table', { hidden: true })).queryByRole('row', { name: new RegExp(displayName), hidden: true }) !== null;
}

describe('IntegrationDirectMembers', () => {
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
    });

    beforeEach(() => {
        jest.clearAllMocks();
        mockListIntegrationRoles.mockResolvedValue([{ name: 'PRIMARY_OWNER' }, { name: 'OWNER' }, { name: 'USER' }]);
    });

    it('shows a loading placeholder, with no Direct Members table, empty card or alert, while the members listing is pending', () => {
        mockListIntegrationMembers.mockReturnValue(new Promise(() => {}));

        renderSection();

        expect(document.querySelector('[data-slot="skeleton"]')).not.toBeNull();
        expect(screen.queryByRole('table')).toBeNull();
        expect(screen.queryByText('No direct members')).toBeNull();
        expect(screen.queryByRole('alert')).toBeNull();
    });

    it('shows a No direct members empty card instead of the Direct Members table when the members listing is empty', async () => {
        mockListIntegrationMembers.mockResolvedValue([]);

        renderSection();

        expect(await screen.findByText('No direct members')).toBeInTheDocument();
        expect(screen.queryByRole('table')).toBeNull();
    });

    it.each<[scenario: string, failure: unknown, expectedAlertText: string]>([
        [
            'an error response carrying a message',
            new ApimApiError(500, 'Members could not be loaded', { message: 'Members could not be loaded' }),
            'Members could not be loaded',
        ],
        // The client fills ApimApiError.message with the raw body text when the body has no message field.
        ['an HTTP 500 error response whose body has no message', new ApimApiError(500, '{}', {}), 'Failed to load members.'],
        ['an HTTP 502 error response whose body is not JSON', new ApimApiError(502, 'Bad Gateway', undefined), 'Failed to load members.'],
        ['a network error with no HTTP response', new TypeError('Failed to fetch'), 'Failed to load members.'],
    ])('shows only an alert reading the failure, with no Direct Members table, for %s', async (_scenario, failure, expectedAlertText) => {
        mockListIntegrationMembers.mockRejectedValue(failure);

        renderSection();

        const alert = await screen.findByRole('alert');
        expect(alert.textContent?.trim()).toBe(expectedAlertText);
        expect(screen.queryByRole('table')).toBeNull();
        expect(screen.queryByText('No direct members')).toBeNull();
    });

    it.each<[match: string, directMemberSearchResult: SearchableUser]>([
        ['its id', { id: 'user-alice-martin', reference: 'ref-alice-martin', displayName: 'Alice Martin' }],
        ['its reference, having no id', { id: null, reference: 'user-alice-martin', displayName: 'Alice Martin' }],
    ])(
        'offers a matching environment user but not a matching direct member found by %s in the add-member search',
        async (_match, directMemberSearchResult) => {
            mockListIntegrationMembers.mockResolvedValue([
                { id: 'user-alice-martin', displayName: 'Alice Martin', roles: [{ name: 'USER', scope: 'INTEGRATION' }] },
            ]);
            mockSearchUsers.mockResolvedValue([
                directMemberSearchResult,
                { id: 'user-alice-moreau', reference: 'ref-alice-moreau', displayName: 'Alice Moreau' },
            ]);
            const user = userEvent.setup();
            renderSection({ canCreateMembers: true });
            await screen.findByRole('table');

            const sheet = await openAddMembersSheet(user);
            await searchInSheet(user, sheet, 'Alice');

            expect(await within(sheet).findByRole('button', { name: /Alice Moreau/ })).toBeInTheDocument();
            expect(within(sheet).queryByRole('button', { name: /Alice Martin/ })).toBeNull();
        },
    );

    it('lists a newly added member in the Direct Members table with the raw name of the chosen role', async () => {
        let serverMembers: IntegrationMember[] = [JANE_OWNER];
        mockListIntegrationMembers.mockImplementation(async () => serverMembers);
        mockAddIntegrationMember.mockImplementation(
            async (_environmentId: string, _integrationId: string, payload: AddIntegrationMember) => {
                serverMembers = [
                    ...serverMembers,
                    { id: 'user-carol', displayName: 'Carol Diaz', roles: [{ name: payload.roleName, scope: 'INTEGRATION' }] },
                ];
            },
        );
        mockSearchUsers.mockResolvedValue([{ id: 'user-carol', reference: 'ref-carol', displayName: 'Carol Diaz' }]);
        const user = userEvent.setup();
        renderSection({ canCreateMembers: true });
        await screen.findByRole('table');

        await addMember(user, 'Carol', 'Carol Diaz', 'USER');

        await waitFor(() => expect(directMemberRows()).toContainEqual([expect.stringContaining('Carol Diaz'), 'USER']));
    });

    it('confirms a successful add and closes the add-member sheet', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER]);
        mockAddIntegrationMember.mockResolvedValue(undefined);
        mockSearchUsers.mockResolvedValue([{ id: 'user-carol', reference: 'ref-carol', displayName: 'Carol Diaz' }]);
        const user = userEvent.setup();
        renderSection({ canCreateMembers: true });
        await screen.findByRole('table');

        await addMember(user, 'Carol', 'Carol Diaz', 'USER');

        expect(await screen.findByText('Changes successfully saved!')).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    });

    it('adds the selected users with the Integration default role when no role is chosen', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER]);
        mockListIntegrationRoles.mockResolvedValue([{ name: 'PRIMARY_OWNER' }, { name: 'OWNER' }, { name: 'USER', default: true }]);
        mockAddIntegrationMember.mockResolvedValue(undefined);
        mockSearchUsers.mockResolvedValue([{ id: 'user-carol', reference: 'ref-carol', displayName: 'Carol Diaz' }]);
        const user = userEvent.setup();
        renderSection({ canCreateMembers: true });
        await screen.findByRole('table');

        const sheet = await openAddMembersSheet(user);
        await searchInSheet(user, sheet, 'Carol');
        await user.click(await within(sheet).findByRole('button', { name: /Carol Diaz/ }));
        await user.click(within(sheet).getByRole('button', { name: 'Add member' }));

        await waitFor(() =>
            expect(mockAddIntegrationMember).toHaveBeenCalledWith(
                expect.anything(),
                'int-1',
                expect.objectContaining({ roleName: 'USER' }),
            ),
        );
    });

    it.each<[state: string, arrangeMembersListing: () => void]>([
        ['loading', () => mockListIntegrationMembers.mockReturnValue(new Promise(() => {}))],
        ['empty', () => mockListIntegrationMembers.mockResolvedValue([])],
        ['failed', () => mockListIntegrationMembers.mockRejectedValue(new TypeError('Failed to fetch'))],
    ])('offers the Add members action while the members listing is %s', async (_state, arrangeMembersListing) => {
        arrangeMembersListing();

        renderSection({ canCreateMembers: true });

        expect(await screen.findByRole('button', { name: 'Add members' })).toBeInTheDocument();
    });

    it('keeps the Add member button disabled and sends no add request when the Integration roles fail to load', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER]);
        mockListIntegrationRoles.mockRejectedValue(new TypeError('Failed to fetch'));
        mockSearchUsers.mockResolvedValue([{ id: 'user-carol', reference: 'ref-carol', displayName: 'Carol Diaz' }]);
        const user = userEvent.setup();
        renderSection({ canCreateMembers: true });
        await screen.findByRole('table');

        const sheet = await openAddMembersSheet(user);
        await searchInSheet(user, sheet, 'Carol');
        await user.click(await within(sheet).findByRole('button', { name: /Carol Diaz/ }));
        await waitFor(() => expect(mockListIntegrationRoles).toHaveBeenCalled());
        const addButton = within(sheet).getByRole('button', { name: 'Add member' });
        await user.click(addButton);

        expect(addButton).toBeDisabled();
        expect(mockAddIntegrationMember).not.toHaveBeenCalled();
    });

    it('shows the error message of a failed add and keeps the Direct Members table as it was', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        mockAddIntegrationMember.mockRejectedValue(
            new ApimApiError(400, 'Member could not be added', { message: 'Member could not be added' }),
        );
        mockSearchUsers.mockResolvedValue([{ id: 'user-carol', reference: 'ref-carol', displayName: 'Carol New' }]);
        const user = userEvent.setup();
        renderSection({ canCreateMembers: true });
        await screen.findByRole('table');

        await addMember(user, 'Carol', 'Carol New', 'USER');

        expect(await screen.findByText(/Member could not be added/)).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Cancel' }));
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(directMemberRows()).toEqual(JANE_OWNER_AND_BOB_READER_ROWS);
    });

    it('reports which users failed and why, and keeps only the failed users selected, after a partial add', async () => {
        const CAROL: SearchableUser = { id: 'user-carol', reference: 'ref-carol', displayName: 'Carol Diaz' };
        const DAN: SearchableUser = { id: null, reference: 'ref-dan', displayName: 'Dan External' };
        const EVE: SearchableUser = { id: 'user-eve', reference: 'ref-eve', displayName: 'Eve Stone' };
        let serverMembers: IntegrationMember[] = [JANE_OWNER];
        mockListIntegrationMembers.mockImplementation(async () => serverMembers);
        mockSearchUsers.mockResolvedValue([CAROL, DAN, EVE]);
        mockAddIntegrationMember.mockImplementation(
            async (_environmentId: string, _integrationId: string, payload: AddIntegrationMember) => {
                switch (payload.externalReference) {
                    case CAROL.reference:
                        serverMembers = [
                            ...serverMembers,
                            { id: 'user-carol', displayName: 'Carol Diaz', roles: [{ name: 'USER', scope: 'INTEGRATION' }] },
                        ];
                        return;
                    case DAN.reference:
                        throw new ApimApiError(400, 'Member already exists', { message: 'Member already exists' });
                    default:
                        throw new Error('boom');
                }
            },
        );
        const user = userEvent.setup();
        renderSection({ canCreateMembers: true });
        await screen.findByRole('table');

        const sheet = await openAddMembersSheet(user);
        for (const selected of [CAROL, DAN, EVE]) {
            await searchInSheet(user, sheet, selected.displayName);
            await user.click(await within(sheet).findByRole('button', { name: new RegExp(selected.displayName) }));
        }
        await user.click(within(sheet).getByRole('button', { name: 'Add 3 members' }));

        expect(
            await screen.findByText(
                content =>
                    content.includes('Dan External') &&
                    content.includes('Member already exists') &&
                    content.includes('Eve Stone') &&
                    content.includes('boom'),
            ),
        ).toBeInTheDocument();
        await waitFor(() => expect(within(sheet).queryByRole('button', { name: 'Remove Carol Diaz' })).toBeNull());
        expect(within(sheet).getByRole('button', { name: 'Remove Dan External' })).toBeInTheDocument();
        expect(within(sheet).getByRole('button', { name: 'Remove Eve Stone' })).toBeInTheDocument();
        expect(within(sheet).getByRole('button', { name: 'Add 2 members' })).toBeInTheDocument();
    });

    it('keeps only the failed user selected and re-sends only that user after a partial add where the added user came from an external identity provider', async () => {
        const DAN: SearchableUser = { id: null, reference: 'ref-dan-ext', displayName: 'Dan External' };
        const CAROL: SearchableUser = { id: 'user-carol', reference: 'ref-carol', displayName: 'Carol Diaz' };
        let serverMembers: IntegrationMember[] = [JANE_OWNER];
        mockListIntegrationMembers.mockImplementation(async () => serverMembers);
        mockSearchUsers.mockResolvedValue([DAN, CAROL]);
        mockAddIntegrationMember.mockImplementation(
            async (_environmentId: string, _integrationId: string, payload: AddIntegrationMember) => {
                if (payload.externalReference === DAN.reference) {
                    // The backend creates a Gravitee user for an external identity, so the new member carries a fresh id.
                    serverMembers = [
                        ...serverMembers,
                        { id: 'user-new-uuid', displayName: 'Dan External', roles: [{ name: 'USER', scope: 'INTEGRATION' }] },
                    ];
                    return;
                }
                throw new ApimApiError(400, 'Member could not be added', { message: 'Member could not be added' });
            },
        );
        const user = userEvent.setup();
        renderSection({ canCreateMembers: true });
        await screen.findByRole('table');

        const sheet = await openAddMembersSheet(user);
        for (const selected of [DAN, CAROL]) {
            await searchInSheet(user, sheet, selected.displayName);
            await user.click(await within(sheet).findByRole('button', { name: new RegExp(selected.displayName) }));
        }
        await user.click(within(sheet).getByRole('button', { name: 'Add 2 members' }));
        await screen.findByText(content => content.includes('Carol Diaz') && content.includes('Member could not be added'));

        await waitFor(() => expect(within(sheet).queryByRole('button', { name: 'Remove Dan External' })).toBeNull());
        expect(within(sheet).getByText('1 user selected')).toBeInTheDocument();
        expect(within(sheet).getByRole('button', { name: 'Remove Carol Diaz' })).toBeInTheDocument();

        mockAddIntegrationMember.mockClear();
        await user.click(within(sheet).getByRole('button', { name: 'Add member' }));

        await waitFor(() => expect(mockAddIntegrationMember).toHaveBeenCalledTimes(1));
        expect(mockAddIntegrationMember).toHaveBeenCalledWith(
            'DEFAULT',
            'int-1',
            expect.objectContaining({ externalReference: 'ref-carol' }),
        );
    });

    it.each([
        ['add and update members', { canCreateMembers: true, canUpdateMembers: true }],
        ['only remove members', { canDeleteMembers: true }],
        ['add, update and remove members', { canCreateMembers: true, canUpdateMembers: true, canDeleteMembers: true }],
    ])(
        'offers a Member actions menu on a member row but none on the Primary Owner row to a viewer who can %s',
        async (_who, permissions) => {
            mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);

            renderSection(permissions);
            await screen.findByRole('table');

            expect(memberActionsTrigger('Bob Reader')).not.toBeNull();
            expect(memberActionsTrigger('Jane Owner')).toBeNull();
        },
    );

    it('does not request the Integration roles for a viewer who cannot update members', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);

        renderSection({ canDeleteMembers: true });
        await screen.findByRole('table');

        expect(mockListIntegrationRoles).not.toHaveBeenCalled();
    });

    it('offers Edit role then Remove member, set apart from each other, to a viewer who can update and remove members', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        const user = userEvent.setup();
        renderSection({ canUpdateMembers: true, canDeleteMembers: true });
        await screen.findByRole('table');

        const menu = await openMemberActions(user, 'Bob Reader');

        expect(
            within(menu)
                .getAllByRole('menuitem')
                .map(item => item.textContent),
        ).toEqual(['Edit role', 'Remove member']);
        expect(within(menu).getByRole('separator')).toBeInTheDocument();
    });

    it.each<[who: string, permissions: { canUpdateMembers?: boolean; canDeleteMembers?: boolean }, expectedItems: string[]]>([
        ['only update members', { canUpdateMembers: true }, ['Edit role']],
        ['only remove members', { canDeleteMembers: true }, ['Remove member']],
    ])('offers only the permitted member action, with no separator, to a viewer who can %s', async (_who, permissions, expectedItems) => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        const user = userEvent.setup();
        renderSection(permissions);
        await screen.findByRole('table');

        const menu = await openMemberActions(user, 'Bob Reader');

        expect(
            within(menu)
                .getAllByRole('menuitem')
                .map(item => item.textContent),
        ).toEqual(expectedItems);
        expect(within(menu).queryByRole('separator')).toBeNull();
    });

    it.each<[roleSet: string, integrationRoles: IntegrationRole[], expectedOptions: string[]]>([
        ['the default Integration roles', [{ name: 'PRIMARY_OWNER' }, { name: 'OWNER' }, { name: 'USER' }], ['OWNER', 'USER']],
        [
            'the default Integration roles and a custom role',
            [{ name: 'PRIMARY_OWNER' }, { name: 'OWNER' }, { name: 'USER' }, { name: 'REVIEWER' }],
            ['OWNER', 'USER', 'REVIEWER'],
        ],
    ])(
        'offers exactly the Integration-scoped roles other than Primary Owner in the edit-role selector for %s',
        async (_roleSet, integrationRoles, expectedOptions) => {
            mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
            mockListIntegrationRoles.mockResolvedValue(integrationRoles);
            const user = userEvent.setup();
            renderSection({ canUpdateMembers: true });
            await screen.findByRole('table');

            await startEditingRole(user, 'Bob Reader');
            await openRoleSelector(user, 'Bob Reader');

            await waitFor(() => expect(screen.getAllByRole('option').map(option => option.textContent)).toEqual(expectedOptions));
        },
    );

    it.each<[roleSet: string, integrationRoles: IntegrationRole[], expectedOptions: string[]]>([
        ['the default Integration roles', [{ name: 'PRIMARY_OWNER' }, { name: 'OWNER' }, { name: 'USER' }], ['OWNER', 'USER']],
        [
            'the default Integration roles and a custom role',
            [{ name: 'PRIMARY_OWNER' }, { name: 'OWNER' }, { name: 'USER' }, { name: 'REVIEWER' }],
            ['OWNER', 'USER', 'REVIEWER'],
        ],
    ])(
        'offers exactly the Integration-scoped roles other than Primary Owner in the add-member selector for %s',
        async (_roleSet, integrationRoles, expectedOptions) => {
            mockListIntegrationMembers.mockResolvedValue([JANE_OWNER]);
            mockListIntegrationRoles.mockResolvedValue(integrationRoles);
            mockSearchUsers.mockResolvedValue([{ id: 'user-carol', reference: 'ref-carol', displayName: 'Carol Diaz' }]);
            const user = userEvent.setup();
            renderSection({ canCreateMembers: true });
            await screen.findByRole('table');

            const sheet = await openAddMembersSheet(user);
            await searchInSheet(user, sheet, 'Carol');
            await user.click(await within(sheet).findByRole('button', { name: /Carol Diaz/ }));
            await user.click(within(sheet).getByRole('combobox'));

            await waitFor(() => expect(screen.getAllByRole('option').map(option => option.textContent)).toEqual(expectedOptions));
        },
    );

    it("shows the new raw role name in the edited member's row after a successful role change", async () => {
        let serverMembers: IntegrationMember[] = [JANE_OWNER, BOB_READER];
        mockListIntegrationMembers.mockImplementation(async () => serverMembers);
        mockUpdateIntegrationMemberRole.mockImplementation(
            async (_environmentId: string, _integrationId: string, memberId: string, roleName: string) => {
                serverMembers = serverMembers.map(member =>
                    member.id === memberId ? { ...member, roles: [{ name: roleName, scope: 'INTEGRATION' }] } : member,
                );
            },
        );
        const user = userEvent.setup();
        renderSection({ canUpdateMembers: true });
        await screen.findByRole('table');

        await changeRole(user, 'Bob Reader', 'OWNER');

        await waitFor(() =>
            expect(directMemberRows()).toEqual([
                [expect.stringContaining('Jane Owner'), 'Primary Owner'],
                [expect.stringContaining('Bob Reader'), 'OWNER'],
            ]),
        );
    });

    it('confirms a successful role change', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        mockUpdateIntegrationMemberRole.mockResolvedValue(undefined);
        const user = userEvent.setup();
        renderSection({ canUpdateMembers: true });
        await screen.findByRole('table');

        await changeRole(user, 'Bob Reader', 'OWNER');

        expect(await screen.findByText('Changes successfully saved!')).toBeInTheDocument();
    });

    it.each<[selection: string, roleName: string, saveEnabled: boolean]>([
        ["the member's current role", 'USER', false],
        ['another role', 'OWNER', true],
    ])('allows saving the edited role only when %s is selected', async (_selection, roleName, saveEnabled) => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        const user = userEvent.setup();
        renderSection({ canUpdateMembers: true });
        await screen.findByRole('table');

        await startEditingRole(user, 'Bob Reader');
        await selectRole(user, 'Bob Reader', roleName);

        expect(saveButton('Bob Reader')).toHaveProperty('disabled', !saveEnabled);
    });

    it('disables Save while the role change is being saved', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        mockUpdateIntegrationMemberRole.mockReturnValue(new Promise(() => {}));
        const user = userEvent.setup();
        renderSection({ canUpdateMembers: true });
        await screen.findByRole('table');

        await changeRole(user, 'Bob Reader', 'OWNER');

        await waitFor(() => expect(saveButton('Bob Reader')).toBeDisabled());
    });

    it('closes the role editor without saving and shows the original role when the edit is cancelled', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        const user = userEvent.setup();
        renderSection({ canUpdateMembers: true });
        await screen.findByRole('table');

        await startEditingRole(user, 'Bob Reader');
        await selectRole(user, 'Bob Reader', 'OWNER');
        await user.click(within(memberRow('Bob Reader')).getByRole('button', { name: 'Cancel edit' }));

        expect(within(memberRow('Bob Reader')).queryByRole('combobox')).toBeNull();
        expect(directMemberRows()).toEqual(JANE_OWNER_AND_BOB_READER_ROWS);
        expect(mockUpdateIntegrationMemberRole).not.toHaveBeenCalled();
    });

    it('hides the Member actions menu on the row whose role is being edited and keeps it on the other member rows', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER, JANE_DOE]);
        const user = userEvent.setup();
        renderSection({ canUpdateMembers: true });
        await screen.findByRole('table');

        await startEditingRole(user, 'Bob Reader');

        await waitFor(() => expect(within(memberRow('Bob Reader')).getByRole('combobox')).toBeInTheDocument());
        expect(memberActionsTrigger('Bob Reader')).toBeNull();
        expect(memberActionsTrigger('Jane Doe')).not.toBeNull();
    });

    it('shows the error message of a failed role change and keeps every member on the role it had before', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        mockUpdateIntegrationMemberRole.mockRejectedValue(
            new ApimApiError(400, 'Role could not be updated', { message: 'Role could not be updated' }),
        );
        const user = userEvent.setup();
        renderSection({ canUpdateMembers: true });
        await screen.findByRole('table');

        await changeRole(user, 'Bob Reader', 'OWNER');

        expect(await screen.findByText('Role could not be updated')).toBeInTheDocument();
        await waitFor(() => expect(directMemberRows()).toEqual(JANE_OWNER_AND_BOB_READER_ROWS));
    });

    it('opens a removal confirmation naming the chosen member', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        const user = userEvent.setup();
        renderSection({ canDeleteMembers: true });
        await screen.findByRole('table');

        const dialog = await openRemoveMemberDialog(user, 'Bob Reader');

        expect(dialog).toHaveTextContent('Bob Reader');
        expect(dialog).not.toHaveTextContent('Jane Owner');
    });

    it('keeps the chosen member listed while the removal confirmation is open and unconfirmed', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        const user = userEvent.setup();
        renderSection({ canDeleteMembers: true });
        await screen.findByRole('table');

        await openRemoveMemberDialog(user, 'Bob Reader');

        expect(isListed('Bob Reader')).toBe(true);
        expect(mockRemoveIntegrationMember).not.toHaveBeenCalled();
    });

    it('closes the removal confirmation and keeps every member listed when the removal is cancelled', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        const user = userEvent.setup();
        renderSection({ canDeleteMembers: true });
        await screen.findByRole('table');

        const dialog = await openRemoveMemberDialog(user, 'Bob Reader');
        await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));

        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(directMemberRows()).toEqual(JANE_OWNER_AND_BOB_READER_ROWS);
        expect(mockRemoveIntegrationMember).not.toHaveBeenCalled();
    });

    it('disables both confirmation buttons and keeps the confirmation open while the removal is in progress', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        mockRemoveIntegrationMember.mockReturnValue(new Promise(() => {}));
        const user = userEvent.setup();
        renderSection({ canDeleteMembers: true });
        await screen.findByRole('table');

        await confirmRemoval(user, 'Bob Reader');
        const dialog = screen.getByRole('dialog');
        await waitFor(() => expect(within(dialog).getByRole('button', { name: 'Removing…' })).toBeDisabled());
        await user.keyboard('{Escape}');

        expect(within(dialog).getByRole('button', { name: 'Cancel' })).toBeDisabled();
        expect(screen.getByRole('dialog')).toBe(dialog);
    });

    it('drops only the removed member from the Direct Members table after a successful removal', async () => {
        let serverMembers: IntegrationMember[] = [JANE_OWNER, JANE_DOE, BOB_READER];
        mockListIntegrationMembers.mockImplementation(async () => serverMembers);
        mockRemoveIntegrationMember.mockImplementation(async (_environmentId: string, _integrationId: string, memberId: string) => {
            serverMembers = serverMembers.filter(member => member.id !== memberId);
        });
        const user = userEvent.setup();
        renderSection({ canDeleteMembers: true });
        await screen.findByRole('table');

        await confirmRemoval(user, 'Jane Doe');

        await waitFor(() => expect(isListed('Jane Doe')).toBe(false));
        expect(isListed('Jane Owner')).toBe(true);
        expect(isListed('Bob Reader')).toBe(true);
    });

    it('confirms a successful removal by naming the removed member and closes the confirmation', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, JANE_DOE, BOB_READER]);
        mockRemoveIntegrationMember.mockResolvedValue(undefined);
        const user = userEvent.setup();
        renderSection({ canDeleteMembers: true });
        await screen.findByRole('table');

        await confirmRemoval(user, 'Jane Doe');

        expect(await screen.findByText('Member Jane Doe has been removed.')).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    });

    it('shows the error message of a failed removal, closes the confirmation and keeps every member listed', async () => {
        mockListIntegrationMembers.mockResolvedValue([JANE_OWNER, BOB_READER]);
        mockRemoveIntegrationMember.mockRejectedValue(
            new ApimApiError(400, 'Member could not be removed', { message: 'Member could not be removed' }),
        );
        const user = userEvent.setup();
        renderSection({ canDeleteMembers: true });
        await screen.findByRole('table');

        await confirmRemoval(user, 'Bob Reader');

        expect(await screen.findByText('Member could not be removed')).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(isListed('Jane Owner')).toBe(true);
        expect(isListed('Bob Reader')).toBe(true);
    });
});
