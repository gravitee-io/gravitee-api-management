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

import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { ConsoleSettings } from '../../organization-settings/types/consoleSettings';

export async function getEnvironmentConsoleSettings(environmentId: string): Promise<ConsoleSettings> {
    return apimFetchJsonV1Env<ConsoleSettings>(environmentId, '/settings');
}

export async function saveEnvironmentConsoleSettings(environmentId: string, settings: ConsoleSettings): Promise<ConsoleSettings> {
    return apimFetchJsonV1Env<ConsoleSettings>(environmentId, '/settings', {
        method: 'POST',
        body: JSON.stringify(settings),
    });
}

/**
 * Drops the environment-level branded-senders override so it falls back to the organization (or system)
 * value. Sending an empty list via {@link saveEnvironmentConsoleSettings} would instead persist an empty
 * override that shadows the organization value — Classic's `portal-settings.service.ts` has the same note.
 */
export async function resetEnvironmentBrandedSenders(environmentId: string): Promise<ConsoleSettings> {
    return apimFetchJsonV1Env<ConsoleSettings>(environmentId, '/settings/email/branded-senders/reset', {
        method: 'POST',
    });
}
