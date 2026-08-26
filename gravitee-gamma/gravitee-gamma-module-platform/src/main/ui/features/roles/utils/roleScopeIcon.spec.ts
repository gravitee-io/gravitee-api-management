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
import { getRoleScopeIcon } from './roleScopeIcon';

describe('getRoleScopeIcon', () => {
    it.each(['API', 'APPLICATION', 'ENVIRONMENT', 'ORGANIZATION', 'INTEGRATION', 'API_PRODUCT', 'AI_WORKSPACE'] as const)(
        'returns an icon for %s',
        scope => {
            expect(getRoleScopeIcon(scope)).toBeDefined();
        },
    );

    it.each(['CLUSTER', 'EXPLORER'] as const)('returns no icon for %s, matching the Angular default case', scope => {
        expect(getRoleScopeIcon(scope)).toBeUndefined();
    });
});
