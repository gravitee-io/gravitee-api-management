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
import { observability } from '../observability';
import { getActiveNavKey, isRouteKey, ROUTES } from '../routes';

const MODULE_PREFIX = 'environments/DEFAULT/apim';

// ROUTE_KEYS spells the observe/* keys by hand for its type; the library is the source. A section it mounts
// without its key here has a nav entry that leads nowhere.
describe('observability route keys', () => {
    it.each(observability.routeKeys)('registers %s with the path and label the library mounts it on', key => {
        expect(isRouteKey(key)).toBe(true);
        expect(ROUTES[key as keyof typeof ROUTES]).toEqual(observability.routes[key]);
    });

    it('mounts the Settings section', () => {
        expect(observability.routeKeys).toContain('observe/settings');
    });

    it("resolves the observability Settings page to its own entry, not to the module's Settings", () => {
        expect(getActiveNavKey(`/${MODULE_PREFIX}/observe/settings`, MODULE_PREFIX)).toBe('observe/settings');
        expect(getActiveNavKey(`/${MODULE_PREFIX}/settings`, MODULE_PREFIX)).toBe('settings');
    });
});
