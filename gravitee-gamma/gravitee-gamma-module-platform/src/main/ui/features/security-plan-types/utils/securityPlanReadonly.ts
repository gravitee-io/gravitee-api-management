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
import { isPortalSettingReadonly } from './isPortalSettingReadonly';
import type { PortalSettings } from '../services/portalSettings';

export type SecurityKey = keyof SecurityState;

export const SECURITY_PLAN_READONLY_PROPERTY: Record<SecurityKey, string> = {
    keyless: 'plan.security.keyless.enabled',
    apikey: 'plan.security.apikey.enabled',
    customApiKey: 'plan.security.apikey.allowCustom.enabled',
    customApiKeyReuse: 'plan.security.apikey.allowCustomReuse.enabled',
    sharedApiKey: 'plan.security.apikey.allowShared.enabled',
    oauth2: 'plan.security.oauth2.enabled',
    jwt: 'plan.security.jwt.enabled',
    push: 'plan.security.push.enabled',
    mtls: 'plan.security.mtls.enabled',
};

const SHARED_API_KEY_LEGACY_READONLY_PROPERTY = 'plan.security.apikey.sharedApiKey.enabled';

function isSecurityKeyReadonly(settings: PortalSettings | undefined, key: SecurityKey): boolean {
    if (key === 'sharedApiKey') {
        return (
            isPortalSettingReadonly(settings, SECURITY_PLAN_READONLY_PROPERTY.sharedApiKey) ||
            isPortalSettingReadonly(settings, SHARED_API_KEY_LEGACY_READONLY_PROPERTY)
        );
    }

    return isPortalSettingReadonly(settings, SECURITY_PLAN_READONLY_PROPERTY[key]);
}

export function getSecurityReadonlyState(settings: PortalSettings | undefined): Record<SecurityKey, boolean> {
    return (Object.keys(SECURITY_PLAN_READONLY_PROPERTY) as SecurityKey[]).reduce(
        (readonlyState, key) => {
            readonlyState[key] = isSecurityKeyReadonly(settings, key);
            return readonlyState;
        },
        {} as Record<SecurityKey, boolean>,
    );
}

export function applyReadonlySecurityState(
    state: SecurityState,
    savedState: SecurityState,
    readonlyState: Record<SecurityKey, boolean>,
): SecurityState {
    const next = { ...state };

    for (const key of Object.keys(readonlyState) as SecurityKey[]) {
        if (readonlyState[key]) {
            next[key] = savedState[key];
        }
    }

    return next;
}
