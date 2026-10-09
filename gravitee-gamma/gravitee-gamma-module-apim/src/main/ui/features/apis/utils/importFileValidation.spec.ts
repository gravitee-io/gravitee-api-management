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

import { FILE_FORMAT_MISMATCH, importFileFormatError } from './importFileFormat';
import { IMPORT_UNEXPECTED_ERROR, readableImportError } from './importFileValidation';
import { ApimApiError } from '../../../shared/api/apimClient';

describe('importFileFormatError', () => {
    it('rejects a V2 Gravitee export', () => {
        const text = JSON.stringify({ api: { name: 'Legacy', definitionVersion: '2.0.0' } });
        expect(importFileFormatError('gravitee', 'api.json', text)).toBe(FILE_FORMAT_MISMATCH);
    });

    it('accepts a V4 Gravitee export', () => {
        const text = JSON.stringify({ api: { name: 'Current', definitionVersion: 'V4' } });
        expect(importFileFormatError('gravitee', 'api.json', text)).toBeNull();
    });

    it('rejects swagger JSON before treating it as a Gravitee export', () => {
        const text = JSON.stringify({ swagger: '2.0', api: { definitionVersion: 'V4' } });
        expect(importFileFormatError('gravitee', 'api.json', text)).toBe(FILE_FORMAT_MISMATCH);
    });
});

describe('readableImportError', () => {
    it('replaces a 500 with a generic message', () => {
        expect(readableImportError(new ApimApiError(500, 'Cannot invoke "String.length()"'))).toBe(IMPORT_UNEXPECTED_ERROR);
    });

    it('keeps a readable 4xx even when the message contains Exception', () => {
        expect(readableImportError(new ApimApiError(400, 'Validation Exception: name is required'))).toBe(
            'Validation Exception: name is required',
        );
    });

    it('still hides raw Java text when no status is available', () => {
        expect(readableImportError('Index 0 out of bounds because of java.lang.String')).toBe(IMPORT_UNEXPECTED_ERROR);
    });

    it('keeps an ordinary message', () => {
        expect(readableImportError('Malformed descriptor')).toBe('Malformed descriptor');
    });
});
