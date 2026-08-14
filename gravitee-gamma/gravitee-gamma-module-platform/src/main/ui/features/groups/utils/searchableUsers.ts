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

import type { SearchableUser } from '../../../shared/types/userSearch';
import { isSameUser } from '../../../shared/utils/userSearch';

export function toggleSearchableUser(selected: SearchableUser[], user: SearchableUser): SearchableUser[] {
    const isAlreadySelected = selected.some(u => isSameUser(u, user));
    if (isAlreadySelected) {
        return selected.filter(u => !isSameUser(u, user));
    }
    return [...selected, user];
}

/** When exclusive, at most one user can be selected (primary-owner add-members). */
export function nextSearchableUserSelection(selected: SearchableUser[], user: SearchableUser, exclusive: boolean): SearchableUser[] {
    if (exclusive) {
        return selected.some(u => isSameUser(u, user)) ? [] : [user];
    }
    return toggleSearchableUser(selected, user);
}
