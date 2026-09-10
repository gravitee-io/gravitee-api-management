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
import { MemoryRouter } from 'react-router-dom';

import { SignUpPage } from './SignUpPage';
import { TEST_MANAGEMENT_BASE } from '../../../testing/factories';
import { respondWith, seedBootstrap, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';
import type { CustomUserField } from '../services/registration.service';

const REGISTRATION_URL = `${TEST_MANAGEMENT_BASE}/users/registration`;
const CUSTOM_FIELDS_URL = `${TEST_MANAGEMENT_BASE}/configuration/custom-user-fields`;

const JOB_TITLE: CustomUserField = { key: 'job_title', label: 'Job title', required: true };
const TEAM: CustomUserField = { key: 'team', label: 'Team', values: ['Platform', 'Payments'], required: false };

beforeAll(() => {
    // Radix's Select drives pointer capture and scrolling, neither of which jsdom implements.
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
});

function renderSignUpPage(customUserFields: CustomUserField[] = []) {
    respondWith('get', CUSTOM_FIELDS_URL, customUserFields);
    return render(
        <MemoryRouter>
            <SignUpPage />
        </MemoryRouter>,
    );
}

/** Waits out the custom-fields read, which submit is blocked on. */
async function fillIdentity(user: ReturnType<typeof userEvent.setup>, email = 'ada@example.com') {
    await user.type(await screen.findByLabelText(/First name/), 'Ada');
    await user.type(screen.getByLabelText(/Last name/), 'Lovelace');
    await user.type(screen.getByLabelText(/Email/), email);
}

function submitButton(): HTMLButtonElement {
    return screen.getByRole('button', { name: 'Request account' }) as HTMLButtonElement;
}

describe('SignUpPage', () => {
    describe('the identity form', () => {
        it('sends what was typed and reports the request as sent', async () => {
            const user = userEvent.setup();
            const tracker = trackHandler('post', REGISTRATION_URL, {});
            renderSignUpPage();

            await fillIdentity(user);
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            await waitFor(() => expect(tracker.callCount).toBe(1));
            expect(tracker.lastCall?.body).toEqual({ firstname: 'Ada', lastname: 'Lovelace', email: 'ada@example.com' });
        });

        it('rejects a malformed email in the field, before anything is sent', async () => {
            const user = userEvent.setup();
            const tracker = trackHandler('post', REGISTRATION_URL, {});
            renderSignUpPage();

            await fillIdentity(user, 'ada@example');
            await user.tab();

            expect(await screen.findByText('Enter a valid email address.')).toBeTruthy();
            expect(submitButton().disabled).toBe(true);
            expect(tracker.callCount).toBe(0);
        });

        it('holds the email to the rules the server applies, so the field and the server never disagree', async () => {
            const user = userEvent.setup();
            renderSignUpPage();

            // A one-letter TLD: a loose shape check accepts it, `EmailValidator` does not.
            await fillIdentity(user, 'ada@example.c');
            await user.tab();

            expect(await screen.findByText('Enter a valid email address.')).toBeTruthy();
            expect(submitButton().disabled).toBe(true);
        });

        it('shows a pending submit and cannot be fired twice', async () => {
            const user = userEvent.setup();
            let resolveRequest: () => void = () => undefined;
            const pending = new Promise<void>(resolve => {
                resolveRequest = resolve;
            });
            let calls = 0;
            server.use(
                http.post(REGISTRATION_URL, async () => {
                    calls += 1;
                    await pending;
                    return HttpResponse.json({});
                }),
            );
            renderSignUpPage();

            await fillIdentity(user);
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            const pendingButton = await screen.findByRole('button', { name: /Sending request/ });
            expect((pendingButton as HTMLButtonElement).disabled).toBe(true);
            await user.click(pendingButton);

            resolveRequest();
            await screen.findByText('Check your email');
            expect(calls).toBe(1);
        });

        it('renders a rejected email against the email field, not as a toast', async () => {
            const user = userEvent.setup();
            server.use(
                http.post(REGISTRATION_URL, () =>
                    HttpResponse.json(
                        { message: 'Value [ada@example.com] is not a valid email.', http_status: 400, technicalCode: 'email.invalid' },
                        { status: 400 },
                    ),
                ),
            );
            renderSignUpPage();

            await fillIdentity(user);
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            const emailError = await screen.findByText('Enter a valid email address.');
            expect(screen.getByLabelText(/Email/).closest('[data-slot="field"]')?.contains(emailError)).toBe(true);
            expect(screen.queryByText(/is not a valid email/)).toBeNull();
        });

        it('reports a rejection no field owns against the form, without the server wording', async () => {
            const user = userEvent.setup();
            server.use(
                http.post(REGISTRATION_URL, () =>
                    HttpResponse.json(
                        { message: 'Gamma URL is not configured for organization: DEFAULT', http_status: 400 },
                        { status: 400 },
                    ),
                ),
            );
            renderSignUpPage();

            await fillIdentity(user);
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            expect(await screen.findByText('Something went wrong while sending your request. Try again.')).toBeTruthy();
            expect(screen.queryByText(/Gamma URL/)).toBeNull();
            expect(screen.queryByText('Check your email')).toBeNull();
        });
    });

    describe('the configured user fields', () => {
        it('renders a fixed value list as a choice and everything else as free text', async () => {
            const user = userEvent.setup();
            renderSignUpPage([JOB_TITLE, TEAM]);

            expect((await screen.findByLabelText(/Job title/)).tagName).toBe('INPUT');

            const team = screen.getByRole('combobox', { name: /Team/ });
            await user.click(team);
            expect(screen.getByRole('option', { name: 'Platform' })).toBeTruthy();
            expect(screen.getByRole('option', { name: 'Payments' })).toBeTruthy();
        });

        it('marks which fields are required, since a form can mix the two', async () => {
            renderSignUpPage([JOB_TITLE, TEAM]);

            const requiredLabel = await screen.findByText((_, element) => element?.getAttribute('for') === 'sign-up-field-job_title');
            const optionalLabel = screen.getByText((_, element) => element?.getAttribute('for') === 'sign-up-field-team');

            expect(requiredLabel.textContent).toBe('Job title*');
            expect(optionalLabel.textContent).toBe('Team');
        });

        it('blocks submit until every required field is answered', async () => {
            const user = userEvent.setup();
            renderSignUpPage([JOB_TITLE, TEAM]);

            await fillIdentity(user);
            expect(submitButton().disabled).toBe(true);

            await user.type(screen.getByLabelText(/Job title/), 'Analyst');
            await waitFor(() => expect(submitButton().disabled).toBe(false));
        });

        it('sends the answers as customFields', async () => {
            const user = userEvent.setup();
            const tracker = trackHandler('post', REGISTRATION_URL, {});
            renderSignUpPage([JOB_TITLE, TEAM]);

            await fillIdentity(user);
            await user.type(screen.getByLabelText(/Job title/), 'Analyst');
            await user.click(screen.getByRole('combobox', { name: /Team/ }));
            await user.click(screen.getByRole('option', { name: 'Payments' }));
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            await waitFor(() => expect(tracker.callCount).toBe(1));
            expect(tracker.lastCall?.body).toEqual({
                firstname: 'Ada',
                lastname: 'Lovelace',
                email: 'ada@example.com',
                customFields: { job_title: 'Analyst', team: 'Payments' },
            });
        });

        it('leaves an unanswered optional field out of customFields', async () => {
            const user = userEvent.setup();
            const tracker = trackHandler('post', REGISTRATION_URL, {});
            renderSignUpPage([JOB_TITLE, TEAM]);

            await fillIdentity(user);
            await user.type(await screen.findByLabelText(/Job title/), 'Analyst');
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            await waitFor(() => expect(tracker.callCount).toBe(1));
            expect(tracker.lastCall?.body).toEqual({
                firstname: 'Ada',
                lastname: 'Lovelace',
                email: 'ada@example.com',
                customFields: { job_title: 'Analyst' },
            });
        });

        it('keeps the form short when no fields are configured', async () => {
            renderSignUpPage([]);

            await screen.findByLabelText(/First name/);
            expect(screen.queryByText('Additional details')).toBeNull();
        });

        it('surfaces a failed fields read without blocking sign-up on identity alone', async () => {
            const user = userEvent.setup();
            const tracker = trackHandler('post', REGISTRATION_URL, {});
            respondWith('get', CUSTOM_FIELDS_URL, { message: 'Service unavailable' }, 503);
            render(
                <MemoryRouter>
                    <SignUpPage />
                </MemoryRouter>,
            );

            expect(await screen.findByText("Couldn't load the additional questions")).toBeTruthy();
            expect(screen.getByText('You can still request an account. Your administrator may ask for the rest later.')).toBeTruthy();
            expect(screen.queryByText('Service unavailable')).toBeNull();

            await fillIdentity(user);
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            await waitFor(() => expect(tracker.callCount).toBe(1));
            expect(tracker.lastCall?.body).toEqual({ firstname: 'Ada', lastname: 'Lovelace', email: 'ada@example.com' });
        });
    });

    describe('composition', () => {
        it('offers sign in to someone who already has an account', async () => {
            renderSignUpPage();

            await screen.findByLabelText(/First name/);
            expect(screen.getByText(/Already have an account\?/)).toBeTruthy();
            expect(screen.getByRole('link', { name: 'Sign in' }).getAttribute('href')).toBe('/login');
        });

        it('groups identity and configured fields under their own headings', async () => {
            renderSignUpPage([JOB_TITLE]);

            await screen.findByLabelText(/Job title/);
            expect(screen.getByText('Your details')).toBeTruthy();
            expect(screen.getByText('Additional details')).toBeTruthy();
        });
    });

    describe('the sent state', () => {
        it('replaces the page, echoes the address back, and offers a route back to sign in', async () => {
            const user = userEvent.setup();
            trackHandler('post', REGISTRATION_URL, {});
            renderSignUpPage();

            await fillIdentity(user);
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            expect(await screen.findByText('Check your email')).toBeTruthy();
            expect(screen.queryByLabelText(/First name/)).toBeNull();
            expect(screen.queryByRole('button', { name: 'Request account' })).toBeNull();
            expect(screen.getByText('ada@example.com')).toBeTruthy();
            expect(screen.getByRole('link', { name: 'Back to sign in' }).getAttribute('href')).toBe('/login');
            expect(screen.queryByText(/Already have an account/)).toBeNull();
        });

        it('offers no resend, because the backend has no such endpoint', async () => {
            const user = userEvent.setup();
            trackHandler('post', REGISTRATION_URL, {});
            renderSignUpPage();

            await fillIdentity(user);
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            await screen.findByText('Check your email');
            expect(screen.queryByRole('button', { name: /resend|send again/i })).toBeNull();
        });

        it('points to the spam folder when requests are validated automatically', async () => {
            const user = userEvent.setup();
            seedBootstrap({ automaticValidationEnabled: true });
            trackHandler('post', REGISTRATION_URL, {});
            renderSignUpPage();

            await fillIdentity(user);
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            expect(await screen.findByText(/check your spam folder/)).toBeTruthy();
            expect(screen.queryByText(/administrator/)).toBeNull();
        });

        it('says an administrator approves the request first when requests are not validated automatically', async () => {
            const user = userEvent.setup();
            seedBootstrap({ automaticValidationEnabled: false });
            trackHandler('post', REGISTRATION_URL, {});
            renderSignUpPage();

            await fillIdentity(user);
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            expect(await screen.findByText(/An administrator reviews each request/)).toBeTruthy();
            expect(screen.queryByText(/spam folder/)).toBeNull();
        });

        it('is identical for an address that already has an account', async () => {
            const user = userEvent.setup();
            // FOUND-303: an anonymous caller must not be able to tell the two apart.
            server.use(
                http.post(REGISTRATION_URL, () =>
                    HttpResponse.json(
                        {
                            message: 'User cannot be created.',
                            http_status: 400,
                            technicalCode: 'user.invalid',
                            parameters: { user: 'ada@example.com', source: 'gravitee' },
                        },
                        { status: 400 },
                    ),
                ),
            );
            renderSignUpPage();

            await fillIdentity(user);
            await waitFor(() => expect(submitButton().disabled).toBe(false));
            await user.click(submitButton());

            expect(await screen.findByText('Check your email')).toBeTruthy();
            expect(screen.getByText('ada@example.com')).toBeTruthy();
            expect(screen.queryByText('User cannot be created.')).toBeNull();
        });
    });
});
