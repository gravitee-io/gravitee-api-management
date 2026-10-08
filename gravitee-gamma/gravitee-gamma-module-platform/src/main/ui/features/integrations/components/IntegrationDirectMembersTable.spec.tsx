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

import { IntegrationDirectMembersTable } from './IntegrationDirectMembersTable';
import type { IntegrationMember } from '../types/integrationMembers';

function renderTable(members: IntegrationMember[]) {
    renderWithGraphene(<IntegrationDirectMembersTable members={members} />);
}

describe('IntegrationDirectMembersTable', () => {
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

    it.each<[scenario: string, members: IntegrationMember[], expectedRows: [name: string | RegExp, role: RegExp][]]>([
        [
            'two members with different roles',
            [
                { id: 'user-1', displayName: 'Jane Doe', roles: [{ name: 'USER', scope: 'INTEGRATION' }] },
                { id: 'user-2', displayName: 'John Smith', roles: [{ name: 'OWNER', scope: 'INTEGRATION' }] },
            ],
            [
                ['Jane Doe', /^user$/i],
                ['John Smith', /^owner$/i],
            ],
        ],
        [
            'a member whose integration role is not listed first, a member with no role, and a member without roles',
            [
                {
                    id: 'user-1',
                    displayName: 'Jane Doe',
                    roles: [
                        { name: 'API_PUBLISHER', scope: 'ENVIRONMENT' },
                        { name: 'OWNER', scope: 'INTEGRATION' },
                    ],
                },
                { id: 'user-2', displayName: 'John Smith', roles: [] },
                { id: 'user-3', displayName: 'No Roles' },
            ],
            [
                ['Jane Doe', /^owner$/i],
                ['John Smith', /^—$/],
                ['No Roles', /^—$/],
            ],
        ],
        [
            'a member with no integration-scope role',
            [{ id: 'user-1', displayName: 'Jane Doe', roles: [{ name: 'API_PUBLISHER', scope: 'ENVIRONMENT' }] }],
            [['Jane Doe', /^api publisher$/i]],
        ],
        ['a member without a display name', [{ id: 'user-1', roles: [{ name: 'USER', scope: 'INTEGRATION' }] }], [[/^\?$/, /^user$/i]]],
    ])("shows one row per member with the member's name and role for %s", (_scenario, members, expectedRows) => {
        renderTable(members);

        const rows = dataTableHarness()
            .getRows()
            .map(row => ({ name: row.getCellText('Name'), role: row.getCellText('Role') }));

        expect(rows).toEqual(
            expectedRows.map(([name, role]) => ({
                name: name instanceof RegExp ? expect.stringMatching(name) : expect.stringContaining(name),
                role: expect.stringMatching(role),
            })),
        );
    });

    it('shows Primary Owner as the role of the Primary Owner row', () => {
        renderTable([
            { id: 'user-1', displayName: 'Paula Owner', roles: [{ name: 'PRIMARY_OWNER', scope: 'INTEGRATION' }] },
            { id: 'user-2', displayName: 'Jane Doe', roles: [{ name: 'USER', scope: 'INTEGRATION' }] },
        ]);

        expect(dataTableHarness().getRow('Paula Owner').getCellText('Role')).toBe('Primary Owner');
        expect(dataTableHarness().getRow('Jane Doe').getCellText('Role')).toBe('User');
    });
});
