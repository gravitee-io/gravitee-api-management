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
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { AuditPage, AuditSearchParams } from '../types/auditLogs.types';

export async function searchAuditLogs(environmentId: string, apiId: string, params: AuditSearchParams): Promise<AuditPage> {
    const qs = new URLSearchParams({ page: String(params.page), perPage: String(params.perPage) });
    if (params.events) qs.set('events', params.events);
    if (params.from !== undefined) qs.set('from', String(params.from));
    if (params.to !== undefined) qs.set('to', String(params.to));
    return apimFetchJsonV2<AuditPage>(environmentId, `/apis/${apiId}/audits?${qs}`);
}

export async function listAuditEvents(environmentId: string, apiId: string): Promise<string[]> {
    const res = await apimFetchJsonV2<{ data: string[] }>(environmentId, `/apis/${apiId}/audits/events`);
    return res.data;
}
