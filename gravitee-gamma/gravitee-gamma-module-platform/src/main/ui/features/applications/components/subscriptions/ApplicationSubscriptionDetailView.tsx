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
    Separator,
    Skeleton,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ApplicationSubscriptionApiKeysCard } from './ApplicationSubscriptionApiKeysCard';
import { ApplicationSubscriptionCloseDialog } from './ApplicationSubscriptionCloseDialog';
import { ApplicationSubscriptionDetailsTable } from './ApplicationSubscriptionDetailsTable';
import { ApplicationSubscriptionEditRequestDialog } from './ApplicationSubscriptionEditRequestDialog';
import { ApplicationSubscriptionExpireApiKeyDialog } from './ApplicationSubscriptionExpireApiKeyDialog';
import { ApplicationSubscriptionRevokeApiKeyDialog } from './ApplicationSubscriptionRevokeApiKeyDialog';
import { SubscriptionStatusLabel } from './SubscriptionStatusLabel';
import { useDetailBasePath } from '../../../shared/hooks/useDetailBasePath';
import {
    useApplicationSubscriptionApiKeys,
    useExpireSubscriptionApiKey,
    useRenewSubscriptionApiKey,
    useRevokeSubscriptionApiKey,
} from '../../hooks/useApplicationSubscriptionApiKeys';
import { useApplicationSubscriptionDetail, useUpdateApplicationSubscription } from '../../hooks/useApplicationSubscriptionDetail';
import { useApplicationSubscriptionPermissions } from '../../hooks/useApplicationSubscriptionPermissions';
import { useCloseApplicationSubscription } from '../../hooks/useCloseApplicationSubscription';
import { resolveSubscriptionApiKeyV2Parent } from '../../services/applicationSubscriptions';
import type { ApplicationListItem } from '../../types/application';
import type { ApplicationSubscriptionApiKeyRow } from '../../types/applicationSubscription';
import { mapDetailToCloseTarget } from '../../utils/applicationSubscriptionDetailMapper';
import { canCloseSubscription } from '../../utils/applicationSubscriptionMapper';

