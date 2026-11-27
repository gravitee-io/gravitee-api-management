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
import { ApimApiError } from '@gravitee/gamma-ui-shared/api';

export function validatePortalName(name: string): string | undefined {
    const blank = Array.from(name).every(character => {
        const codePoint = character.codePointAt(0) ?? 0;
        return /[\p{Z}\p{Cf}]/u.test(character) || (codePoint >= 9 && codePoint <= 13) || (codePoint >= 28 && codePoint <= 31);
    });
    if (blank) return 'Enter a portal name.';
    if (name.length > 256) return 'Name must not exceed 256 characters.';
    return undefined;
}

export function classifyCreatePortalError(error: unknown): 'duplicate' | 'limit' | 'permission' | 'invalid' | 'general' {
    if (!(error instanceof ApimApiError)) return 'general';
    const body = error.body;
    const code = typeof body === 'object' && body !== null && 'technicalCode' in body ? body.technicalCode : undefined;
    if (error.status === 409 && code === 'portal.name.exists') return 'duplicate';
    if (error.status === 403 && code === 'portal.limitReached') return 'limit';
    if (error.status === 403) return 'permission';
    if (error.status === 400) return 'invalid';
    return 'general';
}
