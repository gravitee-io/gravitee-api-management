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
} from '@gravitee/graphene-core';
import { AlertCircleIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useState, type FormEvent } from 'react';

import { DocumentationItemDetailsFields, privateParentOf } from './DocumentationItemDetailsFields';
import { notify } from '../../../../../shared/notify';
import { extractErrorMessage } from '../../../../../shared/notify/extractErrorMessage';
import { useChangeApiDocumentationItem } from '../../../hooks/useApiDocumentation';
import type { PortalNavigationFolder, PortalNavigationPage } from '../../../types/apiDocumentation';
import { allowedChanges, type SyncState } from '../../../utils/documentationTree';

export function EditPageDetailsDialog({
    open,
    apiId,
    page,
    parent,
    syncState,
    onClose,
}: Readonly<{
    open: boolean;
    apiId: string;
    page: PortalNavigationPage;
    /** The folder holding the page, if any. */
    parent?: PortalNavigationFolder;
    syncState: SyncState;
    onClose: () => void;
}>) {
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent style={{ maxWidth: '480px' }}>
                <EditPageDetailsForm apiId={apiId} page={page} parent={parent} syncState={syncState} onClose={onClose} />
            </DialogContent>
        </Dialog>
    );
}

// Rendered inside the dialog content, so its state starts over from the page each time the dialog opens.
function EditPageDetailsForm({
    apiId,
    page,
    parent,
    syncState,
    onClose,
}: Readonly<{
    apiId: string;
    page: PortalNavigationPage;
    parent?: PortalNavigationFolder;
    syncState: SyncState;
    onClose: () => void;
}>) {
    const changeItem = useChangeApiDocumentationItem(apiId);
    const titleEditable = allowedChanges(syncState).title;
    const privateParent = privateParentOf(parent);
    const [title, setTitle] = useState(page.title);
    const [isPrivate, setIsPrivate] = useState(page.visibility === 'PRIVATE');
    const [isSaving, setIsSaving] = useState(false);
    const [refusal, setRefusal] = useState<string | null>(null);

    const visibility = isPrivate ? 'PRIVATE' : 'PUBLIC';
    // A title the server refuses to change goes back exactly as stored: trimmed, it would count as a rename.
    const newTitle = titleEditable ? title.trim() : page.title;
    const hasChanges = newTitle !== page.title || visibility !== page.visibility;
    const canSave = newTitle !== '' && hasChanges && !isSaving;

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
            await changeItem.mutateAsync({ navId: page.id, changes: { title: newTitle, visibility } });
            notify.success(`Page '${newTitle}' saved`);
            onClose();
        } catch (error) {
            setRefusal(extractErrorMessage(error));
            setIsSaving(false);
        }
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>Edit page details</DialogTitle>
                <DialogDescription>Changes apply in every portal that lists this API.</DialogDescription>
            </DialogHeader>

            <form id="edit-documentation-page-details-form" onSubmit={handleSubmit} className="flex flex-col gap-5">
                {syncState === 'OWN_SOURCE' ? (
                    <Alert>
                        <RefreshCwIcon aria-hidden />
                        <AlertDescription>
                            This page is synced from an external source, so its title can&apos;t change here. Its access can.
                        </AlertDescription>
                    </Alert>
                ) : null}

                {refusal ? (
                    <Alert variant="destructive">
                        <AlertCircleIcon aria-hidden />
                        <AlertDescription>{refusal}</AlertDescription>
                    </Alert>
                ) : null}

                <DocumentationItemDetailsFields
                    title={title}
                    onTitleChange={handleTitleChange}
                    isPrivate={isPrivate}
                    onPrivateChange={handlePrivateChange}
                    privateParent={privateParent}
                    titleReadOnly={!titleEditable}
                    disabled={isSaving}
                />
            </form>

            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                    Cancel
                </Button>
                <Button type="submit" form="edit-documentation-page-details-form" disabled={!canSave}>
                    {isSaving ? 'Saving…' : 'Save'}
                </Button>
            </DialogFooter>
        </>
    );
}
