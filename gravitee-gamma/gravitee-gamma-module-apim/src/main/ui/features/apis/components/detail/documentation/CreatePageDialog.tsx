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
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    FieldLegend,
    FieldSet,
} from '@gravitee/graphene-core';
import { useState, type FormEvent } from 'react';

import { RefusalAlert, ScrollableDialogBody, WITHIN_VIEWPORT } from './dialogLayout';
import { DocumentationItemDetailsFields, privateParentOf } from './DocumentationItemDetailsFields';
import { ExternalSourceConfig, type ExternalSourceState } from './ExternalSourceConfig';
import { type ImportedFile, ImportFileField } from './ImportFileField';
import { type CreatePageResult, type NewDocumentationPage, useCreateDocumentationPage } from '../../../hooks/useCreateDocumentationPage';
import { SelectionCards, type SelectionCardItem } from '../../../pages/detail/general/SelectionCards';
import type { PortalNavigationFolder, PortalPageContentType } from '../../../types/apiDocumentation';
import { titleFromFileName } from '../../../utils/documentationFile';
import { PAGE_CONTENT_TYPE_LABELS } from '../../../utils/pageContentType';

type ContentSource = 'FILL' | 'IMPORT' | 'EXTERNAL';

const CONTENT_SOURCES: readonly SelectionCardItem<ContentSource>[] = [
    { id: 'FILL', label: 'Fill in content', description: 'Write the page in the editor' },
    { id: 'IMPORT', label: 'Import from file', description: '.md, .yaml, .yml or .json' },
    { id: 'EXTERNAL', label: 'Link to external source', description: 'Fetched from a repository' },
];

const PAGE_TYPES: readonly SelectionCardItem<PortalPageContentType>[] = (['GRAVITEE_MARKDOWN', 'OPENAPI', 'ASYNCAPI'] as const).map(
    type => ({ id: type, label: PAGE_CONTENT_TYPE_LABELS[type] }),
);

export function CreatePageDialog({
    open,
    apiId,
    parent,
    onClose,
    onCreated,
}: Readonly<{
    open: boolean;
    apiId: string;
    /** A folder owned by the API; without it, the page is created at the top level of the API's documentation. */
    parent?: PortalNavigationFolder;
    onClose: () => void;
    /** Called once the page exists, even when an imported file could not be saved as its content. */
    onCreated: (pageId: string) => void;
}>) {
    const { createPage, isCreating } = useCreateDocumentationPage(apiId);

    return (
        // Escape, a click outside and the close button would leave the creation running, and then open the page anyway.
        <Dialog open={open} onOpenChange={isOpen => !isOpen && !isCreating && onClose()}>
            <DialogContent style={{ maxWidth: '560px', ...WITHIN_VIEWPORT }}>
                <CreatePageForm parent={parent} createPage={createPage} isCreating={isCreating} onClose={onClose} onCreated={onCreated} />
            </DialogContent>
        </Dialog>
    );
}

