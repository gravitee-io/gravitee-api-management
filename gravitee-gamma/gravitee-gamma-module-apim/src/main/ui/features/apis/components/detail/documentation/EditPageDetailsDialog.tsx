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
    FieldLegend,
    FieldSet,
    Label,
    Switch,
} from '@gravitee/graphene-core';
import { AlertCircleIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useId, useState, type FormEvent } from 'react';

import { RefusalAlert, ScrollableDialogBody, WITHIN_VIEWPORT } from './dialogLayout';
import { DocumentationItemDetailsFields, privateParentOf } from './DocumentationItemDetailsFields';
import { ExternalSourceConfig, type ExternalSourceState } from './ExternalSourceConfig';
import { notify } from '../../../../../shared/notify';
import { extractErrorMessage } from '../../../../../shared/notify/extractErrorMessage';
import { useChangeApiDocumentationItem } from '../../../hooks/useApiDocumentation';
import { useFetchNow } from '../../../hooks/useFetchNow';
import type { PortalNavigationFolder, PortalNavigationPage } from '../../../types/apiDocumentation';
import type { SyncState } from '../../../utils/documentationTree';
import { sourceChangeOf } from '../../../utils/sourceChange';

export function EditPageDetailsDialog({
    open,
    apiId,
    page,
    parent,
    syncState,
    hasUnsavedContent = false,
    onClose,
}: Readonly<{
    open: boolean;
    apiId: string;
    page: PortalNavigationPage;
    /** The folder holding the page, if any. */
    parent?: PortalNavigationFolder;
    syncState: SyncState;
    /** Whether the editor holds changes to the content not saved yet, which a source would replace. */
    hasUnsavedContent?: boolean;
    onClose: () => void;
}>) {
    // Closing while saving would lose a refusal from the server.
    const [isSaving, setIsSaving] = useState(false);

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && !isSaving && onClose()}>
            <DialogContent style={{ maxWidth: '560px', ...WITHIN_VIEWPORT }}>
                <EditPageDetailsForm
                    apiId={apiId}
                    page={page}
                    parent={parent}
                    syncState={syncState}
                    hasUnsavedContent={hasUnsavedContent}
                    onClose={onClose}
                    onSavingChange={setIsSaving}
                />
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
    hasUnsavedContent,
    onClose,
    onSavingChange,
}: Readonly<{
    apiId: string;
    page: PortalNavigationPage;
    parent?: PortalNavigationFolder;
    syncState: SyncState;
    hasUnsavedContent: boolean;
    onClose: () => void;
    onSavingChange: (isSaving: boolean) => void;
}>) {
    const changeItem = useChangeApiDocumentationItem(apiId);
    const { fetchNow } = useFetchNow(apiId);
    // A page below a synced folder is fetched through that folder: it has no source of its own to change.
    const ownSourceEditable = syncState !== 'BELOW_SYNCED_FOLDER';
    const [fetched, setFetched] = useState(page.source !== undefined);
    const [sourceState, setSourceState] = useState<ExternalSourceState>({ source: null, valid: false });
    const fetchedId = useId();
    // The server locks the title of a page fetched from a source, and lifts the lock in the update removing it.
    const titleEditable = ownSourceEditable && !fetched;
    const privateParent = privateParentOf(parent);
    const [title, setTitle] = useState(page.title);
    const [isPrivate, setIsPrivate] = useState(page.visibility === 'PRIVATE');
    const [isSaving, setIsSaving] = useState(false);
    const [refusal, setRefusal] = useState<string | null>(null);

    useEffect(() => onSavingChange(isSaving), [isSaving, onSavingChange]);

    const visibility = isPrivate ? 'PRIVATE' : 'PUBLIC';
    // A title the server refuses to change goes back exactly as stored: trimmed, it would count as a rename.
    const newTitle = titleEditable ? title.trim() : page.title;
    const sourceChange = ownSourceEditable ? sourceChangeOf(page.source, fetched, sourceState) : undefined;
    const hasChanges = newTitle !== page.title || visibility !== page.visibility || sourceChange !== undefined;
    const sourceReady = !sourceChange || sourceState.valid;
    const canSave = newTitle !== '' && hasChanges && sourceReady && !isSaving;

    function handleTitleChange(next: string) {
        setTitle(next);
        setRefusal(null);
    }

    function handleFetchedChange(next: boolean) {
        setFetched(next);
        // A source locks the title: it shows the one that will be saved.
        if (next) setTitle(page.title);
        setRefusal(null);
    }

    function handlePrivateChange(next: boolean) {
        setIsPrivate(next);
        setRefusal(null);
    }

    function handleSourceChange(next: ExternalSourceState) {
        setSourceState(next);
        setRefusal(null);
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canSave) return;
        // Cleared first, so that the same refusal given again is shown, and brought into view, again.
        setRefusal(null);
        setIsSaving(true);
        try {
            await changeItem.mutateAsync({
                navId: page.id,
                changes: { title: newTitle, visibility, ...(sourceChange === undefined ? {} : { source: sourceChange }) },
            });
            notify.success(`Page '${newTitle}' saved`);
            // An update never fetches: the page would keep the old content until the next fetch.
            if (sourceChange) void fetchNow({ id: page.id, title: newTitle });
            setIsSaving(false);
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

            <ScrollableDialogBody>
                <div className="flex flex-col gap-5">
                    <form id="edit-documentation-page-details-form" onSubmit={handleSubmit} className="flex flex-col gap-5">
                        {syncState === 'OWN_SOURCE' && fetched ? (
                            <Alert>
                                <RefreshCwIcon aria-hidden />
                                <AlertDescription>
                                    This page is synced from an external source, so its title can&apos;t change here. Its access can.
                                </AlertDescription>
                            </Alert>
                        ) : null}

                        <RefusalAlert refusal={refusal} />

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

                    {/* Outside the form: a plugin's settings form may hold buttons of its own. */}
                    {ownSourceEditable ? (
                        <FieldSet className="gap-3">
                            <FieldLegend variant="label">External source</FieldLegend>
                            <div className="flex items-center gap-2">
                                <Switch id={fetchedId} checked={fetched} onCheckedChange={handleFetchedChange} disabled={isSaving} />
                                <Label htmlFor={fetchedId} className="font-normal">
                                    Fetch this page from an external source
                                </Label>
                            </div>
                            {fetched && !page.source && hasUnsavedContent ? (
                                <Alert>
                                    <AlertCircleIcon aria-hidden />
                                    <AlertDescription>
                                        The unsaved changes to the content will be lost: the content is fetched from the source instead.
                                    </AlertDescription>
                                </Alert>
                            ) : null}
                            {fetched ? (
                                <ExternalSourceConfig storedSource={page.source} onChange={handleSourceChange} disabled={isSaving} />
                            ) : null}
                            {!fetched && page.source ? (
                                <Alert>
                                    <AlertCircleIcon aria-hidden />
                                    <AlertDescription>
                                        The source will be removed. The page stops being fetched and becomes editable. Its current content
                                        is kept.
                                    </AlertDescription>
                                </Alert>
                            ) : null}
                        </FieldSet>
                    ) : null}
                </div>
            </ScrollableDialogBody>

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
