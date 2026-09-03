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

import { PASSWORD_SENTINEL } from '../../organization-settings/types/consoleSettings';
import type { PortalSettings, PortalSettingsEmail } from '../../security-plan-types/services/portalSettings';

export type PortalSettingsSection = 'cors' | 'email';

function mergeEmail(current: PortalSettingsEmail | undefined, overlay: PortalSettingsEmail | undefined): PortalSettingsEmail {
    const password = overlay?.password === PASSWORD_SENTINEL || !overlay?.password ? current?.password : overlay?.password;
    return {
        ...current,
        ...overlay,
        password,
        properties: {
            ...current?.properties,
            ...overlay?.properties,
        },
    };
}

/**
 * Classic portal-settings save: POST the full fetched entity with only the edited section overlaid.
 */
export function buildPortalSettingsSectionPayload(
    current: PortalSettings,
    section: PortalSettingsSection,
    overlay: Pick<PortalSettings, 'cors' | 'email'>,
): PortalSettings {
    return {
        ...current,
        cors: section === 'cors' ? { ...current.cors, ...overlay.cors } : current.cors,
        email: section === 'email' ? mergeEmail(current.email, overlay.email) : current.email,
    };
}
