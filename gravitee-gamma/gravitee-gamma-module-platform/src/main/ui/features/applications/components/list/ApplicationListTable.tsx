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
    Tooltip,
    TooltipContent,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { AppWindowIcon, ExternalLinkIcon, MoreHorizontalIcon, PlugIcon, Wand2Icon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import type { ApplicationListItem, ApplicationStatus } from '../../types/application';
import { formatApplicationDateTime, formatApplicationTypeLabel } from '../../utils/applicationFormatters';

function ActiveSkeletonRow() {
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
            <TableCell />
        </TableRow>
    );
}

function ArchivedSkeletonRow() {
    return (
        <TableRow>
            <TableCell>
                <Skeleton className="h-4 w-40 rounded" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-4 w-32 rounded" />
            </TableCell>
            <TableCell />
        </TableRow>
    );
}

function ApplicationActionsMenu({ applicationId, onNavigate }: { applicationId: string; onNavigate: (path: string) => void }) {
    const navigateTo = (path: string) => (event: Event) => {
        event.preventDefault();
        onNavigate(path);
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="Application actions" onClick={event => event.stopPropagation()}>
                    <MoreHorizontalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-auto min-w-[12rem]" onClick={event => event.stopPropagation()}>
                <DropdownMenuItem className="gap-2" onSelect={navigateTo(`${applicationId}/general`)}>
                    <ExternalLinkIcon className="size-4" aria-hidden />
                    View Details
                </DropdownMenuItem>
                <DropdownMenuItem className="gap-2" onSelect={navigateTo(`${applicationId}/subscriptions`)}>
                    <PlugIcon className="size-4" aria-hidden />
                    Manage Subscriptions
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

interface ApplicationListTableProps {
    readonly applications: ApplicationListItem[];
    readonly isLoading: boolean;
    readonly status: ApplicationStatus;
    readonly skeletonRowCount?: number;
    readonly onRestore?: (application: ApplicationListItem) => void;
}

export function ApplicationListTable({ applications, isLoading, status, skeletonRowCount = 5, onRestore }: ApplicationListTableProps) {
    const navigate = useNavigate();
    const isArchived = status === 'ARCHIVED';

    return (
        <div className="rounded-lg border">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Name</TableHead>
                        {isArchived ? (
                            <>
                                <TableHead>Archived at</TableHead>
                                <TableHead className="w-12" />
                            </>
                        ) : (
                            <>
                                <TableHead>Type</TableHead>
                                <TableHead>Owner</TableHead>
                                <TableHead className="w-12" />
                            </>
                        )}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {isLoading ? (
                        Array.from({ length: skeletonRowCount }).map((_, index) =>
                            isArchived ? <ArchivedSkeletonRow key={index} /> : <ActiveSkeletonRow key={index} />,
                        )
                    ) : applications.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={isArchived ? 3 : 4} className="py-10 text-center text-sm text-muted-foreground">
                                No applications found.
                            </TableCell>
                        </TableRow>
                    ) : isArchived ? (
                        applications.map(application => (
                            <TableRow key={application.id}>
                                <TableCell>
                                    <div className="flex items-center gap-2 font-medium">
                                        <AppWindowIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                                        {application.name}
                                    </div>
                                </TableCell>
                                <TableCell className="text-sm text-muted-foreground">
                                    {formatApplicationDateTime(application.updated_at)}
                                </TableCell>
                                <TableCell className="text-right">
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon"
                                                className="size-8"
                                                aria-label={`Restore ${application.name}`}
                                                onClick={() => onRestore?.(application)}
                                            >
                                                <Wand2Icon className="size-4" aria-hidden />
                                            </Button>
                                        </TooltipTrigger>
                                        <TooltipContent>Restore application</TooltipContent>
                                    </Tooltip>
                                </TableCell>
                            </TableRow>
                        ))
                    ) : (
                        applications.map(application => (
                            <TableRow
                                key={application.id}
                                className="cursor-pointer hover:bg-accent"
                                onClick={() => navigate(`${application.id}/general`)}
                            >
                                <TableCell>
                                    <div className="flex items-center gap-2 font-medium">
                                        <AppWindowIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                                        {application.name}
                                    </div>
                                </TableCell>
                                <TableCell>
                                    <Badge variant="outline" className="border-border bg-background">
                                        {formatApplicationTypeLabel(application)}
                                    </Badge>
                                </TableCell>
                                <TableCell className="text-sm text-muted-foreground">{application.owner?.displayName ?? '—'}</TableCell>
                                <TableCell className="text-right" onClick={event => event.stopPropagation()}>
                                    <ApplicationActionsMenu applicationId={application.id} onNavigate={navigate} />
                                </TableCell>
                            </TableRow>
                        ))
                    )}
                </TableBody>
            </Table>
        </div>
    );
}
