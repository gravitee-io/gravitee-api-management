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
import { ApimApiError } from '../../../shared/api/apimClient';

/**
 * Only a refusal the server explains in its JSON body is shown. A server fault carries operator detail, such as a
 * configuration key, and a proxy page or a network error carries text the user cannot act on.
 */
export function credentialFailureMessage(error: unknown, fallback: string): string {
    if (error instanceof ApimApiError && error.status < 500 && serverMessage(error.body)) {
        return error.message;
    }
    return fallback;
}

function serverMessage(body: unknown): boolean {
    return typeof body === 'object' && body !== null && typeof (body as { message?: unknown }).message === 'string';
}
