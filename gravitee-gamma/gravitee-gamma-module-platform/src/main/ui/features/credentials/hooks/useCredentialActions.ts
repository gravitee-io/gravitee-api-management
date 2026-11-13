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
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import type { Credential } from '../types/credential';
import {
    ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION,
    ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION,
    ENVIRONMENT_SECRET_DELETE_PERMISSION,
} from '../utils/credentialPermissions';
import { isKnownCredentialStrategy } from '../utils/credentialStrategies';

type CredentialCheck = (credential: Pick<Credential, 'strategy' | 'manageable' | 'managed'>) => boolean;

/**
 * The server says whether the caller may manage a credential, because only it knows whether they administer this
 * environment. Deleting one Gravitee stores also deletes its secrets.
 */
export function useCredentialActions(): { canEdit: CredentialCheck; canDelete: CredentialCheck } {
    const canUpdateCredentials = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION]);
    const canDeleteCredentials = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION]);
    const canDeleteSecrets = useHasEnvironmentPermission([ENVIRONMENT_SECRET_DELETE_PERMISSION]);

    return {
        canEdit: credential => canUpdateCredentials && credential.manageable === true && isKnownCredentialStrategy(credential.strategy),
        canDelete: credential =>
            canDeleteCredentials && credential.manageable === true && (credential.managed === false || canDeleteSecrets),
    };
}
