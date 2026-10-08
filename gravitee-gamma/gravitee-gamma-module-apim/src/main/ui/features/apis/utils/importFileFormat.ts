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

/** Same copy Classic shows as soon as the dropped file does not match the selected format. */
export const FILE_FORMAT_MISMATCH = 'The file does not match the selected API format';

export const INVALID_JSON_FILE = 'Invalid JSON file.';

function extensionOf(fileName: string): string | undefined {
    return fileName.split('.').pop()?.toLowerCase();
}

function isOpenApiDocument(value: unknown): boolean {
    if (!value || typeof value !== 'object') return false;
    return 'swagger' in value || 'swaggerVersion' in value || 'openapi' in value;
}

/** Classic treats a Gravitee import as the V4 export wrapper: `{ api: { definitionVersion: 'V4' } }`. */
export function isGraviteeV4Export(value: unknown): boolean {
    if (!value || typeof value !== 'object') return false;
    const api = (value as { api?: { definitionVersion?: string } }).api;
    return api?.definitionVersion === 'V4';
}

/**
 * Returns an error to show immediately, or null when the file matches the selected format.
 * Mirrors Classic `determineImportType` plus the configure-file-source mismatch check.
 */
export function importFileFormatError(format: ApiImportFormat, fileName: string, text: string): string | null {
    const extension = extensionOf(fileName);

    if (format === 'wsdl') {
        return extension === 'wsdl' || extension === 'xml' ? null : FILE_FORMAT_MISMATCH;
    }

    if (format === 'openapi') {
        if (extension === 'yml' || extension === 'yaml') return null;
        if (extension !== 'json') return FILE_FORMAT_MISMATCH;
        try {
            return isOpenApiDocument(JSON.parse(text)) ? null : FILE_FORMAT_MISMATCH;
        } catch {
            return INVALID_JSON_FILE;
        }
    }

    if (extension !== 'json') return FILE_FORMAT_MISMATCH;
    try {
        return isGraviteeV4Export(JSON.parse(text)) ? null : FILE_FORMAT_MISMATCH;
    } catch {
        return INVALID_JSON_FILE;
    }
}
