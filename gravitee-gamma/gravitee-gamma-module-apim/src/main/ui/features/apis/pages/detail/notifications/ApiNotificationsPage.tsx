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
import { Alert, AlertDescription, Button } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { NotificationsEmptyState } from './NotificationsEmptyState';
import { NotificationsSummaryCards } from './NotificationsSummaryCards';
import { NotificationsTable } from './NotificationsTable';
import { ConfirmDialog } from '../../../../../shared/components';
import { notify } from '../../../../../shared/notify';
import type { NotificationRow } from '../../../hooks/useApiNotifications';
import { useApiNotifications, useDeleteNotification } from '../../../hooks/useApiNotifications';

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiNotificationsPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const navigate = useNavigate();

    const canCreate = useHasPermission({ anyOf: ['api-notification-c'] });
    const canUpdate = useHasPermission({ anyOf: ['api-notification-u'] });
    const canDelete = useHasPermission({ anyOf: ['api-notification-d'] });

    const { rows, isLoading, isError } = useApiNotifications(apiId);

    const deleteMutation = useDeleteNotification(apiId ?? '');

    const [deletingRow, setDeletingRow] = useState<NotificationRow | null>(null);

    // ─── Handlers ──────────────────────────────────────────────────────────────

    const handleDeleteConfirm = useCallback(() => {
        if (!deletingRow?.notification.id) return;
        deleteMutation.mutate(deletingRow.notification.id, {
            onSuccess: () => {
                notify.success('Notification deleted');
                setDeletingRow(null);
            },
            onError: error => notify.error(error, 'Failed to delete notification.'),
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
                    <Button type="button" size="sm" onClick={() => navigate('new')}>
                        <PlusIcon className="size-4" aria-hidden="true" />
                        Add notification
                    </Button>
                )}
            </div>

            {/* Summary cards — always visible (shows 0 when empty) */}
            <NotificationsSummaryCards rows={rows} isLoading={isLoading} />

            {/* Empty learning state */}
            {isEmpty && <NotificationsEmptyState />}

            {/* Table — always visible when rows exist or loading */}
            {(isLoading || rows.length > 0) && (
                <NotificationsTable
                    rows={rows}
                    isLoading={isLoading}
                    editingKey={null}
                    canUpdate={canUpdate}
                    canDelete={canDelete}
                    onEdit={key => navigate(key)}
                    onDelete={setDeletingRow}
                />
            )}

            {/* Delete confirmation */}
            <ConfirmDialog
                open={deletingRow !== null}
                onOpenChange={open => !open && setDeletingRow(null)}
                title="Delete notification"
                description={
                    <>
                        <strong>{deletingRow?.notification.name}</strong> will be permanently deleted. This cannot be undone.
                    </>
                }
                confirmLabel="Delete"
                pendingLabel="Deleting…"
                destructive
                isPending={deleteMutation.isPending}
                onConfirm={handleDeleteConfirm}
            />
        </div>
    );
}
