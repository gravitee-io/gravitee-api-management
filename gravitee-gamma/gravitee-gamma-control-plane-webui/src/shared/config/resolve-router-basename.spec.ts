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
import { resolveRouterBasename } from './resolve-router-basename';

describe('resolveRouterBasename', () => {
    it('returns "/" for a root deployment', () => {
        expect(resolveRouterBasename('http://gamma.localhost/')).toBe('/');
    });

    it('strips the trailing slash of a sub-path deployment', () => {
        expect(resolveRouterBasename('http://gamma.localhost/gravitee-gamma/')).toBe('/gravitee-gamma');
    });

    it('keeps a sub-path that already has no trailing slash', () => {
        expect(resolveRouterBasename('http://gamma.localhost/gravitee-gamma')).toBe('/gravitee-gamma');
    });

    it('handles a nested sub-path', () => {
        expect(resolveRouterBasename('http://gamma.localhost/foo/bar/')).toBe('/foo/bar');
    });
});
