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

import type { SecurityState } from './buildPortalSettingsSavePayload';
import { applyReadonlySecurityState, getSecurityReadonlyState } from './securityPlanReadonly';

const BASE_STATE: SecurityState = {
    keyless: true,
    apikey: true,
    customApiKey: true,
    customApiKeyReuse: true,
    sharedApiKey: true,
    oauth2: true,
    jwt: true,
    push: true,
    mtls: true,
};

describe('securityPlanReadonly', () => {
    it('marks only keys listed in metadata.readonly as readonly', () => {
        const readonlyState = getSecurityReadonlyState({
            metadata: {
                readonly: ['plan.security.jwt.enabled', 'plan.security.apikey.allowCustom.enabled'],
            },
        });

        expect(readonlyState.jwt).toBe(true);
        expect(readonlyState.customApiKey).toBe(true);
        expect(readonlyState.keyless).toBe(false);
    });

    it('marks shared API Key readonly using the backend metadata key', () => {
        const readonlyState = getSecurityReadonlyState({
            metadata: {
                readonly: ['plan.security.apikey.allowShared.enabled'],
            },
        });

        expect(readonlyState.sharedApiKey).toBe(true);
        expect(readonlyState.apikey).toBe(false);
    });

    it('marks shared API Key readonly using the legacy Classic metadata key', () => {
        const readonlyState = getSecurityReadonlyState({
            metadata: {
                readonly: ['plan.security.apikey.sharedApiKey.enabled'],
            },
        });

        expect(readonlyState.sharedApiKey).toBe(true);
    });

    it('preserves saved values for readonly keys when building save state', () => {
        const readonlyState = getSecurityReadonlyState({
            metadata: {
                readonly: ['plan.security.jwt.enabled'],
            },
        });

        const localState: SecurityState = { ...BASE_STATE, jwt: false };
        const savedState: SecurityState = { ...BASE_STATE, jwt: true };

        expect(applyReadonlySecurityState(localState, savedState, readonlyState).jwt).toBe(true);
        expect(applyReadonlySecurityState(localState, savedState, readonlyState).push).toBe(true);
    });
});
