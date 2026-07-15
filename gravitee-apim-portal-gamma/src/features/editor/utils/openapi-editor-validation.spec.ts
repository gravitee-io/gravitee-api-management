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
import { PETSTORE_OPENAPI_SPEC } from '../services/openapi.service';
import { getOpenApiEditorValidationState } from './openapi-editor-validation';

describe('getOpenApiEditorValidationState', () => {
    const invalidSpec = 'openapi: 3.0.3\ninfo:\n  title: Broken\ncomponents:\n  schemas:\n    Order:\n      enum: [pending';

    it('should validate inline content only in inline mode', () => {
        const result = getOpenApiEditorValidationState({
            sourceType: 'INLINE',
            inlineContent: PETSTORE_OPENAPI_SPEC,
            specContent: invalidSpec,
            loadingSpec: false,
            hasApiAncestor: false,
            apiSpecResolved: false,
            remoteSpecSynced: false,
        });

        expect(result.showValidationStatus).toBe(true);
        expect(result.validation.valid).toBe(true);
    });

    it('should hide validation when switching away from inline content', () => {
        const result = getOpenApiEditorValidationState({
            sourceType: 'HTTP',
            inlineContent: invalidSpec,
            specContent: invalidSpec,
            loadingSpec: false,
            hasApiAncestor: false,
            apiSpecResolved: false,
            remoteSpecSynced: false,
        });

        expect(result.showValidationStatus).toBe(false);
    });

    it('should validate HTTP content only after sync', () => {
        const result = getOpenApiEditorValidationState({
            sourceType: 'HTTP',
            inlineContent: '',
            specContent: PETSTORE_OPENAPI_SPEC,
            loadingSpec: false,
            hasApiAncestor: false,
            apiSpecResolved: false,
            remoteSpecSynced: true,
        });

        expect(result.showValidationStatus).toBe(true);
        expect(result.validation.valid).toBe(true);
    });

    it('should validate GitHub content only after sync', () => {
        const result = getOpenApiEditorValidationState({
            sourceType: 'GITHUB',
            inlineContent: '',
            specContent: PETSTORE_OPENAPI_SPEC,
            loadingSpec: false,
            hasApiAncestor: false,
            apiSpecResolved: false,
            remoteSpecSynced: true,
        });

        expect(result.showValidationStatus).toBe(true);
        expect(result.validation.valid).toBe(true);
    });
});
