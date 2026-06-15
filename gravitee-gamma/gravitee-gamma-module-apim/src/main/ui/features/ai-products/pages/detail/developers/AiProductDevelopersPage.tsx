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
    Badge,
    Button,
    Card,
    CardContent,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { CheckIcon, CircleXIcon, PencilIcon, UsersRoundIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useParams } from 'react-router-dom';

import { DeveloperKeyCell } from './DeveloperKeyCell';
import { notify } from '../../../../../shared/notify';
import { useApiProductResourcePermissions } from '../../../../api-products/hooks/useApiProductPermissions';
import { useSubscriptionList } from '../../../../apis/hooks/useSubscriptions';
import type { Subscription, SubscriptionContext } from '../../../../apis/types/subscription';
import { DEVELOPER_RATE_LIMIT_METADATA_KEY, DEVELOPER_TOKEN_LIMIT_METADATA_KEY } from '../../../../apis/utils/planTransformers';
import { ApproveRequestSheet } from '../../../components/developers/ApproveRequestSheet';
import { EditLimitsSheet } from '../../../components/developers/EditLimitsSheet';
import { useCloseDeveloper, useRejectDeveloper } from '../../../hooks/useAiProductHooks';

const EMPTY_FILTERS = { statuses: [], planIds: [], applicationIds: [], apiKey: '' };

function metadataNumber(sub: Subscription, key: string): number | null {
    const raw = sub.metadata?.[key];
    const value = raw ? Number(raw) : NaN;
    return Number.isFinite(value) ? value : null;
}

