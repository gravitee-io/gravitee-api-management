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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useCallback } from 'react';

import type { NotificationChannel, NotificationRow } from '../../../hooks/useApiNotifications';
import { CHANNEL_ICON, CHANNEL_LABEL } from '../../../utils/notificationFormatters';

// ─── Channel badge ────────────────────────────────────────────────────────────

function ChannelBadge({ channel }: Readonly<{ channel: NotificationChannel }>) {
    const Icon = CHANNEL_ICON[channel];
    return (
        <Badge variant="outline" className="gap-1">
            <Icon className="size-3" />
            {CHANNEL_LABEL[channel]}
        </Badge>
    );
}

// ─── Event count badge ────────────────────────────────────────────────────────

function EventCountBadge({ count }: Readonly<{ count: number }>) {
    if (count === 0) return <span className="text-xs text-muted-foreground italic">None</span>;
    return (
        <Badge variant="secondary" className="text-xs">
            {count} event{count !== 1 ? 's' : ''}
        </Badge>
    );
}

// ─── Loading rows ─────────────────────────────────────────────────────────────

function LoadingRows({ hasActionsColumn }: Readonly<{ hasActionsColumn: boolean }>) {
    return (
        <>
            {[1, 2, 3].map(i => (
                <TableRow key={i}>
                    <TableCell>
                        <Skeleton className="h-4 w-36 rounded" />
                    </TableCell>
                    <TableCell>
                        <Skeleton className="h-5 w-20 rounded-full" />
                    </TableCell>
                    <TableCell>
                        <Skeleton className="h-5 w-16 rounded-full" />
                    </TableCell>
                    <TableCell>
                        <Skeleton className="h-4 w-48 rounded" />
                    </TableCell>
                    {hasActionsColumn && <TableCell />}
                </TableRow>
            ))}
        </>
    );
}

// ─── Main component ───────────────────────────────────────────────────────────

interface NotificationsTableProps {
    rows: NotificationRow[];
    isLoading: boolean;
    editingKey: string | null;
    canUpdate: boolean;
    canDelete: boolean;
    onEdit: (key: string) => void;
    onDelete: (row: NotificationRow) => void;
}

export function NotificationsTable({
    rows,
    isLoading,
    editingKey,
    canUpdate,
    canDelete,
    onEdit,
    onDelete,
}: Readonly<NotificationsTableProps>) {
    const hasActionsColumn = canUpdate || canDelete;
    const rowHasActions = useCallback(
        (row: NotificationRow) => (canUpdate && !row.isReadonly) || (canDelete && row.canDelete),
        [canUpdate, canDelete],
    );
    return (
        <Card>
            <CardHeader className="pb-0">
                <CardTitle className="text-base">Configured notifications</CardTitle>
            </CardHeader>
            <CardContent className="p-0 pt-2">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Name</TableHead>
                            <TableHead>Channel</TableHead>
                            <TableHead>Events</TableHead>
                            <TableHead>Target</TableHead>
                            {hasActionsColumn && <TableHead className="w-12" />}
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {isLoading ? (
                            <LoadingRows hasActionsColumn={hasActionsColumn} />
                        ) : (
                            rows.map(row => (
                                <TableRow
                                    key={row.key}
                                    style={
                                        editingKey === row.key
                                            ? { backgroundColor: 'color-mix(in oklab, var(--color-accent) 30%, transparent)' }
                                            : undefined
                                    }
                                >
                                    <TableCell className="font-medium">{row.notification.name}</TableCell>
                                    <TableCell>
                                        <ChannelBadge channel={row.channel} />
                                    </TableCell>
                                    <TableCell>
                                        <EventCountBadge
                                            count={row.notification.hooks.length + (row.notification.groupHooks?.length ?? 0)}
                                        />
                                    </TableCell>
                                    <TableCell>
                                        <span
                                            className="text-xs text-muted-foreground font-mono"
                                            style={{
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                whiteSpace: 'nowrap',
                                                display: 'block',
                                                maxWidth: '18rem',
                                            }}
                                        >
                                            {row.notification.config ?? '—'}
                                        </span>
                                    </TableCell>
                                    {hasActionsColumn && (
                                        <TableCell onClick={e => e.stopPropagation()}>
                                            {rowHasActions(row) && (
                                                <DropdownMenu>
                                                    <DropdownMenuTrigger asChild>
                                                        <Button
                                                            type="button"
                                                            variant="ghost"
                                                            size="icon"
                                                            className="size-8"
                                                            aria-label={`Actions for ${row.notification.name}`}
                                                        >
                                                            <MoreHorizontalIcon className="size-4" />
                                                        </Button>
                                                    </DropdownMenuTrigger>
                                                    <DropdownMenuContent align="end">
                                                        {canUpdate && !row.isReadonly && (
                                                            <DropdownMenuItem onSelect={() => onEdit(row.key)}>
                                                                <PencilIcon className="size-4" />
                                                                Edit events
                                                            </DropdownMenuItem>
                                                        )}
                                                        {canDelete && row.canDelete && (
                                                            <>
                                                                {canUpdate && !row.isReadonly && <DropdownMenuSeparator />}
                                                                <DropdownMenuItem
                                                                    onSelect={() => onDelete(row)}
                                                                    className="text-destructive focus:text-destructive"
                                                                >
                                                                    <Trash2Icon className="size-4" />
                                                                    Delete
                                                                </DropdownMenuItem>
                                                            </>
                                                        )}
                                                    </DropdownMenuContent>
                                                </DropdownMenu>
                                            )}
                                        </TableCell>
                                    )}
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </CardContent>
        </Card>
    );
}
