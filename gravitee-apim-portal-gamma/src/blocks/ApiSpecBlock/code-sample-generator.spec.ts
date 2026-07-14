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
import { generateCodeSample } from './code-sample-generator';
import { parseOpenApiDocument } from './openapi-spec-utils';
import { DETAILED_DUMMY_OPENAPI_SPEC } from '../../features/portals/storage/dummy-openapi-spec';

describe('code-sample-generator', () => {
    const parsed = parseOpenApiDocument(DETAILED_DUMMY_OPENAPI_SPEC)!;
    const operation = parsed.operations.find(item => item.method === 'get')!;

    it('generates samples for all supported languages', () => {
        expect(generateCodeSample(parsed.document, operation, 'curl')).toContain('curl -X GET');
        expect(generateCodeSample(parsed.document, operation, 'python')).toContain('requests.get');
        expect(generateCodeSample(parsed.document, operation, 'node')).toContain('fetch(');
        expect(generateCodeSample(parsed.document, operation, 'javascript')).toContain('fetch(');
        expect(generateCodeSample(parsed.document, operation, 'java')).toContain('HttpClient.newHttpClient');
        expect(generateCodeSample(parsed.document, operation, 'go')).toContain('http.Get');
    });
});
