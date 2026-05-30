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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Badge, Button, Card, CardContent, Input, Label, Separator, Skeleton, Switch, Textarea, cn } from '@gravitee/graphene-core';
import {
    BoxesIcon,
    CheckIcon,
    CircleStopIcon,
    ClockIcon,
    CopyIcon,
    DownloadIcon,
    ExternalLinkIcon,
    EyeIcon,
    EyeOffIcon,
    FileUpIcon,
    GlobeIcon,
    PlayIcon,
    Trash2Icon,
    UserIcon,
} from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { CategorySelectInput } from './CategorySelectInput';
import { ChipInput } from './ChipInput';
import { DeleteDialog } from './DeleteDialog';
import { DuplicateApi } from './DuplicateApi';
import { ExportApi } from './ExportApi';
import { ImagePicker } from './ImagePicker';
import { ImportDialog } from './ImportDialog';
import { PromoteDialog } from './PromoteDialog';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiGeneralMutations } from '../../../hooks/useApiGeneralMutations';
import { useEnvCategories } from '../../../hooks/useEnvCategories';
import { exportApiCrd, exportApiDefinition } from '../../../services/apis';
import type { ApiDetailDto } from '../../../types';
import { extractContextPathPlaceholder, extractHostPlaceholder, getDuplicateEntryMode } from '../../../utils/apiGeneralDuplicate';
import { buildExcludeAdditionalData, buildExportFileName, downloadBlob, type ExportIncludeKey } from '../../../utils/apiGeneralExport';

// ─── Types ────────────────────────────────────────────────────────────────────

