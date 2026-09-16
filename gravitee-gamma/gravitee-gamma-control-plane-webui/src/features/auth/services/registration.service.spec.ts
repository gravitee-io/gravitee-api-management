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
import { http, HttpResponse } from 'msw';

import { resetReCaptchaConfigCacheForTests } from './recaptcha.service';
import { fetchCustomUserFields, finalizeRegistration, submitRegistration } from './registration.service';
import { TEST_MANAGEMENT_BASE } from '../../../testing/factories';
import { respondWith, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

const REGISTRATION_URL = `${TEST_MANAGEMENT_BASE}/users/registration`;
const CUSTOM_FIELDS_URL = `${TEST_MANAGEMENT_BASE}/configuration/custom-user-fields`;

const IDENTITY = { firstname: 'Ada', lastname: 'Lovelace', email: 'ada@example.com' };

/** Fails the registration request the way the management API's `ErrorEntity` does. */
function rejectRegistration(body: { message: string; technicalCode?: string; parameters?: Record<string, string> }, status = 400) {
    server.use(http.post(REGISTRATION_URL, () => HttpResponse.json({ ...body, http_status: status }, { status })));
}

beforeEach(() => {
    resetReCaptchaConfigCacheForTests();
});

describe('fetchCustomUserFields', () => {
    it('reads the fields an administrator configured', async () => {
        respondWith('get', CUSTOM_FIELDS_URL, [
            { key: 'job_title', label: 'Job title', required: true },
            { key: 'team', label: 'Team', values: ['Platform', 'Payments'], required: false },
        ]);

        await expect(fetchCustomUserFields()).resolves.toEqual([
            { key: 'job_title', label: 'Job title', required: true },
            { key: 'team', label: 'Team', values: ['Platform', 'Payments'], required: false },
        ]);
    });

    it('reports a failed read rather than pretending there are no fields', async () => {
        respondWith('get', CUSTOM_FIELDS_URL, { message: 'Service unavailable' }, 503);

        await expect(fetchCustomUserFields()).rejects.toThrow('Service unavailable');
    });
});

describe('submitRegistration', () => {
    it('posts the identity and the custom field values, targeting the Gamma activation page', async () => {
        const tracker = trackHandler('post', REGISTRATION_URL, {});

        await expect(submitRegistration({ ...IDENTITY, customFields: { job_title: 'Analyst' } })).resolves.toEqual({
            outcome: 'submitted',
        });

        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.body).toEqual({
            firstname: 'Ada',
            lastname: 'Lovelace',
            email: 'ada@example.com',
            customFields: { job_title: 'Analyst' },
        });
        expect(new URL(tracker.lastCall!.url).searchParams.get('registrationTarget')).toBe('gamma');
    });

    it('omits customFields when the administrator configured none', async () => {
        const tracker = trackHandler('post', REGISTRATION_URL, {});

        await submitRegistration(IDENTITY);

        expect(tracker.lastCall?.body).toEqual({ firstname: 'Ada', lastname: 'Lovelace', email: 'ada@example.com' });
    });

    it('carries a reCAPTCHA token when reCAPTCHA is configured', async () => {
        server.use(http.get(`${TEST_MANAGEMENT_BASE}/console`, () => HttpResponse.json({ reCaptcha: { enabled: true, siteKey: 'site' } })));
        window.grecaptcha = {
            ready: (callback: () => void) => callback(),
            execute: () => Promise.resolve('recaptcha-token'),
        };
        // No script fetch: `loadReCaptchaScript` resolves on the element it finds already present.
        const script = document.createElement('script');
        script.id = 'gamma-recaptcha';
        document.head.appendChild(script);

        const tracker = trackHandler('post', REGISTRATION_URL, {});

        await submitRegistration(IDENTITY);

        expect(tracker.lastCall?.headers.get('X-Recaptcha-Token')).toBe('recaptcha-token');

        script.remove();
        delete window.grecaptcha;
    });

    it('sends no reCAPTCHA header when reCAPTCHA is off', async () => {
        const tracker = trackHandler('post', REGISTRATION_URL, {});

        await submitRegistration(IDENTITY);

        expect(tracker.lastCall?.headers.get('X-Recaptcha-Token')).toBeNull();
    });

    it('attributes a rejected email to the email field, without the server wording', async () => {
        rejectRegistration({ message: 'Value [ada] is not a valid email.', technicalCode: 'email.invalid' });

        await expect(submitRegistration({ ...IDENTITY, email: 'ada' })).resolves.toEqual({ outcome: 'email-rejected' });
    });

    it('says sign-up is off when the organization has turned registration off', async () => {
        rejectRegistration({ message: 'User registration service is unavailable.', technicalCode: 'user.registration.disabled' }, 503);

        await expect(submitRegistration(IDENTITY)).resolves.toEqual({
            outcome: 'rejected',
            message: 'Sign-up is turned off for this organization. Ask your administrator for an account.',
        });
    });

    it('never relays the server wording of a rejection no field owns', async () => {
        // A 400 that names the organization: the caller is anonymous, and nothing in it is written for them.
        rejectRegistration({ message: 'Gamma URL is not configured for organization: DEFAULT' });

        await expect(submitRegistration(IDENTITY)).resolves.toEqual({
            outcome: 'rejected',
            message: 'Something went wrong while sending your request. Try again.',
        });
    });

    it('never relays the server wording of a server failure', async () => {
        rejectRegistration({ message: 'java.lang.NullPointerException', technicalCode: 'unexpected' }, 500);

        await expect(submitRegistration(IDENTITY)).resolves.toEqual({
            outcome: 'rejected',
            message: 'Something went wrong while sending your request. Try again.',
        });
    });

    it('reports an unreachable server against the form', async () => {
        server.use(http.post(REGISTRATION_URL, () => HttpResponse.error()));

        await expect(submitRegistration(IDENTITY)).resolves.toEqual({
            outcome: 'rejected',
            message: 'Something went wrong while sending your request. Try again.',
        });
    });

    it('reports an already-registered address as submitted, so an anonymous caller learns nothing', async () => {
        // FOUND-303: `user.invalid` on this endpoint means only that the address is already taken.
        // Relaying it would hand an anonymous caller an account-existence oracle in plain language.
        rejectRegistration({
            message: 'User cannot be created.',
            technicalCode: 'user.invalid',
            parameters: { user: 'ada@example.com', source: 'gravitee' },
        });

        await expect(submitRegistration(IDENTITY)).resolves.toEqual({ outcome: 'submitted' });
    });
});

