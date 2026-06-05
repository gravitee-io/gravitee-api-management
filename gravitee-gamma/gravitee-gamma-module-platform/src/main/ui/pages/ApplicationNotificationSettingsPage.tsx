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
import { useCallback, useState } from 'react';
import { useParams } from 'react-router-dom';

import { ApplicationMetadataSection } from '../features/applications/components/metadata/ApplicationMetadataSection';
import { DeleteNotificationDialog } from '../features/applications/components/notifications/DeleteNotificationDialog';
import {
    EditNotificationSheet,
    type NotificationSheetCreatePayload,
} from '../features/applications/components/notifications/EditNotificationSheet';
import {
    buildNewNotificationRow,
    notificationNotifierOptions,
} from '../features/applications/components/notifications/notificationHelpers';
import { NotificationsSection } from '../features/applications/components/notifications/NotificationsSection';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';
import { useApplicationNotificationPermissions } from '../features/applications/hooks/useApplicationNotificationPermissions';
import {
    useCreateApplicationNotification,
    useApplicationMetadata,
    useApplicationNotifications,
    useCreateApplicationMetadata,
    useDeleteApplicationMetadata,
    useDeleteApplicationNotification,
    useUpdateApplicationNotification,
    useUpdateApplicationMetadata,
} from '../features/applications/hooks/useApplicationNotifications';
import type {
    ApplicationNotificationRow,
    NewApplicationMetadata,
    UpdateApplicationNotification,
    UpdateApplicationMetadata,
} from '../features/applications/types/applicationNotification';
import { notify } from '../shared/notify';

export function ApplicationNotificationSettingsPage() {
    const { applicationId } = useParams<{ applicationId: string }>();
    const { application } = useApplicationDetailContext();
    const { canCreateNotification, canUpdateNotification, canDeleteNotification, canCreateMetadata, canUpdateMetadata, canDeleteMetadata } =
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
    const deleteNotificationMutation = useDeleteApplicationNotification(applicationId);
    const createMetadataMutation = useCreateApplicationMetadata(applicationId);
    const updateMetadataMutation = useUpdateApplicationMetadata(applicationId);
    const deleteMetadataMutation = useDeleteApplicationMetadata(applicationId);

    const [notificationToEdit, setNotificationToEdit] = useState<ApplicationNotificationRow | null>(null);
    const [notificationToDelete, setNotificationToDelete] = useState<ApplicationNotificationRow | null>(null);

    const isReadOnly = application?.origin === 'KUBERNETES';
    const hasConfigurableNotifiers = notificationNotifierOptions(notifiers).length > 0;
    const canAddNotification = canCreateNotification && !isReadOnly && hasConfigurableNotifiers;
    const canEditNotification = canUpdateNotification && !isReadOnly;
    const canRemoveNotification = canDeleteNotification && !isReadOnly;
    const canAddMetadata = canCreateMetadata && !isReadOnly;
    const canEditMetadata = canUpdateMetadata && !isReadOnly;
    const canRemoveMetadata = canDeleteMetadata && !isReadOnly;

    const handleCreateNotificationClick = useCallback(() => {
        if (!applicationId || !hasConfigurableNotifiers) {
            return;
        }
        setNotificationToEdit(buildNewNotificationRow(applicationId, notifiers));
    }, [applicationId, hasConfigurableNotifiers, notifiers]);

    async function handleCreateNotification(payload: NotificationSheetCreatePayload) {
        if (!applicationId) {
            return;
        }
        try {
            const created = await createNotificationMutation.mutateAsync({
                name: payload.name,
                notifier: payload.notifier,
                referenceType: 'APPLICATION',
                referenceId: applicationId,
                config_type: 'GENERIC',
                hooks: payload.hooks,
            });

            const needsFollowUpUpdate = Boolean(payload.config) || Boolean(payload.useSystemProxy);
            if (needsFollowUpUpdate) {
                await updateNotificationMutation.mutateAsync({
                    ...created,
                    config: payload.config ?? created.config,
                    useSystemProxy: payload.useSystemProxy ?? created.useSystemProxy,
                    hooks: payload.hooks,
                });
            }

            notify.success('Notification created successfully');
            setNotificationToEdit(null);
        } catch (error: unknown) {
            notify.error(error, 'Failed to create notification.');
        }
    }

    function handleUpdateNotification(notification: UpdateApplicationNotification) {
        updateNotificationMutation.mutate(notification, {
            onSuccess: () => {
                notify.success('Notification saved successfully');
                setNotificationToEdit(null);
            },
            onError: error => notify.error(error, 'Failed to save notification.'),
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
            onError: error => notify.error(error, 'Failed to delete notification.'),
        });
    }

    async function handleCreateMetadata(payload: NewApplicationMetadata) {
        try {
            await createMetadataMutation.mutateAsync(payload);
            notify.success('Metadata created successfully');
        } catch (error) {
            notify.error(error, 'Failed to create metadata.');
        }
    }

    async function handleUpdateMetadata(payload: UpdateApplicationMetadata) {
        try {
            await updateMetadataMutation.mutateAsync(payload);
            notify.success('Metadata saved successfully');
        } catch (error) {
            notify.error(error, 'Failed to save metadata.');
        }
    }

    async function handleDeleteMetadata(metadataKey: string) {
        try {
            await deleteMetadataMutation.mutateAsync(metadataKey);
            notify.success('Metadata deleted successfully');
        } catch (error) {
            notify.error(error, 'Failed to delete metadata.');
        }
    }

    const metadataMutationError = createMetadataMutation.error ?? updateMetadataMutation.error ?? deleteMetadataMutation.error;
    const metadataMutationErrorMessage =
        metadataMutationError instanceof Error
            ? metadataMutationError.message
            : metadataMutationError
              ? String(metadataMutationError)
              : null;

    return (
        <div className="space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Notification settings</h1>
                <p className="text-sm text-muted-foreground">Hooks and metadata used in notification templates.</p>
            </div>

            <NotificationsSection
                rows={rows}
                isLoading={notificationsLoading}
                isError={notificationsError}
                canCreate={canAddNotification}
                canUpdate={canEditNotification}
                canDelete={canRemoveNotification}
                onCreateClick={handleCreateNotificationClick}
                onEdit={row => setNotificationToEdit(row)}
                onDelete={row => setNotificationToDelete(row)}
            />

            <ApplicationMetadataSection
                metadata={metadata}
                isLoading={metadataLoading}
                isError={metadataError}
                canCreate={canAddMetadata}
                canUpdate={canEditMetadata}
                canDelete={canRemoveMetadata}
                isCreating={createMetadataMutation.isPending}
                isUpdating={updateMetadataMutation.isPending}
                isDeleting={deleteMetadataMutation.isPending}
                mutationErrorMessage={metadataMutationErrorMessage}
                onCreate={handleCreateMetadata}
                onUpdate={handleUpdateMetadata}
                onDelete={handleDeleteMetadata}
            />

            <EditNotificationSheet
                row={notificationToEdit}
                notifiers={notifiers}
                hookCategories={hookCategories}
                isLoadingHooks={isLoadingHooks}
                isSaving={createNotificationMutation.isPending || updateNotificationMutation.isPending}
                onCancel={() => setNotificationToEdit(null)}
                onSave={handleUpdateNotification}
                onCreate={handleCreateNotification}
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
