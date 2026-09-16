/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { normalizeCurrentUser, retainIdentityFields } from './normalizeCurrentUser';

describe('normalizeCurrentUser', () => {
    it('should keep firstname, lastname, and email from GET /user', () => {
        expect(
            normalizeCurrentUser({
                id: 'user-1',
                firstname: 'Ada',
                lastname: 'Lovelace',
                email: 'ada@example.com',
                displayName: 'Ada Lovelace',
                source: 'gravitee',
            }),
        ).toEqual(
            expect.objectContaining({
                firstname: 'Ada',
                lastname: 'Lovelace',
                email: 'ada@example.com',
                displayName: 'Ada Lovelace',
            }),
        );
    });

    it('should read primaryOwner from GET /user, including Jackson isPrimaryOwner', () => {
        expect(normalizeCurrentUser({ id: 'admin', primaryOwner: true }).primaryOwner).toBe(true);
        expect(normalizeCurrentUser({ id: 'admin', isPrimaryOwner: true }).primaryOwner).toBe(true);
        expect(normalizeCurrentUser({ id: 'admin' }).primaryOwner).toBe(false);
    });

    it('should read primary_owner from PUT /user UserEntity JSON', () => {
        expect(normalizeCurrentUser({ id: 'admin', primary_owner: true }).primaryOwner).toBe(true);
        expect(normalizeCurrentUser({ id: 'admin', primary_owner: false }).primaryOwner).toBe(false);
    });

    it('should keep previous primaryOwner and groups when the PUT body omits them', () => {
        const previous = normalizeCurrentUser({
            id: 'admin',
            primaryOwner: true,
            groupsByEnvironment: { 'env-1': ['api-devs'] },
            roles: [{ scope: 'ORGANIZATION', name: 'ADMIN' }],
            source: 'gravitee',
        });
        const fromPut = normalizeCurrentUser({ id: 'admin', firstname: 'Ada', lastname: 'Lovelace' });

        expect(retainIdentityFields(previous, fromPut)).toEqual(
            expect.objectContaining({
                firstname: 'Ada',
                lastname: 'Lovelace',
                primaryOwner: true,
                groupsByEnvironment: { 'env-1': ['api-devs'] },
                roles: [{ scope: 'ORGANIZATION', name: 'ADMIN' }],
                source: 'gravitee',
            }),
        );
    });

    it('should fall back to sourceId when names are blank', () => {
        expect(normalizeCurrentUser({ id: 'admin', sourceId: 'admin', firstname: '', lastname: '' }).displayName).toBe('admin');
    });
});
