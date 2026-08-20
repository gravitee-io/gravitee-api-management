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
import { apimFetchJsonOrg } from '../api/apimClient';
import type { ConsoleSettings } from '../console-settings';

/** GET /organizations/{orgId}/console (Angular `constants.org.settings`). */
export async function fetchOrgConsoleSettings(): Promise<ConsoleSettings> {
    return apimFetchJsonOrg<ConsoleSettings>('/console');
}

/**
 * POST /organizations/{orgId}/console (Angular ConsoleSettingsService.save()). The backend replaces the
 * whole settings document, so callers must pass the complete object (spread the currently-loaded settings
 * and override only the field being changed) — sending a partial object would wipe out every other console
 * setting (authentication providers, CORS, etc.), not just merge the one field.
 */
export async function saveOrgConsoleSettings(settings: ConsoleSettings): Promise<ConsoleSettings> {
    return apimFetchJsonOrg<ConsoleSettings>('/console', {
        method: 'POST',
        body: JSON.stringify(settings),
    });
}
