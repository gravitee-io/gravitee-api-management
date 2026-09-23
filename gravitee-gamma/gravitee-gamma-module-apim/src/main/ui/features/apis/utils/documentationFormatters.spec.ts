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
import {
    acceptedFileTypes,
    isSupportedEditType,
    normalizeParentId,
    openApiConfigurationFromPage,
    openApiConfigurationToRecord,
    pageTypeTitle,
    parseConfigurationBoolean,
    parseFetcherSchema,
    toApiParentId,
    toPortalContentType,
    fromPortalContentType,
} from './documentationFormatters';

describe('documentationFormatters', () => {
    it('maps page types to titles', () => {
        expect(pageTypeTitle('SWAGGER')).toBe('OpenAPI');
        expect(pageTypeTitle('MARKDOWN')).toBe('Markdown');
        expect(pageTypeTitle('FOLDER')).toBe('Folder');
    });

    it('parses configuration booleans from stringified JSON', () => {
        expect(parseConfigurationBoolean('true')).toBe(true);
        expect(parseConfigurationBoolean(undefined)).toBe(false);
        expect(parseConfigurationBoolean('false')).toBe(false);
    });

    it('hydrates OpenAPI configuration from page strings', () => {
        const config = openApiConfigurationFromPage({ tryIt: 'true', viewer: 'Redoc', maxDisplayedTags: '8' });
        expect(config.tryIt).toBe(true);
        expect(config.viewer).toBe('Redoc');
        expect(config.maxDisplayedTags).toBe(8);
        expect(config.docExpansion).toBe('none');
    });

    it('serializes OpenAPI configuration as a record', () => {
        const config = openApiConfigurationFromPage({ tryIt: 'true', viewer: 'Redoc' });
        expect(openApiConfigurationToRecord(config)).toEqual(expect.objectContaining({ tryIt: true, viewer: 'Redoc' }));
    });

    it('parses fetcher JSON schema and rejects invalid JSON', () => {
        expect(parseFetcherSchema({ id: 'http', schema: '{"type":"object"}' })).toEqual({ type: 'object' });
        expect(parseFetcherSchema({ id: 'http', schema: '{not json' })).toBeUndefined();
    });

    it('returns accepted file types for import', () => {
        expect(acceptedFileTypes('MARKDOWN')).toContain('.md');
        expect(acceptedFileTypes('SWAGGER')).toContain('.yaml');
    });

    it('identifies page types the editor supports', () => {
        expect(isSupportedEditType('MARKDOWN')).toBe(true);
        expect(isSupportedEditType('SWAGGER')).toBe(true);
        expect(isSupportedEditType('FOLDER')).toBe(false);
        expect(isSupportedEditType(undefined)).toBe(false);
    });

    it('maps API documentation types to Next Gen Portal content types', () => {
        expect(toPortalContentType('MARKDOWN')).toBe('GRAVITEE_MARKDOWN');
        expect(toPortalContentType('SWAGGER')).toBe('OPENAPI');
        expect(toPortalContentType('ASYNCAPI')).toBe('ASYNCAPI');
        expect(toPortalContentType('FOLDER')).toBeUndefined();
    });

    it('maps Next Gen Portal content types back to API documentation types', () => {
        expect(fromPortalContentType('GRAVITEE_MARKDOWN')).toBe('MARKDOWN');
        expect(fromPortalContentType('OPENAPI')).toBe('SWAGGER');
        expect(fromPortalContentType('ASYNCAPI')).toBe('ASYNCAPI');
        expect(fromPortalContentType('FOLDER')).toBeUndefined();
    });

    it('normalizes parent ids for the tree versus the management API', () => {
        expect(normalizeParentId(undefined)).toBeNull();
        expect(normalizeParentId('ROOT')).toBeNull();
        expect(normalizeParentId('folder-1')).toBe('folder-1');
        expect(toApiParentId(null)).toBe('ROOT');
        expect(toApiParentId('folder-1')).toBe('folder-1');
    });
});
