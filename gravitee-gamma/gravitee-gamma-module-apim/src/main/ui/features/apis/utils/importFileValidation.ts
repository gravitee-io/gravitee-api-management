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

/** Shown instead of a server failure or a Java exception from the Management API. */
export const IMPORT_UNEXPECTED_ERROR = 'The API could not be imported. Check the file and try again.';

const TECHNICAL_ERROR = /Cannot invoke |Exception|io\.gravitee\.|java\.lang\.|NullPointer/;

/**
 * Server failures and raw Java text become a readable message.
 * A 4xx keeps the API message, including when that message happens to contain "Exception".
 */
export function readableImportError(error: unknown): string | null {
    if (error === null || error === undefined) return null;
    if (error instanceof ApimApiError) {
        if (error.status >= 500) return IMPORT_UNEXPECTED_ERROR;
        return error.message || IMPORT_UNEXPECTED_ERROR;
    }
    let message: string | null = null;
    if (typeof error === 'string') {
        message = error;
    } else if (error instanceof Error) {
        message = error.message;
    }
    if (!message) return null;
    return TECHNICAL_ERROR.test(message) ? IMPORT_UNEXPECTED_ERROR : message;
}
