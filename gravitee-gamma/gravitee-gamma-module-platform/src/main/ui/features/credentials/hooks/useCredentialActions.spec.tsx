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
import { renderHook } from '@testing-library/react';

import { useCredentialActions } from './useCredentialActions';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import type { Credential } from '../types/credential';
import {
    ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION,
    ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION,
    ENVIRONMENT_SECRET_DELETE_PERMISSION,
} from '../utils/credentialPermissions';

jest.mock('../../../shared/hooks/useEnvironmentPermissions');

const ALL_PERMISSIONS = [
    ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION,
    ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION,
    ENVIRONMENT_SECRET_DELETE_PERMISSION,
];
const MANAGEABLE: Credential = { id: 'cred-1', name: 'Mine', strategy: 'BEARER', managed: true, manageable: true };
const NOT_MANAGEABLE: Credential = { id: 'cred-2', name: 'Theirs', strategy: 'BEARER', managed: true, manageable: false };
const MANAGEABLE_REFERENCE: Credential = { ...MANAGEABLE, id: 'cred-3', managed: false };

function actionsWith(granted: readonly string[]) {
    jest.mocked(useHasEnvironmentPermission).mockImplementation(anyOf => anyOf.some(permission => granted.includes(permission)));
    return renderHook(() => useCredentialActions()).result.current;
}

describe('useCredentialActions', () => {
    afterEach(() => jest.clearAllMocks());

    it('lets the caller edit and delete a credential the server says they can manage', () => {
        const actions = actionsWith(ALL_PERMISSIONS);

        expect(actions.canEdit(MANAGEABLE)).toBe(true);
        expect(actions.canDelete(MANAGEABLE)).toBe(true);
    });

    it('hides edit and delete on a credential the server says they cannot manage', () => {
        const actions = actionsWith(ALL_PERMISSIONS);

        expect(actions.canEdit(NOT_MANAGEABLE)).toBe(false);
        expect(actions.canDelete(NOT_MANAGEABLE)).toBe(false);
    });

    it('lets a caller delete but not edit a credential of a type this console does not know', () => {
        const actions = actionsWith(ALL_PERMISSIONS);
        const unknown = { ...MANAGEABLE, strategy: 'MTLS' } as unknown as Credential;

        expect(actions.canEdit(unknown)).toBe(false);
        expect(actions.canDelete(unknown)).toBe(true);
    });

    it('needs the credential permissions even on a manageable credential', () => {
        const actions = actionsWith([]);

        expect(actions.canEdit(MANAGEABLE)).toBe(false);
        expect(actions.canDelete(MANAGEABLE)).toBe(false);
    });

    it('needs the secret delete permission to delete a credential Gravitee stores', () => {
        const actions = actionsWith([ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION]);

        expect(actions.canDelete(MANAGEABLE_REFERENCE)).toBe(true);
        expect(actions.canDelete(MANAGEABLE)).toBe(false);
    });
});
