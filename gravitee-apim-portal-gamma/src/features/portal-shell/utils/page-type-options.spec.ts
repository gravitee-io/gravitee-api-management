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
import {
    normalizeOpenApiRenderer,
    OPENAPI_RENDERER_LABELS,
} from './page-type-options';

describe('page-type-options', () => {
    describe('normalizeOpenApiRenderer', () => {
        it('should return gravitee when renderer is gravitee', () => {
            expect(normalizeOpenApiRenderer('gravitee')).toBe('gravitee');
        });

        it('should return redoc when renderer is redoc', () => {
            expect(normalizeOpenApiRenderer('redoc')).toBe('redoc');
        });

        it('should default to gravitee for unknown values', () => {
            expect(normalizeOpenApiRenderer(undefined)).toBe('gravitee');
            expect(normalizeOpenApiRenderer('unknown')).toBe('gravitee');
        });
    });

    describe('OPENAPI_RENDERER_LABELS', () => {
        it('should include Gravitee Renderer label', () => {
            expect(OPENAPI_RENDERER_LABELS.gravitee).toBe('Gravitee Renderer');
        });
    });
});
