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

import { TEST_CONFIG } from '../factories';

export const bootstrapHandlers = [
    http.get('/constants.json', () => HttpResponse.json({ gammaBaseURL: TEST_CONFIG.gammaBaseURL })),
    http.get(`${TEST_CONFIG.gammaBaseURL}/ui/bootstrap`, () =>
        HttpResponse.json({
            managementBaseURL: TEST_CONFIG.managementBaseURL,
            organizationId: TEST_CONFIG.organizationId,
            gammaBaseURL: TEST_CONFIG.gammaBaseURL,
        }),
    ),
];
