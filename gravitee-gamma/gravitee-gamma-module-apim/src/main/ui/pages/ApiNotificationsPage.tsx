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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
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
} from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';
import { useParams } from 'react-router-dom';

import { AddNotificationDialog } from './api-notifications/AddNotificationDialog';
import { NotificationEventEditor } from './api-notifications/NotificationEventEditor';
import { NotificationsEmptyState } from './api-notifications/NotificationsEmptyState';
import { NotificationsSummaryCards } from './api-notifications/NotificationsSummaryCards';
import { NotificationsTable } from './api-notifications/NotificationsTable';
import type { NotificationRow } from '../features/apis/hooks/useApiNotifications';
import {
    useApiNotifications,
    useCreateNotification,
    useDeleteNotification,
    useUpdateNotification,
} from '../features/apis/hooks/useApiNotifications';

// ─── Delete confirm dialog ────────────────────────────────────────────────────

interface DeleteConfirmDialogProps {
    row: NotificationRow | null;
    isPending: boolean;
    onConfirm: () => void;
    onCancel: () => void;
}

function DeleteConfirmDialog({ row, isPending, onConfirm, onCancel }: Readonly<DeleteConfirmDialogProps>) {
    return (
        <Dialog
            open={row !== null}
            onOpenChange={open => {
                if (!open) onCancel();
            }}
        >
            <DialogContent style={{ maxWidth: '24rem' }}>
                <DialogHeader>
                    <DialogTitle>Delete notification</DialogTitle>
                    <DialogDescription>
                        <strong>{row?.notification.name}</strong> will be permanently deleted. This cannot be undone.
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                    <Button type="button" variant="outline" onClick={onCancel} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button type="button" variant="destructive" onClick={onConfirm} disabled={isPending}>
                        {isPending ? 'Deleting…' : 'Delete'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiNotificationsPage() {
    const { apiId } = useParams<{ apiId: string }>();

    const canCreate = useHasPermission({ anyOf: ['api-notification-c'] });
    const canUpdate = useHasPermission({ anyOf: ['api-notification-u'] });
    const canDelete = useHasPermission({ anyOf: ['api-notification-d'] });

    const { rows, notifiers, hookCategories, isLoading, isLoadingHooks, isError } = useApiNotifications(apiId);

    const createMutation = useCreateNotification(apiId ?? '');
    const updateMutation = useUpdateNotification(apiId ?? '');
    const deleteMutation = useDeleteNotification(apiId ?? '');

    const [addDialogOpen, setAddDialogOpen] = useState(false);
    const [editingKey, setEditingKey] = useState<string | null>(null);
    const [deletingRow, setDeletingRow] = useState<NotificationRow | null>(null);
    const [saveError, setSaveError] = useState<string | null>(null);

    const editingRow = editingKey !== null ? (rows.find(r => r.key === editingKey) ?? null) : null;

    // ─── Handlers ──────────────────────────────────────────────────────────────

    const handleAdd = useCallback(
        (name: string, notifierId: string) => {
            if (!apiId) return;
            const isPortal = notifierId === '__PORTAL__';

            if (isPortal) {
                // The PORTAL notification always exists — just open the editor
                const portalRow = rows.find(r => r.notification.config_type === 'PORTAL');
                setAddDialogOpen(false);
                if (portalRow && canUpdate) setEditingKey(portalRow.key);
                return;
            }

            createMutation.mutate(
                {
                    name,
                    notifier: notifierId,
                    config_type: 'GENERIC',
                    hooks: [],
                    referenceType: 'API',
                    referenceId: apiId,
                },
                {
                    onSuccess: created => {
                        setAddDialogOpen(false);
                        setSaveError(null);
                        if (canUpdate) setEditingKey(created.id ?? 'PORTAL');
                    },
                    onError: (err: unknown) => {
                        const message = err instanceof Error ? err.message : 'Failed to create notification.';
                        setSaveError(message);
                    },
                },
            );
        },
        [apiId, rows, canUpdate, createMutation],
    );

    const handleSaveEvents = useCallback(
        (hooks: string[], config: string) => {
            if (!editingRow) return;
            updateMutation.mutate(
                { ...editingRow.notification, hooks, config },
                {
                    onSuccess: () => {
                        setSaveError(null);
                        setEditingKey(null);
                    },
                    onError: (err: unknown) => {
                        const message = err instanceof Error ? err.message : 'Failed to save notification events.';
                        setSaveError(message);
                    },
                },
            );
        },
        [editingRow, updateMutation],
    );

    const handleDeleteConfirm = useCallback(() => {
        if (!deletingRow?.notification.id) return;
        deleteMutation.mutate(deletingRow.notification.id, {
            onSuccess: () => {
                setSaveError(null);
                setDeletingRow(null);
            },
            onError: (err: unknown) => {
                const message = err instanceof Error ? err.message : 'Failed to delete notification.';
                setSaveError(message);
            },
        });
    }, [deletingRow, deleteMutation]);

    // ─── Render ────────────────────────────────────────────────────────────────

    if (isError) {
        return (
            <div className="p-6">
                <Alert variant="destructive">
                    <AlertDescription>Failed to load notifications. Please refresh the page.</AlertDescription>
                </Alert>
            </div>
        );
    }

    const isEmpty = !isLoading && rows.every(r => r.notification.config_type === 'PORTAL');

    return (
        <div className="flex flex-col gap-6 p-6">
            {/* Header */}
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Notifications</h1>
                    <p className="text-sm text-muted-foreground">
                        Get alerted when key API events occur — deployments, subscription changes, or policy errors.
                    </p>
                </div>
                {canCreate && (
                    <Button type="button" size="sm" onClick={() => setAddDialogOpen(true)}>
                        <PlusIcon className="size-4" aria-hidden="true" />
                        Add notification
                    </Button>
                )}
            </div>

            {/* Mutation error banner */}
            {saveError && (
                <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3">
                    <p className="text-sm text-destructive">{saveError}</p>
                </div>
            )}

            {/* Summary cards — always visible (shows 0 when empty) */}
            <NotificationsSummaryCards rows={rows} isLoading={isLoading} />

            {/* Empty learning state */}
            {isEmpty && <NotificationsEmptyState />}

            {/* Table — always visible when rows exist or loading */}
            {(isLoading || rows.length > 0) && (
                <NotificationsTable
                    rows={rows}
                    isLoading={isLoading}
                    editingKey={editingKey}
                    canUpdate={canUpdate}
                    canDelete={canDelete}
                    onEdit={setEditingKey}
                    onDelete={setDeletingRow}
                />
            )}

            {/* Inline event editor */}
            {editingRow && (
                <NotificationEventEditor
                    key={editingRow.key}
                    row={editingRow}
                    hookCategories={hookCategories}
                    isLoadingHooks={isLoadingHooks}
                    isPending={updateMutation.isPending}
                    onSave={handleSaveEvents}
                    onCancel={() => setEditingKey(null)}
                />
            )}

            {/* Dialogs */}
            <AddNotificationDialog
                open={addDialogOpen}
                notifiers={notifiers}
                isPending={createMutation.isPending}
                onClose={() => setAddDialogOpen(false)}
                onAdd={handleAdd}
            />
            <DeleteConfirmDialog
                row={deletingRow}
                isPending={deleteMutation.isPending}
                onConfirm={handleDeleteConfirm}
                onCancel={() => setDeletingRow(null)}
            />
        </div>
    );
}
