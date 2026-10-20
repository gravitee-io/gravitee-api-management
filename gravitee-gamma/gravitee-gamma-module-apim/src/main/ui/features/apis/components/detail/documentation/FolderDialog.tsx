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
import {
    useChangeApiDocumentationItem,
    useCreateApiDocumentationItem,
    useImportApiDocumentation,
} from '../../../hooks/useApiDocumentation';
import { useFetchNow } from '../../../hooks/useFetchNow';
import { SelectionCards, type SelectionCardItem } from '../../../pages/detail/general/SelectionCards';
import type { PortalNavigationFolder, PortalVisibility } from '../../../types/apiDocumentation';
import { allowedChanges, type SyncState } from '../../../utils/documentationTree';
import { fetchSummaryOutcome } from '../../../utils/fetchOutcome';
import { sourceChangeOf } from '../../../utils/sourceChange';

type FolderContents = 'EMPTY' | 'IMPORT';

const SUBMIT_LABELS = {
    save: { idle: 'Save', running: 'Saving…' },
    import: { idle: 'Import', running: 'Importing…' },
    create: { idle: 'Create', running: 'Creating…' },
} as const;

const FOLDER_CONTENTS: readonly SelectionCardItem<FolderContents>[] = [
    { id: 'EMPTY', label: 'Empty folder', description: 'Add pages to it yourself' },
    { id: 'IMPORT', label: 'Import from a repository', description: 'Pages fetched from a folder' },
];

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
    // An import can take several seconds: closing the dialog meanwhile would leave it running out of sight.
    const [isSaving, setIsSaving] = useState(false);

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && !isSaving && props.onClose()}>
            <DialogContent style={{ maxWidth: '560px', ...WITHIN_VIEWPORT }}>
                <FolderForm {...props} onSavingChange={setIsSaving} />
            </DialogContent>
        </Dialog>
    );
}

