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
import { editorLanguageFor } from './pageContentType';

describe('editorLanguageFor', () => {
    it('edits Gravitee Markdown as Markdown', () => {
        expect(editorLanguageFor('GRAVITEE_MARKDOWN', '{ "not": "json" }')).toBe('markdown');
    });

    it.each(['OPENAPI', 'ASYNCAPI'] as const)('edits a %s document written in JSON as JSON', type => {
        expect(editorLanguageFor(type, '\n  {\n  "openapi": "3.0.0"\n}')).toBe('json');
    });

    it.each(['OPENAPI', 'ASYNCAPI'] as const)('edits any other %s document as YAML, the empty one included', type => {
        expect(editorLanguageFor(type, 'openapi: 3.0.0\n')).toBe('yaml');
        expect(editorLanguageFor(type, '')).toBe('yaml');
    });
});
