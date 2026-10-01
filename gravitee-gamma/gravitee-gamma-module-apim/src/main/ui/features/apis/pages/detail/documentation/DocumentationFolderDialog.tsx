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
import { DocumentationVisibilityField } from './DocumentationVisibilityField';
import type { Visibility } from '../../../types/documentation';

export function DocumentationFolderDialog({
    open,
    folderName,
    visibility: initialVisibility = 'PUBLIC',
    parentForcesPrivate = false,
    existingNames,
    readOnly,
    isSaving,
    description = 'Group Markdown, OpenAPI, and AsyncAPI pages. Nested pages publish with this folder.',
    onClose,
    onSubmit,
}: {
    open: boolean;
    folderName?: string;
    visibility?: Visibility;
    /** When the parent folder is PRIVATE, this folder must stay PRIVATE. */
    parentForcesPrivate?: boolean;
    existingNames: string[];
    readOnly: boolean;
    isSaving: boolean;
    description?: string;
    onClose: () => void;
    onSubmit: (value: { name: string; visibility: Visibility }) => void;
}) {
    const nameId = useId();
    const isEdit = folderName !== undefined;
    const [name, setName] = useState('');
    const [visibility, setVisibility] = useState<Visibility>('PUBLIC');
    const [showErrors, setShowErrors] = useState(false);

    useEffect(() => {
        if (!open) return;
        setName(folderName ?? '');
        setVisibility(parentForcesPrivate ? 'PRIVATE' : (initialVisibility ?? 'PUBLIC'));
        setShowErrors(false);
    }, [folderName, initialVisibility, open, parentForcesPrivate]);

    useEffect(() => {
        if (parentForcesPrivate) setVisibility('PRIVATE');
    }, [parentForcesPrivate]);

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
        onSubmit({
            name: trimmed,
            visibility: parentForcesPrivate ? 'PRIVATE' : visibility,
        });
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
                        <DialogTitle>{isEdit ? 'Edit folder' : 'Add a new folder'}</DialogTitle>
                        <DialogDescription>{description}</DialogDescription>
                    </DialogHeader>

                    <div className="space-y-4 py-4">
                        <Field>
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

                        <DocumentationVisibilityField
                            kind="folder"
                            value={visibility}
                            parentForcesPrivate={parentForcesPrivate}
                            disabled={isSaving || readOnly}
                            onChange={setVisibility}
                        />
                    </div>

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
