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

import { nextSearchableUserSelection, toggleSearchableUser } from './searchableUsers';
import type { SearchableUser } from '../../../shared/types/userSearch';

const ANNA: SearchableUser = { id: '1', reference: 'anna', displayName: 'Anna' };
const JONAS: SearchableUser = { id: '2', reference: 'jonas', displayName: 'Jonas' };

describe('searchableUsers', () => {
    it('toggles a user in and out of the selection', () => {
        expect(toggleSearchableUser([], ANNA)).toEqual([ANNA]);
        expect(toggleSearchableUser([ANNA, JONAS], ANNA)).toEqual([JONAS]);
    });

    it('keeps at most one user when exclusive and replaces the current selection', () => {
        expect(nextSearchableUserSelection([ANNA], JONAS, true)).toEqual([JONAS]);
        expect(nextSearchableUserSelection([ANNA], ANNA, true)).toEqual([]);
    });

    it('delegates to toggle when exclusive is false', () => {
        expect(nextSearchableUserSelection([ANNA], JONAS, false)).toEqual([ANNA, JONAS]);
    });
});