describe('finalizeRegistration', () => {
    const FINALIZE_URL = `${REGISTRATION_URL}/finalize`;
    const ACTIVATION = { token: 'activation-token', password: 'Correct-Horse-9', firstname: 'Ada', lastname: 'Lovelace' };

    function rejectFinalize(body: { message: string; technicalCode?: string }, status: number) {
        server.use(http.post(FINALIZE_URL, () => HttpResponse.json({ ...body, http_status: status }, { status })));
    }

    it('posts the token, the password and the identity from the token', async () => {
        const tracker = trackHandler('post', FINALIZE_URL, { id: 'user-1', status: 'ACTIVE' });

        await finalizeRegistration(ACTIVATION);

        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.body).toEqual(ACTIVATION);
    });

    it('carries a reCAPTCHA token when reCAPTCHA is configured', async () => {
        server.use(http.get(`${TEST_MANAGEMENT_BASE}/console`, () => HttpResponse.json({ reCaptcha: { enabled: true, siteKey: 'site' } })));
        window.grecaptcha = {
            ready: (callback: () => void) => callback(),
            execute: () => Promise.resolve('recaptcha-token'),
        };
        const script = document.createElement('script');
        script.id = 'gamma-recaptcha';
        document.head.appendChild(script);

        const tracker = trackHandler('post', FINALIZE_URL, { id: 'user-1', status: 'ACTIVE' });

        await finalizeRegistration(ACTIVATION);

        expect(tracker.lastCall?.headers.get('X-Recaptcha-Token')).toBe('recaptcha-token');

        script.remove();
        delete window.grecaptcha;
    });

    it('reports an account that is ready to sign in to', async () => {
        respondWith('post', FINALIZE_URL, { id: 'user-1', status: 'ACTIVE' });

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({ outcome: 'active' });
    });

    it('reports an account awaiting administrator approval', async () => {
        respondWith('post', FINALIZE_URL, { id: 'user-1', status: 'PENDING' });

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({ outcome: 'pending-approval' });
    });

    it('treats a success without an explicit PENDING as active, since the password was saved', async () => {
        // Finalize refuses PENDING, REJECTED and ARCHIVED accounts before it writes anything, so a 200
        // means the password is set. "Waiting for approval" here would outlive the link: the next
        // click answers `user.finalized`.
        respondWith('post', FINALIZE_URL, { id: 'user-1' });

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({ outcome: 'active' });
    });

    it('reports a pre-created account that is already awaiting approval as pending', async () => {
        rejectFinalize(
            {
                message: 'The registration request is awaiting approval by an administrator.',
                technicalCode: 'user.registration.pendingApproval',
            },
            409,
        );

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({ outcome: 'pending-approval' });
    });

    it('attributes a refused password to the password field, without the server wording', async () => {
        rejectFinalize({ message: 'The password is not valid according to policy rules.', technicalCode: 'passwordFormat.invalid' }, 400);

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({ outcome: 'password-rejected' });
    });

    it('reports a link that already set a password', async () => {
        rejectFinalize({ message: 'User already finalized in organization DEFAULT.', technicalCode: 'user.finalized' }, 400);

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({ outcome: 'link-used' });
    });

    it.each([
        ['user.notFound', 404],
        ['user.state.conflict', 409],
    ])('reports a link that can no longer be used (%s)', async (technicalCode, status) => {
        rejectFinalize({ message: 'Registration cannot be finalized.', technicalCode }, status);

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({ outcome: 'link-unusable' });
    });

    it('reports registration switched off as its own outcome, not as a dead link', async () => {
        // A live check of the organization's setting: the same link works again once it is back on.
        rejectFinalize({ message: 'User registration service is unavailable.', technicalCode: 'user.registration.disabled' }, 503);

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({ outcome: 'registration-off' });
    });

    it("replaces a server failure's own wording with the page's, so no exception text reaches the reader", async () => {
        rejectFinalize({ message: 'The Token has expired on 2026-09-01T00:00:00Z.', technicalCode: 'unexpected' }, 500);

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({
            outcome: 'rejected',
            message: 'Something went wrong while activating your account. Try again.',
        });
    });

    it('never relays the server wording of a client error it cannot attribute', async () => {
        rejectFinalize({ message: 'Gamma URL is not configured for organization: DEFAULT', technicalCode: 'errors.validation' }, 400);

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({
            outcome: 'rejected',
            message: 'Something went wrong while activating your account. Try again.',
        });
    });

    it('reports an unreachable server against the form', async () => {
        server.use(http.post(FINALIZE_URL, () => HttpResponse.error()));

        await expect(finalizeRegistration(ACTIVATION)).resolves.toEqual({
            outcome: 'rejected',
            message: 'Something went wrong while activating your account. Try again.',
        });
    });
});
