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
import { TextEncoder, TextDecoder } from 'util';

Object.assign(global, { TextEncoder, TextDecoder });

// jsdom does not provide a native fetch — stub it so module-level code that
// calls fetch (e.g. bootstrap-context, auth-context) does not crash on import.
global.fetch = jest.fn((url: string) => {
    if (url === '/constants.json') {
        return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ gammaBaseURL: 'http://localhost:8083/gamma' }),
        });
    }
    if (url.includes('/gamma/ui/bootstrap')) {
        return Promise.resolve({
            ok: true,
            json: () =>
                Promise.resolve({
                    gammaBaseURL: 'http://localhost:8083/gamma',
                    managementBaseURL: 'http://localhost:8083/management',
                    organizationId: 'DEFAULT',
                }),
        });
    }
    if (url.endsWith('/user')) {
        return Promise.resolve({
            ok: true,
            headers: new Headers(),
            json: () => Promise.resolve({ displayName: 'Test', email: 'test@test.com', firstname: 'Test', lastname: 'User' }),
        });
    }
    if (url.includes('/modules')) {
        return Promise.resolve({ ok: true, headers: new Headers(), json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: false, status: 404, headers: new Headers(), json: () => Promise.resolve({}) });
}) as jest.Mock;
