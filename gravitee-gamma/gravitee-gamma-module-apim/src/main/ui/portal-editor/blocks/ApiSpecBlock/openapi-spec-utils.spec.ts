/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { DETAILED_DUMMY_OPENAPI_SPEC } from '../../portals/storage/dummy-openapi-spec';

import {
    extractOperations,
    extractTags,
    getOperationsByTag,
    getSchemasForOperations,
    parseOpenApiDocument,
    UNTAGGED_OPERATIONS_TAG,
} from './openapi-spec-utils';

describe('openapi-spec-utils', () => {
    const parsed = parseOpenApiDocument(DETAILED_DUMMY_OPENAPI_SPEC);

    it('parses the dummy OpenAPI document', () => {
        expect(parsed).toBeDefined();
        expect(parsed?.document.info.title).toBe('Gravitee Commerce Platform API');
        expect(parsed?.operations.length).toBeGreaterThan(0);
    });

    it('extracts tags from operations and top-level tag definitions', () => {
        const operations = extractOperations(parsed!.document);
        const tags = extractTags(parsed!.document, operations);

        expect(tags).toContain('Products');
        expect(tags).toContain('Inventory');
        expect(tags).not.toContain(UNTAGGED_OPERATIONS_TAG);
    });

    it('filters operations by tag', () => {
        const operations = parsed!.operations;
        const productOperations = getOperationsByTag(operations, 'Products');

        expect(productOperations.length).toBeGreaterThan(0);
        expect(productOperations.every(operation => operation.tags.includes('Products'))).toBe(true);
    });

    it('collects schemas referenced by operations in a tag', () => {
        const productOperations = getOperationsByTag(parsed!.operations, 'Products');
        const schemas = getSchemasForOperations(parsed!.document, productOperations);

        expect(Object.keys(schemas).length).toBeGreaterThan(0);
    });
});
