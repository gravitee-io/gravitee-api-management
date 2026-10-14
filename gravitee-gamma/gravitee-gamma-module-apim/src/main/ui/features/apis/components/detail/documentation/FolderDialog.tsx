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
} from '@gravitee/graphene-core';
import { RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState, type FormEvent } from 'react';

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
import { SelectionCards, type SelectionCardItem } from '../../../pages/detail/general/SelectionCards';
import type { PortalNavigationFolder, PortalVisibility } from '../../../types/apiDocumentation';
import { allowedChanges, type SyncState } from '../../../utils/documentationTree';
import { fetchSummaryOutcome } from '../../../utils/fetchOutcome';

type FolderContents = 'EMPTY' | 'IMPORT';

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
    const privateParent = privateParentOf(parent);
    const [contents, setContents] = useState<FolderContents>('EMPTY');
    const [sourceState, setSourceState] = useState<ExternalSourceState>({ source: null, valid: false });
    const [title, setTitle] = useState(folder?.title ?? '');
    const [isPrivate, setIsPrivate] = useState(folder ? folder.visibility === 'PRIVATE' : privateParent !== undefined);
    const [isSaving, setIsSaving] = useState(false);
    const [refusal, setRefusal] = useState<string | null>(null);

    useEffect(() => onSavingChange(isSaving), [isSaving, onSavingChange]);

    const isImport = !folder && contents === 'IMPORT';
    const titleEditable = allowedChanges(syncState).title;
    const visibility: PortalVisibility = isPrivate ? 'PRIVATE' : 'PUBLIC';
    // A title the server refuses to change goes back exactly as stored: trimmed, it would count as a rename.
    const newTitle = folder && !titleEditable ? folder.title : title.trim();
    const hasChanges = !folder || newTitle !== folder.title || visibility !== folder.visibility;
    const sourceReady = !isImport || sourceState.valid;
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
                await changeItem.mutateAsync({ navId: folder.id, changes: { title: newTitle, visibility } });
                notify.success(`Folder '${newTitle}' saved`);
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

    const submitLabel = folder
        ? isSaving
            ? 'Saving…'
            : 'Save'
        : isImport
          ? isSaving
              ? 'Importing…'
              : 'Import'
          : isSaving
            ? 'Creating…'
            : 'Create';

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
                        {syncState === 'OWN_SOURCE' ? (
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
