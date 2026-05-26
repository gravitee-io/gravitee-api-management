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
    CardHeader,
    CardTitle,
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Tooltip,
    TooltipContent,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { ArrowDownIcon, ArrowUpIcon, PencilIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import type { EndpointGroupDto } from '../../../types';

const LB_LABELS: Record<string, string> = {
    ROUND_ROBIN: 'Round robin',
    RANDOM: 'Random',
    WEIGHTED_ROUND_ROBIN: 'Weighted round robin',
    WEIGHTED_RANDOM: 'Weighted random',
};

const TYPE_LABELS: Record<string, string> = {
    'http-proxy': 'HTTP Proxy',
    grpc: 'gRPC',
    kafka: 'Kafka',
    mock: 'Mock',
    rabbitmq: 'RabbitMQ',
};

function formatGroupType(type: string | undefined): string {
    if (!type) return 'Unknown';
    return TYPE_LABELS[type] ?? type;
}

interface PendingDelete {
    type: 'group' | 'endpoint';
    label: string;
    groupIndex: number;
    endpointIndex?: number;
}

interface EndpointGroupListProps {
    groups: EndpointGroupDto[];
    isReadOnly: boolean;
    onEditGroup: (groupIndex: number) => void;
    onDeleteGroup: (groupIndex: number) => void;
    onAddEndpoint: (groupIndex: number) => void;
    onEditEndpoint: (groupIndex: number, endpointIndex: number) => void;
    onDeleteEndpoint: (groupIndex: number, endpointIndex: number) => void;
    onReorderEndpoints: (groupIndex: number, fromIndex: number, toIndex: number) => void;
}

export function EndpointGroupList({
    groups,
    isReadOnly,
    onEditGroup,
    onDeleteGroup,
    onAddEndpoint,
    onEditEndpoint,
    onDeleteEndpoint,
    onReorderEndpoints,
}: Readonly<EndpointGroupListProps>) {
    const [pending, setPending] = useState<PendingDelete | null>(null);

    const isLastGroup = groups.length <= 1;

    function requestDeleteGroup(groupIndex: number) {
        setPending({ type: 'group', label: groups[groupIndex].name, groupIndex });
    }

    function requestDeleteEndpoint(groupIndex: number, endpointIndex: number) {
        const name = groups[groupIndex].endpoints?.[endpointIndex]?.name ?? 'this endpoint';
        setPending({ type: 'endpoint', label: name, groupIndex, endpointIndex });
    }

    function confirmDelete() {
        if (!pending) return;
        if (pending.type === 'group') {
            onDeleteGroup(pending.groupIndex);
        } else if (pending.endpointIndex !== undefined) {
            onDeleteEndpoint(pending.groupIndex, pending.endpointIndex);
        }
        setPending(null);
    }

    return (
        <>
            <div className="space-y-4">
                {groups.map((group, gIdx) => {
                    const epCount = group.endpoints?.length ?? 0;
                    const canDeleteGroup = !isReadOnly && !isLastGroup;
                    const canDeleteEndpoint = !isReadOnly && epCount > 1;

                    return (
                        <Card key={`${group.name}-${gIdx}`}>
                            {/* Group header */}
                            <CardHeader className="pb-3">
                                <div className="flex items-center justify-between gap-3">
                                    <div className="flex items-center gap-2 min-w-0 flex-wrap">
                                        <CardTitle className="text-base truncate">{group.name}</CardTitle>
                                        <Badge variant="secondary" className="shrink-0 text-xs">
                                            {formatGroupType(group.type)}
                                        </Badge>
                                        <Badge variant="secondary" className="shrink-0 text-xs">
                                            {LB_LABELS[group.loadBalancer?.type ?? ''] ?? 'Round robin'}
                                        </Badge>
                                        {gIdx === 0 && (
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <Badge variant="outline" className="shrink-0 text-xs cursor-default">
                                                        Default
                                                    </Badge>
                                                </TooltipTrigger>
                                                <TooltipContent>
                                                    The default group of endpoints used by the API is the first one listed.
                                                </TooltipContent>
                                            </Tooltip>
                                        )}
                                    </div>
                                    {!isReadOnly && (
                                        <div className="flex items-center gap-1 shrink-0">
                                            <Button
                                                type="button"
                                                size="sm"
                                                variant="outline"
                                                className="gap-1.5"
                                                aria-label={`Edit group ${group.name}`}
                                                onClick={() => onEditGroup(gIdx)}
                                            >
                                                <PencilIcon className="size-3.5" aria-hidden />
                                                Edit
                                            </Button>
                                            <Button
                                                type="button"
                                                size="sm"
                                                variant="ghost"
                                                className="size-8 p-0 text-destructive hover:text-destructive hover:bg-destructive/5"
                                                aria-label={`Delete group ${group.name}`}
                                                disabled={!canDeleteGroup}
                                                title={
                                                    isLastGroup
                                                        ? 'Cannot delete the only endpoint group'
                                                        : isReadOnly
                                                          ? 'Read-only'
                                                          : undefined
                                                }
                                                onClick={() => requestDeleteGroup(gIdx)}
                                            >
                                                <Trash2Icon className="size-3.5" aria-hidden />
                                            </Button>
                                        </div>
                                    )}
                                </div>
                            </CardHeader>

                            {/* Endpoint table */}
                            <CardContent className="pt-0">
                                {epCount === 0 ? (
                                    <p className="text-sm text-muted-foreground">No endpoints configured.</p>
                                ) : (
                                    <div className="rounded-md border overflow-hidden">
                                        {/* Column headers */}
                                        <div
                                            className="grid items-center border-b bg-muted/40 px-3 py-2"
                                            style={{ gridTemplateColumns: '56px 1fr 2fr 72px 56px', gap: '12px' }}
                                        >
                                            <span />
                                            <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Name</span>
                                            <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                                                Target URL
                                            </span>
                                            <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                                                Weight
                                            </span>
                                            <span />
                                        </div>

                                        {/* Endpoint rows */}
                                        {(group.endpoints ?? []).map((ep, eIdx) => (
                                            <div
                                                key={`${ep.name}-${eIdx}`}
                                                className="grid items-center px-3 py-3 border-b last:border-0 hover:bg-muted/20 transition-colors"
                                                style={{ gridTemplateColumns: '56px 1fr 2fr 72px 56px', gap: '12px' }}
                                            >
                                                {/* Order buttons — left column */}
                                                <div className="flex items-center gap-0.5 shrink-0">
                                                    {!isReadOnly && (
                                                        <>
                                                            <Tooltip>
                                                                <TooltipTrigger asChild>
                                                                    <Button
                                                                        type="button"
                                                                        size="sm"
                                                                        variant="ghost"
                                                                        className="size-7 p-0"
                                                                        aria-label={`Move ${ep.name} up`}
                                                                        disabled={eIdx === 0}
                                                                        onClick={() => onReorderEndpoints(gIdx, eIdx, eIdx - 1)}
                                                                    >
                                                                        <ArrowUpIcon className="size-3.5" aria-hidden />
                                                                    </Button>
                                                                </TooltipTrigger>
                                                                <TooltipContent>Move up</TooltipContent>
                                                            </Tooltip>
                                                            <Tooltip>
                                                                <TooltipTrigger asChild>
                                                                    <Button
                                                                        type="button"
                                                                        size="sm"
                                                                        variant="ghost"
                                                                        className="size-7 p-0"
                                                                        aria-label={`Move ${ep.name} down`}
                                                                        disabled={eIdx === epCount - 1}
                                                                        onClick={() => onReorderEndpoints(gIdx, eIdx, eIdx + 1)}
                                                                    >
                                                                        <ArrowDownIcon className="size-3.5" aria-hidden />
                                                                    </Button>
                                                                </TooltipTrigger>
                                                                <TooltipContent>Move down</TooltipContent>
                                                            </Tooltip>
                                                        </>
                                                    )}
                                                </div>
                                                <span className="text-sm font-medium truncate">{ep.name}</span>
                                                <span className="text-xs text-muted-foreground truncate font-mono">
                                                    {ep.configuration?.target ?? '—'}
                                                </span>
                                                <span className="text-sm text-muted-foreground">{ep.weight ?? 1}</span>
                                                {/* Edit / delete — right column */}
                                                <div className="flex items-center justify-end gap-0.5 shrink-0">
                                                    {!isReadOnly && (
                                                        <>
                                                            <Button
                                                                type="button"
                                                                size="sm"
                                                                variant="ghost"
                                                                className="size-7 p-0"
                                                                aria-label={`Edit endpoint ${ep.name}`}
                                                                onClick={() => onEditEndpoint(gIdx, eIdx)}
                                                            >
                                                                <PencilIcon className="size-3.5" aria-hidden />
                                                            </Button>
                                                            <Button
                                                                type="button"
                                                                size="sm"
                                                                variant="ghost"
                                                                className="size-7 p-0 text-destructive hover:text-destructive"
                                                                aria-label={`Delete endpoint ${ep.name}`}
                                                                disabled={!canDeleteEndpoint}
                                                                title={!canDeleteEndpoint ? 'Cannot delete the only endpoint' : undefined}
                                                                onClick={() => requestDeleteEndpoint(gIdx, eIdx)}
                                                            >
                                                                <Trash2Icon className="size-3.5" aria-hidden />
                                                            </Button>
                                                        </>
                                                    )}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}

                                {/* Add endpoint button */}
                                {!isReadOnly && (
                                    <div className="mt-3">
                                        <Button
                                            type="button"
                                            size="sm"
                                            variant="outline"
                                            className="gap-1.5"
                                            onClick={() => onAddEndpoint(gIdx)}
                                        >
                                            <PlusIcon className="size-3.5" aria-hidden />
                                            Add endpoint
                                        </Button>
                                    </div>
                                )}
                            </CardContent>
                        </Card>
                    );
                })}
            </div>

            {/* Delete confirmation dialog */}
            <Dialog open={!!pending} onOpenChange={v => !v && setPending(null)}>
                <DialogContent className="sm:max-w-sm">
                    <DialogHeader>
                        <DialogTitle>Delete {pending?.type === 'group' ? 'endpoint group' : 'endpoint'}</DialogTitle>
                    </DialogHeader>
                    <p className="text-sm text-muted-foreground">
                        Are you sure you want to delete <strong className="text-foreground">&ldquo;{pending?.label}&rdquo;</strong>? This
                        action cannot be undone.
                    </p>
                    <DialogFooter>
                        <Button variant="outline" size="sm" onClick={() => setPending(null)}>
                            Cancel
                        </Button>
                        <Button variant="destructive" size="sm" onClick={confirmDelete}>
                            Delete
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </>
    );
}
