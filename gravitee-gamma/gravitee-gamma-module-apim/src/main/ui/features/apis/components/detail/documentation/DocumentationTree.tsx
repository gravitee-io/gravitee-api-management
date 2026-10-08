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
import { type ComponentType, type MouseEvent, useCallback, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import { ItemAccessBadge, ItemPublishedBadge } from './DocumentationItemBadges';
import type { ApiDocumentationItem, PortalNavigationFolder } from '../../../types/apiDocumentation';
import { buildDocumentationRows, type DocumentationRow, hasSource } from '../../../utils/documentationTree';

type ColCell = { row: { original: DocumentationRow } };

interface RowAction {
    label: string;
    icon: ComponentType<{ className?: string }>;
    destructive?: boolean;
    onSelect: () => void;
}

interface RowContextMenuState {
    actions: RowAction[];
    x: number;
    y: number;
}

const TYPE_ICONS = { PAGE: FileTextIcon, FOLDER: FolderOpenIcon, LINK: Link2Icon } as const;

export function DocumentationTree({
    items,
    isLoading,
    expandedIds,
    onToggle,
    canDelete,
    onDelete,
    canAdd,
    onAddPage,
}: Readonly<{
    items: ApiDocumentationItem[];
    isLoading: boolean;
    expandedIds: ReadonlySet<string>;
    onToggle: (folderId: string) => void;
    canDelete: boolean;
    onDelete: (row: DocumentationRow) => void;
    canAdd: boolean;
    onAddPage: (folder: PortalNavigationFolder) => void;
}>) {
    const rows = useMemo(() => buildDocumentationRows(items, expandedIds), [items, expandedIds]);
    const [contextMenu, setContextMenu] = useState<RowContextMenuState | null>(null);

    // The row button and the right-click menu offer the same actions, built here only.
    const actionsFor = useCallback(
        (row: DocumentationRow): RowAction[] => {
            // The server refuses to add anything below a synced folder, or to delete what it syncs.
            if (row.synced) return [];
            const { item } = row;
            const actions: RowAction[] = [];
            if (canAdd && item.type === 'FOLDER') {
                actions.push({ label: 'Add page', icon: PlusIcon, onSelect: () => onAddPage(item) });
            }
            if (canDelete) {
                actions.push({ label: 'Delete', icon: Trash2Icon, destructive: true, onSelect: () => onDelete(row) });
            }
            return actions;
        },
        [canAdd, onAddPage, canDelete, onDelete],
    );

    const columns = useMemo(
        () => buildColumns({ hasActions: canAdd || canDelete, actionsFor, onToggle }),
        [canAdd, canDelete, actionsFor, onToggle],
    );

    // DataTable takes no props for its rows, so the row is found from the title cell it contains.
    function openContextMenu(event: MouseEvent<HTMLDivElement>) {
        const itemId =
            event.target instanceof Element
                ? event.target.closest('tr')?.querySelector('[data-documentation-item]')?.getAttribute('data-documentation-item')
                : undefined;
        const row = rows.find(candidate => candidate.item.id === itemId);
        const actions = row ? actionsFor(row) : [];
        // Without an action, the browser keeps its own menu.
        if (actions.length === 0) return;
        event.preventDefault();
        setContextMenu({ actions, x: event.clientX, y: event.clientY });
    }

    return (
        <div onContextMenu={openContextMenu}>
            <DataTable
                aria-label="Documentation"
                columns={columns}
                data={rows}
                loading={isLoading}
                tableOptions={{ getRowId: row => row.item.id }}
            />
            {contextMenu ? (
                <RowContextMenu key={`${contextMenu.x},${contextMenu.y}`} menu={contextMenu} onClose={() => setContextMenu(null)} />
            ) : null}
        </div>
    );
}

function buildColumns({
    hasActions,
    actionsFor,
    onToggle,
}: {
    hasActions: boolean;
    actionsFor: (row: DocumentationRow) => RowAction[];
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

    if (hasActions) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            cell: ({ row }: ColCell) => {
                const actions = actionsFor(row.original);
                if (actions.length === 0) return null;
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
                                <RowActionItems actions={actions} />
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
        });
    }

    return columns;
}

function RowActionItems({ actions }: Readonly<{ actions: RowAction[] }>) {
    return actions.map(({ label, icon: Icon, destructive, onSelect }) => (
        <DropdownMenuItem key={label} variant={destructive ? 'destructive' : 'default'} onSelect={onSelect}>
            <Icon className="size-4" aria-hidden />
            {label}
        </DropdownMenuItem>
    ));
}

// The row's own button stays the way in for keyboard users; this only adds right-click.
function RowContextMenu({ menu, onClose }: Readonly<{ menu: RowContextMenuState; onClose: () => void }>) {
    return (
        <DropdownMenu open modal={false} onOpenChange={open => !open && onClose()}>
            <DropdownMenuTrigger asChild>
                <span className="pointer-events-none fixed size-0" style={{ left: menu.x, top: menu.y }} />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="min-w-48" onCloseAutoFocus={event => event.preventDefault()}>
                <RowActionItems actions={menu.actions} />
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

function TitleCell({ row, onToggle }: Readonly<{ row: DocumentationRow; onToggle: (id: string) => void }>) {
    const { item, depth, hasChildren, expanded } = row;
    const TypeIcon = TYPE_ICONS[item.type];

    return (
        <div className="flex min-w-0 items-center gap-2" style={{ paddingLeft: `${depth * 1.5}rem` }} data-documentation-item={item.id}>
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
