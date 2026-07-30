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
    Field,
    FieldLabel,
    Input,
    ScrollArea,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { InfoIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';

import { ShardingTagGroupsField } from './ShardingTagGroupsField';
import { ApimApiError } from '../../../shared/api/apimClient';
import { extractErrorMessage } from '../../../shared/notify/extractErrorMessage';
import type { NewOrgTagPayload, OrgGroup, ShardingTagRow, UpdateOrgTagPayload } from '../types/entrypoint';
import {
    findDuplicateTagKey,
    findDuplicateTagName,
    getTagNameError,
    isTagKeyValid,
    isTagNameValid,
    slugifyTagKeyBase,
    slugifyTagKeyFinal,
    TAG_KEY_MAX,
    TAG_NAME_MAX,
} from '../utils/shardingTagFormValidation';

interface ShardingTagForm {
    name: string;
    key: string;
    description: string;
    restrictedGroupIds: string[];
}

const EMPTY_FORM: ShardingTagForm = {
    name: '',
    key: '',
    description: '',
    restrictedGroupIds: [],
};

function tagToForm(tag: ShardingTagRow): ShardingTagForm {
    return {
        name: tag.name,
        key: tag.key,
        description: tag.description,
        restrictedGroupIds: [...tag.restrictedGroupIds],
    };
}

type ShardingTagFormSheetBaseProps = {
    open: boolean;
    tag?: ShardingTagRow | null;
    existingTags: ShardingTagRow[];
    groups: OrgGroup[];
    isGroupsLoading?: boolean;
    onClose: () => void;
    isSaving: boolean;
};

type ShardingTagFormSheetProps =
    | (ShardingTagFormSheetBaseProps & {
          mode: 'create';
          onSubmit: (data: NewOrgTagPayload) => Promise<void>;
      })
    | (ShardingTagFormSheetBaseProps & {
          mode: 'edit';
          onSubmit: (data: UpdateOrgTagPayload) => Promise<void>;
      });

export function ShardingTagFormSheet({
    open,
    mode,
    tag = null,
    existingTags,
    groups,
    isGroupsLoading = false,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<ShardingTagFormSheetProps>) {
    const [form, setForm] = useState<ShardingTagForm>(EMPTY_FORM);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const [duplicateNameError, setDuplicateNameError] = useState<string | null>(null);
    const [duplicateKeyError, setDuplicateKeyError] = useState<string | null>(null);
    const seededTagIdRef = useRef<string | null>(null);

    useEffect(() => {
        if (!open) {
            seededTagIdRef.current = null;
            return;
        }

        if (mode === 'create') {
            setForm(EMPTY_FORM);
            setSubmitError(null);
            setDuplicateNameError(null);
            setDuplicateKeyError(null);
            seededTagIdRef.current = null;
            return;
        }

        if (!tag) {
            if (seededTagIdRef.current === null) {
                setForm(EMPTY_FORM);
                setSubmitError(null);
                setDuplicateNameError(null);
                setDuplicateKeyError(null);
            }
            return;
        }

        if (seededTagIdRef.current === tag.id) return;
        seededTagIdRef.current = tag.id;
        setForm(tagToForm(tag));
        setSubmitError(null);
        setDuplicateNameError(null);
        setDuplicateKeyError(null);
    }, [open, mode, tag]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    const nameError = getTagNameError(form.name);
    const isNameValid = isTagNameValid(form.name);
    const isKeyValid = isTagKeyValid(form.key);
    const isValid =
        (mode === 'create' || Boolean(tag)) &&
        isNameValid &&
        nameError === null &&
        isKeyValid &&
        duplicateNameError === null &&
        duplicateKeyError === null;

    function clearDuplicateErrors() {
        if (duplicateNameError) setDuplicateNameError(null);
        if (duplicateKeyError) setDuplicateKeyError(null);
    }

    function handleKeyChange(value: string) {
        clearDuplicateErrors();
        setForm(prev => ({ ...prev, key: slugifyTagKeyBase(value) }));
    }

    function handleKeyBlur() {
        setForm(prev => {
            const finalized = slugifyTagKeyFinal(prev.key);
            return finalized === prev.key ? prev : { ...prev, key: finalized };
        });
    }

    function validateUniqueness(name: string, key: string): { field: 'name' | 'key'; message: string } | null {
        const excludeId = mode === 'edit' ? tag?.id : undefined;
        if (findDuplicateTagName(existingTags, name, excludeId)) {
            return { field: 'name', message: 'A sharding tag with this name already exists.' };
        }
        if (mode === 'create' && findDuplicateTagKey(existingTags, key, excludeId)) {
            return { field: 'key', message: 'The tag key already exists.' };
        }
        return null;
    }

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!isValid || isSaving) return;

        const name = form.name.trim();
        const key = mode === 'create' ? slugifyTagKeyFinal(form.key) : form.key;
        const uniquenessError = validateUniqueness(name, key);
        if (uniquenessError) {
            if (uniquenessError.field === 'name') {
                setDuplicateNameError(uniquenessError.message);
            } else {
                setDuplicateKeyError(uniquenessError.message);
            }
            return;
        }

        try {
            setSubmitError(null);
            setDuplicateNameError(null);
            setDuplicateKeyError(null);
            if (mode === 'create') {
                await (onSubmit as (data: NewOrgTagPayload) => Promise<void>)({
                    name,
                    key,
                    description: form.description.trim() || undefined,
                    restricted_groups: form.restrictedGroupIds.length > 0 ? form.restrictedGroupIds : undefined,
                });
            } else {
                await (onSubmit as (data: UpdateOrgTagPayload) => Promise<void>)({
                    name,
                    description: form.description.trim(),
                    restricted_groups: form.restrictedGroupIds,
                });
            }
        } catch (error) {
            const fallback = mode === 'create' ? 'Failed to create sharding tag' : 'Failed to update sharding tag';
            const message = extractErrorMessage(error, fallback);
            // DuplicateTagKeyException is returned as HTTP 400 from management REST.
            if (error instanceof ApimApiError && error.status === 400) {
                setDuplicateKeyError(message);
            } else {
                setSubmitError(message);
            }
        }
    }

    const title = mode === 'create' ? 'Add Sharding Tag' : 'Edit Sharding Tag';
    const submitLabel = mode === 'create' ? (isSaving ? 'Adding...' : 'Add Tag') : isSaving ? 'Saving...' : 'Save';
    const nameFieldError = nameError ?? duplicateNameError;

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" showCloseButton={false} className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader className="flex-row items-start justify-between gap-3 space-y-0">
                    <div className="space-y-1.5 text-left">
                        <SheetTitle>{title}</SheetTitle>
                        <SheetDescription>
                            Add the sharding tag&apos;s key to the API Gateway configuration file to manage API deployments.
                        </SheetDescription>
                    </div>
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-8 shrink-0"
                        aria-label="Close"
                        onClick={() => handleOpenChange(false)}
                        disabled={isSaving}
                    >
                        <XIcon className="size-4" aria-hidden />
                    </Button>
                </SheetHeader>

                <ScrollArea className="min-h-0 flex-1">
                    <form id="sharding-tag-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-4 py-4">
                        <Alert>
                            <InfoIcon className="size-4" aria-hidden />
                            <AlertDescription>
                                A sharding tag can be limited to selected groups so only members of those groups can use it.
                            </AlertDescription>
                        </Alert>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="sharding-tag-name">
                                Name{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <Input
                                id="sharding-tag-name"
                                value={form.name}
                                onChange={e => {
                                    clearDuplicateErrors();
                                    setForm(prev => ({ ...prev, name: e.target.value }));
                                }}
                                placeholder="e.g. Internal Gateway"
                                disabled={isSaving}
                                required
                                maxLength={TAG_NAME_MAX}
                                aria-invalid={nameFieldError !== null}
                                aria-describedby={nameFieldError !== null ? 'sharding-tag-name-error' : undefined}
                            />
                            {nameFieldError ? (
                                <p id="sharding-tag-name-error" className="text-sm text-destructive" role="alert">
                                    {nameFieldError}
                                </p>
                            ) : null}
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="sharding-tag-key">
                                Key{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            {mode === 'create' ? (
                                <Input
                                    id="sharding-tag-key"
                                    value={form.key}
                                    onChange={e => handleKeyChange(e.target.value)}
                                    onBlur={handleKeyBlur}
                                    placeholder="e.g. internal"
                                    disabled={isSaving}
                                    maxLength={TAG_KEY_MAX}
                                    aria-invalid={duplicateKeyError !== null}
                                    aria-describedby={duplicateKeyError !== null ? 'sharding-tag-key-error' : undefined}
                                />
                            ) : (
                                <Input
                                    id="sharding-tag-key"
                                    value={form.key}
                                    readOnly
                                    disabled
                                    placeholder="e.g. internal"
                                    className="bg-muted"
                                />
                            )}
                            {duplicateKeyError ? (
                                <p id="sharding-tag-key-error" className="text-sm text-destructive" role="alert">
                                    {duplicateKeyError}
                                </p>
                            ) : null}
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="sharding-tag-description">Description</FieldLabel>
                            <Input
                                id="sharding-tag-description"
                                value={form.description}
                                onChange={e => setForm(prev => ({ ...prev, description: e.target.value }))}
                                placeholder="Describe the purpose of this tag"
                                disabled={isSaving}
                            />
                        </Field>

                        <ShardingTagGroupsField
                            groups={groups}
                            selectedGroupIds={form.restrictedGroupIds}
                            onSelectedGroupIdsChange={ids => setForm(prev => ({ ...prev, restrictedGroupIds: ids }))}
                            isLoading={isGroupsLoading}
                            disabled={isSaving}
                        />

                        {submitError ? (
                            <p className="text-sm text-destructive" role="alert">
                                {submitError}
                            </p>
                        ) : null}
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-col gap-2 border-t pt-4 sm:flex-col">
                    <Button type="button" variant="outline" className="w-full" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="sharding-tag-form" className="w-full" disabled={!isValid || isSaving}>
                        {submitLabel}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
