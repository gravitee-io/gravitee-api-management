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
import { getActiveNavKey } from './routes';

describe('getActiveNavKey', () => {
    it.each([
        ['/', '/', 'dashboard'],
        ['/list', '/', 'portals'],
        ['/portal-id/overview', '/', 'portals'],
        ['/environments/dev/portals', '/environments/dev/portals', 'dashboard'],
        ['/environments/dev/portals/', '/environments/dev/portals', 'dashboard'],
        ['/environments/dev/portals/list', '/environments/dev/portals', 'portals'],
        ['/environments/dev/portals/portal-id/overview', '/environments/dev/portals', 'portals'],
        ['/environments/list/portals', '/environments/list/portals', 'dashboard'],
        ['/environments/portals/portals', '/environments/portals/portals', 'dashboard'],
    ])('selects the right section for %s', (pathname, moduleRoot, expected) => {
        expect(getActiveNavKey(pathname, moduleRoot)).toBe(expected);
    });
});
