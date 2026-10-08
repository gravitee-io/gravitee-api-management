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
import { detectPageContentType, titleFromFileName } from './documentationFile';

describe('detectPageContentType', () => {
    it('reads a Markdown file as Gravitee Markdown, whatever it contains', () => {
        expect(detectPageContentType('README.MD', 'openapi: 3.0.0')).toBe('GRAVITEE_MARKDOWN');
    });

    it.each([
        ['an OpenAPI YAML document', 'petstore.yaml', 'openapi: 3.0.0\ninfo:\n  title: Petstore\n'],
        ['a Swagger YAML document', 'petstore.yml', "swagger: '2.0'\ninfo:\n  title: Petstore\n"],
        ['a quoted root key', 'petstore.yaml', '"openapi": 3.1.0\n'],
        ['an OpenAPI JSON document', 'petstore.json', '{ "openapi": "3.0.0", "info": { "title": "Petstore" } }'],
        ['JSON stored in a YAML file', 'petstore.yaml', '{ "swagger": "2.0" }'],
    ])('detects OpenAPI from %s', (_case, fileName, content) => {
        expect(detectPageContentType(fileName, content)).toBe('OPENAPI');
    });

    it.each([
        ['an AsyncAPI YAML document', 'events.yaml', 'asyncapi: 2.6.0\ninfo:\n  title: Events\n'],
        ['an AsyncAPI JSON document', 'events.json', '{ "asyncapi": "3.0.0" }'],
    ])('detects AsyncAPI from %s', (_case, fileName, content) => {
        expect(detectPageContentType(fileName, content)).toBe('ASYNCAPI');
    });

    it.each([
        ['a nested openapi key', 'spec.yaml', 'info:\n  openapi: 3.0.0\n'],
        ['a commented-out root key', 'spec.yaml', '# openapi: 3.0.0\ninfo: {}\n'],
        ['a JSON document with no spec key', 'spec.json', '{ "info": {} }'],
        ['a JSON array', 'spec.json', '[{ "openapi": "3.0.0" }]'],
        ['an empty file', 'spec.yaml', ''],
    ])('cannot tell the type of %s', (_case, fileName, content) => {
        expect(detectPageContentType(fileName, content)).toBeNull();
    });
});

describe('titleFromFileName', () => {
    it('drops the extension', () => {
        expect(titleFromFileName('getting-started.md')).toBe('getting-started');
    });

    it('drops only the last extension', () => {
        expect(titleFromFileName('petstore.v2.yaml')).toBe('petstore.v2');
    });

    it('keeps a name without an extension', () => {
        expect(titleFromFileName('README')).toBe('README');
    });
});
