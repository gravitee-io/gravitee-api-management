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

import { isSameUser } from './userSearch';
import type { SearchableUser } from '../types/userSearch';

const ANNA: SearchableUser = { id: '1', reference: 'anna', displayName: 'Anna' };
const ANNA_NO_ID: SearchableUser = { id: null, reference: 'anna', displayName: 'Anna' };
const JONAS: SearchableUser = { id: '2', reference: 'jonas', displayName: 'Jonas' };

describe('isSameUser', () => {
    it('matches by id when both sides have one', () => {
        expect(isSameUser(ANNA, { ...ANNA, reference: 'other' })).toBe(true);
        expect(isSameUser(ANNA, JONAS)).toBe(false);
    });

    it('falls back to reference when either id is missing', () => {
        expect(isSameUser(ANNA_NO_ID, { id: undefined, reference: 'anna', displayName: 'Anna' })).toBe(true);
        expect(isSameUser(ANNA_NO_ID, JONAS)).toBe(false);
    });
});
