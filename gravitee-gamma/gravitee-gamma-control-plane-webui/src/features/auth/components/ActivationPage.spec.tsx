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

import { ActivationPage } from './ActivationPage';
import { TEST_MANAGEMENT_BASE } from '../../../testing/factories';
import { seedBootstrap, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

const FINALIZE_URL = `${TEST_MANAGEMENT_BASE}/users/registration/finalize`;

/** sub `user-1`, norm1 norm1 <norm1@gmail.com>, expiring far in the future. */
const VALID_TOKEN =
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
    'eyJzdWIiOiJ1c2VyLTEiLCJlbWFpbCI6Im5vcm0xQGdtYWlsLmNvbSIsImZpcnN0bmFtZSI6Im5vcm0xIiwibGFzdG5hbWUiOiJub3JtMSIsImV4cCI6OTk5OTk5OTk5OTk5fQ.' +
    'signature';

/** The same claims, expired in 2001. */
const EXPIRED_TOKEN =
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
    'eyJzdWIiOiJ1c2VyLTEiLCJlbWFpbCI6Im5vcm0xQGdtYWlsLmNvbSIsImZpcnN0bmFtZSI6Im5vcm0xIiwibGFzdG5hbWUiOiJub3JtMSIsImV4cCI6MTAwMDAwMDAwMH0.' +
    'signature';

const PASSWORD = 'NewPassword1!a';

function renderActivationPage(token = VALID_TOKEN) {
    seedBootstrap();

    return render(
        <MemoryRouter initialEntries={[`/registration/${token}`]}>
            <Routes>
                <Route path="/registration/:token" element={<ActivationPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

function answerFinalize(body: { message: string; technicalCode: string }, status: number) {
    server.use(http.post(FINALIZE_URL, () => HttpResponse.json({ ...body, http_status: status }, { status })));
}

async function submitPassword(password = PASSWORD, confirmation = password) {
    const user = userEvent.setup();
    await screen.findByText('At least 12 characters');
    await user.type(screen.getByLabelText('Password'), password);
    await user.type(screen.getByLabelText('Confirm password'), confirmation);
    await user.click(screen.getByRole('button', { name: 'Activate account' }));
    return user;
}

describe('ActivationPage', () => {
    it('shows whose account this is as one line of context, not as fields', async () => {
        renderActivationPage();

        await screen.findByLabelText('Password');
        expect(screen.getByText('norm1 norm1')).toBeTruthy();
        expect(screen.getByText(/norm1@gmail\.com/)).toBeTruthy();
        expect(screen.queryByLabelText('First name')).toBeNull();
        expect(screen.queryByLabelText('Last name')).toBeNull();
        expect(screen.queryByLabelText('Email')).toBeNull();
    });

    it('submits the token, the password and the identity the token carries, then offers sign-in', async () => {
        const tracker = trackHandler('post', FINALIZE_URL, { id: 'user-1', status: 'ACTIVE' });
        renderActivationPage();

        await submitPassword();

        await waitFor(() => expect(tracker.callCount).toBe(1));
        expect(tracker.lastCall?.body).toEqual({ token: VALID_TOKEN, password: PASSWORD, firstname: 'norm1', lastname: 'norm1' });
        expect((await screen.findByRole('status')).textContent).toContain('Your account is ready.');
        expect(screen.getByRole('link', { name: 'Sign in' }).getAttribute('href')).toBe('/login');
        expect(screen.queryByLabelText('Password')).toBeNull();
        // The submit button went with the form; focus lands on the outcome rather than the body.
        expect(document.activeElement?.textContent).toBe('Account activated');
    });

    it('says an administrator has to approve the account, and offers no sign-in that would fail', async () => {
        answerFinalize(
            {
                message: 'The registration request is awaiting approval by an administrator.',
                technicalCode: 'user.registration.pendingApproval',
            },
            409,
        );
        renderActivationPage();

        await submitPassword();

        expect(await screen.findByText('Waiting for approval')).toBeTruthy();
        expect(screen.getByRole('status').textContent).toContain('An administrator needs to approve your account');
        expect(screen.queryByRole('link', { name: 'Sign in' })).toBeNull();
        expect(screen.getByRole('link', { name: 'Back to sign in' })).toBeTruthy();
    });

    it('treats an explicit PENDING status as awaiting approval', async () => {
        trackHandler('post', FINALIZE_URL, { id: 'user-1', status: 'PENDING' });
        renderActivationPage();

        await submitPassword();

        expect(await screen.findByText('Waiting for approval')).toBeTruthy();
    });

    it('points a link that was already used at sign-in', async () => {
        answerFinalize({ message: 'User already finalized in organization DEFAULT.', technicalCode: 'user.finalized' }, 400);
        renderActivationPage();

        await submitPassword();

        expect(await screen.findByText('Account already activated')).toBeTruthy();
        expect(screen.getByRole('link', { name: 'Sign in' })).toBeTruthy();
    });

    it('replaces the form when the server says the link can no longer be used', async () => {
        answerFinalize({ message: 'User [user-1] cannot be found.', technicalCode: 'user.notFound' }, 404);
        renderActivationPage();

        await submitPassword();

        expect(
            await screen.findByText("This activation link can't be used anymore. Ask your administrator to send a new one."),
        ).toBeTruthy();
        expect(screen.queryByLabelText('Password')).toBeNull();
        // The server's wording names the account's internal id; the reader has no use for it.
        expect(screen.queryByText(/user-1/)).toBeNull();
        expect(document.activeElement?.textContent).toBe('Activate your account');
    });

    it('says activation is switched off, without calling the link dead, when registration is off', async () => {
        answerFinalize({ message: 'User registration service is unavailable.', technicalCode: 'user.registration.disabled' }, 503);
        renderActivationPage();

        await submitPassword();

        expect(await screen.findByText('Activation is turned off')).toBeTruthy();
        expect(
            screen.getByText('Account activation is turned off for now. Try this link again later, or contact your administrator.'),
        ).toBeTruthy();
        expect(screen.queryByText(/can't be used anymore/)).toBeNull();
        expect(screen.queryByLabelText('Password')).toBeNull();
    });

    it('replaces the form when the link is not a token, leaving focus where the page starts', () => {
        renderActivationPage('not-a-token');

        expect(screen.getByText("This activation link isn't valid. Ask your administrator to send a new one.")).toBeTruthy();
        expect(screen.queryByLabelText('Password')).toBeNull();
        expect(document.activeElement).toBe(document.body);
    });

    it('replaces the form when the link has expired', () => {
        renderActivationPage(EXPIRED_TOKEN);

        expect(screen.getByText('This activation link has expired. Ask your administrator to send a new one.')).toBeTruthy();
        expect(screen.queryByLabelText('Password')).toBeNull();
    });

    it('puts a refused password on the password field in its own words, and retires it once the password changes', async () => {
        answerFinalize({ message: 'The password is not valid according to policy rules.', technicalCode: 'passwordFormat.invalid' }, 400);
        renderActivationPage();

        const user = await submitPassword();

        const message = await screen.findByText("This password doesn't meet the password policy. Choose a different one.");
        // As on sign-up, the server's wording never reaches an anonymous reader.
        expect(screen.queryByText(/not valid according to policy rules/)).toBeNull();
        const field = screen.getByLabelText('Password');
        expect(field.getAttribute('aria-invalid')).toBe('true');
        expect(field.getAttribute('aria-describedby')?.split(' ')).toContain(message.id);

        await user.type(field, 'x');

        expect(screen.queryByText("This password doesn't meet the password policy. Choose a different one.")).toBeNull();
        expect(field.getAttribute('aria-invalid')).toBeNull();
    });

    it("reports a server failure above the form in the page's own words, keeping what was typed", async () => {
        answerFinalize({ message: 'java.lang.IllegalStateException: JWT secret is mandatory', technicalCode: 'unexpected' }, 500);
        renderActivationPage();

        await submitPassword();

        expect(await screen.findByText('Something went wrong while activating your account. Try again.')).toBeTruthy();
        expect(screen.queryByText(/IllegalStateException/)).toBeNull();
        expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe(PASSWORD);
    });

    it('keeps submit disabled while the two passwords differ', async () => {
        const user = userEvent.setup();
        renderActivationPage();

        await screen.findByText('At least 12 characters');
        await user.type(screen.getByLabelText('Password'), PASSWORD);
        await user.type(screen.getByLabelText('Confirm password'), 'DifferentPassword1!a');

        const mismatch = screen.getByText('Both passwords must match.');
        expect(screen.getByLabelText('Confirm password').getAttribute('aria-describedby')).toBe(mismatch.id);
        expect((screen.getByRole('button', { name: 'Activate account' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('describes the password field with its requirements', async () => {
        renderActivationPage();

        await screen.findByText('At least 12 characters');
        const describedBy = screen.getByLabelText('Password').getAttribute('aria-describedby') ?? '';
        const description = describedBy
            .split(' ')
            .map(id => document.getElementById(id)?.textContent ?? '')
            .join(' ');
        expect(description).toContain('At least 12 characters');
    });

    it('offers a password manager the address and two new-password fields', async () => {
        const { container } = renderActivationPage();

        await screen.findByLabelText('Password');
        expect(container.querySelector<HTMLInputElement>('input[autocomplete="username"]')?.value).toBe('norm1@gmail.com');
        expect(screen.getByLabelText('Password').getAttribute('autocomplete')).toBe('new-password');
        expect(screen.getByLabelText('Confirm password').getAttribute('autocomplete')).toBe('new-password');
    });

    it('blocks submit when the password rules cannot be loaded', async () => {
        server.use(
            http.get(`${TEST_MANAGEMENT_BASE}/configuration/password-policy`, () =>
                HttpResponse.json({ message: 'Service unavailable' }, { status: 503 }),
            ),
        );
        renderActivationPage();

        expect(await screen.findByText("Couldn't load the password rules. Refresh the page to try again.")).toBeTruthy();
        expect((screen.getByRole('button', { name: 'Activate account' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('keeps submit disabled, and says why, when the pattern refuses a password every listed rule accepts', async () => {
        const user = userEvent.setup();
        server.use(
            http.get(`${TEST_MANAGEMENT_BASE}/configuration/password-policy`, () =>
                // '\\d' defeats the parser, so only the length rule is derived from this pattern.
                HttpResponse.json({
                    description: '',
                    pattern: '^(?=.*\\d).{8,}$',
                    rules: [{ id: 'minLength', label: 'At least 8 characters', pattern: '^.{8,}$' }],
                }),
            ),
        );
        renderActivationPage();

        await screen.findByText('At least 8 characters');
        await user.type(screen.getByLabelText('Password'), 'abcdefghij');
        await user.type(screen.getByLabelText('Confirm password'), 'abcdefghij');

        expect(screen.getByText("This password doesn't meet the full policy yet. One of its requirements isn't listed here.")).toBeTruthy();
        expect((screen.getByRole('button', { name: 'Activate account' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('leaves the verdict to the server for a pattern this browser cannot read', async () => {
        const user = userEvent.setup();
        const tracker = trackHandler('post', FINALIZE_URL, { id: 'user-1', status: 'ACTIVE' });
        server.use(
            http.get(`${TEST_MANAGEMENT_BASE}/configuration/password-policy`, () =>
                // A possessive quantifier: valid for the server's Java engine, a syntax error in JavaScript.
                HttpResponse.json({
                    description: '',
                    pattern: '^[a-z]++$',
                    rules: [{ id: 'lowercase', label: 'Contains lowercase letter', pattern: '[a-z]' }],
                }),
            ),
        );
        renderActivationPage();

        await screen.findByText('Contains lowercase letter');
        await user.type(screen.getByLabelText('Password'), 'abcdefgh');
        await user.type(screen.getByLabelText('Confirm password'), 'abcdefgh');
        await user.click(screen.getByRole('button', { name: 'Activate account' }));

        await waitFor(() => expect(tracker.callCount).toBe(1));
    });
});

describe('ActivationPage, when the link expires while the page is open', () => {
    /** Past VALID_TOKEN's `exp`, which is 999999999999 seconds. */
    const AFTER_EXPIRY = 1_000_000_000_000_000;

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('says the link has expired instead of sending a request the server can only refuse', async () => {
        const user = userEvent.setup();
        const tracker = trackHandler('post', FINALIZE_URL, { id: 'user-1', status: 'ACTIVE' });
        renderActivationPage();

        await screen.findByText('At least 12 characters');
        await user.type(screen.getByLabelText('Password'), PASSWORD);
        await user.type(screen.getByLabelText('Confirm password'), PASSWORD);
        jest.spyOn(Date, 'now').mockReturnValue(AFTER_EXPIRY);
        await user.click(screen.getByRole('button', { name: 'Activate account' }));

        expect(await screen.findByText('This activation link has expired. Ask your administrator to send a new one.')).toBeTruthy();
        expect(tracker.callCount).toBe(0);
        expect(screen.queryByLabelText('Password')).toBeNull();
    });

    it('reads a server failure as expiry when the link ran out in flight', async () => {
        server.use(
            http.post(FINALIZE_URL, () => {
                jest.spyOn(Date, 'now').mockReturnValue(AFTER_EXPIRY);
                return HttpResponse.json(
                    { message: 'The Token has expired on 2026-09-01T00:00:00Z.', technicalCode: 'unexpected', http_status: 500 },
                    { status: 500 },
                );
            }),
        );
        renderActivationPage();

        await submitPassword();

        expect(await screen.findByText('This activation link has expired. Ask your administrator to send a new one.')).toBeTruthy();
        expect(screen.queryByText(/The Token has expired/)).toBeNull();
        expect(document.activeElement?.textContent).toBe('Activate your account');
    });
});
