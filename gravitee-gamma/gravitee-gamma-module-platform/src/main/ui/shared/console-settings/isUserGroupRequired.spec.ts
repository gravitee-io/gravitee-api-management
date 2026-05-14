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
import { isUserGroupRequired } from './isUserGroupRequired';
import type { ConsoleSettings } from './types';

describe('isUserGroupRequired', () => {
    it('returns false when config is undefined', () => {
        expect(isUserGroupRequired(undefined)).toBe(false);
    });

    it('returns false when required is disabled', () => {
        const config: ConsoleSettings = {
            userGroup: { required: { enabled: false } },
        };
        expect(isUserGroupRequired(config)).toBe(false);
    });

    it('returns true when required is enabled', () => {
        const config: ConsoleSettings = {
            userGroup: { required: { enabled: true } },
        };
        expect(isUserGroupRequired(config)).toBe(true);
    });
});
