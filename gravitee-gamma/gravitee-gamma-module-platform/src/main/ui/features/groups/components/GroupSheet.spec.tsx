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
import { fireEvent, render, screen } from '@testing-library/react';

import { GroupSheet } from './GroupSheet';
import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';
import type { Group, GroupRole } from '../types/group';

// Radix Switch measures its thumb via ResizeObserver, and Radix Select scrolls the highlighted
// option into view — neither is implemented in jsdom.
beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
    Element.prototype.scrollIntoView = jest.fn();
});

const API_ROLES: GroupRole[] = [
    { name: 'USER', scope: 'API', default: true },
    { name: 'OWNER', scope: 'API' },
];

const APPLICATION_ROLES: GroupRole[] = [{ name: 'USER', scope: 'APPLICATION', default: true }];

const API_PRODUCT_ROLES: GroupRole[] = [{ name: 'USER', scope: 'API_PRODUCT', default: true }];

const EXISTING_GROUP: Group = {
    id: 'group-1',
    name: 'Support Team',
    event_rules: [{ event: 'API_CREATE' }, { event: 'APPLICATION_CREATE' }],
    roles: { API: 'OWNER', APPLICATION: 'USER', API_PRODUCT: 'USER' },
    lock_api_role: false,
    lock_api_product_role: true,
    lock_application_role: false,
    max_invitation: 25,
    system_invitation: true,
    email_invitation: false,
    disable_membership_notifications: true,
};

function renderSheet({
    open = true,
    mode,
    group,
    isSaving = false,
    apiRoles = API_ROLES,
    applicationRoles = APPLICATION_ROLES,
    apiProductRoles = API_PRODUCT_ROLES,
}: {
    open?: boolean;
    mode: 'create' | 'edit';
    group?: Group;
    isSaving?: boolean;
    apiRoles?: GroupRole[];
    applicationRoles?: GroupRole[];
    apiProductRoles?: GroupRole[];
}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn();
    render(
        <GroupSheet
            open={open}
            mode={mode}
            group={group}
            apiRoles={apiRoles}
            applicationRoles={applicationRoles}
            apiProductRoles={apiProductRoles}
            rolesLoading={false}
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={isSaving}
        />,
    );
    return { onClose, onSubmit };
}