interface GeneralForm {
    name: string;
    apiVersion: string;
    description: string;
    labels: string[];
    categories: string[];
    allowedInApiProducts: boolean;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formFromApi(api: ApiDetailDto): GeneralForm {
    return {
        name: api.name ?? '',
        apiVersion: api.apiVersion ?? '',
        description: api.description ?? '',
        labels: api.labels ?? [],
        categories: api.categories ?? [],
        allowedInApiProducts: api.allowedInApiProducts ?? false,
    };
}

function formatDate(iso?: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

/** Import and promote flows are stubbed — keep disabled until post–2 June release. */
const IMPORT_AND_PROMOTE_UNAVAILABLE = true;

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiGeneralPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const navigate = useNavigate();
    const { api, isLoading, permissionsReady } = useApiDetailContext();

    // ── Permissions (mirrors legacy api-general-info.component.ts) ────────────
    // api-definition-r : read form / see export / see allow-in-products toggle
    // api-definition-u : edit form / save / start / stop / promote
    // api-definition-c : import / duplicate
    // api-definition-d : delete
    const canReadDefinition = useHasPermission({ anyOf: ['api-definition-r'] });
    const canEditDefinition = useHasPermission({ anyOf: ['api-definition-u'] });
    const canCreateDefinition = useHasPermission({ anyOf: ['api-definition-c'] });
    const canDeleteDefinition = useHasPermission({ anyOf: ['api-definition-d'] });

    const isKubernetesManaged = api?.definitionContext?.origin === 'KUBERNETES';

    // Form is read-only until permissions are resolved, if user lacks update rights,
    // or if the API is managed by the Kubernetes operator.
    const isReadOnly = !permissionsReady || !canEditDefinition || isKubernetesManaged;

    // Delete is blocked while the API is running or published (matches legacy canDelete logic).
    const cannotDelete = api?.state === 'STARTED' || api?.lifecycleState === 'PUBLISHED';

    const { data: envCategories = [], isLoading: categoriesLoading } = useEnvCategories();

    // ── Form state ────────────────────────────────────────────────────────────

    const [form, setForm] = useState<GeneralForm | null>(null);
    const [savedForm, setSavedForm] = useState<GeneralForm | null>(null);
    const [saveError, setSaveError] = useState<string | null>(null);

    const [exportOpen, setExportOpen] = useState(false);
    const [isExporting, setIsExporting] = useState(false);
    const [exportError, setExportError] = useState<string | null>(null);
    const [importOpen, setImportOpen] = useState(false);
    const [duplicateOpen, setDuplicateOpen] = useState(false);
    const [promoteOpen, setPromoteOpen] = useState(false);
    const [deleteOpen, setDeleteOpen] = useState(false);

    // Seed form once per API identity. Render-phase setState (not inside an effect) is
    // batched atomically by React 18 — the three calls below are a single update, so
    // there is no concurrent-mode tear between seededApiId, form, and savedForm.
    const [seededApiId, setSeededApiId] = useState<string | null>(null);
    if (api && api.id !== seededApiId) {
        setSeededApiId(api.id ?? null);
        const seed = formFromApi(api);
        setSavedForm(seed);
        setForm(seed);
    }

    const isDirty = useMemo(
        () => form !== null && savedForm !== null && JSON.stringify(form) !== JSON.stringify(savedForm),
        [form, savedForm],
    );

    const setField = useCallback(<K extends keyof GeneralForm>(key: K, value: GeneralForm[K]) => {
        setForm(prev => (prev ? { ...prev, [key]: value } : prev));
        setSaveError(null);
    }, []);

    // ── Mutations ─────────────────────────────────────────────────────────────

    const {
        saveMutation,
        startMutation,
        stopMutation,
        deleteMutation,
        duplicateMutation,
        importMutation,
        pictureMutation,
        removePictureMutation,
        backgroundMutation,
        removeBackgroundMutation,
    } = useApiGeneralMutations(api, {
        onDeleteSuccess: () => {
            setDeleteOpen(false);
            navigate('../..');
        },
        onDuplicateSuccess: newApi => {
            setDuplicateOpen(false);
            navigate(`../../${newApi.id}/general`);
        },
        onImportSuccess: updatedApi => {
            setImportOpen(false);
            const seed = formFromApi(updatedApi);
            setSavedForm(seed);
            setForm(seed);
        },
    });

    const handleSave = useCallback(() => {
        if (!form) return;
        saveMutation.mutate(
            {
                name: form.name,
                apiVersion: form.apiVersion,
                description: form.description,
                labels: form.labels,
                categories: form.categories,
                allowedInApiProducts: form.allowedInApiProducts,
            },
            {
                onSuccess: () => {
                    setSavedForm(form);
                    setSaveError(null);
                },
                onError: (e: unknown) => setSaveError(e instanceof Error ? e.message : 'Failed to save changes.'),
            },
        );
    }, [form, saveMutation]);

    const handleExport = useCallback(
        async (tab: 'gravitee' | 'crd' | 'terraform', include: Record<ExportIncludeKey, boolean>) => {
            if (tab === 'terraform' || !env || !apiId) return;
            setIsExporting(true);
            setExportError(null);
            try {
                if (tab === 'gravitee') {
                    const blob = await exportApiDefinition(env.id, apiId, buildExcludeAdditionalData(include));
                    downloadBlob(blob, `${buildExportFileName(api)}.json`);
                } else {
                    const blob = await exportApiCrd(env.id, apiId);
                    downloadBlob(blob, `${buildExportFileName(api, '-crd')}.yml`);
                }
                setExportOpen(false);
            } catch (e: unknown) {
                setExportError(e instanceof Error ? e.message : 'An error occurred while exporting the API.');
            } finally {
                setIsExporting(false);
            }
        },
        [api, apiId, env],
    );

    const duplicateEntryMode = getDuplicateEntryMode(api);

    const apiStarted = api?.state === 'STARTED';

    const startStopError =
        startMutation.isError || stopMutation.isError
            ? startMutation.isError
                ? startMutation.error instanceof Error
                    ? startMutation.error.message
                    : 'Failed to start API.'
                : stopMutation.error instanceof Error
                  ? stopMutation.error.message
                  : 'Failed to stop API.'
            : null;

    const deleteError = deleteMutation.isError
        ? deleteMutation.error instanceof Error
            ? deleteMutation.error.message
            : 'Failed to delete API.'
        : null;

    const duplicateError = duplicateMutation.isError
        ? duplicateMutation.error instanceof Error
            ? duplicateMutation.error.message
            : 'Failed to duplicate API.'
        : null;

    const importError = importMutation.isError
        ? importMutation.error instanceof Error
            ? importMutation.error.message
            : 'Failed to import API definition.'
        : null;

    // ── Loading ───────────────────────────────────────────────────────────────

    if (isLoading || !form) {
        return (
            <div className="space-y-5 p-6">
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-2">
                        <Skeleton className="h-7 w-24 rounded" />
                        <Skeleton className="h-4 w-64 rounded" />
                    </div>
                </div>
                <Skeleton className="h-72 w-full rounded-xl" />
            </div>
        );
    }

    return (
        <div className="space-y-5 p-6">
            {/* ── Page Header ─────────────────────────────────────────────── */}
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">General</h1>
                    <p className="text-sm text-muted-foreground">Manage name, version, metadata, and lifecycle for this API.</p>
                </div>
                {isDirty && !isReadOnly && (
                    <div className="flex shrink-0 items-center gap-2">
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => {
                                setForm(savedForm);
                                setSaveError(null);
                            }}
                            disabled={saveMutation.isPending}
                        >
                            Discard
                        </Button>
                        <Button type="button" size="sm" onClick={handleSave} disabled={saveMutation.isPending}>
                            <CheckIcon className="size-4" />
                            {saveMutation.isPending ? 'Saving…' : 'Save changes'}
                        </Button>
                    </div>
                )}
            </div>

            {saveError && (
                <Card className="border-destructive/30 bg-destructive/5 p-4">
                    <p className="text-sm text-destructive">{saveError}</p>
                </Card>
            )}

            {isKubernetesManaged && (
                <Card className="border-primary/20 bg-primary/5 p-4">
                    <p className="text-sm text-primary">
                        This API is managed by the Kubernetes operator. Configuration changes must be made in your Kubernetes manifests.
                    </p>
                </Card>
            )}

            {/* ── Main Card ───────────────────────────────────────────────── */}
            <Card>
                <CardContent className="pt-6">
                    <div className="flex flex-row gap-8">
                        {/* ─ Left: form ─ */}
                        <div className="flex-1 min-w-0 space-y-4">
                            <div className="flex gap-4">
                                <div className="flex-1 min-w-0 space-y-1">
                                    <Label htmlFor="api-name">
                                        Name <span className="text-destructive">*</span>
                                    </Label>
                                    <Input
                                        id="api-name"
                                        value={form.name}
                                        onChange={e => setField('name', e.target.value)}
                                        placeholder="e.g. My API"
                                        disabled={isReadOnly}
                                    />
                                </div>
                                <div className="space-y-1 w-32">
                                    <Label htmlFor="api-version">
                                        Version <span className="text-destructive">*</span>
                                    </Label>
                                    <Input
                                        id="api-version"
                                        value={form.apiVersion}
                                        onChange={e => setField('apiVersion', e.target.value)}
                                        maxLength={32}
                                        placeholder="v1.0.0"
                                        disabled={isReadOnly}
                                    />
                                </div>
                            </div>

                            <div className="space-y-1">
                                <Label htmlFor="api-description">Description</Label>
                                <Textarea
                                    id="api-description"
                                    rows={3}
                                    style={{ fieldSizing: 'fixed' } as React.CSSProperties}
                                    value={form.description}
                                    onChange={e => setField('description', e.target.value)}
                                    placeholder="Describe what this API does…"
                                    disabled={isReadOnly}
                                />
                            </div>

                            <div className="space-y-1">
                                <Label htmlFor="api-labels">Labels</Label>
                                <ChipInput
                                    id="api-labels"
                                    values={form.labels}
                                    onChange={v => !isReadOnly && setField('labels', v)}
                                    placeholder={isReadOnly ? '' : 'Type a label and press Enter'}
                                />
                            </div>

                            <div className="space-y-1">
                                <Label htmlFor="api-categories">Categories</Label>
                                <CategorySelectInput
                                    id="api-categories"
                                    selectedKeys={form.categories}
                                    categories={envCategories}
                                    isLoading={categoriesLoading}
                                    disabled={isReadOnly}
                                    onChange={keys => setField('categories', keys)}
                                />
                            </div>

                            {canReadDefinition && (
                                <div className="flex items-center justify-between gap-4 rounded-lg border bg-muted/40 px-4 py-3">
                                    <div className="flex items-start gap-3">
                                        <BoxesIcon className="size-4 text-primary mt-0.5 shrink-0" />
                                        <div>
                                            <p className="text-sm font-medium">Allow in API Products</p>
                                            <p className="text-xs text-muted-foreground">
                                                When enabled, this API can be bundled into API Products for grouped consumer access.
                                            </p>
                                        </div>
                                    </div>
                                    <Switch
                                        checked={form.allowedInApiProducts}
                                        onCheckedChange={v => !isReadOnly && setField('allowedInApiProducts', v)}
                                        disabled={isReadOnly}
                                    />
                                </div>
                            )}
                        </div>

                        {/* ─ Right: images + metadata ─ */}
                        <div className="shrink-0 border-l pl-8 space-y-5" style={{ width: '280px' }}>
                            <div className="space-y-3">
                                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Images</p>
                                <div className="flex gap-3 items-start">
                                    <ImagePicker
                                        label="Picture"
                                        preview={api?._links?.pictureUrl}
                                        width={88}
                                        height={88}
                                        onSelect={b64 => pictureMutation.mutate(b64)}
                                        onRemove={() => removePictureMutation.mutate()}
                                        disabled={isReadOnly || pictureMutation.isPending || removePictureMutation.isPending}
                                    />
                                    <ImagePicker
                                        label="Background"
                                        preview={api?._links?.backgroundUrl}
                                        width={152}
                                        height={88}
                                        onSelect={b64 => backgroundMutation.mutate(b64)}
                                        onRemove={() => removeBackgroundMutation.mutate()}
                                        disabled={isReadOnly || backgroundMutation.isPending || removeBackgroundMutation.isPending}
                                    />
                                </div>
                                <p className="text-center text-muted-foreground" style={{ fontSize: '10px' }}>
                                    PNG, JPG, SVG · max 500 KB
                                </p>
                                {(pictureMutation.isError ||
                                    removePictureMutation.isError ||
                                    backgroundMutation.isError ||
                                    removeBackgroundMutation.isError) && (
                                    <p className="text-xs text-destructive text-center">
                                        {pictureMutation.isError
                                            ? pictureMutation.error instanceof Error
                                                ? pictureMutation.error.message
                                                : 'Failed to update picture.'
                                            : removePictureMutation.isError
                                              ? removePictureMutation.error instanceof Error
                                                  ? removePictureMutation.error.message
                                                  : 'Failed to remove picture.'
                                              : backgroundMutation.isError
                                                ? backgroundMutation.error instanceof Error
                                                    ? backgroundMutation.error.message
                                                    : 'Failed to update background.'
                                                : removeBackgroundMutation.error instanceof Error
                                                  ? removeBackgroundMutation.error.message
                                                  : 'Failed to remove background.'}
                                    </p>
                                )}
                            </div>

                            <Separator />

                            {/* Metadata */}
                            <div className="space-y-3">
                                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Details</p>
                                <dl className="space-y-2.5">
                                    <DetailRow
                                        label={
                                            <>
                                                <UserIcon className="size-3" /> Owner
                                            </>
                                        }
                                        value={api?.primaryOwner?.displayName ?? api?.primaryOwner?.email ?? '—'}
                                    />
                                    <DetailRow
                                        label={
                                            <>
                                                <ClockIcon className="size-3" /> Created
                                            </>
                                        }
                                        value={formatDate(api?.createdAt)}
                                    />
                                    <DetailRow
                                        label={
                                            <>
                                                <ClockIcon className="size-3" /> Updated
                                            </>
                                        }
                                        value={formatDate(api?.updatedAt)}
                                    />
                                    <div className="flex items-center justify-between gap-2">
                                        <dt className="text-muted-foreground flex items-center gap-1.5 shrink-0 text-xs">
                                            <GlobeIcon className="size-3" /> Visibility
                                        </dt>
                                        <dd>
                                            <Badge variant="outline" className="gap-1" style={{ fontSize: '10px' }}>
                                                {api?.visibility === 'PUBLIC' ? (
                                                    <EyeIcon className="size-2.5" />
                                                ) : (
                                                    <EyeOffIcon className="size-2.5" />
                                                )}
                                                {api?.visibility ?? '—'}
                                            </Badge>
                                        </dd>
                                    </div>
                                    <div className="flex items-center justify-between gap-2">
                                        <dt className="text-muted-foreground shrink-0 text-xs">Lifecycle</dt>
                                        <dd>
                                            <Badge variant="secondary" style={{ fontSize: '10px' }}>
                                                {api?.lifecycleState ?? '—'}
                                            </Badge>
                                        </dd>
                                    </div>
                                    <div className="flex items-center justify-between gap-2">
                                        <dt className="text-muted-foreground shrink-0 text-xs">Status</dt>
                                        <dd>
                                            <StatusBadge state={api?.state} />
                                        </dd>
                                    </div>
                                </dl>
                            </div>
                        </div>
                    </div>

                    {/* ─ Action strip ─ */}
                    <Separator className="my-5" />
                    <div className="flex flex-wrap items-center gap-2">
                        {canReadDefinition && (
                            <Button type="button" variant="outline" size="sm" onClick={() => setExportOpen(true)}>
                                <DownloadIcon className="size-3.5" /> Export
                            </Button>
                        )}
                        {canCreateDefinition && (
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setImportOpen(true)}
                                disabled={IMPORT_AND_PROMOTE_UNAVAILABLE || isKubernetesManaged}
                            >
                                <FileUpIcon className="size-3.5" /> Import
                            </Button>
                        )}
                        {canCreateDefinition && api?.type !== 'NATIVE' && (
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setDuplicateOpen(true)}
                                disabled={isKubernetesManaged}
                            >
                                <CopyIcon className="size-3.5" /> Duplicate
                            </Button>
                        )}
                        {canEditDefinition && (
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setPromoteOpen(true)}
                                disabled={IMPORT_AND_PROMOTE_UNAVAILABLE || isKubernetesManaged || api?.lifecycleState === 'DEPRECATED'}
                            >
                                <ExternalLinkIcon className="size-3.5" /> Promote
                            </Button>
                        )}
                    </div>
                </CardContent>
            </Card>

            {/* ── API Events ──────────────────────────────────────────────── */}
            {(canEditDefinition || canDeleteDefinition) && (
                <Card>
                    <CardContent className="pt-5 pb-5">
                        <div className="space-y-4">
                            <div>
                                <p className="text-sm font-semibold">API Events</p>
                                <p className="text-xs text-muted-foreground mt-0.5">
                                    These actions alter the runtime state of your API on the gateway.
                                </p>
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                                {canEditDefinition && (
                                    <button
                                        type="button"
                                        className={cn(
                                            'flex items-center gap-3 rounded-lg border p-4 text-left transition-colors',
                                            isReadOnly || startMutation.isPending || stopMutation.isPending
                                                ? 'cursor-not-allowed opacity-50'
                                                : 'cursor-pointer hover:bg-muted/50',
                                        )}
                                        onClick={() => !isReadOnly && (apiStarted ? stopMutation.mutate() : startMutation.mutate())}
                                        disabled={isReadOnly || startMutation.isPending || stopMutation.isPending}
                                    >
                                        <div className={cn('shrink-0 rounded-lg p-2', apiStarted ? 'bg-warning/10' : 'bg-success/10')}>
                                            {apiStarted ? (
                                                <CircleStopIcon className="size-5 text-warning" />
                                            ) : (
                                                <PlayIcon className="size-5 text-success" />
                                            )}
                                        </div>
                                        <div>
                                            <p className="text-sm font-medium">
                                                {apiStarted
                                                    ? stopMutation.isPending
                                                        ? 'Stopping…'
                                                        : 'Stop API'
                                                    : startMutation.isPending
                                                      ? 'Starting…'
                                                      : 'Start API'}
                                            </p>
                                            <p className="text-xs text-muted-foreground">
                                                {apiStarted
                                                    ? 'Gateway stops accepting requests. Subscriptions are preserved.'
                                                    : 'Start the API and make it available on all connected gateways.'}
                                            </p>
                                        </div>
                                    </button>
                                )}
                                {canDeleteDefinition && (
                                    <button
                                        type="button"
                                        className={cn(
                                            'flex items-center gap-3 rounded-lg border p-4 text-left transition-colors',
                                            cannotDelete ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-destructive/5',
                                        )}
                                        onClick={() => !cannotDelete && setDeleteOpen(true)}
                                        disabled={cannotDelete}
                                    >
                                        <div className="shrink-0 rounded-lg p-2 bg-destructive/10">
                                            <Trash2Icon className="size-5 text-destructive" />
                                        </div>
                                        <div>
                                            <p className="text-sm font-medium text-destructive">Delete this API</p>
                                            <p className="text-xs text-muted-foreground">
                                                {cannotDelete
                                                    ? 'A running or published API cannot be deleted.'
                                                    : 'Permanently removes the API, all plans, subscriptions, and analytics data.'}
                                            </p>
                                        </div>
                                    </button>
                                )}
                            </div>
                            {startStopError && <p className="text-sm text-destructive">{startStopError}</p>}
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* ── Action sheets & dialogs ─────────────────────────────────── */}
            <ExportApi
                open={exportOpen}
                onOpenChange={open => {
                    setExportOpen(open);
                    if (!open) setExportError(null);
                }}
                onExport={(tab, include) => void handleExport(tab, include)}
                isExporting={isExporting}
                error={exportError}
            />
            <ImportDialog
                open={importOpen}
                onOpenChange={setImportOpen}
                onImport={def => importMutation.mutate(def)}
                isLoading={importMutation.isPending}
                error={importError}
            />
            <DuplicateApi
                open={duplicateOpen}
                onOpenChange={setDuplicateOpen}
                initialVersion={api?.apiVersion ?? ''}
                entryMode={duplicateEntryMode}
                contextPathPlaceholder={extractContextPathPlaceholder(api)}
                hostPlaceholder={extractHostPlaceholder(api)}
                onDuplicate={opts => duplicateMutation.mutate(opts)}
                isLoading={duplicateMutation.isPending}
                error={duplicateError}
            />
            <PromoteDialog open={promoteOpen} onOpenChange={setPromoteOpen} />
            <DeleteDialog
                open={deleteOpen}
                onOpenChange={setDeleteOpen}
                apiName={api?.name ?? ''}
                onDelete={() => deleteMutation.mutate()}
                isLoading={deleteMutation.isPending}
                error={deleteError}
            />
        </div>
    );
}

// ─── Small local helpers ──────────────────────────────────────────────────────

function DetailRow({ label, value }: Readonly<{ label: React.ReactNode; value: string }>) {
    return (
        <div className="flex items-center justify-between gap-2">
            <dt className="text-muted-foreground flex items-center gap-1.5 shrink-0 text-xs">{label}</dt>
            <dd className="text-right text-xs font-medium truncate">{value}</dd>
        </div>
    );
}

function StatusBadge({ state }: Readonly<{ state?: string }>) {
    if (state === 'STARTED') {
        return (
            <Badge variant="outline" className="gap-1 border-success/20 text-success" style={{ fontSize: '10px' }}>
                <div className="size-1.5 rounded-full bg-success" /> Started
            </Badge>
        );
    }
    if (state === 'STOPPED') {
        return (
            <Badge variant="outline" className="gap-1 text-warning" style={{ fontSize: '10px', borderColor: 'var(--color-warning)' }}>
                <div className="size-1.5 rounded-full" style={{ backgroundColor: 'var(--color-warning)' }} /> Stopped
            </Badge>
        );
    }
    return (
        <Badge variant="secondary" style={{ fontSize: '10px' }}>
            {state ?? '—'}
        </Badge>
    );
}
