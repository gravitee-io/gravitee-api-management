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
import { Badge, Skeleton, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@gravitee/graphene-core';
import { AppWindowIcon } from '@gravitee/graphene-core/icons';

import type { ApplicationListItem } from '../../types/application';
import { formatApplicationTypeLabel } from '../../utils/applicationFormatters';

function SkeletonRow() {
    return (
        <TableRow>
            <TableCell>
                <Skeleton className="h-4 w-40 rounded" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-5 w-16 rounded-full" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-4 w-24 rounded" />
            </TableCell>
        </TableRow>
    );
}

interface ApplicationListTableProps {
    readonly applications: ApplicationListItem[];
    readonly isLoading: boolean;
    readonly skeletonRowCount?: number;
}

export function ApplicationListTable({ applications, isLoading, skeletonRowCount = 5 }: ApplicationListTableProps) {
    return (
        <div className="rounded-lg border">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Name</TableHead>
                        <TableHead>Type</TableHead>
                        <TableHead>Owner</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {isLoading ? (
                        Array.from({ length: skeletonRowCount }).map((_, i) => <SkeletonRow key={i} />)
                    ) : applications.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={3} className="py-10 text-center text-sm text-muted-foreground">
                                No applications found.
                            </TableCell>
                        </TableRow>
                    ) : (
                        applications.map(application => (
                            <TableRow key={application.id} className="hover:bg-accent">
                                <TableCell>
                                    <div className="flex items-center gap-2 font-medium">
                                        <AppWindowIcon className="size-4 text-muted-foreground shrink-0" aria-hidden />
                                        {application.name}
                                    </div>
                                </TableCell>
                                <TableCell>
                                    <Badge variant="outline" className="border-border bg-background">
                                        {formatApplicationTypeLabel(application)}
                                    </Badge>
                                </TableCell>
                                <TableCell className="text-sm text-muted-foreground">{application.owner?.displayName ?? '—'}</TableCell>
                            </TableRow>
                        ))
                    )}
                </TableBody>
            </Table>
        </div>
    );
}
