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
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { GroupEditMemberSheet } from './GroupEditMemberSheet';
import { installFormActionTestEnvironment } from '../../../shared/testing/formAction';
import type { GroupMember } from '../types/group';

let restoreTestEnvironment: () => void;

beforeAll(() => {
    Element.prototype.scrollIntoView = jest.fn();
    restoreTestEnvironment = installFormActionTestEnvironment();
});

afterAll(() => {
    restoreTestEnvironment();
});

const MEMBER: GroupMember = {
    id: 'user-1',
    displayName: 'Anna Schmidt',
    roles: { API: 'OWNER', APPLICATION: 'USER' },
};

function renderSheet(overrides: Partial<React.ComponentProps<typeof GroupEditMemberSheet>> = {}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn().mockResolvedValue(undefined);
    render(
        <GroupEditMemberSheet
            open
            groupName="API Team"
            member={MEMBER}
            members={[MEMBER]}
            apiRoles={[
                { name: 'OWNER', scope: 'API' },
                { name: 'PRIMARY_OWNER', scope: 'API', system: true },
            ]}
            applicationRoles={[{ name: 'USER', scope: 'APPLICATION' }]}
            apiProductRoles={[{ name: 'USER', scope: 'API_PRODUCT' }]}
            integrationRoles={[{ name: 'USER', scope: 'INTEGRATION' }]}
            clusterRoles={[{ name: 'USER', scope: 'CLUSTER' }]}
            explorerRoles={[{ name: 'USER', scope: 'EXPLORER' }]}
            lockApiRole={false}
            lockApiProductRole={false}
            lockApplicationRole={false}
            canOverrideLocks
            groupAllowsGroupAdmin
            apiPrimaryOwnerMode="HYBRID"
            apiProductPrimaryOwnerMode="HYBRID"
            onClose={onClose}
            onSubmit={onSubmit}
            {...overrides}
        />,
    );
    return { onClose, onSubmit };
}

