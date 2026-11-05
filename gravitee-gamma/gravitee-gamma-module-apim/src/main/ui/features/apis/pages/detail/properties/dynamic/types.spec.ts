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
import { fromDtoToFormState, fromFormStateToDto, fromFormStateToV4Dto, fromV4DtoToFormState } from './types';
import type { DynamicPropertyConfig, DynamicPropertyHttpClientOptions } from '../../../../types';

it('keeps a stored maxConcurrentConnections through a load and save', () => {
    const dto: DynamicPropertyConfig = {
        enabled: true,
        provider: 'HTTP',
        schedule: '*/30 * * * * *',
        configuration: {
            method: 'GET',
            url: 'https://api.example.com/properties',
            httpClientOptions: { maxConcurrentConnections: 10 },
        },
    };

    expect(fromFormStateToDto(fromDtoToFormState(dto)).configuration?.httpClientOptions?.maxConcurrentConnections).toBe(10);
});

describe('dynamic properties type', () => {
    it('sends the http-dynamic-properties type the Management API requires', () => {
        const dto: DynamicPropertyConfig = {
            enabled: true,
            provider: 'HTTP',
            configuration: { method: 'GET', url: 'https://api.example.com/properties' },
        };

        expect(fromFormStateToDto(fromDtoToFormState(dto)).type).toBe('http-dynamic-properties');
    });

    it('sends the type when dynamic properties are disabled', () => {
        expect(fromFormStateToDto(fromDtoToFormState({ enabled: false })).type).toBe('http-dynamic-properties');
    });
});

describe('encrypt on fetch', () => {
    const dto = (encryptOnFetch?: boolean): DynamicPropertyConfig => ({
        enabled: true,
        provider: 'HTTP',
        configuration: {
            method: 'GET',
            url: 'https://api.example.com/properties',
            ...(encryptOnFetch === undefined ? {} : { encryption: { encryptOnFetch } }),
        },
    });

    it('keeps a stored encryptOnFetch through a load and save', () => {
        expect(fromFormStateToDto(fromDtoToFormState(dto(true))).configuration?.encryption?.encryptOnFetch).toBe(true);
    });

    it('is off when the stored configuration has no encryption', () => {
        expect(fromFormStateToDto(fromDtoToFormState(dto())).configuration?.encryption?.encryptOnFetch).toBe(false);
    });
});