describe('GroupSheet', () => {
    describe('visibility', () => {
        it('does not show sheet content when closed', () => {
            renderSheet({ open: false, mode: 'create' });
            expect(querySheetHeading('Create group')).toBeNull();
        });

        it('shows create title when mode is create', () => {
            renderSheet({ mode: 'create' });
            expect(screen.getByRole('heading', { name: 'Create group' })).not.toBeNull();
        });

        it('shows edit title when mode is edit', () => {
            renderSheet({ mode: 'edit', group: EXISTING_GROUP });
            expect(screen.getByRole('heading', { name: 'Edit group' })).not.toBeNull();
        });
    });

    describe('create mode', () => {
        it('renders an empty name field', () => {
            renderSheet({ mode: 'create' });
            expect((screen.getByLabelText(/Name/i) as HTMLInputElement).value).toBe('');
        });

        it('defaults every toggle off', () => {
            renderSheet({ mode: 'create' });
            expect(screen.getByLabelText('Lock API role').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Lock API product role').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Lock application role').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Associate with new APIs').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Associate with new API products').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Associate with new applications').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Allow invitation via user search').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Allow invitation via email').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Notify members when added').getAttribute('aria-checked')).toBe('true');
        });

        it('leaves Maximum members blank by default', () => {
            renderSheet({ mode: 'create' });
            expect((screen.getByLabelText('Maximum members') as HTMLInputElement).value).toBe('');
        });

        it('pre-selects the default role for each scope', () => {
            renderSheet({ mode: 'create' });
            expect(screen.getAllByText('USER').length).toBeGreaterThan(0);
        });

        it('keeps Create disabled until name is filled', () => {
            renderSheet({ mode: 'create' });
            const createBtn = screen.getByRole('button', { name: 'Create group' }) as HTMLButtonElement;
            expect(createBtn.disabled).toBe(true);

            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Group' } });
            expect(createBtn.disabled).toBe(false);
        });

        it('submits trimmed name with default toggles and roles', () => {
            const { onSubmit } = renderSheet({ mode: 'create' });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: '  My Group  ' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create group' }));
            expect(onSubmit).toHaveBeenCalledWith({
                name: 'My Group',
                apiRole: 'USER',
                applicationRole: 'USER',
                apiProductRole: 'USER',
                lockApiRole: false,
                lockApiProductRole: false,
                lockApplicationRole: false,
                defaultGroupForNewApis: false,
                defaultGroupForNewApiProducts: false,
                defaultGroupForNewApplications: false,
                maxInvitation: '',
                systemInvitation: false,
                emailInvitation: false,
                notifyOnMemberAdded: true,
            });
        });

        it('toggles Associate with new APIs into the submitted payload', () => {
            const { onSubmit } = renderSheet({ mode: 'create' });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Group' } });
            fireEvent.click(screen.getByLabelText('Associate with new APIs'));
            fireEvent.click(screen.getByRole('button', { name: 'Create group' }));
            expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ defaultGroupForNewApis: true }));
        });

        it('accepts a positive Maximum members value', () => {
            const { onSubmit } = renderSheet({ mode: 'create' });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Group' } });
            fireEvent.change(screen.getByLabelText('Maximum members'), { target: { value: '10' } });
            expect(screen.queryByText('Enter a positive number or leave blank')).toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Create group' }));
            expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ maxInvitation: '10' }));
        });

        it('rejects a non-positive Maximum members value and blocks submit', () => {
            const { onSubmit } = renderSheet({ mode: 'create' });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Group' } });
            fireEvent.change(screen.getByLabelText('Maximum members'), { target: { value: '0' } });
            expect(screen.getByText('Enter a positive number or leave blank')).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Create group' }));
            expect(onSubmit).not.toHaveBeenCalled();
        });

        it('shows "Creating…" label while saving', () => {
            renderSheet({ mode: 'create', isSaving: true });
            expect(screen.queryByRole('button', { name: 'Creating…' })).not.toBeNull();
        });
    });

    describe('edit mode', () => {
        it('pre-fills every field from the existing group', () => {
            renderSheet({ mode: 'edit', group: EXISTING_GROUP });
            expect((screen.getByLabelText(/Name/i) as HTMLInputElement).value).toBe('Support Team');
            expect(screen.getByLabelText('Lock API role').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Lock API product role').getAttribute('aria-checked')).toBe('true');
            expect(screen.getByLabelText('Lock application role').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Associate with new APIs').getAttribute('aria-checked')).toBe('true');
            expect(screen.getByLabelText('Associate with new API products').getAttribute('aria-checked')).toBe('false');
            expect(screen.getByLabelText('Associate with new applications').getAttribute('aria-checked')).toBe('true');
            expect((screen.getByLabelText('Maximum members') as HTMLInputElement).value).toBe('25');
            expect(screen.getByLabelText('Allow invitation via user search').getAttribute('aria-checked')).toBe('true');
            expect(screen.getByLabelText('Allow invitation via email').getAttribute('aria-checked')).toBe('false');
            // disable_membership_notifications: true -> "Notify members when added" is off.
            expect(screen.getByLabelText('Notify members when added').getAttribute('aria-checked')).toBe('false');
            expect(screen.getAllByText('OWNER').length).toBeGreaterThan(0);
        });

        it('disables Save when nothing has changed', () => {
            renderSheet({ mode: 'edit', group: EXISTING_GROUP });
            expect((screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement).disabled).toBe(true);
        });

        it('enables Save after changing the name', () => {
            renderSheet({ mode: 'edit', group: EXISTING_GROUP });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'New Name' } });
            expect((screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement).disabled).toBe(false);
        });

        it('enables Save after toggling Lock API role', () => {
            renderSheet({ mode: 'edit', group: EXISTING_GROUP });
            fireEvent.click(screen.getByLabelText('Lock API role'));
            expect((screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement).disabled).toBe(false);
        });

        it('enables Save after toggling Notify members when added', () => {
            renderSheet({ mode: 'edit', group: EXISTING_GROUP });
            fireEvent.click(screen.getByLabelText('Notify members when added'));
            expect((screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement).disabled).toBe(false);
        });

        it('submits updated values for the existing group', () => {
            const { onSubmit } = renderSheet({ mode: 'edit', group: EXISTING_GROUP });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Updated Name' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save' }));
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: 'Updated Name',
                    apiRole: 'OWNER',
                    applicationRole: 'USER',
                    apiProductRole: 'USER',
                    lockApiProductRole: true,
                    maxInvitation: '25',
                    systemInvitation: true,
                    emailInvitation: false,
                    notifyOnMemberAdded: false,
                }),
            );
        });

        it('shows "Saving…" label while saving', () => {
            renderSheet({ mode: 'edit', group: EXISTING_GROUP, isSaving: true });
            expect(screen.queryByRole('button', { name: 'Saving…' })).not.toBeNull();
        });
    });

    describe('cancel', () => {
        it('invokes onClose when Cancel is clicked', () => {
            const { onClose } = renderSheet({ mode: 'create' });
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });

    describe('system roles', () => {
        const rolesWithSystem: GroupRole[] = [
            { name: 'USER', scope: 'API', default: true },
            { name: 'PRIMARY_OWNER', scope: 'API', system: true },
        ];

        it('excludes system roles from the picker', () => {
            renderSheet({ mode: 'create', apiRoles: rolesWithSystem });
            fireEvent.click(screen.getAllByRole('combobox')[0]);
            expect(screen.queryByRole('option', { name: 'PRIMARY_OWNER' })).toBeNull();
        });

        it('keeps an already-selected system role visible', () => {
            const groupWithSystemRole: Group = { ...EXISTING_GROUP, roles: { ...EXISTING_GROUP.roles, API: 'PRIMARY_OWNER' } };
            renderSheet({ mode: 'edit', group: groupWithSystemRole, apiRoles: rolesWithSystem });
            expect(screen.getAllByText('PRIMARY_OWNER').length).toBeGreaterThan(0);
        });
    });

    describe('role backfill on cold-cache create', () => {
        it('fills in default roles once they resolve, if Create was opened before they loaded', () => {
            const { rerender } = render(
                <GroupSheet
                    open
                    mode="create"
                    apiRoles={[]}
                    applicationRoles={[]}
                    apiProductRoles={[]}
                    rolesLoading
                    onClose={jest.fn()}
                    onSubmit={jest.fn()}
                    isSaving={false}
                />,
            );

            expect(screen.getByLabelText('Default API role').textContent).not.toContain('USER');

            // Roles resolve.
            rerender(
                <GroupSheet
                    open
                    mode="create"
                    apiRoles={API_ROLES}
                    applicationRoles={APPLICATION_ROLES}
                    apiProductRoles={API_PRODUCT_ROLES}
                    rolesLoading={false}
                    onClose={jest.fn()}
                    onSubmit={jest.fn()}
                    isSaving={false}
                />,
            );

            expect(screen.getByLabelText('Default API role').textContent).toContain('USER');
        });

        it('does not re-apply a default over a role the user explicitly cleared to None', () => {
            const { rerender } = render(
                <GroupSheet
                    open
                    mode="create"
                    apiRoles={API_ROLES}
                    applicationRoles={APPLICATION_ROLES}
                    apiProductRoles={API_PRODUCT_ROLES}
                    rolesLoading={false}
                    onClose={jest.fn()}
                    onSubmit={jest.fn()}
                    isSaving={false}
                />,
            );

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'None' }));
            expect(screen.getByLabelText('Default API role').textContent).not.toContain('USER');

            // Simulate a background roles refetch producing a fresh array reference with the same data.
            rerender(
                <GroupSheet
                    open
                    mode="create"
                    apiRoles={[...API_ROLES]}
                    applicationRoles={APPLICATION_ROLES}
                    apiProductRoles={API_PRODUCT_ROLES}
                    rolesLoading={false}
                    onClose={jest.fn()}
                    onSubmit={jest.fn()}
                    isSaving={false}
                />,
            );

            expect(screen.getByLabelText('Default API role').textContent).not.toContain('USER');
        });
    });
});
