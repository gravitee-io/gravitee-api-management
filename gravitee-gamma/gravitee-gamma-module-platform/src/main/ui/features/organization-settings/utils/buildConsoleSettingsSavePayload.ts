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

import { PASSWORD_SENTINEL, type ConsoleSettings, type ConsoleSettingsEmail } from '../types/consoleSettings';

export type ConsoleSettingsSection = 'management' | 'cors' | 'email';

function mergeEmail(current: ConsoleSettingsEmail | undefined, overlay: ConsoleSettingsEmail | undefined): ConsoleSettingsEmail {
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
 * Classic org-settings save: POST the full fetched entity with only the edited section overlaid.
 */
export function buildConsoleSettingsSavePayload(
    current: ConsoleSettings,
    section: ConsoleSettingsSection,
    overlay: Pick<ConsoleSettings, 'management' | 'scheduler' | 'cors' | 'email'>,
): ConsoleSettings {
    const trialHidesEmail = Boolean(current.trialInstance?.enabled);

    return {
        ...current,
        management:
            section === 'management'
                ? {
                      ...current.management,
                      ...overlay.management,
                      support: { ...current.management?.support, ...overlay.management?.support },
                      userCreation: { ...current.management?.userCreation, ...overlay.management?.userCreation },
                      automaticValidation: { ...current.management?.automaticValidation, ...overlay.management?.automaticValidation },
                  }
                : current.management,
        scheduler: section === 'management' ? { ...current.scheduler, ...overlay.scheduler } : current.scheduler,
        cors: section === 'cors' ? { ...current.cors, ...overlay.cors } : current.cors,
        email: section === 'email' && !trialHidesEmail ? mergeEmail(current.email, overlay.email) : current.email,
    };
}
