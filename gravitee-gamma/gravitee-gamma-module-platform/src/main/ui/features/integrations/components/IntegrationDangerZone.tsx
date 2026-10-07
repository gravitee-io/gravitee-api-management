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
import { Button, Card, CardContent, CardHeader, CardTitle, cn } from '@gravitee/graphene-core';
import { Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useId, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { notify } from '../../../shared/notify/notify';
import { resolveListHrefFromDetailBasePath, useDetailBasePath } from '../../shared/hooks/useDetailBasePath';
import { useDeleteFederatedApis } from '../hooks/useDeleteFederatedApis';
import { useDeleteIntegration } from '../hooks/useDeleteIntegration';
import { useIntegration } from '../hooks/useIntegration';
import { useIntegrationHasFederatedApis } from '../hooks/useIntegrationHasFederatedApis';
import type { IntegrationDeletedFederatedApisResponse } from '../types/integration';
import { integrationErrorMessage } from '../utils/integrationErrorMessage';
import { ENVIRONMENT_API_DELETE_PERMISSION } from '../utils/integrationPermissions';

type FederatedApisCheck = ReturnType<typeof useIntegrationHasFederatedApis>;

function tileSubtitle(federatedApisCheck: FederatedApisCheck, withFederatedApis: string, withoutFederatedApis: string): string {
    if (federatedApisCheck.isError) return 'Could not check whether this integration has federated APIs. Reload the page to try again.';
    if (federatedApisCheck.isPending) return 'Checking whether this integration has federated APIs…';
    return federatedApisCheck.data ? withFederatedApis : withoutFederatedApis;
}

function deleteIntegrationTileSubtitle(federatedApisCheck: FederatedApisCheck): string {
    return tileSubtitle(
        federatedApisCheck,
        'Delete its federated APIs first. An integration with federated APIs can’t be deleted.',
        'Permanently deletes the integration. This can’t be undone.',
    );
}

function deleteApisTileSubtitle(federatedApisCheck: FederatedApisCheck): string {
    return tileSubtitle(
        federatedApisCheck,
        'Deletes the APIs imported from this integration. Published APIs are kept.',
        'This integration has no federated APIs to delete.',
    );
}

function deletedFederatedApisMessage({ deleted, skipped, errors }: IntegrationDeletedFederatedApisResponse): string {
    return ['Federated APIs have been deleted.', `• Deleted: ${deleted}`, `• Not deleted: ${skipped}`, `• Errors: ${errors}`].join('\n');
}

function DangerZoneTile({
    title,
    subtitle,
    buttonLabel,
    disabled,
    onSelect,
}: Readonly<{ title: string; subtitle: string; buttonLabel: string; disabled: boolean; onSelect: () => void }>) {
    const subtitleId = useId();
    return (
        <div className="flex w-full items-center gap-3 rounded-lg border p-4">
            <div className={cn('shrink-0 rounded-lg p-2', disabled ? 'bg-muted' : 'bg-destructive/10')}>
                <Trash2Icon className={cn('size-5', disabled ? 'text-muted-foreground' : 'text-destructive')} aria-hidden />
            </div>
            <div className="min-w-0 flex-1">
                <p className="text-sm font-medium">{title}</p>
                <p id={subtitleId} className="text-sm text-muted-foreground">
                    {subtitle}
                </p>
            </div>
            <Button type="button" variant="destructive" size="sm" disabled={disabled} aria-describedby={subtitleId} onClick={onSelect}>
                {buttonLabel}
            </Button>
        </div>
    );
}

export function IntegrationDangerZone({ integrationId }: Readonly<{ integrationId: string }>) {
    const navigate = useNavigate();
    const listHref = resolveListHrefFromDetailBasePath(useDetailBasePath('integrations', integrationId));
    const integrationQuery = useIntegration(integrationId);
    const federatedApisCheck = useIntegrationHasFederatedApis(integrationId);

    useEffect(() => {
        if (federatedApisCheck.isError) {
            console.warn(`Federated APIs check for integration ${integrationId} failed`, federatedApisCheck.error);
        }
    }, [federatedApisCheck.isError, federatedApisCheck.error, integrationId]);

    const canDeleteApis = useHasEnvironmentPermission([ENVIRONMENT_API_DELETE_PERMISSION]);
    const deleteIntegration = useDeleteIntegration();
    const deleteFederatedApis = useDeleteFederatedApis();
    const [deleteOpen, setDeleteOpen] = useState(false);
    const [deleteApisOpen, setDeleteApisOpen] = useState(false);

    const integration = integrationQuery.data;
    const deleteIntegrationDisabled = !integration || federatedApisCheck.data !== false;
    const deleteApisDisabled = federatedApisCheck.data !== true || deleteFederatedApis.isPending;

    const handleConfirmDelete = async () => {
        try {
            await deleteIntegration.mutateAsync(integrationId);
            notify.success('Integration successfully deleted!');
            navigate(listHref);
        } catch (error) {
            setDeleteOpen(false);
            notify.error(integrationErrorMessage(error));
        }
    };

    const handleConfirmDeleteApis = async () => {
        try {
            const result = await deleteFederatedApis.mutateAsync(integrationId);
            notify.success(deletedFederatedApisMessage(result));
        } catch (error) {
            notify.error(integrationErrorMessage(error));
        } finally {
            setDeleteApisOpen(false);
        }
    };

    return (
        <Card className="border-destructive/40">
            <CardHeader>
                <CardTitle>Danger zone</CardTitle>
            </CardHeader>
            <CardContent>
                <div className="space-y-3">
                    {canDeleteApis ? (
                        <DangerZoneTile
                            title="Delete federated APIs"
                            buttonLabel="Delete APIs"
                            subtitle={deleteApisTileSubtitle(federatedApisCheck)}
                            disabled={deleteApisDisabled}
                            onSelect={() => setDeleteApisOpen(true)}
                        />
                    ) : null}
                    <DangerZoneTile
                        title="Delete integration"
                        buttonLabel="Delete integration"
                        subtitle={deleteIntegrationTileSubtitle(federatedApisCheck)}
                        disabled={deleteIntegrationDisabled}
                        onSelect={() => setDeleteOpen(true)}
                    />
                </div>
            </CardContent>
            {canDeleteApis ? (
                <ConfirmDialog
                    open={deleteApisOpen}
                    onOpenChange={setDeleteApisOpen}
                    title="Delete APIs"
                    description="Published APIs won’t be deleted. Deleted APIs can’t be restored."
                    confirmLabel="Delete APIs"
                    pendingLabel="Deleting…"
                    destructive
                    icon={<Trash2Icon className="size-4" aria-hidden />}
                    isPending={deleteFederatedApis.isPending}
                    onConfirm={() => void handleConfirmDeleteApis()}
                />
            ) : null}
            {integration ? (
                <ConfirmDialog
                    open={deleteOpen}
                    onOpenChange={setDeleteOpen}
                    title="Delete integration"
                    description={
                        <>
                            This will permanently delete <strong>{integration.name}</strong>. This can’t be undone.
                        </>
                    }
                    confirmLabel="Delete permanently"
                    pendingLabel="Deleting…"
                    destructive
                    confirmKeyword={integration.name}
                    contentClassName="sm:max-w-md"
                    icon={<Trash2Icon className="size-4" aria-hidden />}
                    isPending={deleteIntegration.isPending}
                    onConfirm={() => void handleConfirmDelete()}
                />
            ) : null}
        </Card>
    );
}
