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
import type { AsyncJobsResponse, ListAsyncJobsQuery } from '../types/asyncJob';

export function listAsyncJobs(environmentId: string, query: ListAsyncJobsQuery = {}): Promise<AsyncJobsResponse> {
    const params = new URLSearchParams();
    if (query.type) params.set('type', query.type);
    if (query.status) params.set('status', query.status);
    if (query.sourceId) params.set('sourceId', query.sourceId);
    params.set('page', String(query.page ?? 1));
    params.set('perPage', String(query.perPage ?? 10));

    const qs = params.toString();
    return apimFetchJsonV2<AsyncJobsResponse>(environmentId, `/async-jobs?${qs}`);
}
