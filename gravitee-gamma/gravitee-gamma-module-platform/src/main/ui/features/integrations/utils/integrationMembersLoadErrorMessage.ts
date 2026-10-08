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

const MEMBERS_LOAD_ERROR_FALLBACK = 'Failed to load members.';

// Reads the body rather than ApimApiError.message, which the client fills with the raw response text when the body has no message.
export function integrationMembersLoadErrorMessage(error: unknown): string {
    if (!(error instanceof ApimApiError) || typeof error.body !== 'object' || error.body === null) {
        return MEMBERS_LOAD_ERROR_FALLBACK;
    }
    const { message } = error.body as { message?: unknown };
    return typeof message === 'string' && message !== '' ? message : MEMBERS_LOAD_ERROR_FALLBACK;
}