// Rendered inside the dialog content, so its state starts over each time the dialog opens.
function CreatePageForm({
    parent,
    createPage,
    isCreating,
    onClose,
    onCreated,
}: Readonly<{
    parent?: PortalNavigationFolder;
    createPage: (page: NewDocumentationPage) => Promise<CreatePageResult>;
    isCreating: boolean;
    onClose: () => void;
    onCreated: (pageId: string) => void;
}>) {
    const [title, setTitle] = useState('');
    // The title last taken from a file name, replaced by the next file's as long as the user has not changed it.
    const [titleFromFile, setTitleFromFile] = useState('');
    const privateParent = privateParentOf(parent);
    const [isPrivate, setIsPrivate] = useState(privateParent !== undefined);
    const [source, setSource] = useState<ContentSource>('FILL');
    const [pageType, setPageType] = useState<PortalPageContentType>('GRAVITEE_MARKDOWN');
    const [importedFile, setImportedFile] = useState<ImportedFile | null>(null);
    const [externalSource, setExternalSource] = useState<ExternalSourceState>({ source: null, valid: false });
    const [refusal, setRefusal] = useState<string | null>(null);

    const imported = source === 'IMPORT' ? importedFile : null;
    const contentReady = source === 'FILL' || imported !== null || (source === 'EXTERNAL' && externalSource.valid);
    const canCreate = title.trim() !== '' && contentReady && !isCreating;

    // A refusal is about what was entered: it no longer applies once that changes.
    function changing<T>(set: (value: T) => void) {
        return (value: T) => {
            set(value);
            setRefusal(null);
        };
    }

    function handleImportedFile(file: ImportedFile | null) {
        setImportedFile(file);
        setRefusal(null);
        if (!file || (title.trim() !== '' && title !== titleFromFile)) return;
        const fileTitle = titleFromFileName(file.file.name);
        setTitle(fileTitle);
        setTitleFromFile(fileTitle);
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canCreate) return;
        // Cleared first, so that the same refusal given again is shown, and brought into view, again.
        setRefusal(null);
        const result = await createPage({
            title: title.trim(),
            visibility: isPrivate ? 'PRIVATE' : 'PUBLIC',
            contentType: imported?.contentType ?? pageType,
            content: imported?.content ?? '',
            parentId: parent?.id,
            source: source === 'EXTERNAL' ? (externalSource.source ?? undefined) : undefined,
        });
        if ('refusal' in result) {
            setRefusal(result.refusal);
        } else {
            onCreated(result.pageId);
        }
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>Add a page</DialogTitle>
                <DialogDescription>
                    {parent ? `The page is created unpublished, inside ${parent.title}.` : 'The page is created unpublished.'}
                </DialogDescription>
            </DialogHeader>

            <ScrollableDialogBody>
                <div className="flex flex-col gap-5">
                    <RefusalAlert refusal={refusal} />
                    <form id="create-documentation-page-form" onSubmit={handleSubmit} className="flex flex-col gap-5">
                        <DocumentationItemDetailsFields
                            title={title}
                            onTitleChange={changing(setTitle)}
                            isPrivate={isPrivate}
                            onPrivateChange={changing(setIsPrivate)}
                            privateParent={privateParent}
                            disabled={isCreating}
                        />

                        <FieldSet className="gap-1.5">
                            <FieldLegend variant="label">Content</FieldLegend>
                            <SelectionCards
                                options={CONTENT_SOURCES}
                                activeId={source}
                                onChange={changing(setSource)}
                                ariaLabel="Content"
                            />
                        </FieldSet>

                        {source === 'IMPORT' ? null : (
                            <FieldSet className="gap-1.5">
                                <FieldLegend variant="label">Page type</FieldLegend>
                                <SelectionCards
                                    options={PAGE_TYPES}
                                    activeId={pageType}
                                    onChange={changing(setPageType)}
                                    ariaLabel="Page type"
                                />
                            </FieldSet>
                        )}
                    </form>
                    {/* Outside the form: Graphene's FileUpload remove button has no type, so it would submit it. */}
                    {source === 'IMPORT' ? (
                        <ImportFileField value={importedFile} onChange={handleImportedFile} disabled={isCreating} />
                    ) : null}
                    {/* Outside the form too: a plugin's settings form may hold buttons of its own. */}
                    {source === 'EXTERNAL' ? (
                        <div className="flex flex-col gap-3">
                            <ExternalSourceConfig onChange={changing(setExternalSource)} disabled={isCreating} />
                            <p className="text-xs text-muted-foreground">
                                The content is fetched when you create the page, and stays read only here.
                            </p>
                        </div>
                    ) : null}
                </div>
            </ScrollableDialogBody>

            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose} disabled={isCreating}>
                    Cancel
                </Button>
                <Button type="submit" form="create-documentation-page-form" disabled={!canCreate}>
                    {isCreating ? 'Creating…' : 'Create'}
                </Button>
            </DialogFooter>
        </>
    );
}
