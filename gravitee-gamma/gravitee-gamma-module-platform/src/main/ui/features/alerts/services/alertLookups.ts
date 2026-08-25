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

const API_LIST_PAGE_SIZE = 9_999;

interface ApisPage {
    data?: Array<{ id: string; name?: string }>;
    pagination?: { page?: number; pageCount?: number };
}

export async function listAlertApis(environmentId: string): Promise<Array<{ id: string; name: string }>> {
    const collected: Array<{ id: string; name: string }> = [];
    let page = 1;
    let pageCount = 1;
    do {
        const response = await apimFetchJsonV2<ApisPage>(environmentId, `/apis?page=${page}&perPage=${API_LIST_PAGE_SIZE}`);
        collected.push(
            ...(response.data ?? []).map(api => ({
                id: api.id,
                name: api.name || api.id,
            })),
        );
        pageCount = response.pagination?.pageCount ?? 1;
        page += 1;
    } while (page <= pageCount);
    return collected;
}
