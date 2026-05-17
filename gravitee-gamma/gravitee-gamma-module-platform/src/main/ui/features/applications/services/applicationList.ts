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
import type { ApplicationListItem, ApplicationListResponse, ApplicationStatus } from '../types/application';

export async function listApplications(
    environmentId: string,
    {
        query,
        page,
        size,
        status = 'ACTIVE',
    }: {
        query?: string;
        page: number;
        size: number;
        status?: ApplicationStatus;
    },
): Promise<ApplicationListResponse> {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        status,
    });
    if (query) {
        params.set('query', query);
    }
    return apimFetchJsonV1Env<ApplicationListResponse>(environmentId, `/applications/_paged?${params}`);
}

export async function restoreApplication(environmentId: string, applicationId: string): Promise<ApplicationListItem> {
    return apimFetchJsonV1Env<ApplicationListItem>(environmentId, `/applications/${applicationId}/_restore`, {
        method: 'POST',
    });
}
