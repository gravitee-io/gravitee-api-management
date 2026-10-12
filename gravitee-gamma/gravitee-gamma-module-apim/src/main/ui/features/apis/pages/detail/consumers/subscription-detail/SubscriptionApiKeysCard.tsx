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
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    DataTablePagination,
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Tooltip,
    TooltipContent,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { CalendarIcon, CircleCheckIcon, CircleXIcon, CopyIcon, RefreshCwIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import { notify } from '../../../../../../shared/notify';
import { useCustomApiKeyCheck } from '../../../../hooks/useCustomApiKeyCheck';
import { useCustomApiKeyEnabled } from '../../../../hooks/usePlanSecuritySettings';
import {
    useApiKeyList,
    useExpireApiKey,
    useReactivateApiKey,
    useRenewApiKey,
    useRevokeApiKey,
} from '../../../../hooks/useSubscriptionApiKeys';
import type { ApiKey, Subscription, SubscriptionContext } from '../../../../types/subscription';
import { formatDate } from '../../../../utils/formatDate';
import { CustomApiKeyField } from '../CustomApiKeyField';

function InfoBanner({ children }: { children: React.ReactNode }) {
    return (
        <div
            className="rounded-lg px-4 py-3 text-sm text-muted-foreground"
            style={{ backgroundColor: 'color-mix(in oklab, var(--color-muted) 40%, transparent)', border: '1px solid var(--color-border)' }}
        >
            {children}
        </div>
    );
}

interface ApiKeyRowActionsProps {
    apiKey: ApiKey;
    subscriptionStatus: Subscription['status'];
    canUpdate: boolean;
    isShared: boolean;
    isFederated: boolean;
    onRevoke: (id: string) => void;
    onExpire: (id: string) => void;
    onReactivate: (id: string) => void;
    isRevoking: boolean;
    isReactivating: boolean;
}

function ApiKeyRowActions({
    apiKey,
    subscriptionStatus,
    canUpdate,
    isShared,
    isFederated,
    onRevoke,
    onExpire,
    onReactivate,
    isRevoking,
    isReactivating,
}: ApiKeyRowActionsProps) {
    const isSubscriptionActionable = subscriptionStatus === 'ACCEPTED' || subscriptionStatus === 'PENDING';
    if (!canUpdate || isShared || !isSubscriptionActionable) return null;

    const isValid = !apiKey.revoked && !apiKey.expired;
    const canMutateValidKey = isValid && subscriptionStatus === 'ACCEPTED' && !isFederated;

    if (!isValid) {
        return (
            <div className="flex items-center gap-1 justify-end">
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-7"
                            disabled={isReactivating}
                            onClick={() => onReactivate(apiKey.id)}
                        >
                            <RefreshCwIcon className="size-3.5" aria-hidden />
                            <span className="sr-only">Reactivate key</span>
                        </Button>
                    </TooltipTrigger>
                    <TooltipContent>Reactivate key</TooltipContent>
                </Tooltip>
            </div>
        );
    }

    if (!canMutateValidKey) return null;

    return (
        <div className="flex items-center gap-1 justify-end">
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-7"
                        onClick={() => navigator.clipboard.writeText(apiKey.key).catch(() => {})}
                    >
                        <CopyIcon className="size-3.5" aria-hidden />
                        <span className="sr-only">Copy key</span>
                    </Button>
                </TooltipTrigger>
                <TooltipContent>Copy key</TooltipContent>
            </Tooltip>
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-7 text-destructive hover:text-destructive"
                        disabled={isRevoking}
                        onClick={() => onRevoke(apiKey.id)}
                    >
                        <XIcon className="size-3.5" aria-hidden />
                        <span className="sr-only">Revoke key</span>
                    </Button>
                </TooltipTrigger>
                <TooltipContent>Revoke key</TooltipContent>
            </Tooltip>
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button type="button" variant="ghost" size="icon" className="size-7" onClick={() => onExpire(apiKey.id)}>
                        <CalendarIcon className="size-3.5" aria-hidden />
                        <span className="sr-only">Set expiry date</span>
                    </Button>
                </TooltipTrigger>
                <TooltipContent>Set expiry date</TooltipContent>
            </Tooltip>
        </div>
    );
}

interface SubscriptionApiKeysCardProps {
    ctx: SubscriptionContext;
    subscription: Subscription;
    canUpdate: boolean;
    isFederated?: boolean;
}

export function SubscriptionApiKeysCard({ ctx, subscription, canUpdate, isFederated = false }: Readonly<SubscriptionApiKeysCardProps>) {
    const [page, setPage] = useState(1);
    const [expiringKeyId, setExpiringKeyId] = useState<string | null>(null);
    const [expireDate, setExpireDate] = useState('');
    const [renewOpen, setRenewOpen] = useState(false);
    const [renewCustomKey, setRenewCustomKey] = useState('');
    const [reactivatingKeyId, setReactivatingKeyId] = useState<string | null>(null);
    const PER_PAGE = 5;

    const { data, isLoading } = useApiKeyList(ctx, subscription.id, page, PER_PAGE);
    const renewMutation = useRenewApiKey(ctx, subscription.id);
    const revokeMutation = useRevokeApiKey(ctx, subscription.id);
    const expireMutation = useExpireApiKey(ctx, subscription.id);
    const reactivateMutation = useReactivateApiKey(ctx, subscription.id);

    const canUseCustomApiKey = useCustomApiKeyEnabled();
    const renewKeyCheck = useCustomApiKeyCheck(ctx, subscription.application.id, canUseCustomApiKey ? renewCustomKey : '');

    const handleRevoke = useCallback(
        (keyId: string) =>
            revokeMutation.mutate(keyId, {
                onSuccess: () => notify.success('API key revoked'),
                onError: error => notify.error(error, 'Failed to revoke API key.'),
            }),
        [revokeMutation],
    );
    const closeRenew = useCallback(() => {
        setRenewOpen(false);
        setRenewCustomKey('');
    }, []);
    const handleReactivateConfirm = useCallback(() => {
        if (!reactivatingKeyId) return;
        reactivateMutation.mutate(reactivatingKeyId, {
            onSuccess: () => {
                notify.success('API key reactivated');
                setReactivatingKeyId(null);
            },
            onError: error => notify.error(error, 'Failed to reactivate API key.'),
        });
    }, [reactivatingKeyId, reactivateMutation]);
    const handleRenewConfirm = useCallback(() => {
        if (renewKeyCheck.blocked) return;
        renewMutation.mutate(renewCustomKey !== '' ? renewCustomKey : undefined, {
            onSuccess: () => {
                notify.success('API key renewed');
                closeRenew();
            },
            onError: error => notify.error(error, 'Failed to renew API key.'),
        });
    }, [renewMutation, renewCustomKey, renewKeyCheck.blocked, closeRenew]);
    const handleExpireConfirm = useCallback(() => {
        if (!expiringKeyId || !expireDate) return;
        expireMutation.mutate(
            { apiKeyId: expiringKeyId, expireAt: new Date(expireDate + 'T23:59:59').toISOString() },
            {
                onSuccess: () => {
                    notify.success('API key expiry updated');
                    setExpiringKeyId(null);
                    setExpireDate('');
                },
                onError: error => notify.error(error, 'Failed to update API key expiry.'),
            },
        );
    }, [expiringKeyId, expireDate, expireMutation]);

    const securityType = subscription.plan.security?.type;
    const isShared = subscription.application.apiKeyMode === 'SHARED' && securityType === 'API_KEY';
    const canRenew = canUpdate && subscription.status === 'ACCEPTED' && !isShared && !isFederated;

    const totalCount = data?.pagination.totalCount ?? 0;

    if (securityType === 'KEY_LESS') return null;
    if (securityType === 'OAUTH2' || securityType === 'JWT') {
        return (
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Credentials</CardTitle>
                </CardHeader>
                <CardContent>
                    <InfoBanner>OAuth2 / JWT credentials are managed via your application&apos;s security settings.</InfoBanner>
                </CardContent>
            </Card>
        );
    }

    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-base">{isShared ? 'Shared API Keys' : 'API Keys'}</CardTitle>
                {canRenew && (
                    <Button type="button" variant="outline" size="sm" onClick={() => setRenewOpen(true)} disabled={renewMutation.isPending}>
                        <RefreshCwIcon className="size-3.5" aria-hidden />
                        {renewMutation.isPending ? 'Renewing…' : 'Renew'}
                    </Button>
                )}
            </CardHeader>
            <CardContent className="space-y-3">
                {isShared && (
                    <InfoBanner>This subscription uses a shared API Key. You can renew or revoke it at the application level.</InfoBanner>
                )}

                <div className="rounded-md border">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead className="w-8" />
                                <TableHead>Key</TableHead>
                                <TableHead>Created</TableHead>
                                <TableHead>Revoked / Expired</TableHead>
                                <TableHead />
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {isLoading &&
                                Array.from({ length: 2 }).map((_, i) => (
                                    <TableRow key={i}>
                                        {Array.from({ length: 5 }).map((__, j) => (
                                            <TableCell key={j}>
                                                <Skeleton className="h-4 w-full rounded" />
                                            </TableCell>
                                        ))}
                                    </TableRow>
                                ))}
                            {!isLoading &&
                                (data?.data ?? []).map(key => (
                                    <TableRow key={key.id}>
                                        <TableCell>
                                            {key.revoked || key.expired ? (
                                                <CircleXIcon className="size-4 text-muted-foreground" aria-hidden />
                                            ) : (
                                                <CircleCheckIcon className="size-4 text-success" aria-hidden />
                                            )}
                                        </TableCell>
                                        <TableCell className="font-mono text-xs max-w-xs truncate">{key.key}</TableCell>
                                        <TableCell className="text-sm text-muted-foreground">{formatDate(key.createdAt)}</TableCell>
                                        <TableCell className="text-sm text-muted-foreground">
                                            {formatDate(key.revokedAt ?? key.expireAt)}
                                        </TableCell>
                                        <TableCell>
                                            <ApiKeyRowActions
                                                apiKey={key}
                                                subscriptionStatus={subscription.status}
                                                canUpdate={canUpdate}
                                                isShared={isShared}
                                                isFederated={isFederated}
                                                onRevoke={handleRevoke}
                                                onExpire={id => {
                                                    setExpiringKeyId(id);
                                                    setExpireDate('');
                                                }}
                                                onReactivate={setReactivatingKeyId}
                                                isRevoking={revokeMutation.isPending}
                                                isReactivating={reactivateMutation.isPending}
                                            />
                                        </TableCell>
                                    </TableRow>
                                ))}
                        </TableBody>
                    </Table>
                    <div className="flex justify-end border-t px-2 py-2">
                        <DataTablePagination page={page} pageSize={PER_PAGE} totalCount={totalCount} onPageChange={setPage} />
                    </div>
                </div>

                {expiringKeyId && (
                    <div className="flex items-center gap-3 rounded-lg border px-4 py-3 text-sm">
                        <label htmlFor="expire-date" className="shrink-0 text-muted-foreground">
                            Set expiry date:
                        </label>
                        <Input
                            id="expire-date"
                            type="date"
                            className="flex-1"
                            value={expireDate}
                            onChange={e => setExpireDate(e.target.value)}
                        />
                        <Button type="button" variant="ghost" size="sm" onClick={() => setExpiringKeyId(null)}>
                            Cancel
                        </Button>
                        <Button type="button" size="sm" disabled={!expireDate || expireMutation.isPending} onClick={handleExpireConfirm}>
                            {expireMutation.isPending ? 'Saving…' : 'Set'}
                        </Button>
                    </div>
                )}
            </CardContent>

            <Dialog open={renewOpen} onOpenChange={open => !open && closeRenew()}>
                <DialogContent aria-describedby={undefined}>
                    <DialogHeader>
                        <DialogTitle>Renew your API Key</DialogTitle>
                    </DialogHeader>
                    <p className="text-sm">Your previous API Key will be no longer valid in 2 hours!</p>
                    {canUseCustomApiKey ? (
                        <div className="space-y-2">
                            <p className="text-sm text-muted-foreground">You can provide a custom API Key here</p>
                            <CustomApiKeyField
                                id="renew-custom-api-key"
                                label="Custom API Key"
                                value={renewCustomKey}
                                onChange={setRenewCustomKey}
                                error={renewKeyCheck.error}
                                checking={renewKeyCheck.checking}
                                disabled={renewMutation.isPending}
                                placeholder="Leave blank to generate an API Key"
                            />
                        </div>
                    ) : null}
                    <DialogFooter className="flex-row justify-end gap-2">
                        <Button type="button" variant="outline" onClick={closeRenew} disabled={renewMutation.isPending}>
                            Cancel
                        </Button>
                        <Button type="button" onClick={handleRenewConfirm} disabled={renewMutation.isPending || renewKeyCheck.blocked}>
                            {renewMutation.isPending ? 'Renewing…' : 'Renew'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <Dialog open={Boolean(reactivatingKeyId)} onOpenChange={open => !open && setReactivatingKeyId(null)}>
                <DialogContent aria-describedby={undefined}>
                    <DialogHeader>
                        <DialogTitle>Reactivate your API Key</DialogTitle>
                    </DialogHeader>
                    <p className="text-sm">Reactivate your revoked or expired API Key</p>
                    <DialogFooter className="flex-row justify-end gap-2">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setReactivatingKeyId(null)}
                            disabled={reactivateMutation.isPending}
                        >
                            Cancel
                        </Button>
                        <Button type="button" onClick={handleReactivateConfirm} disabled={reactivateMutation.isPending}>
                            {reactivateMutation.isPending ? 'Reactivating…' : 'Reactivate'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </Card>
    );
}
