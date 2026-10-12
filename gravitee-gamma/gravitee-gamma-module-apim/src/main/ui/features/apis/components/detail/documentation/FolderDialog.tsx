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
    Alert,
    AlertDescription,
    Button,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    FieldDescription,
} from '@gravitee/graphene-core';
import { AlertCircleIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useState, type FormEvent } from 'react';

import { DocumentationItemDetailsFields, privateParentOf } from './DocumentationItemDetailsFields';
import { notify } from '../../../../../shared/notify';
import { extractErrorMessage } from '../../../../../shared/notify/extractErrorMessage';
import { useChangeApiDocumentationItem, useCreateApiDocumentationItem } from '../../../hooks/useApiDocumentation';
import type { PortalNavigationFolder, PortalVisibility } from '../../../types/apiDocumentation';
import { allowedChanges, type SyncState } from '../../../utils/documentationTree';

interface FolderDialogProps {
    apiId: string;
    /** The folder to edit; without it, a folder is added. */
    folder?: PortalNavigationFolder;
    /** The folder holding it, if any; a folder added without one goes to the top level of the API's documentation. */
    parent?: PortalNavigationFolder;
    syncState?: SyncState;
    /** How many items the edited folder holds, at any depth: they all become private with it. */
    itemsInside?: number;
    onClose: () => void;
    onCreated?: () => void;
}

export function FolderDialog({ open, ...props }: Readonly<FolderDialogProps & { open: boolean }>) {
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && props.onClose()}>
            <DialogContent style={{ maxWidth: '480px' }}>
                <FolderForm {...props} />
            </DialogContent>
        </Dialog>
    );
}

// Rendered inside the dialog content, so its state starts over from the folder each time the dialog opens.
function FolderForm({ apiId, folder, parent, syncState = 'NOT_SYNCED', itemsInside = 0, onClose, onCreated }: Readonly<FolderDialogProps>) {
    const createItem = useCreateApiDocumentationItem(apiId);
    const changeItem = useChangeApiDocumentationItem(apiId);
    const titleEditable = allowedChanges(syncState).title;
    const privateParent = privateParentOf(parent);
    const [title, setTitle] = useState(folder?.title ?? '');
    const [isPrivate, setIsPrivate] = useState(folder ? folder.visibility === 'PRIVATE' : privateParent !== undefined);
    const [isSaving, setIsSaving] = useState(false);
    const [refusal, setRefusal] = useState<string | null>(null);

    const visibility: PortalVisibility = isPrivate ? 'PRIVATE' : 'PUBLIC';
    // A title the server refuses to change goes back exactly as stored: trimmed, it would count as a rename.
    const newTitle = folder && !titleEditable ? folder.title : title.trim();
    const hasChanges = !folder || newTitle !== folder.title || visibility !== folder.visibility;
    const canSave = newTitle !== '' && hasChanges && !isSaving;
    const madePrivate = folder?.visibility === 'PUBLIC' && isPrivate;

    function handleTitleChange(next: string) {
        setTitle(next);
        setRefusal(null);
    }

    function handlePrivateChange(next: boolean) {
        setIsPrivate(next);
        setRefusal(null);
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canSave) return;
        setIsSaving(true);
        try {
            if (folder) {
                await changeItem.mutateAsync({ navId: folder.id, changes: { title: newTitle, visibility } });
                notify.success(`Folder '${newTitle}' saved`);
            } else {
                await createItem.mutateAsync({ type: 'FOLDER', title: newTitle, area: 'TOP_NAVBAR', visibility, parentId: parent?.id });
                notify.success(`Folder '${newTitle}' created`);
                onCreated?.();
            }
            onClose();
        } catch (error) {
            setRefusal(extractErrorMessage(error));
            setIsSaving(false);
        }
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>{folder ? 'Edit folder details' : 'Add a folder'}</DialogTitle>
                <DialogDescription>
                    {folder
                        ? 'Changes apply in every portal that lists this API.'
                        : parent
                          ? `The folder is created unpublished, inside ${parent.title}.`
                          : 'The folder is created unpublished.'}
                </DialogDescription>
            </DialogHeader>

            <form id="documentation-folder-form" onSubmit={handleSubmit} className="flex flex-col gap-5">
                {syncState === 'OWN_SOURCE' ? (
                    <Alert>
                        <RefreshCwIcon aria-hidden />
                        <AlertDescription>
                            This folder is synced from an external source, so its title can&apos;t change here. Its access can.
                        </AlertDescription>
                    </Alert>
                ) : null}

                {refusal ? (
                    <Alert variant="destructive">
                        <AlertCircleIcon aria-hidden />
                        <AlertDescription>{refusal}</AlertDescription>
                    </Alert>
                ) : null}

                <div className="flex flex-col gap-1.5">
                    <DocumentationItemDetailsFields
                        itemNoun="folder"
                        title={title}
                        onTitleChange={handleTitleChange}
                        isPrivate={isPrivate}
                        onPrivateChange={handlePrivateChange}
                        privateParent={privateParent}
                        titleReadOnly={!titleEditable}
                        disabled={isSaving}
                    />
                    {madePrivate && itemsInside > 0 ? (
                        <FieldDescription>
                            {itemsInside === 1
                                ? 'The 1 item inside becomes private too.'
                                : `The ${itemsInside} items inside become private too.`}
                        </FieldDescription>
                    ) : null}
                </div>
            </form>

            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                    Cancel
                </Button>
                <Button type="submit" form="documentation-folder-form" disabled={!canSave}>
                    {folder ? (isSaving ? 'Saving…' : 'Save') : isSaving ? 'Creating…' : 'Create'}
                </Button>
            </DialogFooter>
        </>
    );
}
