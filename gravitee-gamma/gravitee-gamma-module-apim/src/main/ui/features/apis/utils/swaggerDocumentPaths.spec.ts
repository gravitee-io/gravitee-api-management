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
import { swaggerDocumentPaths } from './swaggerDocumentPaths';

describe('swaggerDocumentPaths', () => {
    it('converts OpenAPI parameters and skips paths the form would reject', () => {
        const document = {
            openapi: '3.0.0',
            paths: {
                '/pets/{petId}': { get: {} },
                '/': { get: {} },
                '/users/@me': { get: {} },
                '/pets': { get: {} },
            },
        };

        expect(swaggerDocumentPaths(JSON.stringify(document))).toEqual(['/pets/:petId', '/pets']);
    });

    it('reads a Swagger YAML document', () => {
        const document = "swagger: '2.0'\npaths:\n  /store/order/{orderId}:\n    get: {}\n";

        expect(swaggerDocumentPaths(document)).toEqual(['/store/order/:orderId']);
    });

    it('returns nothing for a blank document or a URL', () => {
        expect(swaggerDocumentPaths('')).toEqual([]);
        expect(swaggerDocumentPaths(null)).toEqual([]);
        expect(swaggerDocumentPaths('https://example.com/petstore.json')).toEqual([]);
    });
});
