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
import { ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION, ENVIRONMENT_SECRET_DELETE_PERMISSION } from '../utils/credentialPermissions';

/** Deleting a credential Gravitee stores also deletes its secrets, so that needs the secret permission too. */
export function useCanDeleteCredential(): (credential: Pick<Credential, 'managed'>) => boolean {
    const canDeleteCredentials = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION]);
    const canDeleteSecrets = useHasEnvironmentPermission([ENVIRONMENT_SECRET_DELETE_PERMISSION]);
    return credential => canDeleteCredentials && (credential.managed === false || canDeleteSecrets);
}
