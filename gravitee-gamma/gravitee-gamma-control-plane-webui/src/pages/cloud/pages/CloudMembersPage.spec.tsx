/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { CloudMembersPage } from './CloudMembersPage';

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/environments/default/cloud/settings/members']}>
            <CloudMembersPage />
        </MemoryRouter>,
    );
}

describe('CloudMembersPage', () => {
    it('renders Cockpit-style account members table', () => {
        renderPage();

        expect(screen.getByRole('heading', { name: 'Account members' })).toBeTruthy();
        expect(screen.getByText('Karen Rai')).toBeTruthy();
        expect(screen.getByText('karen.rai@graviteesource.com')).toBeTruthy();
        expect(screen.getByText('ACCOUNT_PRIMARY_OWNER')).toBeTruthy();
        expect(screen.getByTestId('add-member-btn')).toBeTruthy();
        expect(screen.getByTestId('account-member-row-member-1')).toBeTruthy();
    });

    it('hides actions for readonly members', () => {
        renderPage();

        const primaryOwnerRow = screen.getByTestId('account-member-row-member-3');
        expect(primaryOwnerRow.querySelector('button[aria-label*="Edit"]')).toBeNull();
        expect(primaryOwnerRow.querySelector('button[aria-label*="Delete"]')).toBeNull();
    });
});
