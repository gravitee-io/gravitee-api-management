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
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Sheet,
    SheetContent,
    SheetTitle,
    Spinner,
    Textarea,
    cn,
    toast,
    useComboboxAnchor,
} from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { authzApiService } from '../../shared/api/authz-api.service';
import { authzQueryKeys } from '../../shared/api/query-keys';
import { formatEntityUid, fromBackend } from '../../shared/entity-adapter';
import { ENTITY_KIND_REGISTRY } from '../../shared/entity-kind-registry';
import { useEntities } from '../../shared/hooks/useEntities';
import { AttributeEditor, type AttributeRow } from './AttributeEditor';
import { attrsFromRows } from './attribute-rows';

type EntityKind = 'PRINCIPAL' | 'RESOURCE';

/** Backend limit; see AuthzEntityIdConstants.MAX_ENTITY_ID_LENGTH. */
const MAX_ENTITY_ID_LENGTH = 255;

/** Single entityId segment: must match what the backend regex accepts between dots. */
const SEGMENT_REGEX = /^[a-z0-9_-]+$/;

/** Custom prefix must additionally start with a letter so it doesn't look like a number. */
const CUSTOM_PREFIX_REGEX = /^[a-z][a-z0-9-]*$/;

const PRINCIPAL_PRESETS: readonly { canonical: string; label: string }[] = [
    { canonical: 'user', label: 'User' },
    { canonical: 'group', label: 'Group' },
    { canonical: 'serviceaccount', label: 'Service Account' },
    { canonical: 'agent-identity', label: 'Agent Identity' },
];

const RESOURCE_PRESETS: readonly { canonical: string; label: string }[] = [
    { canonical: 'mcp', label: 'MCP Server' },
    { canonical: 'model', label: 'AI Model' },
    { canonical: 'agent', label: 'Agent' },
    { canonical: 'api', label: 'API' },
    { canonical: 'event', label: 'Event' },
    { canonical: 'resource', label: 'Generic Resource' },
];

const CUSTOM_TYPE = '__custom__';

function slugify(value: string): string {
    return value
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')
        .toLowerCase()
        .replace(/[^a-z0-9_-]+/g, '-')
        .replace(/^-+|-+$/g, '');
}

function isKnownCanonical(value: string): boolean {
    return ENTITY_KIND_REGISTRY.some(e => e.canonical === value);
}

export interface CreateEntityDialogProps {
    readonly open: boolean;
    readonly kind: EntityKind;
    readonly environmentId: string;
    readonly onOpenChange: (open: boolean) => void;
    readonly onCreated: () => void;
}

