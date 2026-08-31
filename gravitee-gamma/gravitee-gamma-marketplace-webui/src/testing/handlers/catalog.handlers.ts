/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { http, HttpResponse } from 'msw';

import { TEST_PORTAL_API } from '../factories';

function isCatalogSearch(request: Request): boolean {
    const url = new URL(request.url);
    return `${url.origin}${url.pathname}` === `${TEST_PORTAL_API}/apis/_search`;
}

export const catalogHandlers = [
    http.post(
        ({ request }) => isCatalogSearch(request),
        () =>
            HttpResponse.json({
                data: [],
                metadata: { pagination: { current_page: 1, size: 12, total: 0, total_pages: 0 } },
            }),
    ),
];
