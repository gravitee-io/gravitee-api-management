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
    DataTablePagination,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { useNavigate } from 'react-router-dom';

import { SubscriptionStatusBadge } from './SubscriptionStatusBadge';
import type { Subscription } from '../../../types/subscription';
import { formatDate } from '../../../utils/formatDate';

interface ConsumersTableProps {
    subscriptions: Subscription[];
    totalCount: number;
    page: number;
    perPage: number;
    isLoading: boolean;
    onPage: (p: number) => void;
    onPerPageChange: (perPage: number) => void;
}

export function ConsumersTable({
    subscriptions,
    totalCount,
    page,
    perPage,
    isLoading,
    onPage,
    onPerPageChange,
}: Readonly<ConsumersTableProps>) {
    const navigate = useNavigate();

    return (
        <div className="space-y-2">
            <DataTablePagination
                page={page}
                pageSize={perPage}
                totalCount={totalCount}
                pageSizeOptions={[10, 25, 50, 100]}
                onPageChange={onPage}
                onPageSizeChange={onPerPageChange}
            />
            <div className="rounded-md border">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Application</TableHead>
                            <TableHead>Plan</TableHead>
                            <TableHead>Security</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead>Created</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {isLoading &&
                            Array.from({ length: perPage }).map((_, i) => (
                                <TableRow key={i}>
                                    {Array.from({ length: 5 }).map((__, j) => (
                                        <TableCell key={j}>
                                            <Skeleton className="h-4 w-full rounded" />
                                        </TableCell>
                                    ))}
                                </TableRow>
                            ))}

                        {!isLoading && subscriptions.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={5} className="py-10 text-center text-sm text-muted-foreground">
                                    No subscriptions match the current filters.
                                </TableCell>
                            </TableRow>
                        )}

                        {!isLoading &&
                            subscriptions.map(sub => (
                                <TableRow key={sub.id} className="cursor-pointer hover:bg-muted" onClick={() => navigate(sub.id)}>
                                    <TableCell>
                                        <div className="font-medium">{sub.application.name}</div>
                                        {sub.application.primaryOwner?.displayName && (
                                            <div className="text-xs text-muted-foreground">{sub.application.primaryOwner.displayName}</div>
                                        )}
                                    </TableCell>
                                    <TableCell>{sub.plan.name}</TableCell>
                                    <TableCell>
                                        {sub.plan.security?.type ? (
                                            <Badge variant="secondary" className="text-xs font-mono">
                                                {sub.plan.security.type === 'KEY_LESS' ? 'Keyless' : sub.plan.security.type}
                                            </Badge>
                                        ) : (
                                            <span className="text-muted-foreground">—</span>
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        <SubscriptionStatusBadge status={sub.status} />
                                    </TableCell>
                                    <TableCell className="text-muted-foreground text-sm">{formatDate(sub.createdAt)}</TableCell>
                                </TableRow>
                            ))}
                    </TableBody>
                </Table>
            </div>
            <DataTablePagination
                page={page}
                pageSize={perPage}
                totalCount={totalCount}
                pageSizeOptions={[10, 25, 50, 100]}
                onPageChange={onPage}
                onPageSizeChange={onPerPageChange}
            />
        </div>
    );
}
