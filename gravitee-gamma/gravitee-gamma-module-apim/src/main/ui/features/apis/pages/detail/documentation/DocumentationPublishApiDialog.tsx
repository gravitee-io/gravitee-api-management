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
    Field,
    FieldDescription,
    FieldLabel,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { useEffect, useId, useMemo, useState } from 'react';

import type { ApiPortalPlacement, DocumentationPage, PortalFolderOption } from '../../../types/documentation';
import { typeLabel } from './documentation-shared';

export function DocumentationPublishApiDialog({
    open,
    folders,
    currentPlacement,
    pages,
    isSaving,
    onClose,
    onSubmit,
}: {
    open: boolean;
    folders: PortalFolderOption[];
    currentPlacement: ApiPortalPlacement | null;
    /** Root documentation items available for selection. Empty means create Overview. */
    pages: DocumentationPage[];
    isSaving: boolean;
    onClose: () => void;
    onSubmit: (folderId: string, pageIds: string[]) => void;
}) {
    const folderFieldId = useId();
    const isEmpty = pages.length === 0;
    const alreadyPublished = Boolean(currentPlacement?.published);
    const [folderId, setFolderId] = useState('');
    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

    const selectableFolders = useMemo(() => folders, [folders]);
    const selectablePageIds = useMemo(
        () => pages.map(page => page.id).filter((id): id is string => Boolean(id)),
        [pages],
    );
    const allSelected = selectablePageIds.length > 0 && selectablePageIds.every(id => selectedIds.has(id));
    const someSelected = selectedIds.size > 0 && !allSelected;

    useEffect(() => {
        if (!open) return;
        setFolderId(currentPlacement?.folderId ?? (folders.length === 1 ? folders[0]!.id : ''));
        setSelectedIds(
            new Set(
                pages
                    .filter(page => page.id && !page.published)
                    .map(page => page.id!)
                    .filter(Boolean),
            ),
        );
        if (pages.length > 0 && pages.every(page => page.published)) {
            setSelectedIds(new Set(pages.map(page => page.id!).filter(Boolean)));
        }
    }, [open, folders, currentPlacement, pages]);

    const noFolders = folders.length === 0;
    const canSubmit =
        !isSaving && !noFolders && folderId !== '' && (isEmpty || selectedIds.size > 0);

    function togglePage(id: string) {
        setSelectedIds(current => {
            const next = new Set(current);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    }

    function toggleSelectAll() {
        if (allSelected) {
            setSelectedIds(new Set());
            return;
        }
        setSelectedIds(new Set(selectablePageIds));
    }

    return (
        <Sheet
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen) onClose();
            }}
        >
            <SheetContent side="right" className="flex h-full w-full flex-col overflow-hidden sm:max-w-md" showCloseButton>
                <SheetHeader>
                    <SheetTitle>Publish API to Next Gen Portal</SheetTitle>
                    <SheetDescription>
                        {isEmpty
                            ? 'Publish this API under a Navigation folder. A default Overview page will be created.'
                            : 'Choose which documentation to publish with this API, and the Navigation folder to place it under.'}
                    </SheetDescription>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto px-4 py-2">
                    {noFolders ? (
                        <Alert>
                            <AlertDescription>
                                Create a folder in Next Gen Portal Settings → Navigation before you can publish this API.
                            </AlertDescription>
                        </Alert>
                    ) : null}

                    {isEmpty ? (
                        <Alert>
                            <AlertDescription>
                                No documentation exists for this API yet. A default Overview page will be created and published
                                under the selected Navigation folder.
                            </AlertDescription>
                        </Alert>
                    ) : null}

                    {alreadyPublished && currentPlacement ? (
                        <Alert>
                            <AlertDescription>
                                This API is already published in “{currentPlacement.folderPath}”. Choosing another folder moves
                                the API and its documentation there.
                            </AlertDescription>
                        </Alert>
                    ) : null}

                    {!isEmpty ? (
                        <Field className="min-h-0">
                            <div className="flex items-center justify-between gap-3">
                                <FieldLabel>Documentation to publish</FieldLabel>
                                <label className="flex items-center gap-2 cursor-pointer text-sm text-muted-foreground">
                                    <Checkbox
                                        checked={allSelected ? true : someSelected ? 'indeterminate' : false}
                                        disabled={isSaving || selectablePageIds.length === 0}
                                        onCheckedChange={() => toggleSelectAll()}
                                        aria-label={allSelected ? 'Deselect all documentation' : 'Select all documentation'}
                                    />
                                    Select all
                                </label>
                            </div>
                            <div className="mt-2 max-h-60 min-h-0 space-y-2 overflow-y-auto overscroll-contain rounded-md border p-3">
                                {pages.map(page => {
                                    if (!page.id) return null;
                                    const checked = selectedIds.has(page.id);
                                    return (
                                        <label key={page.id} className="flex items-start gap-3 cursor-pointer">
                                            <Checkbox
                                                checked={checked}
                                                disabled={isSaving}
                                                onCheckedChange={() => togglePage(page.id!)}
                                                className="mt-0.5"
                                                aria-label={`Select ${page.name ?? 'Untitled'}`}
                                            />
                                            <span className="text-sm leading-snug">
                                                <span className="font-medium">{page.name ?? 'Untitled'}</span>
                                                <span className="text-muted-foreground"> · {typeLabel(page.type)}</span>
                                                {page.published ? (
                                                    <span className="text-muted-foreground"> · already published</span>
                                                ) : null}
                                            </span>
                                        </label>
                                    );
                                })}
                            </div>
                            <FieldDescription>Nested pages inside a folder publish with that folder.</FieldDescription>
                        </Field>
                    ) : null}

                    {!noFolders ? (
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
                                The API and documentation are published under this folder. An API can live in only one folder.
                            </FieldDescription>
                        </Field>
                    ) : null}
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button
                        type="button"
                        disabled={!canSubmit}
                        onClick={() => onSubmit(folderId, isEmpty ? [] : [...selectedIds])}
                    >
                        {isSaving ? 'Publishing…' : 'Publish'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
