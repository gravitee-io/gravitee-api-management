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

import type { PlanSecuritySettings, PortalSettings } from '../services/portalSettings';

export interface SecurityState {
    keyless: boolean;
    apikey: boolean;
    customApiKey: boolean;
    customApiKeyReuse: boolean;
    sharedApiKey: boolean;
    oauth2: boolean;
    jwt: boolean;
    push: boolean;
    mtls: boolean;
}

export function buildPortalSettingsSavePayload(current: PortalSettings, state: SecurityState): PortalSettings {
    const security: PlanSecuritySettings = {
        ...current.plan?.security,
        keyless: { enabled: state.keyless },
        apikey: { enabled: state.apikey },
        customApiKey: { enabled: state.customApiKey },
        customApiKeyReuse: { enabled: state.customApiKeyReuse },
        sharedApiKey: { enabled: state.sharedApiKey },
        oauth2: { enabled: state.oauth2 },
        jwt: { enabled: state.jwt },
        push: { enabled: state.push },
        mtls: { enabled: state.mtls },
    };

    return {
        ...current,
        plan: {
            ...current.plan,
            security,
        },
    };
}
