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
    DataTable,
    type DataTableProps,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@gravitee/graphene-core';
import {
    ChevronDownIcon,
    ChevronRightIcon,
    FileTextIcon,
    FolderOpenIcon,
    Link2Icon,
    MoreVerticalIcon,
    PlusIcon,
    RefreshCwIcon,
    Trash2Icon,
} from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import { ItemAccessBadge, ItemPublishedBadge } from './DocumentationItemBadges';
import type { ApiDocumentationItem, PortalNavigationFolder } from '../../../types/apiDocumentation';
import { buildDocumentationRows, type DocumentationRow, hasSource } from '../../../utils/documentationTree';

type ColCell = { row: { original: DocumentationRow } };

const TYPE_ICONS = { PAGE: FileTextIcon, FOLDER: FolderOpenIcon, LINK: Link2Icon } as const;

export function DocumentationTree({
    items,
    isLoading,
    canDelete,
    onDelete,
    canAdd,
    onAddPage,
}: Readonly<{
    items: ApiDocumentationItem[];
    isLoading: boolean;
    canDelete: boolean;
    onDelete: (row: DocumentationRow) => void;
    canAdd: boolean;
    onAddPage: (folder: PortalNavigationFolder) => void;
}>) {
    const [expandedIds, setExpandedIds] = useState<ReadonlySet<string>>(new Set());
    const rows = useMemo(() => buildDocumentationRows(items, expandedIds), [items, expandedIds]);

    const columns = useMemo(() => {
        function toggle(id: string) {
            setExpandedIds(previous => {
                const next = new Set(previous);
                if (!next.delete(id)) next.add(id);
                return next;
            });
        }
        return buildColumns({ canDelete, onDelete, canAdd, onAddPage, onToggle: toggle });
    }, [canDelete, onDelete, canAdd, onAddPage]);

    return (
        <DataTable
            aria-label="Documentation"
            columns={columns}
            data={rows}
            loading={isLoading}
            tableOptions={{ getRowId: row => row.item.id }}
        />
    );
}

function buildColumns({
    canDelete,
    onDelete,
    canAdd,
    onAddPage,
    onToggle,
}: {
    canDelete: boolean;
    onDelete: (row: DocumentationRow) => void;
    canAdd: boolean;
    onAddPage: (folder: PortalNavigationFolder) => void;
    onToggle: (id: string) => void;
}): DataTableProps<DocumentationRow>['columns'] {
    const columns: DataTableProps<DocumentationRow>['columns'] = [
        {
            id: 'title',
            header: 'Title',
            cell: ({ row }: ColCell) => <TitleCell row={row.original} onToggle={onToggle} />,
        },
        {
            id: 'status',
            header: 'Status',
            size: 120,
            cell: ({ row }: ColCell) => <ItemPublishedBadge published={row.original.item.published} itemType={row.original.item.type} />,
        },
        {
            id: 'access',
            header: 'Access',
            size: 120,
            cell: ({ row }: ColCell) => <ItemAccessBadge visibility={row.original.item.visibility} itemType={row.original.item.type} />,
        },
    ];

    if (canDelete || canAdd) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            cell: ({ row }: ColCell) => {
                const { item, synced } = row.original;
                // The server refuses to add anything below a synced folder, or to delete what it syncs.
                if (synced) return null;
                const targetFolder = canAdd && item.type === 'FOLDER' ? item : null;
                if (!targetFolder && !canDelete) return null;
                return (
                    <div className="flex justify-end">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    className="size-8"
                                    aria-label={`Actions for ${row.original.item.title}`}
                                >
                                    <MoreVerticalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="min-w-48">
                                {targetFolder ? (
                                    <DropdownMenuItem onSelect={() => onAddPage(targetFolder)}>
                                        <PlusIcon className="size-4" aria-hidden />
                                        Add page
                                    </DropdownMenuItem>
                                ) : null}
                                {canDelete ? (
                                    <DropdownMenuItem variant="destructive" onSelect={() => onDelete(row.original)}>
                                        <Trash2Icon className="size-4" aria-hidden />
                                        Delete
                                    </DropdownMenuItem>
                                ) : null}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
        });
    }

    return columns;
}

function TitleCell({ row, onToggle }: Readonly<{ row: DocumentationRow; onToggle: (id: string) => void }>) {
    const { item, depth, hasChildren, expanded } = row;
    const TypeIcon = TYPE_ICONS[item.type];

    return (
        <div className="flex min-w-0 items-center gap-2" style={{ paddingLeft: `${depth * 1.5}rem` }}>
            {hasChildren ? (
                <Button
                    type="button"
                    variant="ghost"
                    size="icon-xs"
                    aria-expanded={expanded}
                    aria-label={`${expanded ? 'Collapse' : 'Expand'} ${item.title}`}
                    onClick={() => onToggle(item.id)}
                >
                    {expanded ? <ChevronDownIcon className="size-4" aria-hidden /> : <ChevronRightIcon className="size-4" aria-hidden />}
                </Button>
            ) : (
                <span className="size-6 shrink-0" aria-hidden />
            )}
            <TypeIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
            <div className="min-w-0">
                {item.type === 'PAGE' ? (
                    <Link to={`${encodeURIComponent(item.id)}/edit`} className="font-medium hover:underline">
                        {item.title}
                    </Link>
                ) : (
                    <span className="font-medium">{item.title}</span>
                )}
                {item.type === 'LINK' ? <div className="truncate text-xs text-muted-foreground">{item.url}</div> : null}
            </div>
            {hasSource(item) ? (
                <Badge variant="outline" className="gap-1">
                    <RefreshCwIcon className="size-3" aria-hidden />
                    Synced
                </Badge>
            ) : null}
        </div>
    );
}
