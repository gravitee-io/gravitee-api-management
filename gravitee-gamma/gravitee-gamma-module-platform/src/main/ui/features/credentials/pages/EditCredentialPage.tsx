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
import { Skeleton } from '@gravitee/graphene-core';
import { Navigate, useParams } from 'react-router-dom';

import { CredentialLoadError } from '../components/CredentialLoadError';
import { EditCredentialForm } from '../components/EditCredentialForm';
import { useCredentialActions } from '../hooks/useCredentialActions';
import { useCredential } from '../hooks/useCredentials';

export function EditCredentialPage() {
    const { credentialId } = useParams<{ credentialId: string }>();
    const { data: credential, isLoading, error, isFetching, refetch } = useCredential(credentialId);
    const { canEdit } = useCredentialActions();

    if (isLoading) {
        return <Skeleton className="h-40 w-full" />;
    }
    if (!credential) {
        return <CredentialLoadError error={error} retrying={isFetching} onRetry={() => void refetch()} />;
    }
    if (!canEdit(credential)) {
        return <Navigate to=".." relative="path" replace />;
    }
    return <EditCredentialForm key={credential.id} credential={credential} />;
}
