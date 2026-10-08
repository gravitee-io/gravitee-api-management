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
import { adaptSharedConfigurationSchemaForForm, sanitizeSharedConfigurationValues } from './sharedConfigurationSchema';

describe('sanitizeSharedConfigurationValues', () => {
    it('removes empty path so JKS content-only stores satisfy oneOf', () => {
        const sanitized = sanitizeSharedConfigurationValues({
            ssl: {
                trustStore: { type: 'JKS', password: 'secret', content: 'fdf', path: '' },
                keyStore: { type: 'JKS', password: 'secret', content: 'aa', path: '', alias: 'a' },
            },
        });

        expect(sanitized.ssl).toEqual({
            trustStore: { type: 'JKS', password: 'secret', content: 'fdf' },
            keyStore: { type: 'JKS', password: 'secret', content: 'aa', alias: 'a' },
        });
    });

    it('leaves non-empty path and content untouched', () => {
        const input = {
            ssl: {
                trustStore: { type: 'JKS', password: 'secret', path: '/tmp/trust.jks' },
            },
        };
        expect(sanitizeSharedConfigurationValues(input)).toEqual(input);
    });
});

describe('adaptSharedConfigurationSchemaForForm', () => {
    it('strips gioConfig.el from SSL store field definitions', () => {
        const adapted = adaptSharedConfigurationSchemaForForm({
            type: 'object',
            properties: {
                ssl: {
                    type: 'object',
                    properties: {
                        trustStore: {
                            type: 'object',
                            properties: {
                                password: {
                                    type: 'string',
                                    gioConfig: { el: true },
                                },
                            },
                        },
                    },
                },
            },
            gioExternalDefinitions: {
                sslTrustStorePassword: {
                    type: 'string',
                    gioConfig: { el: true },
                },
            },
        });

        const password = (
            adapted.properties as Record<string, { properties: Record<string, { properties: Record<string, { gioConfig?: unknown }> }> }>
        ).ssl.properties.trustStore.properties.password;
        expect(password.gioConfig).toBeUndefined();
        const defs = adapted.gioExternalDefinitions as Record<string, { gioConfig?: unknown }>;
        expect(defs.sslTrustStorePassword.gioConfig).toBeUndefined();
    });

    it('labels HTTP header table columns KEY and VALUE', () => {
        const adapted = adaptSharedConfigurationSchemaForForm({
            type: 'object',
            definitions: {
                headers: {
                    type: 'array',
                    gioConfig: { uiType: 'gio-headers-array' },
                    items: {
                        type: 'object',
                        properties: {
                            name: { type: 'string', title: 'Name' },
                            value: { type: 'string', title: 'Value' },
                        },
                    },
                },
            },
        });

        const headers = (
            adapted.definitions as { headers: { items: { properties: { name: { title: string }; value: { title: string } } } } }
        ).headers;
        expect(headers.items.properties.name.title).toBe('KEY');
        expect(headers.items.properties.value.title).toBe('VALUE');
    });

    it('removes the SSL store EL banner and keeps an unrelated store description', () => {
        const adapted = adaptSharedConfigurationSchemaForForm({
            type: 'object',
            properties: {
                ssl: {
                    type: 'object',
                    properties: {
                        trustStore: {
                            type: 'object',
                            description: 'All fields support EL and secrets',
                        },
                        keyStore: {
                            type: 'object',
                            description: 'Select the store implementation',
                        },
                    },
                },
            },
        });

        const ssl = (
            adapted.properties as {
                ssl: { properties: { trustStore: { description?: string }; keyStore: { description?: string } } };
            }
        ).ssl;
        expect(ssl.properties.trustStore.description).toBeUndefined();
        expect(ssl.properties.keyStore.description).toBe('Select the store implementation');
    });

    it('strips EL hints on a cyclic SSL store schema without overflowing the stack', () => {
        const trustStore: Record<string, unknown> = {
            type: 'object',
            properties: {
                password: { type: 'string', gioConfig: { el: true } },
            },
        };
        (trustStore.properties as Record<string, unknown>).self = trustStore;

        const adapted = adaptSharedConfigurationSchemaForForm({
            type: 'object',
            properties: {
                ssl: {
                    type: 'object',
                    properties: { trustStore },
                },
            },
        });

        const password = (
            adapted.properties as {
                ssl: { properties: { trustStore: { properties: { password: { gioConfig?: unknown } } } } };
            }
        ).ssl.properties.trustStore.properties.password;
        expect(password.gioConfig).toBeUndefined();
    });
});
