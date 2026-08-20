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
import type { AlertTrigger } from '../types/alert';

/** Lists environment / platform alerts. Mirrors classic `AlertService.listAlerts(Scope.ENVIRONMENT, true)`. */
export async function listPlatformAlerts(environmentId: string): Promise<AlertTrigger[]> {
    return apimFetchJsonV1Env<AlertTrigger[]>(environmentId, '/platform/alerts?event_counts=true');
}

/** Updates a platform alert (used for enable/disable toggles on the list page). */
export async function updatePlatformAlert(environmentId: string, alert: AlertTrigger): Promise<AlertTrigger> {
    if (!alert.id) {
        throw new Error('Cannot update a platform alert without an id');
    }
    return apimFetchJsonV1Env<AlertTrigger>(environmentId, `/platform/alerts/${encodeURIComponent(alert.id)}`, {
        method: 'PUT',
        body: JSON.stringify(alert),
    });
}
