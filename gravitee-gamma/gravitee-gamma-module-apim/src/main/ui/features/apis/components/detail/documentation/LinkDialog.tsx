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
    FieldError,
    FieldLabel,
    Input,
} from '@gravitee/graphene-core';
import { AlertCircleIcon } from '@gravitee/graphene-core/icons';
import { useId, useState, type FormEvent } from 'react';

import { DocumentationItemDetailsFields, privateParentOf } from './DocumentationItemDetailsFields';
import { notify } from '../../../../../shared/notify';
import { extractErrorMessage } from '../../../../../shared/notify/extractErrorMessage';
import { useChangeApiDocumentationItem, useCreateApiDocumentationItem } from '../../../hooks/useApiDocumentation';
import type { PortalNavigationFolder, PortalNavigationLink, PortalVisibility } from '../../../types/apiDocumentation';

interface LinkDialogProps {
    apiId: string;
    /** The link to edit; without it, a link is added. */
    link?: PortalNavigationLink;
    /** The folder holding it, if any; a link added without one goes to the top level of the API's documentation. */
    parent?: PortalNavigationFolder;
    onClose: () => void;
    onCreated?: () => void;
}

// The portal editor's check: any address the browser can parse. The server refuses the rest, `javascript:` included.
function isValidUrl(url: string): boolean {
    try {
        new URL(url);
        return true;
    } catch {
        return false;
    }
}

export function LinkDialog({ open, ...props }: Readonly<LinkDialogProps & { open: boolean }>) {
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && props.onClose()}>
            <DialogContent style={{ maxWidth: '480px' }}>
                <LinkForm {...props} />
            </DialogContent>
        </Dialog>
    );
}

// Rendered inside the dialog content, so its state starts over from the link each time the dialog opens.
function LinkForm({ apiId, link, parent, onClose, onCreated }: Readonly<LinkDialogProps>) {
    const createItem = useCreateApiDocumentationItem(apiId);
    const changeItem = useChangeApiDocumentationItem(apiId);
    const urlId = useId();
    const privateParent = privateParentOf(parent);
    const [title, setTitle] = useState(link?.title ?? '');
    const [url, setUrl] = useState(link?.url ?? '');
    const [urlLeft, setUrlLeft] = useState(false);
    const [isPrivate, setIsPrivate] = useState(link ? link.visibility === 'PRIVATE' : privateParent !== undefined);
    const [isSaving, setIsSaving] = useState(false);
    const [refusal, setRefusal] = useState<string | null>(null);

    const visibility: PortalVisibility = isPrivate ? 'PRIVATE' : 'PUBLIC';
    const newTitle = title.trim();
    const newUrl = url.trim();
    const urlValid = isValidUrl(newUrl);
    const showUrlError = urlLeft && newUrl !== '' && !urlValid;
    const hasChanges = !link || newTitle !== link.title || newUrl !== link.url || visibility !== link.visibility;
    const canSave = newTitle !== '' && urlValid && hasChanges && !isSaving;

    function handleTitleChange(next: string) {
        setTitle(next);
        setRefusal(null);
    }

    function handleUrlChange(next: string) {
        setUrl(next);
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
            if (link) {
                await changeItem.mutateAsync({ navId: link.id, changes: { title: newTitle, url: newUrl, visibility } });
                notify.success(`Link '${newTitle}' saved`);
            } else {
                await createItem.mutateAsync({
                    type: 'LINK',
                    title: newTitle,
                    url: newUrl,
                    area: 'TOP_NAVBAR',
                    visibility,
                    parentId: parent?.id,
                });
                notify.success(`Link '${newTitle}' created`);
                onCreated?.();
            }
            onClose();
        } catch (error) {
            setRefusal(extractErrorMessage(error));
            setIsSaving(false);
        }
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>{link ? 'Edit link details' : 'Add a link'}</DialogTitle>
                <DialogDescription>
                    {link
                        ? 'Changes apply in every portal that lists this API.'
                        : parent
                          ? `The link is created unpublished, inside ${parent.title}.`
                          : 'The link is created unpublished.'}
                </DialogDescription>
            </DialogHeader>

            <form id="documentation-link-form" onSubmit={handleSubmit} className="flex flex-col gap-5">
                {refusal ? (
                    <Alert variant="destructive">
                        <AlertCircleIcon aria-hidden />
                        <AlertDescription>{refusal}</AlertDescription>
                    </Alert>
                ) : null}

                <DocumentationItemDetailsFields
                    itemType="LINK"
                    title={title}
                    onTitleChange={handleTitleChange}
                    isPrivate={isPrivate}
                    onPrivateChange={handlePrivateChange}
                    privateParent={privateParent}
                    disabled={isSaving}
                >
                    <Field orientation="vertical" className="gap-1.5" data-invalid={showUrlError || undefined}>
                        <FieldLabel htmlFor={urlId}>URL</FieldLabel>
                        <Input
                            id={urlId}
                            type="url"
                            placeholder="https://status.example.com"
                            value={url}
                            onChange={event => handleUrlChange(event.target.value)}
                            onBlur={() => setUrlLeft(true)}
                            aria-invalid={showUrlError}
                            disabled={isSaving}
                            required
                        />
                        {showUrlError ? <FieldError>Enter a valid URL, such as https://status.example.com</FieldError> : null}
                    </Field>
                </DocumentationItemDetailsFields>
            </form>

            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                    Cancel
                </Button>
                <Button type="submit" form="documentation-link-form" disabled={!canSave}>
                    {link ? (isSaving ? 'Saving…' : 'Save') : isSaving ? 'Creating…' : 'Create'}
                </Button>
            </DialogFooter>
        </>
    );
}
