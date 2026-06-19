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

const CSRF_STORAGE_KEY = 'XSRF-TOKEN';

export function getCsrfToken(): string | null {
    return localStorage.getItem(CSRF_STORAGE_KEY);
}

export function setCsrfToken(value: string): void {
    localStorage.setItem(CSRF_STORAGE_KEY, value);
}

/** Persists the server's new XSRF token so subsequent requests remain authenticated under the rolling-token policy. */
export function persistCsrfFromResponse(response: Response): void {
    const newCsrf = response.headers.get('X-Xsrf-Token');
    if (newCsrf) setCsrfToken(newCsrf);
}

/** Standard authenticated console request headers (excluding Content-Type). */
export function graviteeConsoleAuthHeaders(): Record<string, string> {
    const headers: Record<string, string> = { 'X-Requested-With': 'XMLHttpRequest' };
    const csrf = getCsrfToken();
    if (csrf) headers['X-Xsrf-Token'] = csrf;
    return headers;
}
