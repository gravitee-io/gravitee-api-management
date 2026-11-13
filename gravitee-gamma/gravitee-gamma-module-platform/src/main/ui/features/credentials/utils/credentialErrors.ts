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
import { extractErrorMessage } from '../../../shared/notify/extractErrorMessage';

/** A server fault carries operator detail, such as a configuration key, that the user cannot act on. */
export function credentialFailureMessage(error: unknown, fallback: string): string {
    if (error instanceof ApimApiError && error.status >= 500) {
        return fallback;
    }
    return extractErrorMessage(error, fallback);
}
