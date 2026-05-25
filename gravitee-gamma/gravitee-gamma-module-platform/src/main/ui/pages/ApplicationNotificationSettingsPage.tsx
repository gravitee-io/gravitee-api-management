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
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Checkbox,
    DataTablePagination,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Skeleton,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { PencilIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';
import { useApplicationNotificationPermissions } from '../features/applications/hooks/useApplicationNotificationPermissions';
import {
    useCreateApplicationNotification,
    useApplicationMetadata,
    useApplicationNotifications,
    useCreateApplicationMetadata,
    useDeleteApplicationMetadata,
    useUpdateApplicationNotification,
    useUpdateApplicationMetadata,
} from '../features/applications/hooks/useApplicationNotifications';
import type {
    ApplicationMetadata,
    ApplicationMetadataFormat,
    ApplicationNotificationHookCategory,
    ApplicationNotificationRow,
    ApplicationNotifier,
    UpdateApplicationNotification,
    UpdateApplicationMetadata,
} from '../features/applications/types/applicationNotification';

const METADATA_FORMATS: ApplicationMetadataFormat[] = ['STRING', 'NUMERIC', 'BOOLEAN', 'DATE', 'MAIL', 'URL'];
const METADATA_PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
const DEFAULT_METADATA_PAGE_SIZE = 10;
const MAIL_PATTERN =
    /^((\${.+})|(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,})))$/;
