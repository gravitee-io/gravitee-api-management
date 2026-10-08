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
import { screen } from '@testing-library/react';

import { GroupMembersSection } from './GroupMembersSection';
import type { GroupMember } from '../types/groupMembers';

const ALICE: GroupMember = { id: 'u1', displayName: 'Alice', roles: { GROUP: 'OWNER', APPLICATION: 'USER' } };
const BOB: GroupMember = { id: 'u2', displayName: 'Bob', roles: { GROUP: 'USER' } };

describe('GroupMembersSection', () => {
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
    });

    it('shows the group-scope role of a member when no role resolver is given', () => {
        renderWithGraphene(<GroupMembersSection groupName="Developers" members={[ALICE]} />);

        expect(screen.getByRole('table')).toBeInTheDocument();
        const roleText = dataTableHarness().getRow('Alice').getCellText('Role');
        expect(roleText).toContain('OWNER');
        expect(roleText).not.toContain('USER');
    });

    it('shows a loading placeholder instead of the members table while members load', () => {
        renderWithGraphene(<GroupMembersSection groupName="Developers" members={[ALICE]} isLoading />);

        expect(screen.queryByRole('table')).not.toBeInTheDocument();
        expect(screen.queryByText('Alice')).not.toBeInTheDocument();
        expect(screen.getByText('Developers')).toBeInTheDocument();
    });

    it.each([
        ['1 inherited member', [ALICE]],
        ['2 inherited members', [ALICE, BOB]],
    ])('shows the inherited member count as %s', (expected, members) => {
        renderWithGraphene(<GroupMembersSection groupName="Developers" members={members} />);

        expect(screen.getByText(expected)).toBeInTheDocument();
    });
});
