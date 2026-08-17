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

import { SHARED_POLICY_GROUP_DEFAULT_TAB, sharedPolicyGroupDetailHref } from './sharedPolicyGroupDetailNavigation';

describe('sharedPolicyGroupDetailNavigation', () => {
    it('defaults to the studio tab to match classic Console', () => {
        expect(SHARED_POLICY_GROUP_DEFAULT_TAB).toBe('studio');
        expect(sharedPolicyGroupDetailHref('spg-1')).toBe('spg-1/studio');
    });

    it('builds hrefs for each detail tab', () => {
        expect(sharedPolicyGroupDetailHref('spg-1', 'overview')).toBe('spg-1/overview');
        expect(sharedPolicyGroupDetailHref('spg-1', 'history')).toBe('spg-1/history');
    });
});
