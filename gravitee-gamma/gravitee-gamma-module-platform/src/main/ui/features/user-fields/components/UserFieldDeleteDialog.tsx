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
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import type { UserField } from '../types/userField';

export function UserFieldDeleteDialog({
    open,
    field,
    onClose,
    onConfirm,
    isDeleting,
}: Readonly<{
    open: boolean;
    field: UserField | undefined;
    onClose: () => void;
    onConfirm: () => void;
    isDeleting: boolean;
}>) {
    return (
        <ConfirmDialog
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen && !isDeleting) onClose();
            }}
            title="Delete custom user field"
            description={
                <>
                    Are you sure you want to delete <span className="font-mono text-xs bg-muted px-1 py-0.5 rounded">{field?.key}</span>{' '}
                    field? It will no longer appear on APIM console or developer portal sign-up, and existing answers on user profiles will
                    be removed.
                </>
            }
            confirmLabel="Delete"
            pendingLabel="Deleting…"
            destructive
            isPending={isDeleting}
            confirmDisabled={!field}
            onConfirm={onConfirm}
        />
    );
}
