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
    Input,
    Label,
    Sheet,
    SheetContent,
    SheetTitle,
    Spinner,
    Textarea,
    cn,
    toast,
} from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { authzApiService } from '../../shared/api/authz-api.service';
import { authzQueryKeys } from '../../shared/api/query-keys';

/** Backend limit; see AuthzEntityIdConstants.MAX_ENTITY_ID_LENGTH. */
const MAX_ENTITY_ID_LENGTH = 255;

/** Action id segment: same charset the backend accepts between dots. */
const SEGMENT_REGEX = /^[a-z0-9_-]+$/;

/** Actions are RESOURCE entities under the reserved `action.` prefix. */
const ACTION_PREFIX = 'action';

function slugify(value: string): string {
    return value
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')
        .toLowerCase()
        .replace(/[^a-z0-9_-]+/g, '-')
        .replace(/^-+|-+$/g, '');
}

export interface CreateActionDialogProps {
    readonly open: boolean;
    readonly environmentId: string;
    readonly onOpenChange: (open: boolean) => void;
    readonly onCreated: () => void;
}

export function CreateActionDialog({ open, environmentId, onOpenChange, onCreated }: CreateActionDialogProps) {
    const queryClient = useQueryClient();

    const [displayName, setDisplayName] = useState('');
    const [slug, setSlug] = useState('');
    const [slugTouched, setSlugTouched] = useState(false);
    const [description, setDescription] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    useEffect(() => {
        if (!open) {
            setDisplayName('');
            setSlug('');
            setSlugTouched(false);
            setDescription('');
            setSubmitting(false);
            setSubmitError(null);
        }
    }, [open]);

    // Auto-derive the action id from the display name until the user edits it.
    useEffect(() => {
        if (!slugTouched) setSlug(slugify(displayName));
    }, [displayName, slugTouched]);

    const entityId = slug ? `${ACTION_PREFIX}.${slug}` : '';
    const slugError = slug.length > 0 && !SEGMENT_REGEX.test(slug) ? 'Action ID must match [a-z0-9_-]+ (no dots, no spaces).' : null;
    const lengthError = entityId.length > MAX_ENTITY_ID_LENGTH ? `Entity ID must be at most ${MAX_ENTITY_ID_LENGTH} characters.` : null;

    const canSubmit = !submitting && slug.length > 0 && displayName.trim().length > 0 && !slugError && !lengthError;

    async function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        if (!canSubmit) return;
        setSubmitting(true);
        setSubmitError(null);
        const trimmedDescription = description.trim();
        const attributes: Record<string, unknown> = { _displayName: displayName.trim() };
        if (trimmedDescription) attributes.description = trimmedDescription;
        try {
            await authzApiService.createEntity(environmentId, {
                entityId,
                kind: 'RESOURCE',
                attributes,
                parents: [],
                source: 'local',
            });
            toast.success(`Created ${displayName.trim()}`);
            void queryClient.invalidateQueries({ queryKey: authzQueryKeys.entities.all(environmentId) });
            onCreated();
            onOpenChange(false);
        } catch (err) {
            const message = err instanceof Error ? err.message : String(err);
            if (/already exists|duplicate|conflict/i.test(message) || /409/.test(message)) {
                setSubmitError(`An action with ID "${entityId}" already exists.`);
            } else {
                setSubmitError(message);
            }
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent
                side="right"
                showCloseButton={false}
                aria-label="Add Action"
                style={{ width: 'min(560px, 100vw)', maxWidth: 'min(560px, 100vw)' }}
                className="flex h-full flex-col gap-0 p-0"
            >
                <div className="border-b px-6 py-4">
                    <SheetTitle className="text-lg font-semibold">Add Action</SheetTitle>
                    <p className="mt-1 text-sm text-muted-foreground">
                        Declare an action the policy engine can authorize (e.g. <code className="font-mono">call_tool</code>,{' '}
                        <code className="font-mono">invoke</code>). Actions are referenced in the action clause of a policy.
                    </p>
                </div>

                <form className="flex min-h-0 flex-1 flex-col overflow-y-auto" onSubmit={handleSubmit}>
                    <div className="flex flex-col gap-4 px-6 py-4">
                        {submitError && (
                            <Alert variant="destructive">
                                <AlertDescription>{submitError}</AlertDescription>
                            </Alert>
                        )}

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="create-action-display-name">Display name</Label>
                            <Input
                                id="create-action-display-name"
                                value={displayName}
                                onChange={e => setDisplayName(e.target.value)}
                                placeholder="e.g. Call Tool"
                                required
                            />
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="create-action-slug">Action ID</Label>
                            <Input
                                id="create-action-slug"
                                value={slug}
                                onChange={e => {
                                    setSlug(e.target.value);
                                    setSlugTouched(true);
                                }}
                                onFocus={e => {
                                    if (!slugTouched) e.currentTarget.select();
                                }}
                                placeholder="e.g. call_tool"
                                aria-invalid={slugError !== null}
                                aria-describedby={slugError ? 'create-action-slug-error' : undefined}
                                required
                            />
                            {slugError && (
                                <p id="create-action-slug-error" className="text-xs text-destructive">
                                    {slugError}
                                </p>
                            )}
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="create-action-id-preview">Entity ID</Label>
                            <code
                                id="create-action-id-preview"
                                aria-live="polite"
                                className={cn(
                                    'rounded-md border bg-muted/40 px-3 py-2 font-mono text-sm',
                                    lengthError ? 'border-destructive text-destructive' : 'text-foreground',
                                )}
                            >
                                {entityId || <span className="text-muted-foreground">{`${ACTION_PREFIX}.<id>`}</span>}
                            </code>
                            {lengthError && <p className="text-xs text-destructive">{lengthError}</p>}
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="create-action-description">
                                Description <span className="text-xs text-muted-foreground">(optional)</span>
                            </Label>
                            <Textarea
                                id="create-action-description"
                                value={description}
                                onChange={e => setDescription(e.target.value)}
                                placeholder="Short note about what this action represents."
                                rows={3}
                            />
                        </div>
                    </div>
                </form>

                <div className="flex flex-none items-center justify-end gap-2 border-t bg-muted/40 px-6 py-3.5">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={submitting}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleSubmit as unknown as () => void} disabled={!canSubmit}>
                        {submitting ? <Spinner className="mr-2 size-4" aria-hidden /> : <PlusIcon className="mr-2 size-4" aria-hidden />}
                        Create Action
                    </Button>
                </div>
            </SheetContent>
        </Sheet>
    );
}
