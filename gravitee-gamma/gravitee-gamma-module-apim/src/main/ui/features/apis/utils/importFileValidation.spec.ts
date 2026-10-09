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
import { checkImportFile, IMPORT_FORMAT_MISMATCH, readableImportError } from './importFileValidation';

describe('checkImportFile', () => {
    it('accepts a Gravitee export that contains an api object', () => {
        const result = checkImportFile('gravitee', JSON.stringify({ api: { name: 'My API' } }));
        expect(result.accepted).toBe(true);
        expect(result.error).toBeNull();
    });

    it('rejects JSON that is not a Gravitee export', () => {
        const result = checkImportFile('gravitee', JSON.stringify({ name: 'create payload' }));
        expect(result.accepted).toBe(false);
        expect(result.error).toBe(IMPORT_FORMAT_MISMATCH);
    });

    it('rejects YAML that is not OpenAPI before it is sent', () => {
        const result = checkImportFile('openapi', 'hello: world\n');
        expect(result.accepted).toBe(false);
        expect(result.error).toBe(IMPORT_FORMAT_MISMATCH);
    });

    it('accepts an OpenAPI document', () => {
        expect(checkImportFile('openapi', 'openapi: 3.0.0\ninfo:\n  title: My API\n').accepted).toBe(true);
    });

    it('rejects XML that is not a WSDL', () => {
        expect(checkImportFile('wsdl', '<not>a wsdl</not>').accepted).toBe(false);
    });

    it('accepts a WSDL definitions document', () => {
        expect(checkImportFile('wsdl', '<?xml version="1.0"?><definitions></definitions>').accepted).toBe(true);
    });
});

describe('readableImportError', () => {
    it('replaces a Java exception with a readable message', () => {
        expect(
            readableImportError(
                'Cannot invoke "io.gravitee.apim.core.api.model.import_definition.ApiExport.setPicture(String)" because "apiExport" is null',
            ),
        ).toBe('The API could not be imported. Check the file and try again.');
    });

    it('keeps a server message that is already readable', () => {
        expect(readableImportError('Malformed descriptor')).toBe('Malformed descriptor');
    });
});
