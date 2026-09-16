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
import {
    authenticationFromConsole,
    buildTokenUsageExample,
    currentUserAvatarUrl,
    customFieldsAsStrings,
    displayDangerZone,
    formatGroupsByEnvironment,
    formatRoles,
    hasMissingRequiredCustomFields,
    isDuplicateTokenError,
    isInternalUser,
    pictureFieldForUpdate,
    toUpdateUserPayload,
    validateTokenName,
} from './myAccount.mapping';

describe('isInternalUser', () => {
    it('should treat gravitee and memory sources as editable identity', () => {
        expect(isInternalUser('gravitee')).toBe(true);
        expect(isInternalUser('memory')).toBe(true);
    });

    it('should treat IdP sources as read-only identity', () => {
        expect(isInternalUser('google')).toBe(false);
        expect(isInternalUser('oidc')).toBe(false);
        expect(isInternalUser(undefined)).toBe(false);
    });
});

describe('displayDangerZone', () => {
    it('should hide the danger zone when console settings are unknown', () => {
        expect(displayDangerZone(undefined)).toBe(false);
    });

    it('should show the danger zone when external auth is off', () => {
        expect(displayDangerZone({ externalAuth: { enabled: false } })).toBe(true);
    });

    it('should show the danger zone when external auth allows account deletion', () => {
        expect(
            displayDangerZone({
                externalAuth: { enabled: true },
                externalAuthAccountDeletion: { enabled: true },
            }),
        ).toBe(true);
    });

    it('should hide the danger zone when external auth is on and account deletion is off', () => {
        expect(
            displayDangerZone({
                externalAuth: { enabled: true },
                externalAuthAccountDeletion: { enabled: false },
            }),
        ).toBe(false);
    });
});

describe('pictureFieldForUpdate', () => {
    it('should send a data URL when the operator uploaded a new picture', () => {
        expect(
            pictureFieldForUpdate({
                pictureDataUrl: 'data:image/png;base64,abc',
                resetToDefault: false,
                hadPictureOnLoad: true,
            }),
        ).toBe('data:image/png;base64,abc');
    });

    it('should send an empty string when Use default clears a picture that was present on load', () => {
        expect(
            pictureFieldForUpdate({
                pictureDataUrl: null,
                resetToDefault: true,
                hadPictureOnLoad: true,
            }),
        ).toBe('');
    });

    it('should omit the picture when nothing changed', () => {
        expect(
            pictureFieldForUpdate({
                pictureDataUrl: null,
                resetToDefault: false,
                hadPictureOnLoad: true,
            }),
        ).toBeUndefined();
    });
});

describe('toUpdateUserPayload', () => {
    it('should omit picture when the avatar is unchanged', () => {
        expect(
            toUpdateUserPayload({
                firstname: 'Ada',
                lastname: 'Lovelace',
                email: 'ada@example.com',
                customFields: { team: 'platform' },
                pictureDataUrl: null,
                resetToDefault: false,
                hadPictureOnLoad: true,
            }),
        ).toEqual({
            firstname: 'Ada',
            lastname: 'Lovelace',
            email: 'ada@example.com',
            customFields: { team: 'platform' },
        });
    });
});

describe('hasMissingRequiredCustomFields', () => {
    it('should report an empty required dropdown as missing', () => {
        expect(hasMissingRequiredCustomFields({}, [{ key: 'team', required: true }])).toBe(true);
        expect(hasMissingRequiredCustomFields({ team: '   ' }, [{ key: 'team', required: true }])).toBe(true);
        expect(hasMissingRequiredCustomFields({ team: 'Platform' }, [{ key: 'team', required: true }])).toBe(false);
        expect(hasMissingRequiredCustomFields({}, [{ key: 'team', required: false }])).toBe(false);
    });
});

describe('currentUserAvatarUrl', () => {
    it('should cache-bust the management avatar URL', () => {
        expect(currentUserAvatarUrl('http://api.test/management', 'test-org', 'user-1', 42)).toBe(
            'http://api.test/management/organizations/test-org/user/avatar?user-1&cacheBust=42',
        );
    });

    it('should trim a trailing slash on the management base URL', () => {
        expect(currentUserAvatarUrl('http://api.test/management/', 'test-org', 'user-1', 42)).toBe(
            'http://api.test/management/organizations/test-org/user/avatar?user-1&cacheBust=42',
        );
    });
});

describe('formatRoles', () => {
    it('should render Classic [SCOPE] name labels', () => {
        expect(
            formatRoles([
                { scope: 'ORGANIZATION', name: 'ADMIN' },
                { scope: 'ENVIRONMENT', name: 'USER' },
            ]),
        ).toBe('[ORGANIZATION] ADMIN - [ENVIRONMENT] USER');
    });
});

describe('formatGroupsByEnvironment', () => {
    const envs = [
        { id: 'env-1', name: 'Dev' },
        { id: 'env-2', name: 'Prod' },
    ];

    it('should join a single environment with dashes', () => {
        expect(formatGroupsByEnvironment({ 'env-1': ['api-devs', 'ops'] }, envs)).toBe('api-devs - ops');
    });

    it('should prefix each environment name when the user belongs to more than one', () => {
        expect(formatGroupsByEnvironment({ 'env-1': ['api-devs'], 'env-2': ['ops'] }, envs)).toBe('[Dev] api-devs - [Prod] ops');
    });

    it('should still list groups whose environment is missing from the environments list', () => {
        expect(formatGroupsByEnvironment({ 'env-1': ['api-devs'], 'env-orphan': ['contractors'] }, [{ id: 'env-1', name: 'Dev' }])).toBe(
            '[Dev] api-devs - [env-orphan] contractors',
        );
    });
});

describe('validateTokenName', () => {
    it('should require 2 to 64 characters', () => {
        expect(validateTokenName('')).toBe('Name is required.');
        expect(validateTokenName('a')).toBe('Name has to be at least 2 characters long.');
        expect(validateTokenName('ok')).toBeNull();
        expect(validateTokenName('x'.repeat(65))).toBe('Name has to be at most 64 characters long.');
    });
});

describe('isDuplicateTokenError', () => {
    it('should recognise the backend duplicate-name technical code and message', () => {
        expect(isDuplicateTokenError('token.alreadyExists', 'A token with the name already exists')).toBe(true);
        expect(isDuplicateTokenError(undefined, 'A token with the name CI already exists')).toBe(true);
        expect(isDuplicateTokenError(undefined, 'network down')).toBe(false);
    });
});

describe('buildTokenUsageExample', () => {
    it('should build the Classic curl against the current environment id', () => {
        expect(buildTokenUsageExample('tok', 'http://api.test/management/', 'org', 'env-1-id')).toBe(
            'curl -H "Authorization: Bearer tok" "http://api.test/management/organizations/org/environments/env-1-id"',
        );
    });
});

describe('authenticationFromConsole', () => {
    it('should read authentication flags from GET /console', () => {
        expect(
            authenticationFromConsole({
                authentication: { externalAuth: { enabled: true }, externalAuthAccountDeletion: { enabled: false } },
            }),
        ).toEqual({ externalAuth: { enabled: true }, externalAuthAccountDeletion: { enabled: false } });
    });
});

describe('customFieldsAsStrings', () => {
    it('should stringify stored custom field values', () => {
        expect(customFieldsAsStrings({ team: 'platform', count: 2 })).toEqual({ team: 'platform', count: '2' });
    });
});
