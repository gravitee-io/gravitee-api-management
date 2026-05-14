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
import {
    Badge,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    DataTablePagination,
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Input,
    Label,
    Skeleton,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Textarea,
} from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    LockIcon,
    MoreHorizontalIcon,
    PlusIcon,
    RefreshCwIcon,
    SearchIcon,
    ServerIcon,
    SettingsIcon,
    SparklesIcon,
    Trash2Icon,
    UploadIcon,
    ZapIcon,
} from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { useApiDetail } from '../features/apis/hooks/useApiDetail';
import type { Property } from '../features/apis/types/api';
import { parsePropertiesStringFormat } from '../features/apis/utils/propertiesParser';
import { apiDetailKeys } from '../features/apis/utils/queryKeys';
import { updateApiProperties } from '../services/apis/apis';

// ─── Types ────────────────────────────────────────────────────────────────────

type DialogState = null | { type: 'add' } | { type: 'edit'; property: Property };

// ─── Empty landing ────────────────────────────────────────────────────────────

function PropertiesEmptyLanding() {
    return (
        <Card>
            <CardContent className="pt-6 space-y-6">
                <div>
                    <h3 className="text-base font-semibold">Why define properties?</h3>
                    <p className="text-xs text-muted-foreground mt-0.5">
                        Properties let you externalize configuration — timeouts, feature flags, secrets — so policies can read them at
                        runtime without hardcoding values or redeploying the API.
                    </p>
                </div>

                <div className="rounded-xl p-5 space-y-3 bg-primary/10" style={{ border: '2px solid hsl(var(--primary))' }}>
                    <p className="text-xs font-semibold text-primary">How it works</p>
                    <div className="flex flex-wrap items-center justify-center gap-3">
                        <div className="flex flex-col items-center text-center">
                            <div className="rounded-lg bg-muted p-2">
                                <SettingsIcon className="size-4 text-muted-foreground" />
                            </div>
                            <p className="text-xs font-medium mt-1">Properties store</p>
                        </div>
                        <ArrowRightIcon className="size-4 text-muted-foreground" />
                        <div className="flex flex-col items-center text-center rounded-lg border border-border px-3 py-2 bg-card">
                            <div className="rounded-lg bg-primary/10 p-1.5">
                                <SparklesIcon className="size-4 text-primary" />
                            </div>
                            <p className="text-xs font-semibold mt-1">Policy engine</p>
                        </div>
                        <ArrowRightIcon className="size-4 text-muted-foreground" />
                        <div className="flex flex-col items-center text-center">
                            <div className="rounded-lg bg-muted p-2">
                                <ServerIcon className="size-4 text-muted-foreground" />
                            </div>
                            <p className="text-xs font-medium mt-1">Gateway config</p>
                        </div>
                    </div>
                </div>

                <ul className="space-y-1.5 text-xs text-muted-foreground">
                    <li className="flex items-center gap-1.5">
                        <LockIcon className="size-3 shrink-0 text-success" />
                        Externalize secrets and sensitive values with encryption
                    </li>
                    <li className="flex items-center gap-1.5">
                        <RefreshCwIcon className="size-3 shrink-0 text-success" />
                        Change configuration without redeploying the API
                    </li>
                    <li className="flex items-center gap-1.5">
                        <SparklesIcon className="size-3 shrink-0 text-success" />
                        Dynamic properties auto-synced from external sources
                    </li>
                </ul>
            </CardContent>
        </Card>
    );
}

// ─── Stats cards ──────────────────────────────────────────────────────────────

function StatsCards({ total, encrypted, dynamic }: { total: number; encrypted: number; dynamic: number }) {
    return (
        <div className="grid grid-cols-3 gap-4">
            <Card>
                <CardContent className="pt-5 pb-4">
                    <p className="text-2xl font-semibold">{total}</p>
                    <p className="text-sm text-muted-foreground mt-0.5">Total properties</p>
                </CardContent>
            </Card>
            <Card>
                <CardContent className="pt-5 pb-4">
                    <p className="text-2xl font-semibold">{encrypted}</p>
                    <p className="text-sm text-muted-foreground mt-0.5">Encrypted</p>
                </CardContent>
            </Card>
            <Card>
                <CardContent className="pt-5 pb-4">
                    <p className="text-2xl font-semibold">{dynamic}</p>
                    <p className="text-sm text-muted-foreground mt-0.5">Dynamic (auto-synced)</p>
                </CardContent>
            </Card>
        </div>
    );
}

// ─── Add / Edit Dialog ────────────────────────────────────────────────────────

