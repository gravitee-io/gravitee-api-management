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
import { useState } from 'react';
import { useParams } from 'react-router-dom';

import { ApplicationMetadataSection } from '../features/applications/components/metadata/ApplicationMetadataSection';
import { DeleteNotificationDialog } from '../features/applications/components/notifications/DeleteNotificationDialog';
import { EditNotificationDialog } from '../features/applications/components/notifications/EditNotificationDialog';
import { notifierTypeLabel } from '../features/applications/components/notifications/notificationHelpers';
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
    const canAddNotification = canCreateNotification && !isReadOnly;
    const canEditNotification = canUpdateNotification && !isReadOnly;
    const canRemoveNotification = canDeleteNotification && !isReadOnly;
    const canAddMetadata = canCreateMetadata && !isReadOnly;
    const canEditMetadata = canUpdateMetadata && !isReadOnly;
    const canRemoveMetadata = canDeleteMetadata && !isReadOnly;

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
            notify.success('Notification created successfully');
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
        } catch (error: unknown) {
            notify.error(error, 'Failed to create notification.');
            return false;
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
        await createMetadataMutation.mutateAsync(payload);
    }

    async function handleUpdateMetadata(payload: UpdateApplicationMetadata) {
        await updateMetadataMutation.mutateAsync(payload);
    }

    async function handleDeleteMetadata(metadataKey: string) {
        await deleteMetadataMutation.mutateAsync(metadataKey);
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
                notifiers={notifiers}
                isLoading={notificationsLoading}
                isError={notificationsError}
                canCreate={canAddNotification}
                canUpdate={canEditNotification}
                canDelete={canRemoveNotification}
                isCreating={createNotificationMutation.isPending}
                onAdd={handleAddNotification}
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

            <EditNotificationDialog
                row={notificationToEdit}
                hookCategories={hookCategories}
                isLoadingHooks={isLoadingHooks}
                isSaving={updateNotificationMutation.isPending}
                onCancel={() => setNotificationToEdit(null)}
                onSave={handleUpdateNotification}
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