const URL_PATTERN = /^((\$\{.+\})|(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})\b([-a-zA-Z0-9()@:%_+.~#?&//=]*))$/;

interface NotificationNotifierOption {
    readonly id: string;
    readonly label: string;
    readonly notifier: ApplicationNotifier;
}

function deriveMetadataKey(name: string): string {
    return name
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '');
}

function notifierTypeLabel(notifier: ApplicationNotifier): string {
    if (notifier.type === 'EMAIL') {
        return 'Default Email Notifier';
    }
    if (notifier.type === 'WEBHOOK') {
        return 'Default Webhook Notifier';
    }
    return notifier.name ?? notifier.id ?? 'Notifier';
}

function notificationNotifierOptions(notifiers: ApplicationNotifier[]): NotificationNotifierOption[] {
    return notifiers
        .filter((notifier): notifier is ApplicationNotifier & { id: string } =>
            Boolean(notifier.id && (notifier.type === 'EMAIL' || notifier.type === 'WEBHOOK')),
        )
        .map(notifier => ({ id: notifier.id, label: notifierTypeLabel(notifier), notifier }));
}

function RequiredLabel({ htmlFor, children }: Readonly<{ htmlFor: string; children: string }>) {
    return (
        <Label htmlFor={htmlFor}>
            {children}
            <span className="text-destructive"> *</span>
        </Label>
    );
}

function MetadataValueField({
    id,
    format,
    value,
    disabled,
    onChange,
}: Readonly<{
    id: string;
    format: ApplicationMetadataFormat;
    value: string;
    disabled: boolean;
    onChange: (value: string) => void;
}>) {
    if (format === 'BOOLEAN') {
        return (
            <Select value={value} onValueChange={onChange} disabled={disabled} required>
                <SelectTrigger id={id} className="w-full" aria-required="true">
                    <SelectValue placeholder="Select a value" />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value="true">true</SelectItem>
                    <SelectItem value="false">false</SelectItem>
                </SelectContent>
            </Select>
        );
    }

    const commonProps = {
        id,
        value,
        onChange: (event: React.ChangeEvent<HTMLInputElement>) => onChange(event.target.value),
        disabled,
        required: true,
        'aria-required': true,
    };

    if (format === 'NUMERIC') {
        return <Input {...commonProps} type="number" placeholder="123000.92" />;
    }
    if (format === 'DATE') {
        return <Input {...commonProps} type="date" />;
    }
    if (format === 'MAIL') {
        return <Input {...commonProps} type="email" pattern={MAIL_PATTERN.source} placeholder="john@doe.com" />;
    }
    if (format === 'URL') {
        return <Input {...commonProps} type="url" pattern={URL_PATTERN.source} placeholder="https://gravitee.io" />;
    }
    return <Input {...commonProps} type="text" placeholder="Operations" />;
}

function NotificationsSection({
    rows,
    notifiers,
    isLoading,
    isError,
    canCreate,
    canUpdate,
    isCreating,
    onAdd,
    onEdit,
}: {
    readonly rows: ApplicationNotificationRow[];
    readonly notifiers: ApplicationNotifier[];
    readonly isLoading: boolean;
    readonly isError: boolean;
    readonly canCreate: boolean;
    readonly canUpdate: boolean;
    readonly isCreating: boolean;
    readonly onAdd: (name: string, notifierId: string) => Promise<boolean>;
    readonly onEdit: (row: ApplicationNotificationRow) => void;
}) {
    const notifierOptions = useMemo(() => notificationNotifierOptions(notifiers), [notifiers]);
    const [name, setName] = useState('');
    const [notifierId, setNotifierId] = useState('');

    useEffect(() => {
        if (!notifierId && notifierOptions[0]) {
            setNotifierId(notifierOptions[0].id);
        }
    }, [notifierId, notifierOptions]);

    const canSubmit = Boolean(canCreate && name.trim() && notifierId);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!canSubmit) {
            return;
        }
        const created = await onAdd(name.trim(), notifierId);
        if (created) {
            setName('');
            setNotifierId(notifierOptions[0]?.id ?? '');
        }
    }

    return (
        <Card>
            <CardHeader>
                <CardTitle className="text-base">Notifications</CardTitle>
                <CardDescription>Events that trigger notifiers for this application.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                {canCreate ? (
                    <form className="space-y-3" onSubmit={handleSubmit}>
                        <div className="grid gap-3 md:grid-cols-2">
                            <div className="space-y-2">
                                <RequiredLabel htmlFor="notification-name">Name</RequiredLabel>
                                <Input
                                    id="notification-name"
                                    value={name}
                                    onChange={event => setName(event.target.value)}
                                    placeholder="Notification name"
                                    disabled={isCreating}
                                    required
                                    aria-required="true"
                                />
                            </div>
                            <div className="space-y-2">
                                <RequiredLabel htmlFor="notification-notifier">Notifier</RequiredLabel>
                                <Select
                                    value={notifierId}
                                    onValueChange={setNotifierId}
                                    disabled={isCreating || notifierOptions.length === 0}
                                >
                                    <SelectTrigger id="notification-notifier" className="w-full" aria-required="true">
                                        <SelectValue placeholder="Select a notifier" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {notifierOptions.map(option => (
                                            <SelectItem key={option.id} value={option.id}>
                                                {option.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>
                        <Button type="submit" size="sm" disabled={!canSubmit || isCreating}>
                            <PlusIcon className="size-4" aria-hidden />
                            {isCreating ? 'Adding…' : 'Add notification'}
                        </Button>
                    </form>
                ) : null}
                {isError ? (
                    <Alert variant="destructive">
                        <AlertDescription>Failed to load notification settings. Please refresh the page.</AlertDescription>
                    </Alert>
                ) : isLoading ? (
                    <div className="space-y-2">
                        {Array.from({ length: 3 }).map((_, index) => (
                            <Skeleton key={index} className="h-10 rounded-lg" />
                        ))}
                    </div>
                ) : (
                    <Table aria-label="Application notification settings">
                        <TableHeader>
                            <TableRow>
                                <TableHead>Name</TableHead>
                                <TableHead>Events subscribed</TableHead>
                                <TableHead>Notifier</TableHead>
                                <TableHead className="w-16 text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {rows.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={4} className="h-16 text-center text-sm text-muted-foreground">
                                        No notifications configured.
                                    </TableCell>
                                </TableRow>
                            ) : (
                                rows.map(row => (
                                    <TableRow key={row.key}>
                                        <TableCell className="font-medium">{row.name}</TableCell>
                                        <TableCell>
                                            <span className="inline-flex rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground">
                                                {row.subscribedEvents} events
                                            </span>
                                        </TableCell>
                                        <TableCell>
                                            <span className="inline-flex rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground">
                                                {row.notifierName}
                                            </span>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            {canUpdate ? (
                                                <Button
                                                    type="button"
                                                    variant="ghost"
                                                    size="icon"
                                                    className="size-8"
                                                    aria-label={`Edit ${row.name} notification`}
                                                    disabled={row.isReadonly}
                                                    onClick={() => onEdit(row)}
                                                >
                                                    <PencilIcon className="size-4" aria-hidden />
                                                </Button>
                                            ) : null}
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                )}
            </CardContent>
        </Card>
    );
}

function NotificationHookCategorySection({
    category,
    selectedHooks,
    groupHookIds,
    disabled,
    onToggle,
}: Readonly<{
    category: ApplicationNotificationHookCategory;
    selectedHooks: Set<string>;
    groupHookIds: Set<string>;
    disabled: boolean;
    onToggle: (hookId: string) => void;
}>) {
    return (
        <div className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{category.name}</p>
            <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
                {category.hooks.map(hook => {
                    const isGroupHook = groupHookIds.has(hook.id);
                    const isDisabled = disabled || isGroupHook;
                    return (
                        <label key={hook.id} className="flex items-start gap-2 rounded-md p-2">
                            <Checkbox
                                checked={selectedHooks.has(hook.id)}
                                onCheckedChange={checked => checked !== 'indeterminate' && !isDisabled && onToggle(hook.id)}
                                disabled={isDisabled}
                                className="mt-0.5 shrink-0"
                            />
                            <span className="min-w-0">
                                <span className="block text-sm font-medium leading-snug">{hook.label}</span>
                                {hook.description ? <span className="block text-xs text-muted-foreground">{hook.description}</span> : null}
                            </span>
                        </label>
                    );
                })}
            </div>
        </div>
    );
}

function EditNotificationDialog({
    row,
    hookCategories,
    isLoadingHooks,
    isSaving,
    onCancel,
    onSave,
}: Readonly<{
    row: ApplicationNotificationRow | null;
    hookCategories: ApplicationNotificationHookCategory[];
    isLoadingHooks: boolean;
    isSaving: boolean;
    onCancel: () => void;
    onSave: (notification: UpdateApplicationNotification) => void;
}>) {
    const notification = row?.notification ?? null;
    const notifier = row?.notifier;
    const groupHookIds = useMemo(() => new Set(notification?.groupHooks ?? []), [notification?.groupHooks]);
    const [selectedHooks, setSelectedHooks] = useState<Set<string>>(new Set());
    const [config, setConfig] = useState('');
    const [useSystemProxy, setUseSystemProxy] = useState(false);

    useEffect(() => {
        setSelectedHooks(new Set([...(notification?.hooks ?? []), ...(notification?.groupHooks ?? [])]));
        setConfig(notification?.config ?? '');
        setUseSystemProxy(Boolean(notification?.useSystemProxy));
    }, [notification]);

    function toggleHook(hookId: string) {
        setSelectedHooks(prev => {
            const next = new Set(prev);
            if (next.has(hookId)) {
                next.delete(hookId);
            } else {
                next.add(hookId);
            }
            return next;
        });
    }

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!notification || row?.isReadonly) {
            return;
        }
        onSave({
            ...notification,
            config,
            useSystemProxy,
            hooks: [...selectedHooks].filter(hookId => !groupHookIds.has(hookId)),
        });
    }

    const needsNotifierConfig = notifier?.type === 'EMAIL' || notifier?.type === 'WEBHOOK';
    const configLabel = notifier?.type === 'EMAIL' ? 'Email list' : 'Webhook';
    const configHelp =
        notifier?.type === 'EMAIL'
            ? "Use space, ',' or ';' to separate emails. EL supported."
            : 'URL (Gravitee will POST datas to this url)';
    const disabled = isSaving || Boolean(row?.isReadonly);

    return (
        <Dialog open={row !== null} onOpenChange={open => !open && onCancel()}>
            <DialogContent style={{ width: 'min(72rem, 98vw)', maxWidth: '98vw' }}>
                <DialogHeader>
                    <DialogTitle>Edit Console Notification</DialogTitle>
                </DialogHeader>
                <form className="space-y-5" onSubmit={handleSubmit}>
                    <div className="grid gap-4 lg:grid-cols-3">
                        {needsNotifierConfig ? (
                            <div className="space-y-2 lg:col-span-2">
                                <Label htmlFor="notification-config">{configLabel}</Label>
                                <Input
                                    id="notification-config"
                                    value={config}
                                    onChange={event => setConfig(event.target.value)}
                                    disabled={disabled}
                                />
                                <p className="text-xs text-muted-foreground">{configHelp}</p>
                            </div>
                        ) : null}
                        {notifier?.type === 'WEBHOOK' ? (
                            <div className="flex items-center justify-between gap-4 self-end rounded-lg border px-3 py-2">
                                <Label htmlFor="notification-system-proxy">Use system proxy</Label>
                                <Switch
                                    id="notification-system-proxy"
                                    checked={useSystemProxy}
                                    onCheckedChange={setUseSystemProxy}
                                    disabled={disabled}
                                />
                            </div>
                        ) : null}
                    </div>
                    <div className="space-y-4">
                        <h3 className="text-sm font-semibold">Event subscribed</h3>
                        {isLoadingHooks ? (
                            <div className="space-y-2">
                                {Array.from({ length: 3 }).map((_, index) => (
                                    <Skeleton key={index} className="h-16 rounded-lg" />
                                ))}
                            </div>
                        ) : (
                            hookCategories.map(category => (
                                <NotificationHookCategorySection
                                    key={category.name}
                                    category={category}
                                    selectedHooks={selectedHooks}
                                    groupHookIds={groupHookIds}
                                    disabled={disabled}
                                    onToggle={toggleHook}
                                />
                            ))
                        )}
                    </div>
                    <DialogFooter className="border-t px-6 py-4 gap-2">
                        <Button type="button" variant="outline" onClick={onCancel} disabled={isSaving}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={disabled || isLoadingHooks || !notification}>
                            {isSaving ? 'Saving…' : 'Save'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}

function MetadataTable({
    metadata,
    isLoading,
    canUpdate,
    canDelete,
    isMutating,
    onEdit,
    onDelete,
}: {
    readonly metadata: ApplicationMetadata[];
    readonly isLoading: boolean;
    readonly canUpdate: boolean;
    readonly canDelete: boolean;
    readonly isMutating: boolean;
    readonly onEdit: (metadata: ApplicationMetadata) => void;
    readonly onDelete: (metadata: ApplicationMetadata) => void;
}) {
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_METADATA_PAGE_SIZE);
    const totalCount = metadata.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
    const paginatedMetadata = useMemo(() => {
        const start = (page - 1) * pageSize;
        return metadata.slice(start, start + pageSize);
    }, [metadata, page, pageSize]);

    useEffect(() => {
        if (page > totalPages) {
            setPage(totalPages);
        }
    }, [page, totalPages]);

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    const hasActions = canUpdate || canDelete;

    if (isLoading) {
        return (
            <div className="space-y-2">
                {Array.from({ length: pageSize }).map((_, index) => (
                    <Skeleton key={index} className="h-10 rounded-lg" />
                ))}
            </div>
        );
    }

    return (
        <div className="space-y-4">
            <div className="flex justify-end">
                <DataTablePagination
                    page={page}
                    pageSize={pageSize}
                    totalCount={totalCount}
                    pageSizeOptions={METADATA_PAGE_SIZE_OPTIONS}
                    onPageChange={setPage}
                    onPageSizeChange={handlePageSizeChange}
                />
            </div>
            <Table aria-label="Application metadata">
                <TableHeader>
                    <TableRow>
                        <TableHead>Key</TableHead>
                        <TableHead>Name</TableHead>
                        <TableHead>Format</TableHead>
                        <TableHead>Value</TableHead>
                        {hasActions ? <TableHead className="w-24 text-right" aria-label="Actions" /> : null}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {metadata.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={hasActions ? 5 : 4} className="h-16 text-center text-sm text-muted-foreground">
                                No metadata configured.
                            </TableCell>
                        </TableRow>
                    ) : (
                        paginatedMetadata.map(item => {
                            const value = item.value ?? item.defaultValue ?? '—';
                            const isDeletable = item.value !== undefined;
                            return (
                                <TableRow key={item.key}>
                                    <TableCell className="font-medium">{item.key}</TableCell>
                                    <TableCell>{item.name}</TableCell>
                                    <TableCell className="text-sm text-muted-foreground">{item.format ?? 'STRING'}</TableCell>
                                    <TableCell>{value}</TableCell>
                                    {hasActions ? (
                                        <TableCell className="text-right">
                                            <div className="flex justify-end gap-1">
                                                {canUpdate ? (
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8"
                                                        aria-label={`Edit ${item.name} metadata`}
                                                        disabled={isMutating}
                                                        onClick={() => onEdit(item)}
                                                    >
                                                        <PencilIcon className="size-4" aria-hidden />
                                                    </Button>
                                                ) : null}
                                                {canDelete && isDeletable ? (
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8 text-destructive hover:text-destructive"
                                                        aria-label={`Delete ${item.name} metadata`}
                                                        disabled={isMutating}
                                                        onClick={() => onDelete(item)}
                                                    >
                                                        <Trash2Icon className="size-4" aria-hidden />
                                                    </Button>
                                                ) : null}
                                            </div>
                                        </TableCell>
                                    ) : null}
                                </TableRow>
                            );
                        })
                    )}
                </TableBody>
            </Table>
            <div className="flex justify-end">
                <DataTablePagination
                    page={page}
                    pageSize={pageSize}
                    totalCount={totalCount}
                    pageSizeOptions={METADATA_PAGE_SIZE_OPTIONS}
                    onPageChange={setPage}
                    onPageSizeChange={handlePageSizeChange}
                />
            </div>
        </div>
    );
}

function DeleteMetadataDialog({
    metadata,
    isDeleting,
    onCancel,
    onConfirm,
}: Readonly<{
    metadata: ApplicationMetadata | null;
    isDeleting: boolean;
    onCancel: () => void;
    onConfirm: () => void;
}>) {
    return (
        <Dialog open={metadata !== null} onOpenChange={open => !open && onCancel()}>
            <DialogContent className="max-w-sm">
                <DialogHeader>
                    <DialogTitle>Delete Application metadata</DialogTitle>
                    <DialogDescription>{`Are you sure you want to delete Application metadata '${metadata?.name}'?`}</DialogDescription>
                </DialogHeader>
                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <Button type="button" variant="outline" onClick={onCancel} disabled={isDeleting}>
                        Cancel
                    </Button>
                    <Button type="button" variant="destructive" onClick={onConfirm} disabled={isDeleting || metadata === null}>
                        {isDeleting ? 'Deleting…' : 'Delete'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

function metadataFormat(metadata: ApplicationMetadata | null): ApplicationMetadataFormat {
    return metadata?.format ?? 'STRING';
}

function metadataValue(metadata: ApplicationMetadata | null): string {
    if (!metadata) {
        return '';
    }
    const value = metadata.value ?? metadata.defaultValue ?? '';
    if (metadataFormat(metadata) === 'DATE') {
        return value.slice(0, 10);
    }
    return String(value);
}

function EditMetadataDialog({
    metadata,
    isSaving,
    onCancel,
    onSave,
}: Readonly<{
    metadata: ApplicationMetadata | null;
    isSaving: boolean;
    onCancel: () => void;
    onSave: (metadata: UpdateApplicationMetadata) => void;
}>) {
    const [name, setName] = useState('');
    const [value, setValue] = useState('');

    useEffect(() => {
        setName(metadata?.name ?? '');
        setValue(metadataValue(metadata));
    }, [metadata]);

    const format = metadataFormat(metadata);
    const initialName = metadata?.name ?? '';
    const initialValue = metadataValue(metadata);
    const hasChange = name !== initialName || value !== initialValue;
    const canSave = Boolean(metadata && name.trim() && value && hasChange);

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!metadata || !canSave) {
            return;
        }
        onSave({
            key: metadata.key,
            name: name.trim(),
            format,
            value,
            defaultValue: metadata.defaultValue,
        });
    }

    return (
        <Dialog open={metadata !== null} onOpenChange={open => !open && onCancel()}>
            <DialogContent className="max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Update Application metadata</DialogTitle>
                </DialogHeader>
                <form className="space-y-4" onSubmit={handleSubmit}>
                    <div className="grid gap-3 md:grid-cols-2">
                        <div className="space-y-2">
                            <RequiredLabel htmlFor="edit-metadata-key">Key</RequiredLabel>
                            <Input id="edit-metadata-key" value={metadata?.key ?? ''} disabled required aria-required="true" />
                        </div>
                        <div className="space-y-2">
                            <RequiredLabel htmlFor="edit-metadata-name">Name</RequiredLabel>
                            <Input
                                id="edit-metadata-name"
                                value={name}
                                onChange={event => setName(event.target.value)}
                                disabled={isSaving}
                                required
                                aria-required="true"
                            />
                        </div>
                        <div className="space-y-2">
                            <RequiredLabel htmlFor="edit-metadata-format">Format</RequiredLabel>
                            <Select value={format} disabled required>
                                <SelectTrigger id="edit-metadata-format" className="w-full" aria-required="true">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {METADATA_FORMATS.map(item => (
                                        <SelectItem key={item} value={item}>
                                            {item.toLowerCase()}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="space-y-2">
                            <RequiredLabel htmlFor="edit-metadata-value">Value</RequiredLabel>
                            <MetadataValueField
                                id="edit-metadata-value"
                                format={format}
                                value={value}
                                disabled={isSaving}
                                onChange={setValue}
                            />
                        </div>
                    </div>
                    <DialogFooter className="border-t px-6 py-4 gap-2">
                        <Button type="button" variant="outline" onClick={onCancel} disabled={isSaving}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!canSave || isSaving}>
                            {isSaving ? 'Saving…' : 'Save'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}

export function ApplicationNotificationSettingsPage() {
    const { applicationId } = useParams<{ applicationId: string }>();
    const { application } = useApplicationDetailContext();
    const { canCreateNotification, canUpdateNotification, canCreateMetadata, canUpdateMetadata, canDeleteMetadata } =
        useApplicationNotificationPermissions();

    const {
        rows,
        notifiers,
        hookCategories,
        isLoading: notificationsLoading,
        isLoadingHooks,
        isError: notificationsError,
    } = useApplicationNotifications(applicationId);
    const { data: metadata = [], isLoading: metadataLoading, isError: metadataError } = useApplicationMetadata(applicationId);
    const createNotificationMutation = useCreateApplicationNotification(applicationId);
    const updateNotificationMutation = useUpdateApplicationNotification(applicationId);
    const createMetadataMutation = useCreateApplicationMetadata(applicationId);
    const updateMetadataMutation = useUpdateApplicationMetadata(applicationId);
    const deleteMetadataMutation = useDeleteApplicationMetadata(applicationId);

    const [notificationToEdit, setNotificationToEdit] = useState<ApplicationNotificationRow | null>(null);
    const [metadataName, setMetadataName] = useState('');
    const [metadataFormat, setMetadataFormat] = useState<ApplicationMetadataFormat>('STRING');
    const [metadataValue, setMetadataValue] = useState('');
    const [metadataToEdit, setMetadataToEdit] = useState<ApplicationMetadata | null>(null);
    const [metadataToDelete, setMetadataToDelete] = useState<ApplicationMetadata | null>(null);

    const metadataKey = useMemo(() => deriveMetadataKey(metadataName), [metadataName]);
    const isReadOnly = application?.origin === 'KUBERNETES';
    const canAddNotification = canCreateNotification && !isReadOnly;
    const canEditNotification = canUpdateNotification && !isReadOnly;
    const canAddMetadata = canCreateMetadata && !isReadOnly;
    const canEditMetadata = canUpdateMetadata && !isReadOnly;
    const canRemoveMetadata = canDeleteMetadata && !isReadOnly;
    const canSubmitMetadata =
        canAddMetadata && metadataKey.length > 0 && metadataName.trim().length > 0 && metadataFormat.length > 0 && metadataValue.length > 0;

    function handleAddMetadata(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!canSubmitMetadata) {
            return;
        }
        createMetadataMutation.mutate(
            { name: metadataName.trim(), format: metadataFormat, value: metadataValue },
            {
                onSuccess: () => {
                    setMetadataName('');
                    setMetadataFormat('STRING');
                    setMetadataValue('');
                },
            },
        );
    }

    async function handleAddNotification(name: string, notifierId: string): Promise<boolean> {
        if (!applicationId) {
            return false;
        }
        try {
            const created = await createNotificationMutation.mutateAsync({
                name,
                notifier: notifierId,
                referenceType: 'APPLICATION',
                referenceId: applicationId,
                config_type: 'GENERIC',
                hooks: [],
            });
            const notifier = notifiers.find(item => item.id === created.notifier);
            setNotificationToEdit({
                key: created.id ?? created.config_type,
                name: created.name,
                subscribedEvents: (created.hooks ?? []).length + (created.groupHooks ?? []).length,
                notifierName: notifier ? notifierTypeLabel(notifier) : (created.notifier ?? '—'),
                notification: created,
                notifier,
                isReadonly: Boolean(created.origin && created.origin !== 'MANAGEMENT'),
            });
            return true;
        } catch {
            return false;
        }
    }

    function handleUpdateNotification(notification: UpdateApplicationNotification) {
        updateNotificationMutation.mutate(notification, {
            onSuccess: () => setNotificationToEdit(null),
        });
    }

    function handleDeleteMetadataConfirm() {
        if (!metadataToDelete) {
            return;
        }
        deleteMetadataMutation.mutate(metadataToDelete.key, {
            onSuccess: () => setMetadataToDelete(null),
        });
    }

    function handleUpdateMetadata(updatedMetadata: UpdateApplicationMetadata) {
        updateMetadataMutation.mutate(updatedMetadata, {
            onSuccess: () => setMetadataToEdit(null),
        });
    }

    const mutationError =
        createNotificationMutation.error ??
        updateNotificationMutation.error ??
        createMetadataMutation.error ??
        updateMetadataMutation.error ??
        deleteMetadataMutation.error;
    const mutationErrorMessage = mutationError instanceof Error ? mutationError.message : mutationError ? String(mutationError) : null;

    return (
        <div className="space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Notification settings</h1>
                <p className="text-sm text-muted-foreground">Hooks and metadata used in notification templates.</p>
            </div>

            <NotificationsSection
                rows={rows}
                notifiers={notifiers}
                isLoading={notificationsLoading}
                isError={notificationsError}
                canCreate={canAddNotification}
                canUpdate={canEditNotification}
                isCreating={createNotificationMutation.isPending}
                onAdd={handleAddNotification}
                onEdit={setNotificationToEdit}
            />

            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Metadata</CardTitle>
                    <CardDescription>Custom metadata available in notification templates (classic console pattern).</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                    {metadataError ? (
                        <Alert variant="destructive">
                            <AlertDescription>Failed to load metadata. Please refresh the page.</AlertDescription>
                        </Alert>
                    ) : null}
                    {mutationErrorMessage ? (
                        <Alert variant="destructive">
                            <AlertDescription>{mutationErrorMessage}</AlertDescription>
                        </Alert>
                    ) : null}

                    {canAddMetadata ? (
                        <form className="space-y-3" onSubmit={handleAddMetadata}>
                            <div className="grid gap-3 md:grid-cols-4">
                                <div className="space-y-2">
                                    <RequiredLabel htmlFor="metadata-key">Key</RequiredLabel>
                                    <Input
                                        id="metadata-key"
                                        value={metadataKey}
                                        placeholder="department"
                                        readOnly
                                        required
                                        aria-required="true"
                                        className="bg-muted/40"
                                    />
                                </div>
                                <div className="space-y-2">
                                    <RequiredLabel htmlFor="metadata-name">Name</RequiredLabel>
                                    <Input
                                        id="metadata-name"
                                        value={metadataName}
                                        onChange={event => setMetadataName(event.target.value)}
                                        placeholder="Department"
                                        disabled={createMetadataMutation.isPending}
                                        required
                                        aria-required="true"
                                    />
                                </div>
                                <div className="space-y-2">
                                    <RequiredLabel htmlFor="metadata-format">Format</RequiredLabel>
                                    <Select
                                        value={metadataFormat}
                                        onValueChange={value => {
                                            const nextFormat = value as ApplicationMetadataFormat;
                                            setMetadataFormat(nextFormat);
                                            setMetadataValue(nextFormat === 'BOOLEAN' ? 'false' : '');
                                        }}
                                        disabled={createMetadataMutation.isPending}
                                        required
                                    >
                                        <SelectTrigger id="metadata-format" className="w-full" aria-required="true">
                                            <SelectValue placeholder="Select a format" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {METADATA_FORMATS.map(format => (
                                                <SelectItem key={format} value={format}>
                                                    {format.toLowerCase()}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="space-y-2">
                                    <RequiredLabel htmlFor="metadata-value">Value</RequiredLabel>
                                    <MetadataValueField
                                        id="metadata-value"
                                        format={metadataFormat}
                                        value={metadataValue}
                                        disabled={createMetadataMutation.isPending}
                                        onChange={setMetadataValue}
                                    />
                                </div>
                            </div>
                            <Button type="submit" size="sm" disabled={!canSubmitMetadata || createMetadataMutation.isPending}>
                                <PlusIcon className="size-4" aria-hidden />
                                {createMetadataMutation.isPending ? 'Adding…' : 'Add metadata'}
                            </Button>
                        </form>
                    ) : null}

                    <MetadataTable
                        metadata={metadata}
                        isLoading={metadataLoading}
                        canUpdate={canEditMetadata}
                        canDelete={canRemoveMetadata}
                        isMutating={updateMetadataMutation.isPending || deleteMetadataMutation.isPending}
                        onEdit={setMetadataToEdit}
                        onDelete={setMetadataToDelete}
                    />
                </CardContent>
            </Card>
            <DeleteMetadataDialog
                metadata={metadataToDelete}
                isDeleting={deleteMetadataMutation.isPending}
                onCancel={() => setMetadataToDelete(null)}
                onConfirm={handleDeleteMetadataConfirm}
            />
            <EditMetadataDialog
                metadata={metadataToEdit}
                isSaving={updateMetadataMutation.isPending}
                onCancel={() => setMetadataToEdit(null)}
                onSave={handleUpdateMetadata}
            />
            <EditNotificationDialog
                row={notificationToEdit}
                hookCategories={hookCategories}
                isLoadingHooks={isLoadingHooks}
                isSaving={updateNotificationMutation.isPending}
                onCancel={() => setNotificationToEdit(null)}
                onSave={handleUpdateNotification}
            />
        </div>
    );
}
