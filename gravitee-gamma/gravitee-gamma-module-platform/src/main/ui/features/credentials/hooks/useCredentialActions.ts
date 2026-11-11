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
import { useQuery } from '@tanstack/react-query';

import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { fetchCurrentUser } from '../../../shared/services/currentUser';
import { currentUserKeys } from '../../../shared/utils/queryKeys';
import type { Credential } from '../types/credential';
import {
    ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION,
    ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION,
    ENVIRONMENT_SECRET_DELETE_PERMISSION,
} from '../utils/credentialPermissions';

type CredentialCheck = (credential: Pick<Credential, 'ownerId' | 'managed'>) => boolean;

/**
 * The backend lets only the owner or an organization or environment admin change or delete a credential, and
 * deleting one Gravitee stores also deletes its secrets.
 */
export function useCredentialActions(): { canEdit: CredentialCheck; canDelete: CredentialCheck } {
    const canUpdateCredentials = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION]);
    const canDeleteCredentials = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION]);
    const canDeleteSecrets = useHasEnvironmentPermission([ENVIRONMENT_SECRET_DELETE_PERMISSION]);
    const { data: user } = useQuery({ queryKey: currentUserKeys.detail(), queryFn: fetchCurrentUser, staleTime: 300_000 });

    const isAdmin =
        user?.roles?.some(role => role.name === 'ADMIN' && (role.scope === 'ORGANIZATION' || role.scope === 'ENVIRONMENT')) ?? false;
    const manages: CredentialCheck = credential => isAdmin || (Boolean(user?.id) && credential.ownerId === user?.id);

    return {
        canEdit: credential => canUpdateCredentials && manages(credential),
        canDelete: credential => canDeleteCredentials && manages(credential) && (credential.managed === false || canDeleteSecrets),
    };
}