// Rendered inside the dialog content, so its state starts over from the folder each time the dialog opens.
function FolderForm({
    apiId,
    folder,
    parent,
    syncState = 'NOT_SYNCED',
    itemsInside = 0,
    onClose,
    onCreated,
    onSavingChange,
}: Readonly<FolderDialogProps & { onSavingChange: (isSaving: boolean) => void }>) {
    const createItem = useCreateApiDocumentationItem(apiId);
    const changeItem = useChangeApiDocumentationItem(apiId);
    const importFolder = useImportApiDocumentation(apiId);
    const { fetchNow } = useFetchNow(apiId);
    const privateParent = privateParentOf(parent);
    const [contents, setContents] = useState<FolderContents>('EMPTY');
    const [sourceState, setSourceState] = useState<ExternalSourceState>({ source: null, valid: false });
    const [title, setTitle] = useState(folder?.title ?? '');
    const [isPrivate, setIsPrivate] = useState(folder ? folder.visibility === 'PRIVATE' : privateParent !== undefined);
    const [isSaving, setIsSaving] = useState(false);
    const [refusal, setRefusal] = useState<string | null>(null);
    // Only a folder imported from a source has one to change: the server refuses to attach one to any other folder.
    const editsOwnSource = folder?.source !== undefined && syncState === 'OWN_SOURCE';
    // A source the folder was not imported with can only be removed: the server refuses to replace it.
    const sourceReplaceable = folder?.source?.subtreeImport === true;
    const [fetched, setFetched] = useState(editsOwnSource);
    const fetchedId = useId();

    useEffect(() => onSavingChange(isSaving), [isSaving, onSavingChange]);

    const isImport = !folder && contents === 'IMPORT';
    // The server locks the title of a synced folder, and lifts the lock in the update removing its source.
    const titleEditable = allowedChanges(syncState).title || (editsOwnSource && !fetched);
    const visibility: PortalVisibility = isPrivate ? 'PRIVATE' : 'PUBLIC';
    // A title the server refuses to change goes back exactly as stored: trimmed, it would count as a rename.
    const newTitle = folder && !titleEditable ? folder.title : title.trim();
    const sourceChange = editsOwnSource ? sourceChangeOf(folder.source, fetched, sourceState) : undefined;
    const hasChanges = !folder || newTitle !== folder.title || visibility !== folder.visibility || sourceChange !== undefined;
    const sourceReady = isImport ? sourceState.valid : !sourceChange || sourceState.valid;
    const canSave = newTitle !== '' && hasChanges && sourceReady && !isSaving;
    const madePrivate = folder?.visibility === 'PUBLIC' && isPrivate;

    function handleTitleChange(next: string) {
        setTitle(next);
        setRefusal(null);
    }

    function handlePrivateChange(next: boolean) {
        setIsPrivate(next);
        setRefusal(null);
    }

    function handleFetchedChange(next: boolean) {
        setFetched(next);
        // A source locks the title: it shows the one that will be saved.
        if (next && folder) setTitle(folder.title);
        setRefusal(null);
    }

    function handleContentsChange(next: FolderContents) {
        setContents(next);
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
            if (folder) {
                await changeItem.mutateAsync({
                    navId: folder.id,
                    changes: { title: newTitle, visibility, ...(sourceChange === undefined ? {} : { source: sourceChange }) },
                });
                notify.success(`Folder '${newTitle}' saved`);
                // An update never fetches: the folder would keep the old pages until the next fetch.
                if (sourceChange) void fetchNow({ id: folder.id, title: newTitle });
            } else if (isImport && sourceState.source) {
                const { summary } = await importFolder.mutateAsync({
                    title: newTitle,
                    parentId: parent?.id,
                    visibility,
                    source: sourceState.source,
                });
                const outcome = fetchSummaryOutcome(summary);
                const message = `Folder '${newTitle}' imported. ${outcome.message}`;
                if (outcome.level === 'success') notify.success(message);
                else notify.warning(message);
                onCreated?.();
            } else {
                await createItem.mutateAsync({ type: 'FOLDER', title: newTitle, area: 'TOP_NAVBAR', visibility, parentId: parent?.id });
                notify.success(`Folder '${newTitle}' created`);
                onCreated?.();
            }
            setIsSaving(false);
            onClose();
        } catch (error) {
            setRefusal(extractErrorMessage(error));
            setIsSaving(false);
        }
    }

    const submitLabel = SUBMIT_LABELS[folder ? 'save' : isImport ? 'import' : 'create'][isSaving ? 'running' : 'idle'];

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

            <ScrollableDialogBody>
                <div className="flex flex-col gap-5">
                    <form id="documentation-folder-form" onSubmit={handleSubmit} className="flex flex-col gap-5">
                        {syncState === 'OWN_SOURCE' && fetched ? (
                            <Alert>
                                <RefreshCwIcon aria-hidden />
                                <AlertDescription>
                                    This folder is synced from an external source, so its title can&apos;t change here. Its access can.
                                </AlertDescription>
                            </Alert>
                        ) : null}

                        <RefusalAlert refusal={refusal} />

                        {folder ? null : (
                            <FieldSet className="gap-1.5">
                                <FieldLegend variant="label">Contents</FieldLegend>
                                <SelectionCards
                                    options={FOLDER_CONTENTS}
                                    activeId={contents}
                                    onChange={handleContentsChange}
                                    ariaLabel="Contents"
                                    disabled={isSaving}
                                />
                            </FieldSet>
                        )}

                        <div className="flex flex-col gap-1.5">
                            <DocumentationItemDetailsFields
                                itemType="FOLDER"
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

                    {/* Outside the form: a plugin's settings form may hold buttons of its own. */}
                    {isImport ? (
                        <div className="flex flex-col gap-3">
                            <p className="text-xs text-muted-foreground">
                                The pages are fetched from the repository and stay read only. A .gravitee.json manifest decides the
                                hierarchy; without one, the folder tree is mirrored.
                            </p>
                            <ExternalSourceConfig forImport onChange={handleSourceChange} disabled={isSaving} />
                        </div>
                    ) : null}
                    {editsOwnSource ? (
                        <FieldSet className="gap-3">
                            <FieldLegend variant="label">External source</FieldLegend>
                            <div className="flex items-center gap-2">
                                <Switch id={fetchedId} checked={fetched} onCheckedChange={handleFetchedChange} disabled={isSaving} />
                                <Label htmlFor={fetchedId} className="font-normal">
                                    Fetch this folder from an external source
                                </Label>
                            </div>
                            {fetched && sourceReplaceable ? (
                                <ExternalSourceConfig
                                    forImport
                                    storedSource={folder.source}
                                    onChange={handleSourceChange}
                                    disabled={isSaving}
                                />
                            ) : null}
                            {fetched && !sourceReplaceable ? (
                                <p className="text-xs text-muted-foreground">
                                    This folder was not imported from its source, so the source can only be removed here.
                                </p>
                            ) : null}
                            {fetched ? null : (
                                <Alert>
                                    <AlertCircleIcon aria-hidden />
                                    <AlertDescription>
                                        The source will be removed. The folder and the items in it stop being fetched and become editable.
                                        Their current content is kept.
                                    </AlertDescription>
                                </Alert>
                            )}
                        </FieldSet>
                    ) : null}
                </div>
            </ScrollableDialogBody>

            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                    Cancel
                </Button>
                <Button type="submit" form="documentation-folder-form" disabled={!canSave}>
                    {submitLabel}
                </Button>
            </DialogFooter>
        </>
    );
}
