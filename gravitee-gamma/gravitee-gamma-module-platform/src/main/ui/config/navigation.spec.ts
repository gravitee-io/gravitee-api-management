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
import { NAV_GROUPS } from './navigation';
import { PLATFORM_ROUTE_CONFIG, ROUTES } from './routes';

describe('platform navigation config', () => {
    it('registers Users and User Groups under Identity & Access', () => {
        const identityGroup = NAV_GROUPS.find(group => group.label === 'Identity & Access');
        expect(identityGroup).toBeDefined();
        expect(identityGroup?.items.map(item => item.key)).toEqual(['users', 'user-groups']);
        expect(identityGroup?.items[0]?.title).toBe('Users');
        expect(identityGroup?.items[0]?.icon).toBeUndefined();
        expect(identityGroup?.items[1]?.title).toBe('User Groups');
        expect(identityGroup?.items[1]?.icon).toBeUndefined();
    });

    it('declares the users route in platform routing config', () => {
        expect(PLATFORM_ROUTE_CONFIG.routeKeys).toContain('users');
        expect(ROUTES.users).toEqual({ path: 'users', label: 'Users' });
    });

    it('declares the user-groups route in platform routing config', () => {
        expect(PLATFORM_ROUTE_CONFIG.routeKeys).toContain('user-groups');
        expect(ROUTES['user-groups']).toEqual({ path: 'user-groups', label: 'User Groups' });
    });
});
