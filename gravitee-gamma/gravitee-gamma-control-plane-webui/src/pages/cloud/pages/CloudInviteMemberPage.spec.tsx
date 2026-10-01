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
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { CloudInviteMemberPage } from './CloudInviteMemberPage';

function renderPage(initialEntry = '/environments/default/cloud/settings/invite-member') {
    return render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <CloudInviteMemberPage />
        </MemoryRouter>,
    );
}

describe('CloudInviteMemberPage', () => {
    it('renders Cockpit-style invite member form', () => {
        renderPage();

        expect(screen.getByRole('heading', { name: 'Invite member to account' })).toBeTruthy();
        expect(screen.getByTestId('member-email-input')).toBeTruthy();
        expect(screen.getByTestId('member-role-select')).toBeTruthy();
        expect((screen.getByTestId('send-invitation-btn') as HTMLButtonElement).disabled).toBe(true);
    });

    it('prefills email from query param', () => {
        renderPage('/environments/default/cloud/settings/invite-member?name=alice%40example.com');

        expect((screen.getByTestId('member-email-input') as HTMLInputElement).value).toBe('alice@example.com');
    });

    it('keeps send invitation disabled until email and role are valid', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.type(screen.getByTestId('member-email-input'), 'new.user@example.com');
        expect((screen.getByTestId('send-invitation-btn') as HTMLButtonElement).disabled).toBe(true);

        await user.clear(screen.getByTestId('member-email-input'));
        await user.type(screen.getByTestId('member-email-input'), 'not-an-email');
        expect((screen.getByTestId('send-invitation-btn') as HTMLButtonElement).disabled).toBe(true);
    });
});
