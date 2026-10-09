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

import { PageDetailsFields, privateParentOf } from './PageDetailsFields';
import { notify } from '../../../../../shared/notify';
import { extractErrorMessage } from '../../../../../shared/notify/extractErrorMessage';
import { useChangeApiDocumentationItem } from '../../../hooks/useApiDocumentation';
import type { PortalNavigationFolder, PortalNavigationPage } from '../../../types/apiDocumentation';
import { hasSource } from '../../../utils/documentationTree';

export function EditPageDetailsDialog({
    open,
    apiId,
    page,
    parent,
    onClose,
}: Readonly<{
    open: boolean;
    apiId: string;
    page: PortalNavigationPage;
    /** The folder holding the page, if any. */
    parent?: PortalNavigationFolder;
    onClose: () => void;
}>) {
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent style={{ maxWidth: '480px' }}>
                <EditPageDetailsForm apiId={apiId} page={page} parent={parent} onClose={onClose} />
            </DialogContent>
        </Dialog>
    );
}

// Rendered inside the dialog content, so its state starts over from the page each time the dialog opens.
function EditPageDetailsForm({
    apiId,
    page,
    parent,
    onClose,
}: Readonly<{ apiId: string; page: PortalNavigationPage; parent?: PortalNavigationFolder; onClose: () => void }>) {
    const changeItem = useChangeApiDocumentationItem(apiId);
    const privateParent = privateParentOf(parent);
    const [title, setTitle] = useState(page.title);
    const [isPrivate, setIsPrivate] = useState(page.visibility === 'PRIVATE');
    const [isSaving, setIsSaving] = useState(false);
    const [refusal, setRefusal] = useState<string | null>(null);

    const visibility = isPrivate ? 'PRIVATE' : 'PUBLIC';
    const hasChanges = title.trim() !== page.title || visibility !== page.visibility;
    const canSave = title.trim() !== '' && hasChanges && !isSaving;

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
            await changeItem.mutateAsync({ navId: page.id, changes: { title: title.trim(), visibility } });
            notify.success(`Page '${title.trim()}' saved`);
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
                {hasSource(page) ? (
                    <Alert>
                        <RefreshCwIcon aria-hidden />
                        <AlertDescription>
                            This page is synced from an external source. Renaming it or changing its access keeps it synced.
                        </AlertDescription>
                    </Alert>
                ) : null}

                {refusal ? (
                    <Alert variant="destructive">
                        <AlertCircleIcon aria-hidden />
                        <AlertDescription>{refusal}</AlertDescription>
                    </Alert>
                ) : null}

                <PageDetailsFields
                    title={title}
                    onTitleChange={handleTitleChange}
                    isPrivate={isPrivate}
                    onPrivateChange={handlePrivateChange}
                    privateParent={privateParent}
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
