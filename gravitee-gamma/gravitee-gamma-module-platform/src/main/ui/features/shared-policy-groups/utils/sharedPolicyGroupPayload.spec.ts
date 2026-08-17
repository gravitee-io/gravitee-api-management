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

import { toUpdateSharedPolicyGroupPayload } from './sharedPolicyGroupPayload';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

const SPG: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Reusable auth',
    prerequisiteMessage: 'Needs cache',
    apiType: 'PROXY',
    phase: 'REQUEST',
    steps: [{ name: 'jwt' }],
};

describe('toUpdateSharedPolicyGroupPayload', () => {
    it('maps form values and preserves existing steps', () => {
        expect(
            toUpdateSharedPolicyGroupPayload(SPG, {
                name: 'Updated',
                description: 'New description',
                prerequisiteMessage: 'New prerequisite',
            }),
        ).toEqual({
            name: 'Updated',
            description: 'New description',
            prerequisiteMessage: 'New prerequisite',
            steps: [{ name: 'jwt' }],
        });
    });

    it('omits empty optional strings and defaults missing steps to an empty array', () => {
        expect(
            toUpdateSharedPolicyGroupPayload({ ...SPG, steps: undefined }, { name: 'Updated', description: '', prerequisiteMessage: '' }),
        ).toEqual({
            name: 'Updated',
            description: undefined,
            prerequisiteMessage: undefined,
            steps: [],
        });
    });
});
