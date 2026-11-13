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
import { CredentialConsumerList } from './CredentialConsumerList';
import { ApimApiError } from '../../../shared/api/apimClient';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify';
import { useDeleteCredential } from '../hooks/useCredentialMutations';
import { useCredentialConsumers } from '../hooks/useCredentials';
import type { Credential } from '../types/credential';
import { credentialFailureMessage } from '../utils/credentialErrors';

interface DeleteCredentialDialogProps {
    readonly credential: Pick<Credential, 'id' | 'name'>;
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onDeleted: () => void;
}

export function DeleteCredentialDialog({ credential, open, onOpenChange, onDeleted }: DeleteCredentialDialogProps) {
    const { data: consumers, isError: consumersFailed, refetch } = useCredentialConsumers(credential.id, open);
    const deleteMutation = useDeleteCredential();
    const inUse = (consumers?.length ?? 0) > 0;

    async function handleConfirm() {
        try {
            await deleteMutation.mutateAsync(credential.id);
            notify.success('Credential deleted');
            onDeleted();
        } catch (error) {
            if (error instanceof ApimApiError && error.status === 409) {
                void refetch();
            }
            notify.error(credentialFailureMessage(error, 'The credential could not be deleted.'));
        }
    }

    return (
        <ConfirmDialog
            open={open}
            onOpenChange={onOpenChange}
            title={`Delete ${credential.name}`}
            description={
                inUse
                    ? 'Something still uses this credential. Point each consumer below at another credential, then delete it.'
                    : 'The credential and the values Gravitee stores for it are deleted. This cannot be undone.'
            }
            confirmLabel="Delete"
            pendingLabel="Deleting…"
            destructive
            isPending={deleteMutation.isPending}
            // When the consumers cannot load, the delete stays possible: the server refuses it while something uses the credential.
            confirmDisabled={(consumers === undefined && !consumersFailed) || inUse}
            onConfirm={() => void handleConfirm()}
        >
            {inUse ? <CredentialConsumerList consumers={consumers ?? []} /> : null}
            {consumersFailed ? (
                <p className="text-sm text-muted-foreground">
                    Could not check what uses this credential. Gravitee refuses the delete if something still uses it.
                </p>
            ) : null}
        </ConfirmDialog>
    );
}