describe('GroupEditMemberSheet', () => {
    it('does not render sheet content when closed', () => {
        renderSheet({ open: false });
        expect(screen.queryByRole('heading', { name: 'Edit roles' })).toBeNull();
    });

    it('renders nothing when there is no member', () => {
        const { container } = render(
            <GroupEditMemberSheet
                open
                groupName="API Team"
                member={undefined}
                members={[]}
                apiRoles={[]}
                applicationRoles={[]}
                apiProductRoles={[]}
                integrationRoles={[]}
                clusterRoles={[]}
                explorerRoles={[]}
                lockApiRole={false}
                lockApiProductRole={false}
                lockApplicationRole={false}
                canOverrideLocks
                groupAllowsGroupAdmin
                onClose={jest.fn()}
                onSubmit={jest.fn()}
            />,
        );
        expect(container.firstChild).toBeNull();
    });

    it('shows the member name in the description', () => {
        renderSheet();
        expect(screen.getByText(/Update Anna Schmidt.?s roles in API Team\./)).not.toBeNull();
    });

    it('pre-fills the Group admin checkbox from the member’s current GROUP role', () => {
        renderSheet({ member: { ...MEMBER, roles: { ...MEMBER.roles, GROUP: 'ADMIN' } } });
        expect(screen.getByRole('checkbox', { name: /Group admin/i }).getAttribute('data-state')).toBe('checked');
    });

    it('disables the Group admin checkbox when the group does not allow it', () => {
        renderSheet({ groupAllowsGroupAdmin: false });
        expect(screen.getByRole('checkbox', { name: /Group admin/i })).toHaveProperty('disabled', true);
        expect(screen.getByText(/Enable "Allow adding members via user search"/)).not.toBeNull();
    });

    it('leaves the API role select enabled when the member is already the primary owner', () => {
        renderSheet({ member: { ...MEMBER, roles: { ...MEMBER.roles, API: 'PRIMARY_OWNER' } } });
        expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', false);
    });

    it('has no "None" role option — classic\'s edit-member-dialog has no such mat-option', () => {
        renderSheet();
        fireEvent.click(screen.getAllByRole('combobox')[0]);
        expect(screen.queryByRole('option', { name: 'None' })).toBeNull();
    });

    it('leaves the PRIMARY_OWNER option selectable even when another member already holds it', () => {
        const otherOwner: GroupMember = { id: 'user-2', displayName: 'Ravi Patel', roles: { API: 'PRIMARY_OWNER' } };
        renderSheet({ members: [MEMBER, otherOwner] });

        fireEvent.click(screen.getAllByRole('combobox')[0]);

        expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).not.toBe('true');
    });

    it('submits the current roles plus GROUP/ADMIN only when Group admin is checked', () => {
        const { onSubmit } = renderSheet();

        fireEvent.click(screen.getByRole('checkbox', { name: /Group admin/i }));
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        expect(onSubmit).toHaveBeenCalledWith([
            {
                id: 'user-1',
                roles: [
                    { scope: 'API', name: 'OWNER' },
                    { scope: 'APPLICATION', name: 'USER' },
                    { scope: 'GROUP', name: 'ADMIN' },
                ],
            },
        ]);
    });

    it('omits the GROUP scope entirely when Group admin is left unchecked', () => {
        const { onSubmit } = renderSheet();

        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        expect(onSubmit).toHaveBeenCalledWith([
            {
                id: 'user-1',
                roles: [
                    { scope: 'API', name: 'OWNER' },
                    { scope: 'APPLICATION', name: 'USER' },
                ],
            },
        ]);
    });

    it('calls onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('locks role fields while the form action is pending', async () => {
        let resolveSubmit!: () => void;
        const onSubmit = jest.fn(
            () =>
                new Promise<void>(resolve => {
                    resolveSubmit = resolve;
                }),
        );
        renderSheet({ onSubmit });

        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(screen.getByRole('button', { name: 'Saving…' })).toHaveProperty('disabled', true));
        expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', true);
        expect(screen.getByRole('checkbox', { name: /Group admin/i })).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Cancel' })).toHaveProperty('disabled', true);

        await act(async () => resolveSubmit());
    });

    it('disables PRIMARY_OWNER when API primary-owner mode is USER', () => {
        renderSheet({ apiPrimaryOwnerMode: 'USER' });
        fireEvent.click(screen.getAllByRole('combobox')[0]);
        expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).toBe('true');
    });

    it('disables PRIMARY_OWNER until primary-owner mode settings have loaded', () => {
        renderSheet({ apiPrimaryOwnerMode: undefined });
        fireEvent.click(screen.getAllByRole('combobox')[0]);
        expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).toBe('true');
    });

    describe('primary ownership transfer', () => {
        const otherOwner: GroupMember = {
            id: 'user-2',
            displayName: 'Ravi Patel',
            roles: { API: 'PRIMARY_OWNER', APPLICATION: 'USER' },
        };

        it('shows the transfer message and submits a demotion before the promotion', () => {
            const { onSubmit } = renderSheet({ members: [MEMBER, otherOwner] });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'PRIMARY_OWNER' }));

            expect(
                screen.getByText(
                    'Ravi Patel is the API primary owner. The API primary ownership will be transferred to Anna Schmidt and Ravi Patel will be updated as owner.',
                ),
            ).not.toBeNull();

            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            expect(onSubmit).toHaveBeenCalledWith([
                {
                    id: 'user-2',
                    roles: [
                        { scope: 'API', name: 'OWNER' },
                        { scope: 'APPLICATION', name: 'USER' },
                    ],
                },
                {
                    id: 'user-1',
                    roles: [
                        { scope: 'API', name: 'PRIMARY_OWNER' },
                        { scope: 'APPLICATION', name: 'USER' },
                    ],
                },
            ]);
        });

        it('merges into a single demotion when the same previous owner holds both API and API product primary ownership', () => {
            const bothOwner: GroupMember = {
                id: 'user-2',
                displayName: 'Ravi Patel',
                roles: { API: 'PRIMARY_OWNER', API_PRODUCT: 'PRIMARY_OWNER', APPLICATION: 'USER' },
            };
            const { onSubmit } = renderSheet({
                member: { ...MEMBER, roles: { ...MEMBER.roles, API_PRODUCT: 'USER' } },
                members: [MEMBER, bothOwner],
                apiProductRoles: [
                    { name: 'USER', scope: 'API_PRODUCT' },
                    { name: 'PRIMARY_OWNER', scope: 'API_PRODUCT', system: true },
                ],
            });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'PRIMARY_OWNER' }));
            fireEvent.click(screen.getAllByRole('combobox')[1]);
            fireEvent.click(screen.getByRole('option', { name: 'PRIMARY_OWNER' }));

            expect(
                screen.getByText(
                    'Ravi Patel is the API and API Product primary owner. Primary ownership will be transferred to Anna Schmidt and Ravi Patel will be updated as owner.',
                ),
            ).not.toBeNull();

            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            expect(onSubmit).toHaveBeenCalledWith([
                {
                    id: 'user-2',
                    roles: [
                        { scope: 'API', name: 'OWNER' },
                        { scope: 'API_PRODUCT', name: 'OWNER' },
                        { scope: 'APPLICATION', name: 'USER' },
                    ],
                },
                {
                    id: 'user-1',
                    roles: [
                        { scope: 'API', name: 'PRIMARY_OWNER' },
                        { scope: 'API_PRODUCT', name: 'PRIMARY_OWNER' },
                        { scope: 'APPLICATION', name: 'USER' },
                    ],
                },
            ]);
        });

        it('does not show a transfer message when the member already owns that scope', () => {
            renderSheet({ member: { ...MEMBER, roles: { ...MEMBER.roles, API: 'PRIMARY_OWNER' } }, members: [MEMBER] });
            expect(screen.queryByText(/will be transferred/)).toBeNull();
        });
    });

    describe('primary ownership downgrade', () => {
        const primaryOwnerMember: GroupMember = {
            id: 'user-1',
            displayName: 'Anna Schmidt',
            roles: { API: 'PRIMARY_OWNER', APPLICATION: 'USER' },
        };
        const otherMember: GroupMember = { id: 'user-2', displayName: 'Ravi Patel', roles: { API: 'OWNER' } };

        it('shows a successor search field only once the role changes away from PRIMARY_OWNER', () => {
            renderSheet({ member: primaryOwnerMember, members: [primaryOwnerMember, otherMember] });
            expect(screen.queryByLabelText('Search members')).toBeNull();

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));

            expect(screen.getByLabelText('Search members')).not.toBeNull();
        });

        it('disables Save until a successor is picked', () => {
            renderSheet({ member: primaryOwnerMember, members: [primaryOwnerMember, otherMember] });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));

            expect(screen.getByRole('button', { name: 'Save' })).toHaveProperty('disabled', true);
        });

        it('does not submit a primary-owner downgrade without a successor when the form action is invoked directly', () => {
            const { onSubmit } = renderSheet({ member: primaryOwnerMember, members: [primaryOwnerMember, otherMember] });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));
            fireEvent.submit(screen.getByRole('form', { name: 'Edit member roles' }));

            expect(onSubmit).not.toHaveBeenCalled();
        });

        it('excludes the member being edited from the successor list', async () => {
            const user = userEvent.setup();
            renderSheet({ member: primaryOwnerMember, members: [primaryOwnerMember, otherMember] });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));
            await user.click(screen.getByLabelText('Search members'));

            expect(screen.queryByRole('option', { name: 'Anna Schmidt' })).toBeNull();
            expect(screen.getByRole('option', { name: 'Ravi Patel' })).not.toBeNull();
        });

        it('shows the transfer message and enables Save once a successor is picked', async () => {
            const user = userEvent.setup();
            renderSheet({ member: primaryOwnerMember, members: [primaryOwnerMember, otherMember] });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));
            await user.click(screen.getByLabelText('Search members'));
            await user.click(screen.getByRole('option', { name: 'Ravi Patel' }));

            expect(
                screen.getByText(
                    'Anna Schmidt is the API primary owner. The API primary ownership will be transferred to Ravi Patel and Anna Schmidt will be updated as owner.',
                ),
            ).not.toBeNull();
            expect(screen.getByRole('button', { name: 'Save' })).toHaveProperty('disabled', false);
        });

        it('names the selected non-owner role in the downgrade transfer message', async () => {
            const user = userEvent.setup();
            renderSheet({
                member: primaryOwnerMember,
                members: [primaryOwnerMember, otherMember],
                apiRoles: [
                    { name: 'USER', scope: 'API' },
                    { name: 'OWNER', scope: 'API' },
                    { name: 'PRIMARY_OWNER', scope: 'API', system: true },
                ],
            });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'USER' }));
            await user.click(screen.getByLabelText('Search members'));
            await user.click(screen.getByRole('option', { name: 'Ravi Patel' }));

            expect(
                screen.getByText(
                    'Anna Schmidt is the API primary owner. The API primary ownership will be transferred to Ravi Patel and Anna Schmidt will be updated as user.',
                ),
            ).not.toBeNull();
        });

        it('submits the promoted successor before the demoted member', async () => {
            const user = userEvent.setup();
            const { onSubmit } = renderSheet({ member: primaryOwnerMember, members: [primaryOwnerMember, otherMember] });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));
            await user.click(screen.getByLabelText('Search members'));
            await user.click(screen.getByRole('option', { name: 'Ravi Patel' }));

            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            expect(onSubmit).toHaveBeenCalledWith([
                { id: 'user-2', roles: [{ scope: 'API', name: 'PRIMARY_OWNER' }] },
                {
                    id: 'user-1',
                    roles: [
                        { scope: 'API', name: 'OWNER' },
                        { scope: 'APPLICATION', name: 'USER' },
                    ],
                },
            ]);
        });

        it('promotes a single successor for both scopes when API and API product are both downgraded together', async () => {
            const user = userEvent.setup();
            const bothPrimaryOwner: GroupMember = {
                id: 'user-1',
                displayName: 'Anna Schmidt',
                roles: { API: 'PRIMARY_OWNER', API_PRODUCT: 'PRIMARY_OWNER', APPLICATION: 'USER' },
            };
            const { onSubmit } = renderSheet({
                member: bothPrimaryOwner,
                members: [bothPrimaryOwner, otherMember],
                apiProductRoles: [
                    { name: 'OWNER', scope: 'API_PRODUCT' },
                    { name: 'PRIMARY_OWNER', scope: 'API_PRODUCT', system: true },
                ],
            });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));
            fireEvent.click(screen.getAllByRole('combobox')[1]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));
            await user.click(screen.getByLabelText('Search members'));
            await user.click(screen.getByRole('option', { name: 'Ravi Patel' }));

            expect(
                screen.getByText(
                    'Anna Schmidt is the API and API Product primary owner. Primary ownership will be transferred to Ravi Patel and Anna Schmidt will be updated as owner.',
                ),
            ).not.toBeNull();

            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            expect(onSubmit).toHaveBeenCalledWith([
                {
                    id: 'user-2',
                    roles: [
                        { scope: 'API', name: 'PRIMARY_OWNER' },
                        { scope: 'API_PRODUCT', name: 'PRIMARY_OWNER' },
                    ],
                },
                {
                    id: 'user-1',
                    roles: [
                        { scope: 'API', name: 'OWNER' },
                        { scope: 'API_PRODUCT', name: 'OWNER' },
                        { scope: 'APPLICATION', name: 'USER' },
                    ],
                },
            ]);
        });

        it('re-hides the successor field and clears the pick if the role is changed back to PRIMARY_OWNER', async () => {
            const user = userEvent.setup();
            renderSheet({ member: primaryOwnerMember, members: [primaryOwnerMember, otherMember] });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));
            await user.click(screen.getByLabelText('Search members'));
            await user.click(screen.getByRole('option', { name: 'Ravi Patel' }));

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'PRIMARY_OWNER' }));

            expect(screen.queryByLabelText('Search members')).toBeNull();
            expect(screen.getByRole('button', { name: 'Save' })).toHaveProperty('disabled', false);
        });

        it('requires a successor when demoting an Application primary owner', async () => {
            const user = userEvent.setup();
            const applicationOwner: GroupMember = {
                id: 'user-1',
                displayName: 'Anna Schmidt',
                roles: { API: 'USER', APPLICATION: 'PRIMARY_OWNER' },
            };
            const other: GroupMember = { id: 'user-2', displayName: 'Ravi Patel', roles: { APPLICATION: 'USER' } };
            const { onSubmit } = renderSheet({
                member: applicationOwner,
                members: [applicationOwner, other],
                applicationRoles: [
                    { name: 'USER', scope: 'APPLICATION' },
                    { name: 'OWNER', scope: 'APPLICATION' },
                    { name: 'PRIMARY_OWNER', scope: 'APPLICATION', system: true },
                ],
            });

            fireEvent.click(screen.getAllByRole('combobox')[2]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));
            expect(screen.getByRole('button', { name: 'Save' })).toHaveProperty('disabled', true);

            await user.click(screen.getByLabelText('Search members'));
            await user.click(screen.getByRole('option', { name: 'Ravi Patel' }));

            expect(
                screen.getByText(
                    'Anna Schmidt is the Application primary owner. The Application primary ownership will be transferred to Ravi Patel and Anna Schmidt will be updated as owner.',
                ),
            ).not.toBeNull();

            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            expect(onSubmit).toHaveBeenCalledWith([
                { id: 'user-2', roles: [{ scope: 'APPLICATION', name: 'PRIMARY_OWNER' }] },
                {
                    id: 'user-1',
                    roles: [
                        { scope: 'API', name: 'USER' },
                        { scope: 'APPLICATION', name: 'OWNER' },
                    ],
                },
            ]);
        });
    });

    describe('lock flags', () => {
        it('disables a locked role select without canOverrideLocks', () => {
            renderSheet({ lockApiRole: true, canOverrideLocks: false });
            expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', true);
        });

        it('leaves a locked role select enabled with canOverrideLocks', () => {
            renderSheet({ lockApiRole: true, canOverrideLocks: true });
            expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', false);
        });

        it('disables Integration and Cluster without canOverrideLocks, regardless of lock flags', () => {
            renderSheet({ canOverrideLocks: false });
            const comboboxes = screen.getAllByRole('combobox');
            expect(comboboxes[3]).toHaveProperty('disabled', true);
            expect(comboboxes[4]).toHaveProperty('disabled', true);
        });
    });
});
