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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ResetPasswordPage } from './ResetPasswordPage';
import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { buildBootstrapConfig, TEST_MANAGEMENT_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

const VALID_TOKEN =
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
    'eyJzdWIiOiJ1c2VyLTEiLCJlbWFpbCI6Im5vcm0xQGdtYWlsLmNvbSIsImZpcnN0bmFtZSI6Im5vcm0xIiwibGFzdG5hbWUiOiJub3JtMSIsImV4cCI6OTk5OTk5OTk5OTk5fQ.' +
    'signature';

const EXPIRED_TOKEN =
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
    'eyJzdWIiOiJ1c2VyLTEiLCJlbWFpbCI6Im5vcm0xQGdtYWlsLmNvbSIsImZpcnN0bmFtZSI6Im5vcm0xIiwibGFzdG5hbWUiOiJub3JtMSIsImV4cCI6MTAwMDAwMDAwMH0.' +
    'signature';

function renderResetPasswordPage(token = VALID_TOKEN) {
    useBootstrapStore.setState({
        config: buildBootstrapConfig(),
        loading: false,
        error: null,
    });

    return render(
        <MemoryRouter initialEntries={[`/reset-password/${token}`]}>
            <Routes>
                <Route path="/reset-password/:token" element={<ResetPasswordPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('ResetPasswordPage', () => {
    it('renders user details from the token and submits the new password', async () => {
        const user = userEvent.setup();
        const changePasswordTracker = trackHandler('post', `${TEST_MANAGEMENT_BASE}/users/user-1/changePassword`, null, 204);

        const { container } = renderResetPasswordPage();

        await waitFor(() => {
            expect(screen.getByText('At least 12 characters')).toBeTruthy();
        });

        // The account is shown as context, not as form controls the reader cannot fill.
        expect(screen.getByText('norm1 norm1')).toBeTruthy();
        expect(screen.getByText('norm1@gmail.com')).toBeTruthy();
        expect(screen.queryByLabelText('First name')).toBeNull();
        expect(screen.queryByLabelText('Last name')).toBeNull();
        expect(screen.queryByLabelText('Email')).toBeNull();

        await user.type(screen.getByLabelText('Password'), 'NewPassword1!a');
        await user.type(screen.getByLabelText('Confirm password'), 'NewPassword1!a');

        const submitButton = screen.getByRole('button', { name: 'Reset password' }) as HTMLButtonElement;
        expect(submitButton.disabled).toBe(false);
        await user.click(submitButton);

        await waitFor(() => expect(changePasswordTracker.callCount).toBe(1));
        expect(screen.getByText('Password updated')).toBeTruthy();
        // Nothing is left to choose, so the instruction goes with the form.
        expect(container.querySelector('[data-slot="card-description"]')).toBeNull();
    });

    it('instructs the outcome that can be acted on', async () => {
        renderResetPasswordPage();

        await screen.findByLabelText('Password');
        expect(screen.getByText('Choose a new password for your account.')).toBeTruthy();
    });

    it('does not instruct a dead link', () => {
        // Nothing here can be acted on, so telling the reader to choose a password is wrong.
        const { container } = renderResetPasswordPage('invalid-token');

        expect(container.querySelector('[data-slot="card-description"]')).toBeNull();
    });

    it('lets the person see the password they are typing', async () => {
        const user = userEvent.setup();
        renderResetPasswordPage();

        const field = await screen.findByLabelText('Password');
        expect(field.getAttribute('type')).toBe('password');

        // Two fields, two toggles; the first belongs to the password being chosen.
        await user.click(screen.getAllByRole('button', { name: 'Show password' })[0]);

        expect(screen.getByLabelText('Password').getAttribute('type')).toBe('text');
    });

    it('shows an error when the token is invalid', () => {
        renderResetPasswordPage('invalid-token');

        expect(screen.getByText("This reset link isn't valid. Ask your administrator to send a new one.")).toBeTruthy();
    });

    it('shows an error when the token is expired', () => {
        renderResetPasswordPage(EXPIRED_TOKEN);

        expect(screen.getByText('This reset link has expired. Ask your administrator to send a new one.')).toBeTruthy();
    });

    it('keeps submit disabled when passwords do not match', async () => {
        const user = userEvent.setup();

        renderResetPasswordPage();

        await waitFor(() => {
            expect(screen.getByText('At least 12 characters')).toBeTruthy();
        });

        await user.type(screen.getByLabelText('Password'), 'NewPassword1!a');
        await user.type(screen.getByLabelText('Confirm password'), 'DifferentPassword1!a');

        expect(screen.getByText('Both passwords must match.')).toBeTruthy();
        expect((screen.getByRole('button', { name: 'Reset password' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('shows the API error message when password reset fails', async () => {
        const user = userEvent.setup();
        server.use(
            http.post(`${TEST_MANAGEMENT_BASE}/users/user-1/changePassword`, () =>
                HttpResponse.json({ message: 'Password does not meet policy requirements.' }, { status: 400 }),
            ),
        );

        renderResetPasswordPage();

        await waitFor(() => {
            expect(screen.getByText('At least 12 characters')).toBeTruthy();
        });

        await user.type(screen.getByLabelText('Password'), 'NewPassword1!a');
        await user.type(screen.getByLabelText('Confirm password'), 'NewPassword1!a');
        await user.click(screen.getByRole('button', { name: 'Reset password' }));

        expect(await screen.findByText('Password does not meet policy requirements.')).toBeTruthy();
    });

    it('blocks submit when password policy cannot be loaded', async () => {
        server.use(
            http.get(`${TEST_MANAGEMENT_BASE}/configuration/password-policy`, () =>
                HttpResponse.json({ message: 'Service unavailable' }, { status: 503 }),
            ),
        );

        renderResetPasswordPage();

        expect(await screen.findByText("Couldn't load the password rules. Refresh the page to try again.")).toBeTruthy();
        expect((screen.getByRole('button', { name: 'Reset password' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('blocks submit when password policy returns no rules', async () => {
        server.use(
            http.get(`${TEST_MANAGEMENT_BASE}/configuration/password-policy`, () =>
                HttpResponse.json({ description: 'Policy', pattern: '.*', rules: [] }),
            ),
        );

        renderResetPasswordPage();

        expect(await screen.findByText("Couldn't load the password rules. Refresh the page to try again.")).toBeTruthy();
        expect((screen.getByRole('button', { name: 'Reset password' }) as HTMLButtonElement).disabled).toBe(true);
    });
});
