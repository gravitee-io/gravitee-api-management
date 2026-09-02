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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { Button } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { DeleteNotificationDialog } from '../features/applications/components/notifications/DeleteNotificationDialog';
import {
    EditNotificationSheet,
    type NotificationSheetCreatePayload,
} from '../features/applications/components/notifications/EditNotificationSheet';
import { notificationNotifierOptions } from '../features/applications/components/notifications/notificationHelpers';
import type {
    ApplicationNotificationRow,
    ApplicationNotificationSettings,
    UpdateApplicationNotification,
} from '../features/applications/types/applicationNotification';
import { EnvironmentNotificationsTable } from '../features/environment-notifications/components/EnvironmentNotificationsTable';
import { useEnvironmentNotificationPermissions } from '../features/environment-notifications/hooks/useEnvironmentNotificationPermissions';
import {
    useCreateEnvironmentNotification,
    useDeleteEnvironmentNotification,
    useEnvironmentNotifications,
    useUpdateEnvironmentNotification,
} from '../features/environment-notifications/hooks/useEnvironmentNotifications';
import { buildNewEnvironmentNotificationRow } from '../features/environment-notifications/utils/buildNewEnvironmentNotificationRow';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { notify } from '../shared/notify';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

export function EnvironmentNotificationSettingsPage() {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    const { canCreate, canUpdateGeneric, canUpdatePortal, canDelete } = useEnvironmentNotificationPermissions();
    const { rows, notifiers, hookCategories, isLoading, isLoadingHooks, isError, error } = useEnvironmentNotifications();
    const createNotificationMutation = useCreateEnvironmentNotification();
    const updateNotificationMutation = useUpdateEnvironmentNotification();
    const deleteNotificationMutation = useDeleteEnvironmentNotification();

    const [notificationToEdit, setNotificationToEdit] = useState<ApplicationNotificationRow | null>(null);
    const [notificationToDelete, setNotificationToDelete] = useState<ApplicationNotificationRow | null>(null);

    const isForbidden = isForbiddenApiError(isError, error);
    useForbiddenResourceRedirect({
        isForbidden,
        navItemKey: 'notification-settings',
        permissionPrefix: 'environment-notification-',
        redirectTo: '../applications',
    });

    const hasConfigurableNotifiers = notificationNotifierOptions(notifiers).length > 0;
    const canAddNotification = canCreate && hasConfigurableNotifiers;

    // Classic parity: the Console/Portal row only needs read permission to edit (it's the caller's own
    // notification preference); GENERIC rows need update permission (see useEnvironmentNotificationPermissions).
    const canEditRow = (row: ApplicationNotificationRow) =>
        row.notification.config_type === 'PORTAL' ? canUpdatePortal : canUpdateGeneric;

    function handleCreateNotificationClick() {
        if (!envId || !hasConfigurableNotifiers) {
            return;
        }
        setNotificationToEdit(buildNewEnvironmentNotificationRow(envId, notifiers));
    }

    async function handleCreateNotification(payload: NotificationSheetCreatePayload) {
        if (!envId) {
            return;
        }
        let created: ApplicationNotificationSettings;
        try {
            created = await createNotificationMutation.mutateAsync({
                name: payload.name,
                notifier: payload.notifier,
                referenceType: 'ENVIRONMENT',
                referenceId: envId,
                config_type: 'GENERIC',
                hooks: payload.hooks,
            });
        } catch (err: unknown) {
            notify.error(err, 'Failed to create notification.');
            return;
        }

        const needsFollowUpUpdate = Boolean(payload.config) || Boolean(payload.useSystemProxy);
        if (needsFollowUpUpdate) {
            try {
                await updateNotificationMutation.mutateAsync({
                    ...created,
                    config: payload.config ?? created.config,
                    useSystemProxy: payload.useSystemProxy ?? created.useSystemProxy,
                    hooks: payload.hooks,
                });
            } catch (err: unknown) {
                notify.error(err, `"${created.name}" was created, but saving its configuration failed — edit it to finish setup.`);
                setNotificationToEdit(null);
                return;
            }
        }

        notify.success('Notification created successfully');
        setNotificationToEdit(null);
    }

    function handleUpdateNotification(notification: UpdateApplicationNotification) {
        updateNotificationMutation.mutate(notification, {
            onSuccess: () => {
                notify.success('Notification saved successfully');
                setNotificationToEdit(null);
            },
            onError: err => notify.error(err, 'Failed to save notification.'),
        });
    }

    function handleDeleteNotificationConfirm() {
        const notificationId = notificationToDelete?.notification?.id;
        const notificationName = notificationToDelete?.name;
        if (!notificationId) {
            return;
        }
        deleteNotificationMutation.mutate(notificationId, {
            onSuccess: () => {
                notify.success(`"${notificationName}" has been deleted`);
                setNotificationToDelete(null);
            },
            onError: err => notify.error(err, 'Failed to delete notification.'),
        });
    }

    function renderContent() {
        if (isForbidden) {
            // Redirecting away in the effect above — render nothing rather than flash an error.
            return null;
        }
        return (
            <EnvironmentNotificationsTable
                rows={rows}
                isLoading={isLoading}
                isError={isError}
                canUpdate={canEditRow}
                canDelete={canDelete}
                onEdit={row => setNotificationToEdit(row)}
                onDelete={row => setNotificationToDelete(row)}
            />
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Notification settings</h1>
                    <p className="text-sm text-muted-foreground">Configure how this environment notifies you and your team about events.</p>
                </div>
                {canAddNotification ? (
                    <Button type="button" size="sm" className="shrink-0" onClick={handleCreateNotificationClick}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add notification
                    </Button>
                ) : null}
            </div>

            {renderContent()}

            <EditNotificationSheet
                row={notificationToEdit}
                notifiers={notifiers}
                hookCategories={hookCategories}
                isLoadingHooks={isLoadingHooks}
                isSaving={createNotificationMutation.isPending || updateNotificationMutation.isPending}
                onCancel={() => setNotificationToEdit(null)}
                onSave={handleUpdateNotification}
                onCreate={handleCreateNotification}
                layout="standard"
            />
            <DeleteNotificationDialog
                row={notificationToDelete}
                isDeleting={deleteNotificationMutation.isPending}
                onCancel={() => setNotificationToDelete(null)}
                onConfirm={handleDeleteNotificationConfirm}
            />
        </div>
    );
}
