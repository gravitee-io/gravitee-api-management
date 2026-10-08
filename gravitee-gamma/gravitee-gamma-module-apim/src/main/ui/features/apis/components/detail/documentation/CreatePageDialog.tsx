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
    Field,
    FieldDescription,
    FieldError,
    FieldLabel,
    FieldLegend,
    FieldSet,
    FileUploadInput,
    Input,
    Label,
    Switch,
    type FileRejection,
} from '@gravitee/graphene-core';
import { useState, type FormEvent } from 'react';

import { notify } from '../../../../../shared/notify';
import { extractErrorMessage } from '../../../../../shared/notify/extractErrorMessage';
import { useCreateApiDocumentationItem, useSaveApiDocumentationPageContent } from '../../../hooks/useApiDocumentation';
import { SelectionCards, type SelectionCardItem } from '../../../pages/detail/general/SelectionCards';
import type { PortalNavigationFolder, PortalPageContentType } from '../../../types/apiDocumentation';
import {
    DOCUMENTATION_FILE_ACCEPT,
    detectPageContentType,
    MAX_DOCUMENTATION_FILE_SIZE_MB,
    titleFromFileName,
} from '../../../utils/documentationFile';
import { PAGE_CONTENT_TYPE_LABELS } from '../../../utils/pageContentType';

type ContentSource = 'FILL' | 'IMPORT';

interface ImportedFile {
    name: string;
    content: string;
    contentType: PortalPageContentType;
}

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
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent style={{ maxWidth: '560px' }}>
                <CreatePageForm apiId={apiId} parent={parent} onClose={onClose} onCreated={onCreated} />
            </DialogContent>
        </Dialog>
    );
}

// Rendered inside the dialog content, so its state starts over each time the dialog opens.
function CreatePageForm({
    apiId,
    parent,
    onClose,
    onCreated,
}: Readonly<{ apiId: string; parent?: PortalNavigationFolder; onClose: () => void; onCreated: (pageId: string) => void }>) {
    const createItem = useCreateApiDocumentationItem(apiId);
    const saveContent = useSaveApiDocumentationPageContent(apiId);

    const [title, setTitle] = useState('');
    // The server refuses a public page inside a folder that requires authentication.
    const privateParent = parent?.visibility === 'PRIVATE' ? parent : undefined;
    const [isPrivate, setIsPrivate] = useState(privateParent !== undefined);
    const [source, setSource] = useState<ContentSource>('FILL');
    const [pageType, setPageType] = useState<PortalPageContentType>('GRAVITEE_MARKDOWN');
    const [importedFile, setImportedFile] = useState<ImportedFile | null>(null);
    const [fileError, setFileError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const canCreate = title.trim() !== '' && (source === 'FILL' || importedFile !== null) && !isSubmitting;

    function refuseFile(message: string) {
        setImportedFile(null);
        setFileError(message);
    }

    async function handleFilesAccepted([file]: File[]) {
        if (!file) return;
        let content: string;
        try {
            content = await file.text();
        } catch {
            refuseFile(`'${file.name}' could not be read.`);
            return;
        }
        const contentType = detectPageContentType(file.name, content);
        if (!contentType) {
            refuseFile(`Cannot tell whether '${file.name}' is OpenAPI or AsyncAPI: it needs a root openapi, swagger or asyncapi property.`);
            return;
        }
        setFileError(null);
        setImportedFile({ name: file.name, content, contentType });
        setTitle(current => (current.trim() ? current : titleFromFileName(file.name)));
    }

    function handleFilesRejected([rejection]: FileRejection[]) {
        if (!rejection) return;
        const tooLarge = rejection.errors.some(error => error.code === 'file-too-large');
        refuseFile(
            tooLarge
                ? `'${rejection.file.name}' is larger than ${MAX_DOCUMENTATION_FILE_SIZE_MB} MB.`
                : 'Only .md, .yaml, .yml and .json files can be imported.',
        );
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canCreate) return;
        const pageTitle = title.trim();
        const imported = source === 'IMPORT' ? importedFile : null;
        setIsSubmitting(true);

        let pageId: string;
        try {
            const page = await createItem.mutateAsync({
                type: 'PAGE',
                title: pageTitle,
                contentType: imported?.contentType ?? pageType,
                area: 'TOP_NAVBAR',
                visibility: isPrivate ? 'PRIVATE' : 'PUBLIC',
                parentId: parent?.id,
            });
            pageId = page.id;
        } catch (error) {
            notify.error(error, 'Failed to create the page');
            setIsSubmitting(false);
            return;
        }

        // A page is created empty: its text can only be written through its content, once the page exists.
        if (imported && imported.content !== '') {
            try {
                await saveContent.mutateAsync({ navId: pageId, content: imported.content });
            } catch (error) {
                notify.warning(`Page '${pageTitle}' was created, but its content could not be saved: ${extractErrorMessage(error)}`);
                onCreated(pageId);
                return;
            }
        }

        notify.success(`Page '${pageTitle}' created`);
        onCreated(pageId);
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>Add a page</DialogTitle>
                <DialogDescription>
                    {parent ? `The page is created unpublished, inside ${parent.title}.` : 'The page is created unpublished.'}
                </DialogDescription>
            </DialogHeader>

            <form id="create-documentation-page-form" onSubmit={handleSubmit} className="flex flex-col gap-5">
                <Field orientation="vertical" className="gap-1.5">
                    <FieldLabel htmlFor="documentation-page-title">Title</FieldLabel>
                    <Input
                        id="documentation-page-title"
                        value={title}
                        onChange={event => setTitle(event.target.value)}
                        disabled={isSubmitting}
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
                            disabled={isSubmitting || privateParent !== undefined}
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
                ) : (
                    <>
                        <Field orientation="vertical" className="gap-1.5">
                            <FileUploadInput
                                label={importedFile?.name ?? 'Choose a file to import'}
                                accept={DOCUMENTATION_FILE_ACCEPT}
                                maxFileSize={MAX_DOCUMENTATION_FILE_SIZE_MB * 1024 * 1024}
                                invalid={fileError !== null}
                                disabled={isSubmitting}
                                onFilesAccepted={files => void handleFilesAccepted(files)}
                                onFilesRejected={handleFilesRejected}
                            />
                            {fileError && <FieldError>{fileError}</FieldError>}
                        </Field>

                        {importedFile && (
                            <Alert>
                                <AlertDescription>
                                    {importedFile.name} will be imported as {PAGE_CONTENT_TYPE_LABELS[importedFile.contentType]}.
                                </AlertDescription>
                            </Alert>
                        )}
                    </>
                )}
            </form>

            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
                    Cancel
                </Button>
                <Button type="submit" form="create-documentation-page-form" disabled={!canCreate}>
                    {isSubmitting ? 'Creating…' : 'Create'}
                </Button>
            </DialogFooter>
        </>
    );
}
