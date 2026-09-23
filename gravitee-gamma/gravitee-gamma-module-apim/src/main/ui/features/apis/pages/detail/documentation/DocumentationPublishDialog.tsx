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
    Checkbox,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldDescription,
    FieldLabel,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { useEffect, useId, useMemo, useState } from 'react';

import type { ApiPortalPlacement, PortalFolderOption } from '../../../types/documentation';

export function DocumentationPublishDialog({
    open,
    itemCount,
    folders,
    currentPlacement,
    isSaving,
    onClose,
    onSubmit,
}: {
    open: boolean;
    itemCount: number;
    folders: PortalFolderOption[];
    currentPlacement: ApiPortalPlacement | null;
    isSaving: boolean;
    onClose: () => void;
    onSubmit: (folderId: string) => void;
}) {
    const folderFieldId = useId();
    const moveOptionId = useId();
    const alreadyPublished = Boolean(currentPlacement?.published);
    const [wantMove, setWantMove] = useState(false);
    const [folderId, setFolderId] = useState('');

    const selectableFolders = useMemo(() => {
        if (alreadyPublished && wantMove && currentPlacement) {
            return folders.filter(folder => folder.id !== currentPlacement.folderId);
        }
        return folders;
    }, [alreadyPublished, wantMove, currentPlacement, folders]);

    useEffect(() => {
        if (!open) return;
        setWantMove(false);
        if (alreadyPublished) {
            setFolderId('');
            return;
        }
        setFolderId(currentPlacement?.folderId ?? (folders.length === 1 ? folders[0]!.id : ''));
    }, [folders, currentPlacement, open, alreadyPublished]);

    useEffect(() => {
        if (!open || !alreadyPublished || !wantMove) return;
        setFolderId(selectableFolders.length === 1 ? selectableFolders[0]!.id : '');
    }, [open, alreadyPublished, wantMove, selectableFolders]);

    const noFolders = folders.length === 0;
    const noMoveTargets = alreadyPublished && wantMove && selectableFolders.length === 0;
    const effectiveFolderId = alreadyPublished && !wantMove ? (currentPlacement?.folderId ?? '') : folderId;
    const canSubmit = !isSaving && !noFolders && !noMoveTargets && effectiveFolderId !== '';
    const showFolderSelect = !noFolders && (!alreadyPublished || wantMove);

    return (
        <Dialog
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen) onClose();
            }}
        >
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>Publish to Next Gen Portal</DialogTitle>
                    <DialogDescription>
                        {alreadyPublished
                            ? itemCount === 1
                                ? 'Publish the selected documentation under the existing Navigation folder, or move the whole API documentation elsewhere.'
                                : `Publish the ${itemCount} selected items under the existing Navigation folder, or move the whole API documentation elsewhere.`
                            : itemCount === 1
                              ? 'Choose the Navigation folder for this API and the selected documentation.'
                              : `Choose the Navigation folder for this API and the ${itemCount} selected items.`}
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-4 py-2">
                    {noFolders ? (
                        <Alert>
                            <AlertDescription>
                                Create a folder in Next Gen Portal Settings → Navigation before you can publish this API and its
                                documentation.
                            </AlertDescription>
                        </Alert>
                    ) : null}

                    {alreadyPublished && currentPlacement ? (
                        <>
                            <Alert>
                                <AlertDescription>
                                    This API is already published in “{currentPlacement.folderPath}”.
                                </AlertDescription>
                            </Alert>

                            <label htmlFor={moveOptionId} className="flex items-start gap-3 cursor-pointer">
                                <Checkbox
                                    id={moveOptionId}
                                    checked={wantMove}
                                    disabled={isSaving || noFolders}
                                    onCheckedChange={checked => {
                                        const next = checked === true;
                                        setWantMove(next);
                                        setFolderId('');
                                    }}
                                    className="mt-0.5"
                                />
                                <span className="text-sm leading-snug">
                                    Publish the whole API documentation to another folder
                                </span>
                            </label>

                            {wantMove && noMoveTargets ? (
                                <Alert>
                                    <AlertDescription>
                                        There are no other Navigation folders available. Create another folder in Next Gen Portal
                                        Settings → Navigation to move this API.
                                    </AlertDescription>
                                </Alert>
                            ) : null}
                        </>
                    ) : null}

                    {!alreadyPublished && currentPlacement ? (
                        <Alert>
                            <AlertDescription>
                                This API is currently under “{currentPlacement.folderPath}”. Choose a folder to publish it and its
                                documentation.
                            </AlertDescription>
                        </Alert>
                    ) : null}

                    {showFolderSelect && !noMoveTargets ? (
                        <Field>
                            <FieldLabel htmlFor={folderFieldId}>Navigation folder</FieldLabel>
                            <Select value={folderId || undefined} disabled={isSaving} onValueChange={setFolderId}>
                                <SelectTrigger id={folderFieldId} className="w-full">
                                    <SelectValue placeholder="Select a folder" />
                                </SelectTrigger>
                                <SelectContent
                                    position="popper"
                                    align="start"
                                    className="max-h-60 overflow-y-auto"
                                    style={{ width: 'var(--radix-select-trigger-width)', minWidth: 'unset' }}
                                >
                                    {selectableFolders.map(folder => (
                                        <SelectItem key={folder.id} value={folder.id}>
                                            {folder.path}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <FieldDescription>
                                {alreadyPublished && wantMove
                                    ? 'The whole API and its documentation will be published under this folder.'
                                    : 'The API and selected documentation are published under this folder. An API can live in only one folder.'}
                            </FieldDescription>
                        </Field>
                    ) : null}
                </div>

                <DialogFooter>
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={!canSubmit} onClick={() => onSubmit(effectiveFolderId)}>
                        {isSaving ? 'Publishing…' : 'Publish'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