interface PropertyDialogProps {
    state: NonNullable<DialogState>;
    existingKeys: string[];
    isSaving: boolean;
    onClose: () => void;
    onSave: (property: Property) => void;
}

function PropertyDialog({ state, existingKeys, isSaving, onClose, onSave }: PropertyDialogProps) {
    const isAdd = state.type === 'add';
    const editProp = state.type === 'edit' ? state.property : null;
    const isEditEncrypted = !!editProp?.encrypted;

    const [key, setKey] = useState(editProp?.key ?? '');
    const [value, setValue] = useState(isEditEncrypted ? '' : (editProp?.value ?? ''));
    const [encryptOnSave, setEncryptOnSave] = useState(false);

    const keyTrimmed = key.trim();
    const valueTrimmed = value.trim();

    const keyError = useMemo(() => {
        if (isAdd && keyTrimmed && existingKeys.includes(keyTrimmed)) return 'A property with this key already exists.';
        return null;
    }, [isAdd, keyTrimmed, existingKeys]);

    const canSave = isAdd ? keyTrimmed.length > 0 && valueTrimmed.length > 0 && !keyError : !isEditEncrypted || valueTrimmed.length > 0;

    const handleSave = () => {
        if (isAdd) {
            onSave({ key: keyTrimmed, value, encryptable: encryptOnSave });
        } else if (editProp) {
            if (isEditEncrypted) {
                onSave({ ...editProp, value, encrypted: false, encryptable: true });
            } else {
                onSave({ ...editProp, value });
            }
        }
    };

    const title = isAdd ? 'Add property' : 'Edit property';
    const saveLabel = isAdd ? 'Add property' : 'Save';

    return (
        <Dialog open onOpenChange={open => !open && onClose()}>
            <DialogContent className="max-w-md w-full">
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                </DialogHeader>

                <div className="space-y-5 pt-3">
                    {isAdd ? (
                        <div className="space-y-2">
                            <Label htmlFor="prop-key">Key</Label>
                            <Input id="prop-key" placeholder="e.g. backend.timeout" value={key} onChange={e => setKey(e.target.value)} />
                            {keyError ? <p className="text-xs text-destructive">{keyError}</p> : null}
                        </div>
                    ) : (
                        <div className="space-y-2">
                            <p className="text-xs font-medium text-muted-foreground">Key</p>
                            <p className="text-sm font-medium font-mono">{editProp?.key}</p>
                        </div>
                    )}

                    <div className="space-y-2">
                        <Label htmlFor="prop-value">Value</Label>
                        {isEditEncrypted ? (
                            <p className="text-xs text-muted-foreground">Enter a new plaintext value. It will be re-encrypted on save.</p>
                        ) : null}
                        <Input
                            id="prop-value"
                            placeholder="e.g. 5000"
                            type={isAdd && encryptOnSave ? 'password' : 'text'}
                            value={value}
                            onChange={e => setValue(e.target.value)}
                        />
                    </div>

                    {isAdd ? (
                        <div className="rounded-lg border border-border p-3 space-y-2">
                            <div className="flex items-center justify-between gap-3">
                                <Label htmlFor="prop-encrypt-toggle" className="text-sm font-medium">
                                    Encrypt on save
                                </Label>
                                <Switch id="prop-encrypt-toggle" checked={encryptOnSave} onCheckedChange={setEncryptOnSave} />
                            </div>
                            <p className="text-xs text-muted-foreground">
                                The value will be encrypted on save, and cannot be retrieved later on.
                            </p>
                        </div>
                    ) : null}
                </div>

                <DialogFooter>
                    <Button variant="outline" size="sm" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button size="sm" onClick={handleSave} disabled={isSaving || !canSave}>
                        {isSaving ? 'Saving…' : saveLabel}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

// ─── Import properties dialog ─────────────────────────────────────────────────

interface ImportPropertiesDialogProps {
    existingProperties: Property[];
    isSaving: boolean;
    onClose: () => void;
    onImport: (merged: Property[]) => void;
}

function ImportPropertiesDialog({ existingProperties, isSaving, onClose, onImport }: ImportPropertiesDialogProps) {
    const [raw, setRaw] = useState('');

    const parsed = useMemo(() => parsePropertiesStringFormat(raw), [raw]);

    const overwrittenKeys = useMemo(() => {
        const existingNotEncryptedSet = new Set(existingProperties.filter(p => !p.encrypted).map(p => p.key));
        return parsed.properties.map(p => p.key).filter(k => existingNotEncryptedSet.has(k));
    }, [parsed.properties, existingProperties]);

    const skippedKeys = useMemo(() => {
        const existingEncryptedSet = new Set(existingProperties.filter(p => p.encrypted).map(p => p.key));
        return parsed.properties.map(p => p.key).filter(k => existingEncryptedSet.has(k));
    }, [parsed.properties, existingProperties]);

    const hasErrors = parsed.errors.length > 0;
    const hasValidProperties = parsed.properties.length > 0;

    const handleImport = () => {
        const encryptedKeys = new Set(existingProperties.filter(p => p.encrypted).map(p => p.key));
        const toImport = parsed.properties.filter(p => !encryptedKeys.has(p.key));
        const toImportKeys = new Set(toImport.map(i => i.key));
        const toKeep = existingProperties.filter(p => !toImportKeys.has(p.key));
        onImport([...toKeep, ...toImport].sort((a, b) => a.key.localeCompare(b.key)));
    };

    return (
        <Dialog open onOpenChange={open => !open && onClose()}>
            <DialogContent className="max-w-xl w-full">
                <DialogHeader>
                    <DialogTitle>Import properties</DialogTitle>
                </DialogHeader>

                <div className="space-y-4 pt-1">
                    <p className="text-sm text-muted-foreground">
                        Easily import your API property list. If the properties already exist, their values will be overwritten. If the
                        properties already exist and are encrypted, the import will be skipped.
                    </p>

                    <Textarea
                        value={raw}
                        onChange={e => setRaw(e.target.value)}
                        placeholder={'NEW_PROPERTY_KEY="New property value"\nANOTHER_KEY=some_value'}
                        className="font-mono resize-y text-sm min-h-52"
                    />

                    {hasErrors ? (
                        <div
                            className="rounded-lg p-3 space-y-1"
                            style={{ background: 'hsl(var(--destructive) / 0.08)', border: '1px solid hsl(var(--destructive) / 0.3)' }}
                        >
                            <p className="text-xs font-semibold text-destructive">Errors in properties</p>
                            {parsed.errors.map((err, i) => (
                                <p key={i} className="text-xs text-destructive">
                                    {err}
                                </p>
                            ))}
                        </div>
                    ) : null}

                    {!hasErrors && (overwrittenKeys.length > 0 || skippedKeys.length > 0) ? (
                        <div
                            className="rounded-lg p-3 space-y-1"
                            style={{ background: 'hsl(var(--warning) / 0.08)', border: '1px solid hsl(var(--warning) / 0.3)' }}
                        >
                            <p className="text-xs font-semibold text-warning">Conflicts with existing properties</p>
                            {overwrittenKeys.length > 0 ? (
                                <p className="text-xs text-muted-foreground">
                                    Overwritten keys: <span className="font-mono">{overwrittenKeys.join(', ')}</span>
                                </p>
                            ) : null}
                            {skippedKeys.length > 0 ? (
                                <p className="text-xs text-muted-foreground">
                                    Skipped keys (encrypted): <span className="font-mono">{skippedKeys.join(', ')}</span>
                                </p>
                            ) : null}
                        </div>
                    ) : null}
                </div>

                <DialogFooter>
                    <Button variant="outline" size="sm" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button size="sm" onClick={handleImport} disabled={isSaving || hasErrors || !hasValidProperties}>
                        {isSaving ? 'Importing…' : 'Import properties'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

const PAGE_SIZE_OPTIONS = [10, 25, 50];

export function ApiPropertiesPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const canEdit = useHasPermission({ anyOf: ['api-definition-u'] });
    const { data: api, isLoading, isError } = useApiDetail(apiId);
    const properties = useMemo<Property[]>(() => api?.properties ?? [], [api?.properties]);

    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [dialog, setDialog] = useState<DialogState>(null);
    const [showImportDialog, setShowImportDialog] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);

    const mutation = useMutation({
        mutationFn: (newProps: Property[]) => updateApiProperties(env!.id, apiId!, newProps),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
            setDialog(null);
            setShowImportDialog(false);
            setSaveError(null);
            setPage(1);
        },
        onError: (e: Error) => {
            setSaveError(e.message || 'Failed to save changes.');
        },
    });

    const filtered = useMemo(() => {
        if (!search.trim()) return properties;
        const q = search.toLowerCase();
        return properties.filter(p => p.key.toLowerCase().includes(q));
    }, [properties, search]);

    const totalCount = filtered.length;
    const pageStart = (page - 1) * pageSize;
    const paginated = filtered.slice(pageStart, pageStart + pageSize);

    const encryptedCount = properties.filter(p => p.encrypted).length;
    const dynamicCount = properties.filter(p => p.dynamic).length;

    const existingKeys = useMemo(() => properties.map(p => p.key), [properties]);

    const handlePageSizeChange = useCallback((size: number) => {
        setPageSize(size);
        setPage(1);
    }, []);

    const handleSearchChange = useCallback((val: string) => {
        setSearch(val);
        setPage(1);
    }, []);

    const handleSaveProperty = useCallback(
        (newProp: Property) => {
            if (dialog?.type === 'add') {
                const updated = [...properties, newProp].sort((a, b) => a.key.localeCompare(b.key));
                mutation.mutate(updated);
            } else if (dialog?.type === 'edit') {
                const updated = properties.map(p => (p.key === dialog.property.key ? newProp : p));
                mutation.mutate(updated);
            }
        },
        [dialog, mutation, properties],
    );

    const handleDelete = useCallback(
        (propKey: string) => {
            mutation.mutate(properties.filter(p => p.key !== propKey));
        },
        [mutation, properties],
    );

    const handleEncryptValue = useCallback(
        (propKey: string) => {
            mutation.mutate(properties.map(p => (p.key === propKey ? { ...p, encryptable: true } : p)));
        },
        [mutation, properties],
    );

    const paginationBar =
        properties.length > 0 ? (
            <DataTablePagination
                page={page}
                pageSize={pageSize}
                totalCount={totalCount}
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                onPageChange={setPage}
                onPageSizeChange={handlePageSizeChange}
            />
        ) : null;

    if (isLoading) {
        return (
            <div className="space-y-6 p-6">
                <div className="space-y-2">
                    <Skeleton className="h-8 w-48 rounded" />
                    <Skeleton className="h-4 w-72 rounded" />
                </div>
                <div className="grid grid-cols-3 gap-4">
                    {[1, 2, 3].map(i => (
                        <Skeleton key={i} className="h-20 rounded-xl" />
                    ))}
                </div>
                <Skeleton className="h-10 w-full rounded-lg" />
                <Skeleton className="h-48 w-full rounded-xl" />
            </div>
        );
    }

    if (isError) {
        return (
            <div className="p-6">
                <Card className="border-destructive/30">
                    <CardContent className="pt-4 pb-4">
                        <p className="text-sm text-destructive">Failed to load API properties. Please try again.</p>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="space-y-6 p-6">
            {/* ─ Header: always visible regardless of empty/non-empty ─ */}
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">API Properties</h1>
                    <p className="text-sm text-muted-foreground">
                        {properties.length === 0 ? (
                            'Define key/value pairs accessible from policies at runtime.'
                        ) : (
                            <>
                                Define key/value pairs accessible from policies via{' '}
                                <code className="text-xs font-mono bg-muted px-1 py-0.5 rounded">{"{#api.properties['key']}"}</code>
                            </>
                        )}
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    {canEdit ? (
                        <Button variant="outline" size="sm" onClick={() => setShowImportDialog(true)}>
                            <UploadIcon className="size-4" />
                            Import
                        </Button>
                    ) : null}
                    <Button variant="outline" size="sm" disabled>
                        <ZapIcon className="size-4" />
                        Manage dynamically
                    </Button>
                    {canEdit ? (
                        <Button size="sm" onClick={() => setDialog({ type: 'add' })}>
                            <PlusIcon className="size-4" />
                            Add property
                        </Button>
                    ) : null}
                </div>
            </div>

            {saveError ? (
                <Card className="border-destructive/30">
                    <CardContent className="pt-4 pb-4">
                        <p className="text-sm text-destructive">{saveError}</p>
                    </CardContent>
                </Card>
            ) : null}

            {/* ─ Body: empty landing or stats + table ─ */}
            {properties.length === 0 ? (
                <PropertiesEmptyLanding />
            ) : (
                <>
                    <StatsCards total={properties.length} encrypted={encryptedCount} dynamic={dynamicCount} />

                    <div className="flex items-center gap-4">
                        <div className="relative flex-1">
                            <SearchIcon
                                className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground pointer-events-none"
                                aria-hidden
                            />
                            <Input
                                placeholder="Search properties by key…"
                                value={search}
                                onChange={e => handleSearchChange(e.target.value)}
                                className="pl-10"
                            />
                        </div>
                        {paginationBar}
                    </div>

                    <Card>
                        <CardHeader className="pb-2">
                            <CardTitle className="text-base">Defined properties</CardTitle>
                            <CardDescription>
                                {search.trim()
                                    ? `${filtered.length} of ${properties.length} properties`
                                    : `${properties.length} properties`}
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="p-0">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead style={{ width: '30%' }}>Key</TableHead>
                                        <TableHead>Value</TableHead>
                                        <TableHead>Characteristics</TableHead>
                                        <TableHead className="w-14" />
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {paginated.map(prop => (
                                        <TableRow key={prop.key}>
                                            <TableCell className="font-mono text-sm font-medium">{prop.key}</TableCell>
                                            <TableCell className="font-mono text-sm">
                                                {prop.encrypted ? (
                                                    <span className="text-muted-foreground">{'•'.repeat(8)}</span>
                                                ) : (
                                                    prop.value
                                                )}
                                            </TableCell>
                                            <TableCell>
                                                <div className="flex flex-wrap gap-1">
                                                    {prop.encrypted ? (
                                                        <Badge className="gap-1">
                                                            <LockIcon className="size-3" />
                                                            Encrypted
                                                        </Badge>
                                                    ) : prop.encryptable ? (
                                                        <Badge variant="secondary">Encrypt on save</Badge>
                                                    ) : (
                                                        <Badge variant="outline">Unencrypted</Badge>
                                                    )}
                                                    {prop.dynamic ? (
                                                        <Badge variant="outline" className="gap-1">
                                                            <RefreshCwIcon className="size-3" />
                                                            Dynamic
                                                        </Badge>
                                                    ) : null}
                                                </div>
                                            </TableCell>
                                            <TableCell>
                                                {canEdit ? (
                                                    <DropdownMenu>
                                                        <DropdownMenuTrigger asChild>
                                                            <Button variant="ghost" size="icon" className="size-8">
                                                                <MoreHorizontalIcon className="size-4" />
                                                                <span className="sr-only">Open actions</span>
                                                            </Button>
                                                        </DropdownMenuTrigger>
                                                        <DropdownMenuContent align="end" className="whitespace-nowrap min-w-48">
                                                            {!prop.dynamic ? (
                                                                <>
                                                                    {prop.encrypted ? (
                                                                        <DropdownMenuItem
                                                                            onSelect={() => setDialog({ type: 'edit', property: prop })}
                                                                        >
                                                                            <RefreshCwIcon className="size-4" />
                                                                            Renew encryption
                                                                        </DropdownMenuItem>
                                                                    ) : (
                                                                        <>
                                                                            <DropdownMenuItem
                                                                                onSelect={() => setDialog({ type: 'edit', property: prop })}
                                                                            >
                                                                                Edit value
                                                                            </DropdownMenuItem>
                                                                            <DropdownMenuItem
                                                                                onSelect={() => handleEncryptValue(prop.key)}
                                                                                disabled={mutation.isPending}
                                                                            >
                                                                                <LockIcon className="size-4" />
                                                                                Encrypt value
                                                                            </DropdownMenuItem>
                                                                        </>
                                                                    )}
                                                                    <DropdownMenuSeparator />
                                                                </>
                                                            ) : null}
                                                            <DropdownMenuItem
                                                                className="text-destructive"
                                                                onSelect={() => handleDelete(prop.key)}
                                                                disabled={mutation.isPending}
                                                            >
                                                                <Trash2Icon className="size-4" />
                                                                {prop.dynamic ? 'Remove (re-added on next sync)' : 'Delete'}
                                                            </DropdownMenuItem>
                                                        </DropdownMenuContent>
                                                    </DropdownMenu>
                                                ) : null}
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                    {paginated.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={4} className="text-center text-muted-foreground py-10">
                                                No properties match your search.
                                            </TableCell>
                                        </TableRow>
                                    ) : null}
                                </TableBody>
                            </Table>
                        </CardContent>
                    </Card>

                    {paginationBar}
                </>
            )}

            {dialog ? (
                <PropertyDialog
                    state={dialog}
                    existingKeys={existingKeys}
                    isSaving={mutation.isPending}
                    onClose={() => {
                        setDialog(null);
                        setSaveError(null);
                    }}
                    onSave={handleSaveProperty}
                />
            ) : null}

            {showImportDialog ? (
                <ImportPropertiesDialog
                    existingProperties={properties}
                    isSaving={mutation.isPending}
                    onClose={() => {
                        setShowImportDialog(false);
                        setSaveError(null);
                    }}
                    onImport={merged => mutation.mutate(merged)}
                />
            ) : null}
        </div>
    );
}
