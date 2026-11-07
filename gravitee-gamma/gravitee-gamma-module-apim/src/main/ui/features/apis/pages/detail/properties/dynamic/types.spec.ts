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
import type { DynamicPropertyConfig } from '../../../../types';

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
});
