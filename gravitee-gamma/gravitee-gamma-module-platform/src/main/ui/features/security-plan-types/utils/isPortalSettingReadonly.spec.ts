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

import { isPortalSettingReadonly } from './isPortalSettingReadonly';

describe('isPortalSettingReadonly', () => {
    it('returns false when settings or readonly metadata are missing', () => {
        expect(isPortalSettingReadonly(undefined, 'http.api.portal.cors.allow-origin')).toBe(false);
        expect(isPortalSettingReadonly({}, 'http.api.portal.cors.allow-origin')).toBe(false);
        expect(isPortalSettingReadonly({ metadata: {} }, 'http.api.portal.cors.allow-origin')).toBe(false);
    });

    it('returns true only for keys listed in metadata.readonly', () => {
        const settings = { metadata: { readonly: ['http.api.portal.cors.allow-origin', 'email.host'] } };
        expect(isPortalSettingReadonly(settings, 'http.api.portal.cors.allow-origin')).toBe(true);
        expect(isPortalSettingReadonly(settings, 'email.host')).toBe(true);
        expect(isPortalSettingReadonly(settings, 'http.api.portal.cors.max-age')).toBe(false);
        expect(isPortalSettingReadonly(settings, 'email.password')).toBe(false);
    });
});
