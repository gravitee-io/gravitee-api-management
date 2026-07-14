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
import { highlightCode } from './highlight-code';

describe('highlight-code', () => {
    it('should highlight shell and json snippets', () => {
        const shell = highlightCode("curl -X GET 'https://api.example.com'", 'bash');
        expect(shell).toContain('hljs');

        const json = highlightCode('{"id": 1}', 'json');
        expect(json).toContain('hljs');
    });
});
