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
import {
    ORGANIZATION_USER_ACCESS_PERMISSIONS,
    ORGANIZATION_USER_CREATE_PERMISSION,
    ORGANIZATION_USER_DELETE_PERMISSION,
    ORGANIZATION_USER_UPDATE_PERMISSION,
} from './userPermissions';

describe('userPermissions', () => {
    it('matches classic list and navigation access permissions', () => {
        expect(ORGANIZATION_USER_ACCESS_PERMISSIONS).toEqual([
            'organization-user-c',
            'organization-user-r',
            'organization-user-u',
            'organization-user-d',
        ]);
    });

    it('matches classic create, update, and delete permission keys', () => {
        expect(ORGANIZATION_USER_CREATE_PERMISSION).toBe('organization-user-c');
        expect(ORGANIZATION_USER_UPDATE_PERMISSION).toBe('organization-user-u');
        expect(ORGANIZATION_USER_DELETE_PERMISSION).toBe('organization-user-d');
    });
});
