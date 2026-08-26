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
    canRoleBeDeleted,
    fromFormPermissionsToPermissions,
    isPermissionEligibleScope,
    isPermissionMovedToOrganizationScope,
    isRoleReadOnly,
    isRoleScope,
    toFormPermissions,
} from './rolePermissions';
import { ROLE_SCOPES, type Role } from '../types/role';

describe('isPermissionMovedToOrganizationScope', () => {
    it.each(['TAG', 'TENANT', 'ENTRYPOINT'])('is true for %s under ENVIRONMENT scope', permission => {
        expect(isPermissionMovedToOrganizationScope('ENVIRONMENT', permission)).toBe(true);
    });

    it('is false for other permissions under ENVIRONMENT scope', () => {
        expect(isPermissionMovedToOrganizationScope('ENVIRONMENT', 'GROUP')).toBe(false);
    });

    it('is false outside ENVIRONMENT scope', () => {
        expect(isPermissionMovedToOrganizationScope('ORGANIZATION', 'TAG')).toBe(false);
    });
});

describe('toFormPermissions / fromFormPermissionsToPermissions', () => {
    it('round-trips server permissions through the form shape', () => {
        const role: Role = { name: 'CUSTOM', scope: 'API', permissions: { DEFINITION: ['C', 'R'], PLAN: ['D'] } };

        const form = toFormPermissions(role, ['DEFINITION', 'PLAN', 'MEMBER']);

        expect(form).toEqual({
            DEFINITION: { C: true, R: true, U: false, D: false },
            PLAN: { C: false, R: false, U: false, D: true },
            MEMBER: { C: false, R: false, U: false, D: false },
        });
        expect(fromFormPermissionsToPermissions(form)).toEqual({
            DEFINITION: ['C', 'R'],
            PLAN: ['D'],
            MEMBER: [],
        });
    });

    it('defaults to every right unchecked when there is no existing role', () => {
        expect(toFormPermissions(undefined, ['MEMBER'])).toEqual({ MEMBER: { C: false, R: false, U: false, D: false } });
    });
});

describe('isRoleReadOnly', () => {
    it('is read-only for any system role when system role edition is disabled', () => {
        expect(isRoleReadOnly({ scope: 'API', name: 'USER', system: true }, false)).toBe(true);
        expect(isRoleReadOnly({ scope: 'API', name: 'USER', system: false }, false)).toBe(false);
    });

    it('when system role edition is enabled, only ORGANIZATION/ADMIN stays read-only', () => {
        expect(isRoleReadOnly({ scope: 'ORGANIZATION', name: 'ADMIN', system: true }, true)).toBe(true);
        expect(isRoleReadOnly({ scope: 'ORGANIZATION', name: 'USER', system: true }, true)).toBe(false);
        expect(isRoleReadOnly({ scope: 'API', name: 'USER', system: true }, true)).toBe(false);
    });
});

describe('canRoleBeDeleted', () => {
    it('cannot delete default or system roles', () => {
        expect(canRoleBeDeleted({ default: true, system: false })).toBe(false);
        expect(canRoleBeDeleted({ default: false, system: true })).toBe(false);
    });

    it('can delete a custom, non-default role', () => {
        expect(canRoleBeDeleted({ default: false, system: false })).toBe(true);
    });
});

describe('isPermissionEligibleScope', () => {
    it('accepts the scopes covered by the permissions-by-scope endpoint', () => {
        expect(isPermissionEligibleScope('API')).toBe(true);
        expect(isPermissionEligibleScope('ORGANIZATION')).toBe(true);
    });

    it('rejects EXPLORER and AI_WORKSPACE, which the endpoint does not cover', () => {
        expect(isPermissionEligibleScope('EXPLORER')).toBe(false);
        expect(isPermissionEligibleScope('AI_WORKSPACE')).toBe(false);
    });
});

describe('isRoleScope', () => {
    it('accepts every known role scope', () => {
        ROLE_SCOPES.forEach(scope => expect(isRoleScope(scope)).toBe(true));
    });

    it('rejects a value outside ROLE_SCOPES (e.g. a hand-edited URL param)', () => {
        expect(isRoleScope('FOO')).toBe(false);
        expect(isRoleScope('')).toBe(false);
        expect(isRoleScope(undefined)).toBe(false);
    });
});
