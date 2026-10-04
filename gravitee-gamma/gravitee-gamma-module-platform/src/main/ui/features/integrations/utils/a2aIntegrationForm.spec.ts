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
import { validateA2aIntegrationForm } from './a2aIntegrationForm';

const INVALID_URL = 'Enter a valid http:// or https:// URL.';
const DUPLICATE_URL = 'This URL is already in the list.';

function urlErrors(wellKnownUrls: string[]) {
    return validateA2aIntegrationForm({ name: 'Weather Agents', description: '', wellKnownUrls }).wellKnownUrls;
}

function nameError(name: string) {
    return validateA2aIntegrationForm({ name, description: '', wellKnownUrls: ['https://a.io'] }).name;
}

describe('validateA2aIntegrationForm', () => {
    it.each([
        { url: 'http://a.io', expected: undefined },
        { url: 'HTTPS://a.io/x', expected: undefined },
        { url: 'hTTp://a.io:8080', expected: undefined },
        { url: 'http://[::1]:8080/x', expected: undefined },
        { url: ' http://a.io', expected: INVALID_URL },
        { url: 'http://a.io ', expected: INVALID_URL },
        { url: ' http://a.io ', expected: INVALID_URL },
        { url: '\thttp://a.io', expected: INVALID_URL },
        { url: 'http://a.io\n', expected: INVALID_URL },
        { url: 'ftp://a.io', expected: INVALID_URL },
        { url: 'a.io', expected: INVALID_URL },
        { url: 'http://', expected: INVALID_URL },
        { url: 'http:///path', expected: INVALID_URL },
        { url: 'http://user@', expected: INVALID_URL },
        { url: 'http://:8080/x', expected: INVALID_URL },
        { url: 'https://a.io/x y', expected: INVALID_URL },
        { url: 'https://a.io/x\ty', expected: INVALID_URL },
        { url: 'https://a.io/x\ny', expected: INVALID_URL },
        { url: '   ', expected: INVALID_URL },
    ])('reports $expected for the well-known URL $url', ({ url, expected }) => {
        expect(urlErrors([url])).toEqual([expected]);
    });

    it.each([
        { urls: ['http://a.io/x', 'HTTP://A.io/x'], expected: [undefined, DUPLICATE_URL] },
        { urls: ['http://a.io/%2f', 'http://a.io/%2F'], expected: [undefined, DUPLICATE_URL] },
        { urls: ['https://a.io', 'https://a.io/'], expected: [undefined, DUPLICATE_URL] },
        { urls: ['https://a.io?x=1', 'https://a.io/?x=1'], expected: [undefined, DUPLICATE_URL] },
        { urls: ['http://a.io/x', 'http://a.io:80/x'], expected: [undefined, DUPLICATE_URL] },
        { urls: ['http://a.io/x', 'http://a.io:/x'], expected: [undefined, DUPLICATE_URL] },
        { urls: ['https://a.io/', 'https://a.io:443/'], expected: [undefined, DUPLICATE_URL] },
        { urls: ['https://a.io/x', 'https://A.io/x', 'https://a.io:443/x'], expected: [undefined, DUPLICATE_URL, DUPLICATE_URL] },
        { urls: ['http://user@a.io/x', 'http://USER@a.io/x'], expected: [undefined, undefined] },
        { urls: ['http://a.io/x', 'http://a.io/X'], expected: [undefined, undefined] },
        { urls: ['http://a.io/x?q=1', 'http://a.io/x?q=2'], expected: [undefined, undefined] },
        { urls: ['http://a.io/x#one', 'http://a.io/x#two'], expected: [undefined, undefined] },
        { urls: ['http://a.io/a/../b', 'http://a.io/b'], expected: [undefined, undefined] },
        { urls: ['http://a.io:443/', 'http://a.io/'], expected: [undefined, undefined] },
        { urls: ['http://a.io/%41', 'http://a.io/A'], expected: [undefined, undefined] },
    ])('reports $expected for the well-known URLs $urls', ({ urls, expected }) => {
        expect(urlErrors(urls)).toEqual(expected);
    });

    it.each([
        { name: '\t', expected: 'Name is required.' },
        { name: '\n', expected: 'Name is required.' },
        { name: ' Weather Agents ', expected: undefined },
        { name: ` ${'n'.repeat(49)}`, expected: undefined },
    ])('reports $expected for the name $name', ({ name, expected }) => {
        expect(nameError(name)).toEqual(expected);
    });

    it('reports a missing well-known URL when the list is empty', () => {
        expect(validateA2aIntegrationForm({ name: 'Weather Agents', description: '', wellKnownUrls: [] })).toEqual({
            wellKnownUrls: [],
            missingWellKnownUrls: 'Add at least one well-known URL.',
        });
    });

    it('reports no error for a valid name and a single valid well-known URL', () => {
        expect(validateA2aIntegrationForm({ name: 'Weather Agents', description: '', wellKnownUrls: ['https://a.io'] })).toEqual({
            wellKnownUrls: [undefined],
        });
    });
});
