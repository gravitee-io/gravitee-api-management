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
import type { HttpOptions } from '@gravitee/gamma-lib-observability';

import { graviteeConsoleAuthHeaders, persistCsrfFromResponse } from './csrf';

/**
 * `fetch` wrapper that keeps the rolling XSRF token in sync: the Gravitee
 * management API echoes a fresh `X-Xsrf-Token` on every response, which we
 * persist for the next request.
 */
function persistCsrfAndReturnResponse(res: Response): Response {
    persistCsrfFromResponse(res);
    return res;
}

function authenticatedFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    return fetch(input, init).then(persistCsrfAndReturnResponse);
}

/**
 * HTTP configuration for Gamma UI libs that perform their own fetch calls
 * (e.g. `@gravitee/gamma-lib-observability`). Authenticates with credentialed
 * cookies, the `X-Requested-With` marker, and the rolling XSRF token.
 */
export const gammaConsoleHttpOptions: HttpOptions = {
    fetch: authenticatedFetch,
    credentials: 'include',
    headers: graviteeConsoleAuthHeaders,
};
