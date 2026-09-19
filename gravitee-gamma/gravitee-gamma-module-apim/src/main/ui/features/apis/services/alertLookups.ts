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
import { listApiPlans, listSubscriptions } from './subscriptions';
import { getTenants } from './tenants';

export async function listAlertTenants(environmentId: string): Promise<Array<{ id: string; name: string }>> {
    const tenants = await getTenants(environmentId);
    return tenants.map(t => ({ id: t.id, name: t.name }));
}

export async function listAlertApplications(environmentId: string, apiId: string): Promise<Array<{ id: string; name: string }>> {
    const page = await listSubscriptions(
        environmentId,
        { type: 'api', entityId: apiId },
        { statuses: ['ACCEPTED', 'PENDING'], perPage: 9999 },
    );
    const seen = new Map<string, string>();
    for (const sub of page.data ?? []) {
        const app = sub.application;
        if (app?.id && !seen.has(app.id)) {
            seen.set(app.id, app.name ?? app.id);
        }
    }
    return [...seen.entries()].map(([id, name]) => ({ id, name }));
}

export async function listAlertPlans(environmentId: string, apiId: string): Promise<Array<{ id: string; name: string }>> {
    const page = await listApiPlans(environmentId, { type: 'api', entityId: apiId });
    return (page.data ?? []).map(plan => ({ id: plan.id, name: plan.name ?? plan.id }));
}
