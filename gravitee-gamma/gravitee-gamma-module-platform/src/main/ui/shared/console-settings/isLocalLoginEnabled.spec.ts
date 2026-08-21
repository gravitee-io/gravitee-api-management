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
import { isLocalLoginEnabled } from './index';

describe('isLocalLoginEnabled', () => {
    it('defaults a missing local-login setting to enabled', () => {
        expect(isLocalLoginEnabled(undefined)).toBe(true);
        expect(isLocalLoginEnabled({})).toBe(true);
        expect(isLocalLoginEnabled({ authentication: {} })).toBe(true);
        expect(isLocalLoginEnabled({ authentication: { localLogin: {} } })).toBe(true);
    });

    it('honors an explicit local-login value', () => {
        expect(isLocalLoginEnabled({ authentication: { localLogin: { enabled: true } } })).toBe(true);
        expect(isLocalLoginEnabled({ authentication: { localLogin: { enabled: false } } })).toBe(false);
    });
});
