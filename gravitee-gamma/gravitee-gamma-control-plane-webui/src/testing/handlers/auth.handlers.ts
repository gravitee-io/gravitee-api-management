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

import { TEST_MANAGEMENT_BASE, buildUser } from '../factories';

export const authHandlers = [
    http.get(`${TEST_MANAGEMENT_BASE}/user`, () => HttpResponse.json(buildUser())),
    http.post(`${TEST_MANAGEMENT_BASE}/user/login`, () => new HttpResponse(null, { status: 200 })),
    http.post(`${TEST_MANAGEMENT_BASE}/user/logout`, () => new HttpResponse(null, { status: 200 })),
];
