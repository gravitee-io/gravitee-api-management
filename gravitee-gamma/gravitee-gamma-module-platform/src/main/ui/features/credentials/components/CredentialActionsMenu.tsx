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
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@gravitee/graphene-core';
import { MoreVerticalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { createContext, use, useRef, useState } from 'react';
import { Link } from 'react-router-dom';

import { DeleteCredentialDialog } from './DeleteCredentialDialog';
import { useCredentialActions } from '../hooks/useCredentialActions';
import type { Credential } from '../types/credential';

/** Called once a row's credential is deleted, to move the focus off the row that goes with it. */
export const CredentialDeletedContext = createContext<() => void>(() => {});

export function CredentialActionsMenu({ credential }: Readonly<{ credential: Credential }>) {
    const onDeleted = use(CredentialDeletedContext);
    const { canEdit, canDelete } = useCredentialActions();
    const [confirmingDelete, setConfirmingDelete] = useState(false);
    const triggerRef = useRef<HTMLButtonElement>(null);
    const deleted = useRef(false);
    const editable = canEdit(credential);
    const deletable = canDelete(credential);

    if (!editable && !deletable) {
        return null;
    }

    return (
        <div className="flex justify-end">
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button ref={triggerRef} variant="ghost" size="icon-xs" aria-label={`Actions for ${credential.name}`}>
                        <MoreVerticalIcon aria-hidden />
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                    {editable && (
                        <DropdownMenuItem asChild>
                            <Link to={`${credential.id}/edit`}>
                                <PencilIcon aria-hidden />
                                Edit
                            </Link>
                        </DropdownMenuItem>
                    )}
                    {editable && deletable && <DropdownMenuSeparator />}
                    {deletable && (
                        <DropdownMenuItem variant="destructive" onSelect={() => setConfirmingDelete(true)}>
                            <Trash2Icon aria-hidden />
                            Delete
                        </DropdownMenuItem>
                    )}
                </DropdownMenuContent>
            </DropdownMenu>
            {deletable && (
                <DeleteCredentialDialog
                    credential={credential}
                    open={confirmingDelete}
                    onOpenChange={setConfirmingDelete}
                    onDeleted={() => {
                        deleted.current = true;
                        setConfirmingDelete(false);
                    }}
                    onCloseAutoFocus={event => {
                        event.preventDefault();
                        if (deleted.current) onDeleted();
                        else triggerRef.current?.focus();
                    }}
                />
            )}
        </div>
    );
}