describe('V4 API configuration', () => {
    const stored: DynamicPropertyConfig = {
        enabled: true,
        type: 'http-dynamic-properties',
        overrideConfiguration: false,
        configuration: {
            schedule: '0 */5 * * * *',
            method: 'GET',
            url: 'https://api.example.com/properties',
            headers: [{ name: 'X-Key', value: 'abc' }],
            transformation: '[{"operation":"shift"}]',
            systemProxy: true,
            encryption: { encryptOnFetch: true },
            http: { version: 'HTTP_2', connectTimeout: 1234, maxConcurrentConnections: 7 },
            proxy: { enabled: true, useSystemProxy: false, type: 'SOCKS5', host: 'proxy.example.com', port: 3128 },
            ssl: { hostnameVerifier: false, trustAll: true },
        },
    };

    it('reads what the V4 service stores, including its schedule', () => {
        const form = fromV4DtoToFormState(stored);

        expect(form.schedule).toBe('0 */5 * * * *');
        expect(form.specification).toBe('[{"operation":"shift"}]');
        expect(form.useSystemProxy).toBe(true);
        expect(form.encryptOnFetch).toBe(true);
        expect(form.httpClient.connectTimeout).toBe(1234);
        expect(form.proxy).toEqual(expect.objectContaining({ enabled: true, type: 'SOCKS5', host: 'proxy.example.com', port: '3128' }));
        expect(form.ssl).toEqual(expect.objectContaining({ hostnameVerifier: false, trustAll: true }));
    });

    it('writes the keys the V4 service reads and none of the V2 ones', () => {
        const dto = fromFormStateToV4Dto(fromV4DtoToFormState(stored), stored);

        expect(dto.schedule).toBeUndefined();
        expect(dto.provider).toBeUndefined();
        expect(dto.type).toBe('http-dynamic-properties');
        expect(dto.configuration).toEqual(
            expect.objectContaining({
                schedule: '0 */5 * * * *',
                transformation: '[{"operation":"shift"}]',
                systemProxy: true,
                encryption: { encryptOnFetch: true },
            }),
        );
        for (const v2Key of ['specification', 'useSystemProxy', 'httpClientOptions', 'httpProxyOptions', 'sslOptions']) {
            expect(dto.configuration).not.toHaveProperty(v2Key);
        }
    });

    it('keeps the settings the form does not show through a load and save', () => {
        const dto = fromFormStateToV4Dto(fromV4DtoToFormState(stored), stored);

        expect(dto.overrideConfiguration).toBe(false);
        expect(dto.configuration?.http).toEqual(
            expect.objectContaining({ version: 'HTTP_2', connectTimeout: 1234, maxConcurrentConnections: 7 }),
        );
    });

    it('writes a complete configuration when the API has none yet', () => {
        const dto = fromFormStateToV4Dto(
            { ...fromV4DtoToFormState({ enabled: false }), enabled: true, url: 'https://api.example.com/properties' },
            undefined,
        );

        expect(dto.enabled).toBe(true);
        expect(dto.configuration?.transformation).toContain('"operation": "default"');
        expect(dto.configuration?.schedule).toBe('*/30 * * * * *');
    });

    it('reads the V2 keys of an API saved before the V4 shape was written', () => {
        const form = fromV4DtoToFormState({
            enabled: true,
            schedule: '0 */10 * * * *',
            configuration: {
                method: 'GET',
                url: 'https://api.example.com/properties',
                specification: '[{"operation":"remove"}]',
                useSystemProxy: true,
                httpClientOptions: { connectTimeout: 4321 },
                httpProxyOptions: { enabled: true, type: 'HTTP', host: 'old-proxy.example.com', port: 8080 },
                sslOptions: { trustAll: true },
            },
        });

        expect(form.schedule).toBe('0 */10 * * * *');
        expect(form.specification).toBe('[{"operation":"remove"}]');
        expect(form.useSystemProxy).toBe(true);
        expect(form.httpClient.connectTimeout).toBe(4321);
        expect(form.proxy.host).toBe('old-proxy.example.com');
        expect(form.ssl.trustAll).toBe(true);
    });

    it('prefers the V4 keys when an API stores both shapes', () => {
        const form = fromV4DtoToFormState({
            enabled: true,
            schedule: '0 */10 * * * *',
            configuration: { schedule: '0 */5 * * * *', transformation: 'v4', specification: 'v2' },
        });

        expect(form.schedule).toBe('0 */5 * * * *');
        expect(form.specification).toBe('v4');
    });

    it('drops the V2 keys, and the credentials they hold, that an earlier save left in the stored configuration', () => {
        const legacy: DynamicPropertyConfig = {
            enabled: true,
            schedule: '0 */10 * * * *',
            configuration: {
                url: 'https://api.example.com/properties',
                specification: '[{"operation":"remove"}]',
                useSystemProxy: false,
                httpClientOptions: { connectTimeout: 4321 },
                httpProxyOptions: { enabled: true, type: 'HTTP', host: 'old-proxy.example.com', port: 8080, password: 'proxy-secret' },
                sslOptions: { keyStore: { type: 'JKS', path: '/ks.jks', password: 'keystore-secret' } },
            },
        };

        const dto = fromFormStateToV4Dto(fromV4DtoToFormState(legacy), legacy);

        for (const v2Key of ['specification', 'useSystemProxy', 'httpClientOptions', 'httpProxyOptions', 'sslOptions']) {
            expect(dto.configuration).not.toHaveProperty(v2Key);
        }
        expect(dto.configuration?.transformation).toBe('[{"operation":"remove"}]');
        expect(dto.configuration?.schedule).toBe('0 */10 * * * *');
    });

    it('drops the stored proxy host and credentials when the proxy is turned off', () => {
        const withProxy: DynamicPropertyConfig = {
            ...stored,
            configuration: {
                ...stored.configuration,
                systemProxy: false,
                proxy: { enabled: true, type: 'HTTP', host: 'proxy.example.com', port: 3128, username: 'user', password: 'proxy-secret' },
            },
        };
        const form = fromV4DtoToFormState(withProxy);

        const dto = fromFormStateToV4Dto({ ...form, proxy: { ...form.proxy, enabled: false } }, withProxy);

        expect(dto.configuration?.proxy).toEqual({ enabled: false });
    });

    it('keeps the system proxy on when the request-level switch is on and the Proxy section is off', () => {
        const migrated: DynamicPropertyConfig = {
            enabled: true,
            type: 'http-dynamic-properties',
            configuration: {
                schedule: '0 */5 * * * *',
                url: 'https://api.example.com/properties',
                transformation: '[]',
                systemProxy: true,
            },
        };

        const dto = fromFormStateToV4Dto(fromV4DtoToFormState(migrated), migrated);

        expect(dto.configuration?.systemProxy).toBe(true);
        expect(dto.configuration?.proxy).toEqual({ enabled: true, useSystemProxy: true });
    });

    describe('compression', () => {
        const withHttp = (http?: DynamicPropertyHttpClientOptions): DynamicPropertyConfig => ({
            ...stored,
            configuration: { ...stored.configuration, http },
        });

        it('is left unset when the API stores none and the form is still at its default', () => {
            const noHttp = withHttp(undefined);

            const dto = fromFormStateToV4Dto(fromV4DtoToFormState(noHttp), noHttp);

            expect(dto.configuration?.http).not.toHaveProperty('useCompression');
        });

        it('stays as stored when the API stores it', () => {
            const on = withHttp({ useCompression: true });
            const off = withHttp({ useCompression: false });

            expect(fromFormStateToV4Dto(fromV4DtoToFormState(on), on).configuration?.http?.useCompression).toBe(true);
            expect(fromFormStateToV4Dto(fromV4DtoToFormState(off), off).configuration?.http?.useCompression).toBe(false);
        });

        it('shows compression on when the API stores none, as the service compresses by default', () => {
            expect(fromV4DtoToFormState(withHttp(undefined)).httpClient.useCompression).toBe(true);
        });

        it('is saved as off in one save when the user turns it off', () => {
            const noHttp = withHttp(undefined);
            const form = fromV4DtoToFormState(noHttp);

            const dto = fromFormStateToV4Dto({ ...form, httpClient: { ...form.httpClient, useCompression: false } }, noHttp);

            expect(dto.configuration?.http?.useCompression).toBe(false);
        });
    });

    describe('system proxy', () => {
        const withSystemProxy: DynamicPropertyConfig = {
            ...stored,
            configuration: {
                ...stored.configuration,
                systemProxy: true,
                proxy: { enabled: false, type: 'HTTP', host: 'proxy.example.com', port: 3128, username: 'user', password: 'proxy-secret' },
            },
        };

        it('keeps the stored proxy and turns the system proxy on when the request-level switch is on', () => {
            const dto = fromFormStateToV4Dto(fromV4DtoToFormState(withSystemProxy), withSystemProxy);

            expect(dto.configuration?.proxy).toEqual(
                expect.objectContaining({ enabled: true, useSystemProxy: true, host: 'proxy.example.com', username: 'user' }),
            );
        });

        it('applies the Proxy section edits when that section is on as well', () => {
            const form = fromV4DtoToFormState(withSystemProxy);

            const dto = fromFormStateToV4Dto(
                { ...form, proxy: { ...form.proxy, enabled: true, host: 'edited.example.com' } },
                withSystemProxy,
            );

            expect(dto.configuration?.proxy).toEqual(
                expect.objectContaining({ enabled: true, useSystemProxy: true, host: 'edited.example.com' }),
            );
        });
    });

    describe('SSL stores', () => {
        const withStores: DynamicPropertyConfig = {
            ...stored,
            configuration: {
                ...stored.configuration,
                ssl: {
                    trustStore: { type: 'JKS', path: '/trust.jks', password: 'p', alias: 'a', content: 'BASE64' },
                    keyStore: { type: 'JKS', path: '/key.jks', password: 'p', alias: 'a', keyPassword: 'kp' },
                },
            },
        };

        it('keeps the part of a stored store the form does not show while its type is unchanged', () => {
            const dto = fromFormStateToV4Dto(fromV4DtoToFormState(withStores), withStores);

            expect(dto.configuration?.ssl?.trustStore).toEqual(expect.objectContaining({ path: '/trust.jks', content: 'BASE64' }));
            expect(dto.configuration?.ssl?.keyStore).toEqual(expect.objectContaining({ path: '/key.jks', keyPassword: 'kp' }));
        });

        it('does not carry a stored store over when the user picks another store type', () => {
            const form = fromV4DtoToFormState(withStores);

            const dto = fromFormStateToV4Dto(
                { ...form, ssl: { ...form.ssl, trustStore: { type: 'PEM', path: '/trust.pem' } } },
                withStores,
            );

            expect(dto.configuration?.ssl?.trustStore).toEqual({ type: 'PEM', path: '/trust.pem' });
        });
    });
});
