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

import { mapEnvironmentRolesToRecipients } from './mapEnvironmentRoles';

describe('mapEnvironmentRolesToRecipients', () => {
    it('sorts roles by name and uses environment member labels', () => {
        expect(mapEnvironmentRolesToRecipients([{ name: 'USER' }, { name: 'ADMIN' }, { name: 'API_PUBLISHER' }])).toEqual([
            { name: 'ADMIN', displayName: 'Members with the ADMIN role on this environment' },
            { name: 'API_PUBLISHER', displayName: 'Members with the API_PUBLISHER role on this environment' },
            { name: 'USER', displayName: 'Members with the USER role on this environment' },
        ]);
    });

    it('does not prepend API_SUBSCRIBERS', () => {
        const options = mapEnvironmentRolesToRecipients([{ name: 'USER' }]);
        expect(options.map(option => option.name)).toEqual(['USER']);
    });
});
