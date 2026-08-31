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

import { TEST_CONFIG, TEST_PORTAL_API } from '../factories';

export const bootstrapHandlers = [
    http.get('/assets/config.json', () => HttpResponse.json({ baseURL: TEST_CONFIG.portalBaseURL })),
    http.get(`${TEST_CONFIG.portalBaseURL}/ui/bootstrap`, () =>
        HttpResponse.json({
            baseURL: TEST_CONFIG.portalBaseURL,
            environmentId: TEST_CONFIG.environmentId,
            organizationId: TEST_CONFIG.organizationId,
        }),
    ),
    http.get(`${TEST_PORTAL_API}/configuration`, () =>
        HttpResponse.json({
            authentication: { localLogin: { enabled: true }, forceLogin: { enabled: false } },
        }),
    ),
    http.get(`${TEST_PORTAL_API}/configuration/identities`, () => HttpResponse.json({ data: [] })),
];
