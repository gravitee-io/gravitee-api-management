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
    http.get(`${TEST_MANAGEMENT_BASE}/user`, () =>
        HttpResponse.json(
            buildUser({
                roles: [{ scope: 'ORGANIZATION', permissions: { USER: ['R'] } }],
            }),
        ),
    ),
    http.post(`${TEST_MANAGEMENT_BASE}/user/login`, () => new HttpResponse(null, { status: 200 })),
    http.post(`${TEST_MANAGEMENT_BASE}/user/logout`, () => new HttpResponse(null, { status: 200 })),
    http.get(`${TEST_MANAGEMENT_BASE}/configuration/password-policy`, () =>
        HttpResponse.json({
            description:
                'Password must be at least 12 characters long, contain at least one digit, one upper case letter, one lower case letter, one special character, and no more than 2 consecutive equal characters.',
            pattern: '^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[!~<>.,;:_=?/*+\\-#\\"\'&§`£€%°()|\\[\\]$^@])(?!.*(.)\\1{2,}).{12,128}$',
            rules: [
                { id: 'minLength', label: 'At least 12 characters', pattern: '^.{12,}$' },
                { id: 'maxLength', label: 'At most 128 characters', pattern: '^.{0,128}$' },
                { id: 'digit', label: 'Contains a number', pattern: '[0-9]' },
                { id: 'uppercase', label: 'Contains uppercase letter', pattern: '[A-Z]' },
                { id: 'lowercase', label: 'Contains lowercase letter', pattern: '[a-z]' },
                { id: 'special', label: 'Contains a special character', pattern: '[!~<>.,;:_=?/*+\\-#\\"\'&§`£€%°()|\\[\\]$^@]' },
                { id: 'noConsecutive', label: 'No more than 2 consecutive equal characters', pattern: '^(?!.*(.)\\1{2,}).+$' },
            ],
        }),
    ),
    http.get(`${TEST_MANAGEMENT_BASE}/console`, () => HttpResponse.json({ reCaptcha: { enabled: false } })),
];
