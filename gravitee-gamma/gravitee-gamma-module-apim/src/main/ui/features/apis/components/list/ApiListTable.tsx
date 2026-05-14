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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { AlertCircleIcon, CircleCheckIcon, CircleXIcon, MoreHorizontalIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import type { ApiDeploymentState, ApiListItem, ApiState } from '../../types/api';
import { getApiAccessPath } from '../../utils/apiAccess';

// ─── Status helpers ───────────────────────────────────────────────────────────

function RuntimeStatusBadge({ state }: { state: ApiState | undefined }) {
    switch (state) {
        case 'STARTED':
            return (
                <Badge variant="outline" className="border-success/20 text-success">
                    <CircleCheckIcon className="size-3 mr-1" aria-hidden />
                    Started
                </Badge>
            );
        case 'STOPPED':
            return (
                <Badge variant="outline" className="border-destructive/20 text-destructive">
                    <CircleXIcon className="size-3 mr-1" aria-hidden />
                    Stopped
                </Badge>
            );
        case 'CLOSED':
            return (
                <Badge variant="outline" className="text-muted-foreground">
                    Closed
                </Badge>
            );
        default:
            return <span className="text-muted-foreground text-xs">—</span>;
    }
}

function SyncStatusBadge({ deploymentState }: { deploymentState: ApiDeploymentState | undefined }) {
    if (deploymentState === 'NEED_REDEPLOY') {
        return (
            <Badge variant="outline" className="border-warning/30 text-warning">
                <AlertCircleIcon className="size-3 mr-1" aria-hidden />
                Out of sync
            </Badge>
        );
    }
    return (
        <Badge variant="outline" className="border-success/20 text-success">
            <RefreshCwIcon className="size-3 mr-1" aria-hidden />
            In sync
        </Badge>
    );
}

// ─── Skeleton row ─────────────────────────────────────────────────────────────

function SkeletonRow() {
    return (
        <TableRow>
            <TableCell>
                <Skeleton className="h-4 w-32 rounded" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-5 w-16 rounded-full" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-5 w-14 rounded-full" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-5 w-24 rounded-full" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-4 w-28 rounded" />
            </TableCell>
            <TableCell />
        </TableRow>
    );
}

// ─── Actions dropdown ─────────────────────────────────────────────────────────

function ApiActionsMenu({ apiId, onNavigate }: { apiId: string; onNavigate: (path: string) => void }) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="API actions" onClick={e => e.stopPropagation()}>
                    <MoreHorizontalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                <DropdownMenuItem onSelect={() => onNavigate(`${apiId}/overview`)}>View Details</DropdownMenuItem>
                <DropdownMenuItem onSelect={() => onNavigate(`${apiId}/general`)}>Edit Configuration</DropdownMenuItem>
                <DropdownMenuItem onSelect={() => onNavigate(`${apiId}/analytics`)}>View Analytics</DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

// ─── Main table ───────────────────────────────────────────────────────────────

interface ApiListTableProps {
    readonly apis: ApiListItem[];
    readonly isLoading: boolean;
    readonly skeletonRowCount?: number;
}

export function ApiListTable({ apis, isLoading, skeletonRowCount = 5 }: ApiListTableProps) {
    const navigate = useNavigate();

    return (
        <div className="rounded-lg border">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>API Name</TableHead>
                        <TableHead>Runtime Status</TableHead>
                        <TableHead>Sync Status</TableHead>
                        <TableHead>Access</TableHead>
                        <TableHead>Owner</TableHead>
                        <TableHead className="w-10 text-right" />
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {isLoading ? (
                        Array.from({ length: skeletonRowCount }).map((_, i) => <SkeletonRow key={i} />)
                    ) : apis.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={6} className="py-10 text-center text-sm text-muted-foreground">
                                No APIs found.
                            </TableCell>
                        </TableRow>
                    ) : (
                        apis.map(api => {
                            const accessPath = getApiAccessPath(api);
                            return (
                                <TableRow
                                    key={api.id}
                                    className="cursor-pointer hover:bg-accent"
                                    onClick={() => navigate(`${api.id}/overview`)}
                                >
                                    <TableCell className="font-medium">{api.name}</TableCell>
                                    <TableCell>
                                        <RuntimeStatusBadge state={api.state} />
                                    </TableCell>
                                    <TableCell>
                                        <SyncStatusBadge deploymentState={api.deploymentState} />
                                    </TableCell>
                                    <TableCell>
                                        {accessPath ? (
                                            <Badge variant="outline" className="font-mono text-xs">
                                                {accessPath}
                                            </Badge>
                                        ) : (
                                            <span className="text-muted-foreground text-xs">—</span>
                                        )}
                                    </TableCell>
                                    <TableCell className="text-sm text-muted-foreground">{api.primaryOwner?.displayName ?? '—'}</TableCell>
                                    <TableCell className="text-right">
                                        <ApiActionsMenu apiId={api.id} onNavigate={navigate} />
                                    </TableCell>
                                </TableRow>
                            );
                        })
                    )}
                </TableBody>
            </Table>
        </div>
    );
}
