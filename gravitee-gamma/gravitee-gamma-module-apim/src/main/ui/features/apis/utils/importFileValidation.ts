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

import type { ApiImportFormat } from '../types';

/** Classic `api-import-v4-form` mismatch banner. */
export const IMPORT_FORMAT_MISMATCH = 'The file does not match the selected API format';

export const IMPORT_INVALID_JSON = 'Invalid JSON. Please upload a valid Gravitee API definition file.';

/** Shown instead of a Java exception or stack trace from the Management API. */
export const IMPORT_UNEXPECTED_ERROR = 'The API could not be imported. Check the file and try again.';

const TECHNICAL_ERROR = /Cannot invoke |Exception|io\.gravitee\.|java\.lang\.|NullPointer/;

export interface ImportFileCheck {
    accepted: boolean;
    definition: unknown;
    error: string | null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
}

/** Classic `api-import-file-picker` swagger detection, plus YAML `openapi:` / `swagger:`. */
function isOpenApiContent(text: string): boolean {
    const trimmed = text.trim();
    if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
        try {
            const json: unknown = JSON.parse(trimmed);
            return (
                isRecord(json) &&
                (Object.prototype.hasOwnProperty.call(json, 'openapi') ||
                    Object.prototype.hasOwnProperty.call(json, 'swagger') ||
                    Object.prototype.hasOwnProperty.call(json, 'swaggerVersion'))
            );
        } catch {
            return false;
        }
    }
    return /^(openapi|swagger|swaggerVersion)\s*:/m.test(trimmed);
}

function isWsdlContent(text: string): boolean {
    return /<(?:[\w.-]+:)?definitions[\s>]/.test(text);
}

function isGraviteeExport(value: unknown): boolean {
    return isRecord(value) && isRecord(value.api);
}

/**
 * Client-side file check before import. Gravitee requires an export (`api` object).
 * OpenAPI and WSDL are rejected before the request when the contents don't match.
 */
export function checkImportFile(format: ApiImportFormat, text: string): ImportFileCheck {
    if (format === 'gravitee') {
        let parsed: unknown;
        try {
            parsed = JSON.parse(text);
        } catch {
            return { accepted: false, definition: null, error: IMPORT_INVALID_JSON };
        }
        if (!isGraviteeExport(parsed)) {
            return { accepted: false, definition: null, error: IMPORT_FORMAT_MISMATCH };
        }
        return { accepted: true, definition: parsed, error: null };
    }

    if (format === 'openapi') {
        return isOpenApiContent(text)
            ? { accepted: true, definition: null, error: null }
            : { accepted: false, definition: null, error: IMPORT_FORMAT_MISMATCH };
    }

    return isWsdlContent(text)
        ? { accepted: true, definition: null, error: null }
        : { accepted: false, definition: null, error: IMPORT_FORMAT_MISMATCH };
}

export function readableImportError(message: string | null | undefined): string | null {
    if (!message) return null;
    return TECHNICAL_ERROR.test(message) ? IMPORT_UNEXPECTED_ERROR : message;
}
