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
import { parseOpenApiContent } from '../utils/parse-openapi-spec';

describe('parseOpenApiContent', () => {
    it('should validate a JSON OpenAPI document', () => {
        const result = parseOpenApiContent(PETSTORE_OPENAPI_SPEC);

        expect(result.valid).toBe(true);
        expect(result.metadata?.title).toBe('Gravitee Commerce Platform API');
    });

    it('should treat empty content as invalid without an error message', () => {
        const result = parseOpenApiContent('   ');

        expect(result.valid).toBe(false);
        expect(result.error).toBeUndefined();
    });

    it('should reject non-OpenAPI JSON', () => {
        expect(parseOpenApiContent('{"foo":"bar"}').valid).toBe(false);
    });
});
