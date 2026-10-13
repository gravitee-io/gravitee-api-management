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
import { parseSpecDocument } from './specDocument';

describe('parseSpecDocument', () => {
    it('parses an OpenAPI document written in YAML', () => {
        expect(parseSpecDocument('OPENAPI', 'openapi: 3.0.3\ninfo:\n  title: Payments\n')).toEqual({
            status: 'valid',
            document: { openapi: '3.0.3', info: { title: 'Payments' } },
        });
    });

    it('parses an OpenAPI document written in JSON', () => {
        expect(parseSpecDocument('OPENAPI', '{\n  "openapi": "3.1.0",\n  "paths": {}\n}')).toEqual({
            status: 'valid',
            document: { openapi: '3.1.0', paths: {} },
        });
    });

    it('accepts a Swagger 2.0 document as OpenAPI', () => {
        expect(parseSpecDocument('OPENAPI', 'swagger: "2.0"\n')).toEqual({ status: 'valid', document: { swagger: '2.0' } });
    });

    it('parses an AsyncAPI document', () => {
        expect(parseSpecDocument('ASYNCAPI', 'asyncapi: 3.0.0\n')).toEqual({ status: 'valid', document: { asyncapi: '3.0.0' } });
    });

    it('names the line of a YAML syntax error', () => {
        const result = parseSpecDocument('OPENAPI', 'openapi: 3.0.3\ninfo:\n  title: Payments\n   version: 1.0.0\n');

        expect(result).toEqual({ status: 'invalid', message: expect.stringMatching(/^YAML error on line 4: bad indentation/) });
    });

    it('names the line of a JSON syntax error', () => {
        const result = parseSpecDocument('OPENAPI', '{\n  "openapi": "3.0.3",\n  "paths": {\n}\n');

        expect(result).toEqual({ status: 'invalid', message: expect.stringMatching(/^YAML error on line \d+: /) });
    });

    it.each([
        ['an OpenAPI page holding an AsyncAPI document', 'OPENAPI', 'asyncapi: 3.0.0\n', 'OpenAPI', 'an openapi or swagger'],
        ['an AsyncAPI page holding an OpenAPI document', 'ASYNCAPI', 'openapi: 3.0.3\n', 'AsyncAPI', 'an asyncapi'],
        ['a scalar', 'OPENAPI', 'just some text\n', 'OpenAPI', 'an openapi or swagger'],
        ['a list', 'ASYNCAPI', '- asyncapi: 3.0.0\n', 'AsyncAPI', 'an asyncapi'],
    ] as const)('refuses %s', (_case, type, content, label, field) => {
        expect(parseSpecDocument(type, content)).toEqual({
            status: 'invalid',
            message: `This doesn't look like an ${label} document: it needs ${field} field at the top level.`,
        });
    });

    it.each(['', '   \n\n', '# only a comment\n'])('reports %j as empty', content => {
        expect(parseSpecDocument('OPENAPI', content)).toEqual({ status: 'empty' });
    });
});
