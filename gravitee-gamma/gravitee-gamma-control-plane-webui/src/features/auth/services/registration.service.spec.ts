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
import { fetchCustomUserFields, submitRegistration } from './registration.service';
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
