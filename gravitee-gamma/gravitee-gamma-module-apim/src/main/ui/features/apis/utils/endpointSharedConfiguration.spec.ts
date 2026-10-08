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
import { serializeSharedConfiguration, serializeSharedConfigurationOverride } from './endpointSharedConfiguration';

describe('endpointSharedConfiguration', () => {
    it('persists schema form values as shared configuration without reshaping', () => {
        const config = {
            tcp: { connectTimeout: 5000, reconnectAttempts: 3 },
            proxy: { enabled: true, useSystemProxy: true },
            ssl: { hostnameVerifier: true, trustAll: false },
        };
        expect(serializeSharedConfiguration(config)).toEqual(config);
    });

    it('serializes endpoint overrides as a plain record', () => {
        expect(serializeSharedConfigurationOverride(undefined)).toEqual({});
        expect(serializeSharedConfigurationOverride({ http: { version: 'HTTP_1_1' } })).toEqual({
            http: { version: 'HTTP_1_1' },
        });
    });
});
