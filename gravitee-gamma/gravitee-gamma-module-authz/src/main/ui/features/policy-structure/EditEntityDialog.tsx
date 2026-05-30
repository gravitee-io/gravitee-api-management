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
    Badge,
    Button,
    Combobox,
    ComboboxChip,
    ComboboxChips,
    ComboboxChipsInput,
    ComboboxContent,
    ComboboxEmpty,
    ComboboxItem,
    ComboboxList,
    Input,
    Label,
    Sheet,
    SheetContent,
    SheetTitle,
    Spinner,
    Textarea,
    toast,
    useComboboxAnchor,
} from '@gravitee/graphene-core';
import { CheckIcon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { authzApiService } from '../../shared/api/authz-api.service';
import { authzQueryKeys } from '../../shared/api/query-keys';
import { formatEntityUid, fromBackend, toBackend } from '../../shared/entity-adapter';
import type { EntityInstance } from '../../shared/entity.types';
import { useEntities } from '../../shared/hooks/useEntities';
import { AttributeEditor, type AttributeRow } from './AttributeEditor';
import { attrsFromRows, rowsFromAttrs } from './attribute-rows';

type EntityKind = 'PRINCIPAL' | 'RESOURCE';

export interface EditEntityDialogProps {
    readonly open: boolean;
    readonly entity: EntityInstance | null;
    readonly kind: EntityKind;
    readonly environmentId: string;
    readonly onOpenChange: (open: boolean) => void;
    readonly onUpdated: () => void;
}

function displayNameOf(entity: EntityInstance): string {
    if (entity.displayName) return entity.displayName;
    const name = entity.attrs.name;
    return typeof name === 'string' ? name : entity.uid.id;
}

function descriptionOf(entity: EntityInstance): string {
    const description = entity.attrs.description;
    return typeof description === 'string' ? description : '';
}

export function EditEntityDialog({ open, entity, kind, environmentId, onOpenChange, onUpdated }: EditEntityDialogProps) {
    const queryClient = useQueryClient();

    const [displayName, setDisplayName] = useState('');
    const [description, setDescription] = useState('');
    const [parentIds, setParentIds] = useState<string[]>([]);
    const [attrRows, setAttrRows] = useState<AttributeRow[]>([]);
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    // Re-seed the form whenever a different entity is opened for editing.
    useEffect(() => {
        if (open && entity) {
            setDisplayName(displayNameOf(entity));
            setDescription(descriptionOf(entity));
            setParentIds(entity.parents.map(formatEntityUid));
            setAttrRows(rowsFromAttrs(entity.attrs));
            setSubmitError(null);
            setSubmitting(false);
        }
    }, [open, entity]);

    const entityUid = entity ? formatEntityUid(entity.uid) : '';
    const displayNameError = displayName.trim().length === 0 ? 'Display name is required.' : null;
    const canSubmit = !submitting && entity !== null && displayName.trim().length > 0 && !displayNameError;

    // Parents come from the same kind, mirroring the create flow.
    const parentsQuery = useEntities(environmentId, 200, { kind });
    const parentOptions = useMemo(() => {
        const all = parentsQuery.data?.data ?? [];
        return all
            .map(fromBackend)
            .filter(e => !entity || formatEntityUid(e.uid) !== entityUid) // an entity can't parent itself
            .map(e => {
                const uid = formatEntityUid(e.uid);
                const label =
                    (typeof e.attrs._displayName === 'string' && (e.attrs._displayName as string)) ||
                    (typeof e.attrs.name === 'string' && (e.attrs.name as string)) ||
                    e.uid.id;
                return { uid, label, type: e.uid.type };
            });
    }, [parentsQuery.data, entity, entityUid]);

    const parentAnchorRef = useComboboxAnchor();

    async function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        if (!canSubmit || !entity) return;
        setSubmitting(true);
        setSubmitError(null);
        const rowsResult = attrsFromRows(attrRows);
        if (rowsResult.error) {
            setSubmitError(rowsResult.error);
            setSubmitting(false);
            return;
        }
        // Full replace: start from the entity's complete attribute map so meta
        // (_kind, _source, …) and description are preserved, drop the old editable
        // attrs, then apply the editor's set and override the edited fields.
        const attributes: Record<string, unknown> = { ...toBackend(entity).attributes };
        for (const k of Object.keys(attributes)) {
            if (!k.startsWith('_') && k !== 'description') delete attributes[k];
        }
        Object.assign(attributes, rowsResult.attributes);
        attributes._displayName = displayName.trim();
        const trimmedDescription = description.trim();
        if (trimmedDescription) attributes.description = trimmedDescription;
        else delete attributes.description;
        try {
            await authzApiService.updateEntity(environmentId, entityUid, { attributes, parents: parentIds });
            toast.success(`Updated ${displayName.trim()}`);
            void queryClient.invalidateQueries({ queryKey: authzQueryKeys.entities.all(environmentId) });
            onUpdated();
            onOpenChange(false);
        } catch (err) {
            setSubmitError(err instanceof Error ? err.message : String(err));
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent
                side="right"
                showCloseButton={false}
                aria-label="Edit entity"
                style={{ width: 'min(640px, 100vw)', maxWidth: 'min(640px, 100vw)' }}
                className="flex h-full flex-col gap-0 p-0"
            >
                <div className="border-b px-6 py-4">
                    <SheetTitle className="text-lg font-semibold">Edit entity</SheetTitle>
                    <p className="mt-1 text-sm text-muted-foreground">
                        Update the display name, description, and parents of this local entity. The Entity ID is immutable.
                    </p>
                </div>

                <form id="edit-entity-form" className="flex min-h-0 flex-1 flex-col overflow-y-auto" onSubmit={handleSubmit}>
                    <div className="flex flex-col gap-4 px-6 py-4">
                        {submitError && (
                            <Alert variant="destructive">
                                <AlertDescription>{submitError}</AlertDescription>
                            </Alert>
                        )}

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="edit-entity-id">Entity ID</Label>
                            <code
                                id="edit-entity-id"
                                className="rounded-md border bg-muted/40 px-3 py-2 font-mono text-sm text-muted-foreground"
                            >
                                {entityUid || '—'}
                            </code>
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="edit-entity-display-name">Display name</Label>
                            <Input
                                id="edit-entity-display-name"
                                value={displayName}
                                onChange={e => setDisplayName(e.target.value)}
                                placeholder="e.g. Alice"
                                aria-invalid={displayName.length > 0 && displayNameError !== null}
                                required
                            />
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="edit-entity-description">
                                Description <span className="text-xs text-muted-foreground">(optional)</span>
                            </Label>
                            <Textarea
                                id="edit-entity-description"
                                value={description}
                                onChange={e => setDescription(e.target.value)}
                                placeholder="Short note about what this entity represents."
                                rows={3}
                            />
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label>
                                Parents <span className="text-xs text-muted-foreground">(optional)</span>
                            </Label>
                            <Combobox multiple value={parentIds} onValueChange={next => setParentIds(next as string[])} autoComplete="list">
                                <ComboboxChips ref={parentAnchorRef}>
                                    {parentIds.map(pid => {
                                        const opt = parentOptions.find(o => o.uid === pid);
                                        return (
                                            <ComboboxChip key={pid} removeAriaLabel={`Remove ${opt?.label ?? pid}`}>
                                                {opt?.label ?? pid}
                                            </ComboboxChip>
                                        );
                                    })}
                                    <ComboboxChipsInput
                                        placeholder={parentIds.length === 0 ? 'Search existing entities…' : ''}
                                        aria-label="Add parent"
                                    />
                                </ComboboxChips>
                                <ComboboxContent anchor={parentAnchorRef} className="max-h-64 min-w-60" style={{ pointerEvents: 'auto' }}>
                                    <ComboboxList>
                                        <ComboboxEmpty>No matches.</ComboboxEmpty>
                                        {parentOptions.map(opt => (
                                            <ComboboxItem key={opt.uid} value={opt.uid}>
                                                <div className="flex min-w-0 flex-1 items-center gap-2">
                                                    <span className="truncate">{opt.label}</span>
                                                    <Badge variant="outline" className="shrink-0 font-mono text-xs">
                                                        {opt.type}
                                                    </Badge>
                                                </div>
                                            </ComboboxItem>
                                        ))}
                                    </ComboboxList>
                                </ComboboxContent>
                            </Combobox>
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label>
                                Attributes <span className="text-xs text-muted-foreground">(optional)</span>
                            </Label>
                            <AttributeEditor
                                value={attrRows}
                                onChange={setAttrRows}
                                readOnly={entity?.source !== 'local'}
                                keySuggestions={[]}
                            />
                            {entity?.source !== 'local' && (
                                <p className="text-xs text-muted-foreground">Attributes are managed by the source and are read-only.</p>
                            )}
                        </div>
                    </div>
                </form>

                <div className="flex flex-none items-center justify-end gap-2 border-t bg-muted/40 px-6 py-3.5">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={submitting}>
                        Cancel
                    </Button>
                    <Button type="submit" form="edit-entity-form" disabled={!canSubmit}>
                        {submitting ? <Spinner className="mr-2 size-4" aria-hidden /> : <CheckIcon className="mr-2 size-4" aria-hidden />}
                        Save changes
                    </Button>
                </div>
            </SheetContent>
        </Sheet>
    );
}
