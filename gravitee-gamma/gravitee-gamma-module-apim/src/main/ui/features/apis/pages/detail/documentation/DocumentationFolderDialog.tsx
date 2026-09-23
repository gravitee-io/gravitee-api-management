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
    FieldError,
    FieldLabel,
    Input,
} from '@gravitee/graphene-core';
import { useEffect, useId, useState, type FormEvent } from 'react';

import { NAME_MAX } from './documentation-shared';

export function DocumentationFolderDialog({
    open,
    folderName,
    existingNames,
    readOnly,
    isSaving,
    description = 'Group Markdown, OpenAPI, and AsyncAPI pages. Nested pages publish with this folder.',
    onClose,
    onSubmit,
}: {
    open: boolean;
    folderName?: string;
    existingNames: string[];
    readOnly: boolean;
    isSaving: boolean;
    description?: string;
    onClose: () => void;
    onSubmit: (value: { name: string }) => void;
}) {
    const nameId = useId();
    const isEdit = folderName !== undefined;
    const [name, setName] = useState('');
    const [showErrors, setShowErrors] = useState(false);

    useEffect(() => {
        if (!open) return;
        setName(folderName ?? '');
        setShowErrors(false);
    }, [folderName, open]);

    const trimmed = name.trim();
    const taken = existingNames.includes(trimmed.toLowerCase());
    const nameError =
        trimmed === '' ? 'Folder name is required.' : taken ? 'A page or folder with this name already exists here.' : null;

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        event.stopPropagation();
        if (nameError) {
            setShowErrors(true);
            return;
        }
        onSubmit({ name: trimmed });
    }

    return (
        <Dialog
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen) onClose();
            }}
        >
            <DialogContent className="max-w-md">
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>{isEdit ? 'Rename folder' : 'Add a new folder'}</DialogTitle>
                        <DialogDescription>{description}</DialogDescription>
                    </DialogHeader>

                    <Field className="py-4">
                        <FieldLabel htmlFor={nameId} required>
                            Name
                        </FieldLabel>
                        <Input
                            id={nameId}
                            value={name}
                            maxLength={NAME_MAX}
                            autoFocus
                            disabled={isSaving || readOnly}
                            aria-invalid={showErrors && nameError ? true : undefined}
                            onChange={event => setName(event.target.value)}
                        />
                        <FieldDescription>
                            {name.length}/{NAME_MAX}
                        </FieldDescription>
                        {showErrors && nameError ? <FieldError>{nameError}</FieldError> : null}
                    </Field>

                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                            Cancel
                        </Button>
                        {readOnly ? null : (
                            <Button type="submit" disabled={isSaving}>
                                {isSaving ? 'Saving…' : isEdit ? 'Save' : 'Create folder'}
                            </Button>
                        )}
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