export function CreateEntityDialog({ open, kind, environmentId, onOpenChange, onCreated }: CreateEntityDialogProps) {
    const queryClient = useQueryClient();
    const presets = kind === 'PRINCIPAL' ? PRINCIPAL_PRESETS : RESOURCE_PRESETS;

    const [typeSelection, setTypeSelection] = useState<string>(presets[0]!.canonical);
    const [customPrefix, setCustomPrefix] = useState('');
    const [slug, setSlug] = useState('');
    const [slugTouched, setSlugTouched] = useState(false);
    const [displayName, setDisplayName] = useState('');
    const [description, setDescription] = useState('');
    const [parentIds, setParentIds] = useState<string[]>([]);
    const [attrRows, setAttrRows] = useState<AttributeRow[]>([]);
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    // Reset state on close so the next open is fresh. Done as effect so external
    // resets (e.g. from successful import) don't fight the local state machine.
    useEffect(() => {
        if (!open) {
            setTypeSelection(presets[0]!.canonical);
            setCustomPrefix('');
            setSlug('');
            setSlugTouched(false);
            setDisplayName('');
            setDescription('');
            setParentIds([]);
            setAttrRows([]);
            setSubmitError(null);
            setSubmitting(false);
        }
    }, [open, presets]);

    // Auto-derive slug from displayName until the user touches the slug field.
    useEffect(() => {
        if (!slugTouched) setSlug(slugify(displayName));
    }, [displayName, slugTouched]);

    const isCustom = typeSelection === CUSTOM_TYPE;
    const canonicalPrefix = isCustom ? customPrefix.trim().toLowerCase() : typeSelection;
    const entityId = canonicalPrefix && slug ? `${canonicalPrefix}.${slug}` : '';

    const customPrefixError =
        isCustom && customPrefix.length > 0 && !CUSTOM_PREFIX_REGEX.test(customPrefix.trim().toLowerCase())
            ? 'Prefix must start with a letter and contain only lowercase letters, digits, or dashes.'
            : null;
    const slugError = slug.length > 0 && !SEGMENT_REGEX.test(slug) ? 'Slug must match [a-z0-9_-]+ (no dots, no spaces).' : null;
    const lengthError = entityId.length > MAX_ENTITY_ID_LENGTH ? `Entity ID must be at most ${MAX_ENTITY_ID_LENGTH} characters.` : null;
    const displayNameError = displayName.trim().length === 0 ? 'Display name is required.' : null;

    const canSubmit =
        !submitting &&
        slug.length > 0 &&
        canonicalPrefix.length > 0 &&
        displayName.trim().length > 0 &&
        !customPrefixError &&
        !slugError &&
        !lengthError &&
        (isCustom ? !isKnownCanonical(canonicalPrefix) : true);

    // Parents come from the same kind: a Group is a parent of a User; a generic
    // Resource can be a parent of an MCP/Model/etc. Fetching a reasonable page
    // covers most envs without pulling thousands of options into the picker.
    const parentsQuery = useEntities(environmentId, 200, { kind });
    const parentOptions = useMemo(() => {
        const all = parentsQuery.data?.data ?? [];
        return all.map(fromBackend).map(e => {
            const uid = formatEntityUid(e.uid);
            const label =
                (typeof e.attrs._displayName === 'string' && (e.attrs._displayName as string)) ||
                (typeof e.attrs.name === 'string' && (e.attrs.name as string)) ||
                e.uid.id;
            return { uid, label, type: e.uid.type };
        });
    }, [parentsQuery.data]);

    const parentAnchorRef = useComboboxAnchor();

    async function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        if (!canSubmit) return;
        setSubmitting(true);
        setSubmitError(null);
        const rowsResult = attrsFromRows(attrRows);
        if (rowsResult.error) {
            setSubmitError(rowsResult.error);
            setSubmitting(false);
            return;
        }
        const trimmedDescription = description.trim();
        const attributes: Record<string, unknown> = {
            ...rowsResult.attributes,
            _displayName: displayName.trim(),
        };
        if (trimmedDescription) attributes.description = trimmedDescription;
        try {
            // The backend "save" is create-or-replace (upsert), shared with the
            // import/sync flows. The Add dialog means *create*, so guard against
            // silently overwriting an existing entity with the same Entity ID.
            const existing = await authzApiService.getEntity(environmentId, entityId);
            if (existing) {
                setSubmitError(`An entity with ID "${entityId}" already exists. Pick a different slug or prefix.`);
                return;
            }
            await authzApiService.createEntity(environmentId, {
                entityId,
                kind,
                attributes,
                parents: parentIds,
                source: 'local',
            });
            toast.success(`Created ${displayName.trim()}`);
            void queryClient.invalidateQueries({ queryKey: authzQueryKeys.entities.all(environmentId) });
            onCreated();
            onOpenChange(false);
        } catch (err) {
            const message = err instanceof Error ? err.message : String(err);
            // Map the common backend error so the user sees something actionable.
            if (/already exists|duplicate|conflict/i.test(message) || /409/.test(message)) {
                setSubmitError(`An entity with ID "${entityId}" already exists.`);
            } else {
                setSubmitError(message);
            }
        } finally {
            setSubmitting(false);
        }
    }

    const labelForKind = kind === 'PRINCIPAL' ? 'Principal' : 'Resource';

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent
                side="right"
                showCloseButton={false}
                aria-label={`Add ${labelForKind}`}
                style={{ width: 'min(640px, 100vw)', maxWidth: 'min(640px, 100vw)' }}
                className="flex h-full flex-col gap-0 p-0"
            >
                <div className="border-b px-6 py-4">
                    <SheetTitle className="text-lg font-semibold">{`Add ${labelForKind}`}</SheetTitle>
                    <p className="mt-1 text-sm text-muted-foreground">
                        {kind === 'PRINCIPAL'
                            ? 'Create a local principal that the policy engine can reference. Use this for actors that are not synced from APIM or imported from the Context Catalog.'
                            : 'Create a local resource entity that the policy engine can target. For catalog-managed resources, use Import from Context Catalog instead.'}
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
                            <Label htmlFor="create-entity-type">Type</Label>
                            <Select value={typeSelection} onValueChange={setTypeSelection}>
                                <SelectTrigger id="create-entity-type" aria-label="Entity type">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {presets.map(p => (
                                        <SelectItem key={p.canonical} value={p.canonical}>
                                            {p.label}
                                            <Badge variant="outline" className="ml-2 font-mono text-xs">
                                                {p.canonical}
                                            </Badge>
                                        </SelectItem>
                                    ))}
                                    <SelectItem value={CUSTOM_TYPE}>Other (custom prefix)</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        {isCustom && (
                            <div className="flex flex-col gap-1.5">
                                <Label htmlFor="create-entity-custom-prefix">Custom prefix</Label>
                                <Input
                                    id="create-entity-custom-prefix"
                                    value={customPrefix}
                                    onChange={e => setCustomPrefix(e.target.value)}
                                    placeholder="e.g. webhook"
                                    aria-invalid={customPrefixError !== null}
                                    aria-describedby={customPrefixError ? 'create-entity-custom-prefix-error' : undefined}
                                />
                                {customPrefixError && (
                                    <p id="create-entity-custom-prefix-error" className="text-xs text-destructive">
                                        {customPrefixError}
                                    </p>
                                )}
                                {!customPrefixError && isKnownCanonical(canonicalPrefix) && customPrefix.length > 0 && (
                                    <p className="text-xs text-warning">
                                        &quot;{canonicalPrefix}&quot; is a preset type — pick it from the dropdown instead.
                                    </p>
                                )}
                            </div>
                        )}

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="create-entity-display-name">Display name</Label>
                            <Input
                                id="create-entity-display-name"
                                value={displayName}
                                onChange={e => setDisplayName(e.target.value)}
                                placeholder="e.g. Alice"
                                aria-invalid={displayName.length > 0 && displayNameError !== null}
                                required
                            />
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="create-entity-slug">Slug</Label>
                            <Input
                                id="create-entity-slug"
                                value={slug}
                                onChange={e => {
                                    setSlug(e.target.value);
                                    setSlugTouched(true);
                                }}
                                onFocus={e => {
                                    // First focus on an auto-derived slug selects it, so typing
                                    // replaces the suggestion instead of appending to it.
                                    if (!slugTouched) e.currentTarget.select();
                                }}
                                placeholder="e.g. alice"
                                aria-invalid={slugError !== null}
                                aria-describedby={slugError ? 'create-entity-slug-error' : undefined}
                                required
                            />
                            {slugError && (
                                <p id="create-entity-slug-error" className="text-xs text-destructive">
                                    {slugError}
                                </p>
                            )}
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="create-entity-id-preview">Entity ID</Label>
                            <code
                                id="create-entity-id-preview"
                                aria-live="polite"
                                className={cn(
                                    'rounded-md border bg-muted/40 px-3 py-2 font-mono text-sm',
                                    lengthError ? 'border-destructive text-destructive' : 'text-foreground',
                                )}
                            >
                                {entityId || <span className="text-muted-foreground">{`${canonicalPrefix || 'kind'}.<slug>`}</span>}
                            </code>
                            {lengthError && <p className="text-xs text-destructive">{lengthError}</p>}
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="create-entity-description">
                                Description <span className="text-xs text-muted-foreground">(optional)</span>
                            </Label>
                            <Textarea
                                id="create-entity-description"
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
                                        <ComboboxEmpty>
                                            {parentOptions.length === 0
                                                ? `No ${labelForKind.toLowerCase()}s available as parents yet.`
                                                : 'No matches.'}
                                        </ComboboxEmpty>
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
                            <AttributeEditor value={attrRows} onChange={setAttrRows} keySuggestions={[]} />
                        </div>
                    </div>
                </form>

                <div className="flex flex-none items-center justify-end gap-2 border-t bg-muted/40 px-6 py-3.5">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={submitting}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleSubmit as unknown as () => void} disabled={!canSubmit}>
                        {submitting ? <Spinner className="mr-2 size-4" aria-hidden /> : <PlusIcon className="mr-2 size-4" aria-hidden />}
                        {`Create ${labelForKind}`}
                    </Button>
                </div>
            </SheetContent>
        </Sheet>
    );
}