export function ApplicationSubscriptionDetailView({
    application,
    subscriptionId,
}: Readonly<{
    application: ApplicationListItem;
    subscriptionId: string;
}>) {
    const navigate = useNavigate();
    const basePath = useDetailBasePath('applications', application.id);
    const readOnly = application.status === 'ARCHIVED';
    const { canUpdate, canDelete } = useApplicationSubscriptionPermissions();

    const { data, isLoading, isError } = useApplicationSubscriptionDetail(application.id, subscriptionId, application.api_key_mode);
    const updateMutation = useUpdateApplicationSubscription(application.id, subscriptionId);
    const closeMutation = useCloseApplicationSubscription(application.id);

    const [editOpen, setEditOpen] = useState(false);
    const [editError, setEditError] = useState<string | null>(null);
    const [closeTarget, setCloseTarget] = useState<ReturnType<typeof mapDetailToCloseTarget> | null>(null);
    const [closeError, setCloseError] = useState<string | null>(null);
    const [revokeTarget, setRevokeTarget] = useState<ApplicationSubscriptionApiKeyRow | null>(null);
    const [revokeError, setRevokeError] = useState<string | null>(null);
    const [expireTarget, setExpireTarget] = useState<ApplicationSubscriptionApiKeyRow | null>(null);
    const [expireError, setExpireError] = useState<string | null>(null);
    const [renewError, setRenewError] = useState<string | null>(null);

    const detail = data?.detail;
    const entity = data?.entity;
    const v2Parent = entity ? resolveSubscriptionApiKeyV2Parent(entity) : null;

    const showApiKeys = Boolean(
        detail && detail.securityType === 'API Key' && detail.status === 'ACCEPTED' && application.api_key_mode !== 'SHARED',
    );
    const showSharedKeysHint = Boolean(detail && detail.securityType === 'API Key' && application.api_key_mode === 'SHARED');

    const apiKeysQuery = useApplicationSubscriptionApiKeys(application.id, subscriptionId, showApiKeys);
    const renewMutation = useRenewSubscriptionApiKey(application.id, subscriptionId);
    const revokeMutation = useRevokeSubscriptionApiKey(application.id, subscriptionId);
    const expireMutation = useExpireSubscriptionApiKey(application.id, subscriptionId, v2Parent);

    if (isLoading) {
        return (
            <div className="space-y-5">
                <Skeleton className="h-8 w-48" />
                <Skeleton className="h-10 w-72" />
                <Skeleton className="h-96 w-full" />
            </div>
        );
    }

    if (isError || !detail || !entity) {
        return (
            <div className="space-y-4">
                <Button variant="ghost" size="sm" className="-ml-2 w-fit" asChild>
                    <Link to={`${basePath}/subscriptions`}>
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to subscriptions list
                    </Link>
                </Button>
                <p className="text-sm text-muted-foreground">Subscription not found or you may not have access.</p>
            </div>
        );
    }

    const isKubernetesManaged = detail.origin === 'KUBERNETES';
    const effectiveReadOnly = readOnly || isKubernetesManaged;
    const canEdit = !effectiveReadOnly && canUpdate && detail.status === 'PENDING';
    const canClose = !effectiveReadOnly && canDelete && canCloseSubscription(detail.status);
    const canManageApiKeys = !effectiveReadOnly && canUpdate;
    const subtitle = `${detail.apiDisplay} · ${detail.planName}`;

    const handleSaveEdit = async (request: string) => {
        setEditError(null);
        try {
            await updateMutation.mutateAsync({ entity, request });
            setEditOpen(false);
        } catch {
            setEditError('Failed to update subscription. Please try again.');
        }
    };

    const handleClose = async () => {
        if (!closeTarget) return;
        setCloseError(null);
        try {
            await closeMutation.mutateAsync(closeTarget.id);
            setCloseTarget(null);
            navigate(`${basePath}/subscriptions`);
        } catch {
            setCloseError('Failed to close subscription. Please try again.');
        }
    };

    const handleRenew = async () => {
        setRenewError(null);
        try {
            await renewMutation.mutateAsync();
        } catch {
            setRenewError('Failed to renew API key. Please try again.');
        }
    };

    const handleRevoke = async () => {
        if (!revokeTarget) return;
        setRevokeError(null);
        try {
            await revokeMutation.mutateAsync(revokeTarget.id);
            setRevokeTarget(null);
        } catch {
            setRevokeError('Failed to revoke API key. Please try again.');
        }
    };

    const handleExpireConfirm = async (expirationDate: Date) => {
        if (!expireTarget) return;
        setExpireError(null);
        try {
            await expireMutation.mutateAsync({ apiKeyId: expireTarget.id, expireAt: expirationDate });
            setExpireTarget(null);
        } catch {
            setExpireError('Failed to update API key expiration. Please try again.');
        }
    };

    return (
        <div className="space-y-5">
            <Button variant="ghost" size="sm" className="-ml-2 w-fit" asChild>
                <Link to={`${basePath}/subscriptions`}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to subscriptions list
                </Link>
            </Button>

            <div className="min-w-0 space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Subscription details</h1>
                <p className="truncate text-sm text-muted-foreground">{subtitle}</p>
            </div>

            {isKubernetesManaged ? (
                <Alert>
                    <AlertDescription>
                        This subscription was created by the Kubernetes Operator and cannot be managed through the console.
                    </AlertDescription>
                </Alert>
            ) : null}

            <Card>
                <CardHeader className="pb-3">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                        <CardTitle className="text-base">Subscription</CardTitle>
                        <div className="flex items-center gap-2">
                            <SubscriptionStatusLabel status={detail.status} />
                            {canEdit ? (
                                <Button type="button" variant="outline" size="sm" onClick={() => setEditOpen(true)}>
                                    <PencilIcon className="size-3.5" aria-hidden />
                                    Edit subscription message
                                </Button>
                            ) : null}
                        </div>
                    </div>
                    <CardDescription>Identifier: {detail.id}</CardDescription>
                </CardHeader>
                <CardContent className="px-6 pt-0">
                    <ApplicationSubscriptionDetailsTable detail={detail} />
                </CardContent>
                {canClose ? (
                    <>
                        <Separator />
                        <CardContent className="flex justify-end pt-4">
                            <Button
                                type="button"
                                variant="destructive"
                                size="sm"
                                onClick={() => {
                                    setCloseError(null);
                                    setCloseTarget(mapDetailToCloseTarget(detail));
                                }}
                            >
                                <Trash2Icon className="size-3.5" aria-hidden />
                                Close
                            </Button>
                        </CardContent>
                    </>
                ) : null}
            </Card>

            {showSharedKeysHint ? (
                <Card className="border-dashed">
                    <CardContent className="pt-6 text-sm text-muted-foreground">
                        This application uses <strong className="text-foreground">shared API keys</strong>. Manage keys at the application
                        level from User Permissions.
                    </CardContent>
                </Card>
            ) : null}

            {showApiKeys ? (
                <div className="space-y-2">
                    {renewError ? <p className="text-sm text-destructive">{renewError}</p> : null}
                    <ApplicationSubscriptionApiKeysCard
                        apiKeys={apiKeysQuery.data ?? []}
                        isLoading={apiKeysQuery.isLoading}
                        readOnly={!canManageApiKeys}
                        renewPending={renewMutation.isPending}
                        expireAvailable={Boolean(v2Parent)}
                        onRenew={() => void handleRenew()}
                        onRevoke={apiKey => {
                            setRevokeError(null);
                            setRevokeTarget(apiKey);
                        }}
                        onExpire={apiKey => {
                            setExpireError(null);
                            setExpireTarget(apiKey);
                        }}
                    />
                </div>
            ) : null}

            <ApplicationSubscriptionEditRequestDialog
                open={editOpen}
                initialRequest={detail.request ?? ''}
                onOpenChange={setEditOpen}
                onSave={request => void handleSaveEdit(request)}
                isLoading={updateMutation.isPending}
                error={editError}
            />

            <ApplicationSubscriptionCloseDialog
                subscription={closeTarget}
                onClose={() => {
                    setCloseTarget(null);
                    setCloseError(null);
                }}
                onConfirm={() => void handleClose()}
                isLoading={closeMutation.isPending}
                error={closeError}
            />

            <ApplicationSubscriptionRevokeApiKeyDialog
                apiKey={revokeTarget}
                onClose={() => {
                    setRevokeTarget(null);
                    setRevokeError(null);
                }}
                onConfirm={() => void handleRevoke()}
                isLoading={revokeMutation.isPending}
                error={revokeError}
            />

            <ApplicationSubscriptionExpireApiKeyDialog
                apiKey={expireTarget}
                onClose={() => {
                    setExpireTarget(null);
                    setExpireError(null);
                }}
                onConfirm={at => void handleExpireConfirm(at)}
                isLoading={expireMutation.isPending}
                error={expireError}
            />
        </div>
    );
}