export function AiProductDevelopersPage() {
    const { productId } = useParams<{ productId: string }>();
    const ctx: SubscriptionContext = { type: 'api-product', entityId: productId ?? '' };
    const [page, setPage] = useState(1);
    const [approvingSub, setApprovingSub] = useState<Subscription | null>(null);
    const [editingSub, setEditingSub] = useState<Subscription | null>(null);

    const { canCreate } = useApiProductResourcePermissions(productId, 'subscription');
    const { data, isLoading } = useSubscriptionList(ctx, EMPTY_FILTERS, page, 25);
    const { mutate: reject, isPending: isRejecting } = useRejectDeveloper();
    const { mutate: close, isPending: isClosing } = useCloseDeveloper();

    const developers = data?.data ?? [];
    const totalCount = data?.pagination.totalCount ?? 0;

    function handleReject(sub: Subscription) {
        reject(
            { productId: productId ?? '', subscriptionId: sub.id },
            {
                onSuccess: () => notify.success('Request rejected.'),
                onError: error => notify.error(error, 'Failed to reject the request.'),
            },
        );
    }

    function handleRevoke(sub: Subscription) {
        const who = sub.subscribedBy?.displayName ?? sub.application.name;
        if (!window.confirm(`Revoke ${who}'s access? Their API key stops working immediately. They can request access again later.`)) {
            return;
        }
        close(
            { productId: productId ?? '', subscriptionId: sub.id },
            {
                onSuccess: () => notify.success('Access revoked — the API key no longer works.'),
                onError: error => notify.error(error, 'Failed to revoke access.'),
            },
        );
    }

    return (
        <div className="space-y-6 p-6">
            <div className="flex items-center justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Subscribers</h1>
                    <p className="text-sm text-muted-foreground">
                        Anyone can request access from the <span className="font-medium">Developer Portal</span>. Approve a request to grant
                        that user their <span className="font-medium">own API key</span>, <span className="font-medium">token budget</span>,
                        and <span className="font-medium">rate limit</span> — set right here, applied only to them.
                    </p>
                </div>
            </div>

            {isLoading ? (
                <div className="space-y-2">
                    {Array.from({ length: 3 }).map((_, i) => (
                        <Skeleton key={i} className="h-12 rounded-lg" />
                    ))}
                </div>
            ) : developers.length === 0 ? (
                <Card className="border-dashed">
                    <CardContent className="flex flex-col items-center gap-3 py-10 text-center">
                        <UsersRoundIcon className="size-6 text-muted-foreground opacity-60" aria-hidden />
                        <div className="space-y-1">
                            <p className="text-sm font-medium">No subscribers yet</p>
                            <p className="text-xs text-muted-foreground max-w-md">
                                When a developer requests access to this product from the Developer Portal, their request shows up here for
                                you to approve and set their personal limits.
                            </p>
                        </div>
                    </CardContent>
                </Card>
            ) : (
                <div className="rounded-lg border">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>User</TableHead>
                                <TableHead>Application</TableHead>
                                <TableHead>Plan</TableHead>
                                <TableHead>Token budget</TableHead>
                                <TableHead>Rate limit</TableHead>
                                <TableHead>Status</TableHead>
                                <TableHead>API key</TableHead>
                                <TableHead className="text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {developers.map(sub => {
                                const tokens = metadataNumber(sub, DEVELOPER_TOKEN_LIMIT_METADATA_KEY);
                                const rate = metadataNumber(sub, DEVELOPER_RATE_LIMIT_METADATA_KEY);
                                return (
                                    <TableRow key={sub.id}>
                                        <TableCell className="font-medium">
                                            {sub.subscribedBy?.displayName ?? sub.application.primaryOwner?.displayName ?? '—'}
                                        </TableCell>
                                        <TableCell className="text-sm text-muted-foreground">{sub.application.name}</TableCell>
                                        <TableCell className="text-sm text-muted-foreground">{sub.plan?.name ?? '—'}</TableCell>
                                        <TableCell className="tabular-nums text-sm text-muted-foreground">
                                            {tokens !== null ? `${tokens.toLocaleString()} tokens` : '—'}
                                        </TableCell>
                                        <TableCell className="tabular-nums text-sm text-muted-foreground">
                                            {rate !== null ? `${rate.toLocaleString()} req/min` : '—'}
                                        </TableCell>
                                        <TableCell>
                                            <Badge variant={sub.status === 'ACCEPTED' ? 'secondary' : 'outline'} className="text-xs">
                                                {sub.status}
                                            </Badge>
                                        </TableCell>
                                        <TableCell>
                                            {sub.status === 'ACCEPTED' ? (
                                                <DeveloperKeyCell ctx={ctx} subscriptionId={sub.id} />
                                            ) : (
                                                <span className="text-xs text-muted-foreground">—</span>
                                            )}
                                        </TableCell>
                                        <TableCell className="text-right">
                                            {canCreate && sub.status === 'PENDING' && (
                                                <div className="flex justify-end gap-1">
                                                    <Button
                                                        variant="ghost"
                                                        size="sm"
                                                        onClick={() => setApprovingSub(sub)}
                                                        aria-label={`Approve request from ${sub.application.name}`}
                                                    >
                                                        <CheckIcon className="size-4" aria-hidden />
                                                        Approve
                                                    </Button>
                                                    <Button
                                                        variant="ghost"
                                                        size="sm"
                                                        disabled={isRejecting}
                                                        onClick={() => handleReject(sub)}
                                                        aria-label={`Reject request from ${sub.application.name}`}
                                                    >
                                                        <XIcon className="size-4" aria-hidden />
                                                        Reject
                                                    </Button>
                                                </div>
                                            )}
                                            {canCreate && sub.status === 'ACCEPTED' && (
                                                <div className="flex justify-end gap-1">
                                                    <Button
                                                        variant="ghost"
                                                        size="sm"
                                                        onClick={() => setEditingSub(sub)}
                                                        aria-label={`Edit limits for ${sub.application.name}`}
                                                    >
                                                        <PencilIcon className="size-4" aria-hidden />
                                                        Edit
                                                    </Button>
                                                    <Button
                                                        variant="ghost"
                                                        size="sm"
                                                        disabled={isClosing}
                                                        onClick={() => handleRevoke(sub)}
                                                        aria-label={`Revoke access for ${sub.application.name}`}
                                                        className="text-destructive hover:text-destructive"
                                                    >
                                                        <CircleXIcon className="size-4" aria-hidden />
                                                        Revoke
                                                    </Button>
                                                </div>
                                            )}
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </div>
            )}

            {totalCount > 25 ? (
                <div className="flex justify-end gap-2">
                    <Button variant="outline" size="sm" disabled={page === 1} onClick={() => setPage(p => p - 1)}>
                        Previous
                    </Button>
                    <Button variant="outline" size="sm" disabled={page * 25 >= totalCount} onClick={() => setPage(p => p + 1)}>
                        Next
                    </Button>
                </div>
            ) : null}

            <ApproveRequestSheet
                open={approvingSub !== null}
                productId={productId ?? ''}
                subscription={approvingSub}
                onClose={() => setApprovingSub(null)}
            />
            <EditLimitsSheet
                open={editingSub !== null}
                productId={productId ?? ''}
                subscription={editingSub}
                onClose={() => setEditingSub(null)}
            />
        </div>
    );
}
