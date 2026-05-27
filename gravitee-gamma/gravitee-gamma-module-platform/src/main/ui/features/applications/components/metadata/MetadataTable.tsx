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
    DataTablePagination,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { DEFAULT_METADATA_PAGE_SIZE, METADATA_PAGE_SIZE_OPTIONS } from './metadataConstants';
import type { ApplicationMetadata } from '../../types/applicationNotification';

export function MetadataTable({
    metadata,
    isLoading,
    canUpdate,
    canDelete,
    isMutating,
    onEdit,
    onDelete,
}: {
    readonly metadata: ApplicationMetadata[];
    readonly isLoading: boolean;
    readonly canUpdate: boolean;
    readonly canDelete: boolean;
    readonly isMutating: boolean;
    readonly onEdit: (metadata: ApplicationMetadata) => void;
    readonly onDelete: (metadata: ApplicationMetadata) => void;
}) {
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_METADATA_PAGE_SIZE);
    const totalCount = metadata.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
    const paginatedMetadata = useMemo(() => {
        const start = (page - 1) * pageSize;
        return metadata.slice(start, start + pageSize);
    }, [metadata, page, pageSize]);

    useEffect(() => {
        if (page > totalPages) {
            setPage(totalPages);
        }
    }, [page, totalPages]);

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    const hasActions = canUpdate || canDelete;

    if (isLoading) {
        return (
            <div className="space-y-2">
                {Array.from({ length: pageSize }).map((_, index) => (
                    <Skeleton key={index} className="h-10 rounded-lg" />
                ))}
            </div>
        );
    }

    return (
        <div className="space-y-4">
            <div className="flex justify-end">
                <DataTablePagination
                    page={page}
                    pageSize={pageSize}
                    totalCount={totalCount}
                    pageSizeOptions={METADATA_PAGE_SIZE_OPTIONS}
                    onPageChange={setPage}
                    onPageSizeChange={handlePageSizeChange}
                />
            </div>
            <Table aria-label="Application metadata">
                <TableHeader>
                    <TableRow>
                        <TableHead>Key</TableHead>
                        <TableHead>Name</TableHead>
                        <TableHead>Format</TableHead>
                        <TableHead>Value</TableHead>
                        {hasActions ? <TableHead className="w-24 text-right" aria-label="Actions" /> : null}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {metadata.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={hasActions ? 5 : 4} className="h-16 text-center text-sm text-muted-foreground">
                                No metadata configured.
                            </TableCell>
                        </TableRow>
                    ) : (
                        paginatedMetadata.map(item => {
                            const value = item.value ?? item.defaultValue ?? '—';
                            const isDeletable = item.value !== undefined;
                            return (
                                <TableRow key={item.key}>
                                    <TableCell className="font-medium">{item.key}</TableCell>
                                    <TableCell>{item.name}</TableCell>
                                    <TableCell className="text-sm text-muted-foreground">{item.format ?? 'STRING'}</TableCell>
                                    <TableCell>{value}</TableCell>
                                    {hasActions ? (
                                        <TableCell className="text-right">
                                            <div className="flex justify-end gap-1">
                                                {canUpdate ? (
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8"
                                                        aria-label={`Edit ${item.name} metadata`}
                                                        disabled={isMutating}
                                                        onClick={() => onEdit(item)}
                                                    >
                                                        <PencilIcon className="size-4" aria-hidden />
                                                    </Button>
                                                ) : null}
                                                {canDelete && isDeletable ? (
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8 text-destructive hover:text-destructive"
                                                        aria-label={`Delete ${item.name} metadata`}
                                                        disabled={isMutating}
                                                        onClick={() => onDelete(item)}
                                                    >
                                                        <Trash2Icon className="size-4" aria-hidden />
                                                    </Button>
                                                ) : null}
                                            </div>
                                        </TableCell>
                                    ) : null}
                                </TableRow>
                            );
                        })
                    )}
                </TableBody>
            </Table>
            <div className="flex justify-end">
                <DataTablePagination
                    page={page}
                    pageSize={pageSize}
                    totalCount={totalCount}
                    pageSizeOptions={METADATA_PAGE_SIZE_OPTIONS}
                    onPageChange={setPage}
                    onPageSizeChange={handlePageSizeChange}
                />
            </div>
        </div>
    );
}
