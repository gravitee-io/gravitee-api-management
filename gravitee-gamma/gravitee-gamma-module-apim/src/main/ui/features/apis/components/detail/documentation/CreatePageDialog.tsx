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
    Field,
    FieldDescription,
    FieldLabel,
    FieldLegend,
    FieldSet,
    Input,
    Label,
    Switch,
} from '@gravitee/graphene-core';
import { useState, type FormEvent } from 'react';

import { type ImportedFile, ImportFileField } from './ImportFileField';
import { type NewDocumentationPage, useCreateDocumentationPage } from '../../../hooks/useCreateDocumentationPage';
import { SelectionCards, type SelectionCardItem } from '../../../pages/detail/general/SelectionCards';
import type { PortalNavigationFolder, PortalPageContentType } from '../../../types/apiDocumentation';
import { titleFromFileName } from '../../../utils/documentationFile';
import { PAGE_CONTENT_TYPE_LABELS } from '../../../utils/pageContentType';

type ContentSource = 'FILL' | 'IMPORT';

const CONTENT_SOURCES: readonly SelectionCardItem<ContentSource>[] = [
    { id: 'FILL', label: 'Fill in content', description: 'Write the page in the editor' },
    { id: 'IMPORT', label: 'Import from file', description: '.md, .yaml, .yml or .json' },
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
            <DialogContent style={{ maxWidth: '560px' }}>
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
    createPage: (page: NewDocumentationPage) => Promise<string | null>;
    isCreating: boolean;
    onClose: () => void;
    onCreated: (pageId: string) => void;
}>) {
    const [title, setTitle] = useState('');
    // The title last taken from a file name, replaced by the next file's as long as the user has not changed it.
    const [titleFromFile, setTitleFromFile] = useState('');
    // The server refuses a public page inside a folder that requires authentication.
    const privateParent = parent?.visibility === 'PRIVATE' ? parent : undefined;
    const [isPrivate, setIsPrivate] = useState(privateParent !== undefined);
    const [source, setSource] = useState<ContentSource>('FILL');
    const [pageType, setPageType] = useState<PortalPageContentType>('GRAVITEE_MARKDOWN');
    const [importedFile, setImportedFile] = useState<ImportedFile | null>(null);

    const imported = source === 'IMPORT' ? importedFile : null;
    const canCreate = title.trim() !== '' && (source === 'FILL' || imported !== null) && !isCreating;

    function handleImportedFile(file: ImportedFile | null) {
        setImportedFile(file);
        if (!file || (title.trim() !== '' && title !== titleFromFile)) return;
        const fileTitle = titleFromFileName(file.file.name);
        setTitle(fileTitle);
        setTitleFromFile(fileTitle);
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canCreate) return;
        const pageId = await createPage({
            title: title.trim(),
            visibility: isPrivate ? 'PRIVATE' : 'PUBLIC',
            contentType: imported?.contentType ?? pageType,
            content: imported?.content ?? '',
            parentId: parent?.id,
        });
        if (pageId) onCreated(pageId);
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>Add a page</DialogTitle>
                <DialogDescription>
                    {parent ? `The page is created unpublished, inside ${parent.title}.` : 'The page is created unpublished.'}
                </DialogDescription>
            </DialogHeader>

            <div className="flex flex-col gap-5">
                <form id="create-documentation-page-form" onSubmit={handleSubmit} className="flex flex-col gap-5">
                    <Field orientation="vertical" className="gap-1.5">
                        <FieldLabel htmlFor="documentation-page-title">Title</FieldLabel>
                        <Input
                            id="documentation-page-title"
                            value={title}
                            onChange={event => setTitle(event.target.value)}
                            disabled={isCreating}
                            required
                        />
                    </Field>

                    <FieldSet className="gap-1.5">
                        <FieldLegend variant="label">Access</FieldLegend>
                        <div className="flex items-center gap-2">
                            <Switch
                                id="documentation-page-private"
                                checked={isPrivate}
                                onCheckedChange={setIsPrivate}
                                disabled={isCreating || privateParent !== undefined}
                            />
                            <Label htmlFor="documentation-page-private" className="font-normal">
                                Authentication is required to view this page
                            </Label>
                        </div>
                        {privateParent ? (
                            <FieldDescription>{privateParent.title} requires authentication, so this page does too.</FieldDescription>
                        ) : null}
                    </FieldSet>

                    <FieldSet className="gap-1.5">
                        <FieldLegend variant="label">Content</FieldLegend>
                        <SelectionCards options={CONTENT_SOURCES} activeId={source} onChange={setSource} ariaLabel="Content" />
                    </FieldSet>

                    {source === 'FILL' ? (
                        <FieldSet className="gap-1.5">
                            <FieldLegend variant="label">Page type</FieldLegend>
                            <SelectionCards options={PAGE_TYPES} activeId={pageType} onChange={setPageType} ariaLabel="Page type" />
                        </FieldSet>
                    ) : null}
                </form>
                {/* Outside the form: Graphene's FileUpload remove button has no type, so it would submit it. */}
                {source === 'IMPORT' ? <ImportFileField value={importedFile} onChange={handleImportedFile} disabled={isCreating} /> : null}
            </div>

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
