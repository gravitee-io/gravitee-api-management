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

import { isConsoleSettingReadonly } from './isConsoleSettingReadonly';

describe('isConsoleSettingReadonly', () => {
    it('returns false when settings or readonly metadata are missing', () => {
        expect(isConsoleSettingReadonly(undefined, 'email.password')).toBe(false);
        expect(isConsoleSettingReadonly({}, 'email.password')).toBe(false);
        expect(isConsoleSettingReadonly({ metadata: {} }, 'email.password')).toBe(false);
    });

    it('returns true only for keys listed in metadata.readonly', () => {
        const settings = { metadata: { readonly: ['email.password', 'console.support.enabled'] } };
        expect(isConsoleSettingReadonly(settings, 'email.password')).toBe(true);
        expect(isConsoleSettingReadonly(settings, 'console.support.enabled')).toBe(true);
        expect(isConsoleSettingReadonly(settings, 'email.host')).toBe(false);
    });
});
