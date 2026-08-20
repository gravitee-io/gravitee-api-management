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
import {
    adaptNotifierSchemaForForm,
    alertNotificationsIncompleteReason,
    areAlertNotificationsComplete,
    isNotifierConfigurationComplete,
} from './notifierSchema';

describe('adaptNotifierSchemaForForm', () => {
    it('marks enum arrays as unique so Graphene renders a multi-select (classic checkboxes)', () => {
        const schema = {
            type: 'object',
            properties: {
                authMethods: {
                    title: 'Allowed authentication methods',
                    type: 'array',
                    items: {
                        type: 'string',
                        enum: ['XOAUTH2', 'NTLM', 'PLAIN'],
                    },
                },
            },
        };

        const adapted = adaptNotifierSchemaForForm(schema);

        expect(adapted.properties?.authMethods).toEqual(
            expect.objectContaining({
                type: 'array',
                uniqueItems: true,
                items: { type: 'string', enum: ['XOAUTH2', 'NTLM', 'PLAIN'] },
            }),
        );
        expect(schema.properties.authMethods).not.toHaveProperty('uniqueItems');
    });

    it('does not treat object-item arrays as a multi-select', () => {
        const schema = {
            type: 'object',
            properties: {
                headers: {
                    type: 'array',
                    items: {
                        type: 'object',
                        properties: { key: { type: 'string' }, value: { type: 'string' } },
                    },
                },
            },
        };

        const adapted = adaptNotifierSchemaForForm(schema);

        expect(adapted.properties?.headers).not.toHaveProperty('uniqueItems');
    });
});

const EMAIL_SCHEMA = {
    type: 'object',
    properties: {
        host: { type: 'string' },
        port: { type: 'integer' },
        username: { type: 'string' },
        from: { type: 'string' },
        to: { type: 'string' },
        subject: { type: 'string' },
        body: { type: 'string' },
        method: { type: 'string', default: 'POST' },
        url: { type: 'string' },
    },
    required: ['host', 'port', 'from', 'to', 'subject', 'body'],
};

describe('isNotifierConfigurationComplete', () => {
    it('is false until classic email required fields are filled (username is optional)', () => {
        expect(isNotifierConfigurationComplete(EMAIL_SCHEMA, { host: 'smtp.example.com' })).toBe(false);
        expect(
            isNotifierConfigurationComplete(EMAIL_SCHEMA, {
                host: 'smtp.example.com',
                port: 587,
                from: 'alerts@example.com',
                to: 'ops@example.com',
                subject: 'Alert',
                body: '${alert}',
            }),
        ).toBe(true);
    });

    it('treats blank strings as missing required values', () => {
        expect(
            isNotifierConfigurationComplete(EMAIL_SCHEMA, {
                host: 'smtp.example.com',
                port: 587,
                from: 'alerts@example.com',
                to: '   ',
                subject: 'Alert',
                body: '${alert}',
            }),
        ).toBe(false);
    });

    it('applies schema defaults so webhook method need not be re-entered', () => {
        const webhook = {
            type: 'object',
            properties: {
                method: { type: 'string', default: 'POST' },
                url: { type: 'string' },
            },
            required: ['url', 'method'],
        };
        expect(isNotifierConfigurationComplete(webhook, {})).toBe(false);
        expect(isNotifierConfigurationComplete(webhook, { url: 'https://example.com' })).toBe(true);
    });
});

describe('areAlertNotificationsComplete', () => {
    it('is true when there are no notifications', () => {
        expect(areAlertNotificationsComplete([], {}, false)).toBe(true);
    });

    it('is false when a notification has no channel', () => {
        expect(areAlertNotificationsComplete([{ type: '', configuration: {} }], {}, false)).toBe(false);
    });

    it('is false while the notifier schema is loading', () => {
        expect(areAlertNotificationsComplete([{ type: 'email-notifier', configuration: {} }], {}, true)).toBe(false);
    });

    it('does not block save when the notifier schema request failed and errors are treated as complete', () => {
        expect(
            areAlertNotificationsComplete(
                [{ type: 'email-notifier', configuration: { to: 'ops@example.com' } }],
                {},
                false,
                new Set(['email-notifier']),
                { treatSchemaErrorAsComplete: true },
            ),
        ).toBe(true);
    });

    it('fails closed on create when the notifier schema request failed', () => {
        expect(
            areAlertNotificationsComplete(
                [{ type: 'email-notifier', configuration: { to: 'ops@example.com' } }],
                {},
                false,
                new Set(['email-notifier']),
            ),
        ).toBe(false);
    });
});

describe('alertNotificationsIncompleteReason', () => {
    it('explains a missing channel', () => {
        expect(alertNotificationsIncompleteReason([{ type: '', configuration: {} }], {}, false)).toBe(
            'Select a channel for each notification.',
        );
    });

    it('explains a schema load failure on create', () => {
        expect(
            alertNotificationsIncompleteReason([{ type: 'email-notifier', configuration: {} }], {}, false, new Set(['email-notifier'])),
        ).toBe('Notification settings could not be loaded.');
    });
});
