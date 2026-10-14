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
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import {
    ChevronDownIcon,
    ChevronRightIcon,
    ExternalLinkIcon,
    EyeOffIcon,
    FileTextIcon,
    FolderOpenIcon,
    GlobeIcon,
    Link2Icon,
    MoreVerticalIcon,
    PencilIcon,
    PlusIcon,
    RefreshCwIcon,
    Trash2Icon,
} from '@gravitee/graphene-core/icons';
import { type ComponentType, type MouseEvent, useCallback, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import { ITEM_NOUNS, ItemAccessBadge, ItemPublishedBadge } from './DocumentationItemBadges';
import type {
    ApiDocumentationItem,
    PortalNavigationFolder,
    PortalNavigationLink,
    PortalNavigationPage,
} from '../../../types/apiDocumentation';
import {
    allowedChanges,
    buildDocumentationRows,
    type DocumentationRow,
    fetchableItemIds,
    hasSource,
} from '../../../utils/documentationTree';

type ColCell = { row: { original: DocumentationRow } };

interface RowAction {
    label: string;
    icon: ComponentType<{ className?: string }>;
    destructive?: boolean;
    /** Why the action is unavailable, shown on hover and focus; a disabled action is still listed, so its reason can be read. */
    disabledReason?: string;
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
    onAddFolder,
    onAddLink,
    canEdit,
    onEditDetails,
    isApiPublished,
    onChangePublication,
    isFetching,
    onFetch,
}: Readonly<{
    items: ApiDocumentationItem[];
    isLoading: boolean;
    expandedIds: ReadonlySet<string>;
    onToggle: (folderId: string) => void;
    canDelete: boolean;
    onDelete: (row: DocumentationRow) => void;
    canAdd: boolean;
    onAddPage: (folder: PortalNavigationFolder) => void;
    onAddFolder: (folder: PortalNavigationFolder) => void;
    onAddLink: (folder: PortalNavigationFolder) => void;
    canEdit: boolean;
    onEditDetails: (item: ApiDocumentationItem) => void;
    isApiPublished: boolean;
    onChangePublication: (row: DocumentationRow) => void;
    /** Whether a fetch is running: another one is not offered meanwhile, as the server would fetch the same sources twice. */
    isFetching: boolean;
    onFetch: (item: PortalNavigationPage | PortalNavigationFolder) => void;
}>) {
    const rows = useMemo(() => buildDocumentationRows(items, expandedIds), [items, expandedIds]);
    const itemsById = useMemo(() => new Map(items.map(item => [item.id, item])), [items]);
    const fetchable = useMemo(() => fetchableItemIds(items), [items]);
    const [contextMenu, setContextMenu] = useState<RowContextMenuState | null>(null);
    const canEditDetails = useCallback((row: DocumentationRow) => canEdit && allowedChanges(row.syncState).access, [canEdit]);

    // The row button and the right-click menu offer the same actions, built here only.
    const actionsFor = useCallback(
        (row: DocumentationRow): RowAction[] => {
            const { item } = row;
            const allowed = allowedChanges(row.syncState);
            const actions: RowAction[] = [];
            if (canEdit && item.type !== 'LINK' && fetchable.has(item.id)) {
                actions.push({
                    label: 'Fetch now',
                    icon: RefreshCwIcon,
                    disabledReason: isFetching ? 'A fetch is already running.' : undefined,
                    onSelect: () => onFetch(item),
                });
            }
            if (canAdd && item.type === 'FOLDER' && allowed.addInside) {
                actions.push({ label: 'Add page', icon: PlusIcon, onSelect: () => onAddPage(item) });
                actions.push({ label: 'Add folder', icon: FolderOpenIcon, onSelect: () => onAddFolder(item) });
                actions.push({ label: 'Add link', icon: Link2Icon, onSelect: () => onAddLink(item) });
            }
            if (canEditDetails(row)) {
                actions.push({ label: 'Edit details', icon: PencilIcon, onSelect: () => onEditDetails(item) });
            }
            if (canEdit && allowed.publication) {
                actions.push(
                    item.published
                        ? { label: 'Unpublish', icon: EyeOffIcon, onSelect: () => onChangePublication(row) }
                        : {
                              label: 'Publish',
                              icon: GlobeIcon,
                              disabledReason: publishBlockedReason(item, isApiPublished, itemsById),
                              onSelect: () => onChangePublication(row),
                          },
                );
            }
            if (canDelete && allowed.delete) {
                actions.push({ label: 'Delete', icon: Trash2Icon, destructive: true, onSelect: () => onDelete(row) });
            }
            return actions;
        },
        [
            canEdit,
            fetchable,
            isFetching,
            onFetch,
            canEditDetails,
            onEditDetails,
            isApiPublished,
            itemsById,
            onChangePublication,
            canAdd,
            onAddPage,
            onAddFolder,
            onAddLink,
            canDelete,
            onDelete,
        ],
    );

    // A link has no content to open, so its title opens its details instead.
    const editLinkFor = useCallback(
        (row: DocumentationRow) => (row.item.type === 'LINK' && canEditDetails(row) ? () => onEditDetails(row.item) : undefined),
        [canEditDetails, onEditDetails],
    );

    const columns = useMemo(
        () => buildColumns({ hasActions: canEdit || canAdd || canDelete, actionsFor, editLinkFor, onToggle }),
        [canEdit, canAdd, canDelete, actionsFor, editLinkFor, onToggle],
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
    editLinkFor,
    onToggle,
}: {
    hasActions: boolean;
    actionsFor: (row: DocumentationRow) => RowAction[];
    editLinkFor: (row: DocumentationRow) => (() => void) | undefined;
    onToggle: (id: string) => void;
}): DataTableProps<DocumentationRow>['columns'] {
    const columns: DataTableProps<DocumentationRow>['columns'] = [
        {
            id: 'title',
            header: 'Title',
            cell: ({ row }: ColCell) => <TitleCell row={row.original} onEditLink={editLinkFor(row.original)} onToggle={onToggle} />,
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

// The server refuses to publish an item whose folder is unpublished; the whole API's publication covers the top level.
function publishBlockedReason(
    item: ApiDocumentationItem,
    isApiPublished: boolean,
    itemsById: ReadonlyMap<string, ApiDocumentationItem>,
): string | undefined {
    const noun = ITEM_NOUNS[item.type];
    if (!isApiPublished) {
        return `A ${noun} can't be published while the API is unpublished.`;
    }
    const parent = item.parentId === undefined ? undefined : itemsById.get(item.parentId);
    if (parent && !parent.published) {
        return `A ${noun} can't be published inside an unpublished folder.`;
    }
    return undefined;
}

function RowActionItems({ actions }: Readonly<{ actions: RowAction[] }>) {
    return actions.map(({ label, icon: Icon, destructive, disabledReason, onSelect }) => {
        const content = (
            <>
                <Icon className="size-4" aria-hidden />
                {label}
            </>
        );
        if (!disabledReason) {
            return (
                <DropdownMenuItem key={label} variant={destructive ? 'destructive' : 'default'} onSelect={onSelect}>
                    {content}
                </DropdownMenuItem>
            );
        }
        // Marked disabled rather than made so: a disabled item is skipped by the keyboard and ignores the mouse, so
        // its reason could never show. The wrapper takes the hover the item lets through.
        return (
            <TooltipProvider key={label} delayDuration={200}>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span className="block">
                            <DropdownMenuItem aria-disabled data-disabled="" onSelect={event => event.preventDefault()}>
                                {content}
                            </DropdownMenuItem>
                        </span>
                    </TooltipTrigger>
                    <TooltipContent side="right">{disabledReason}</TooltipContent>
                </Tooltip>
            </TooltipProvider>
        );
    });
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

function TitleCell({
    row,
    onEditLink,
    onToggle,
}: Readonly<{ row: DocumentationRow; onEditLink?: () => void; onToggle: (id: string) => void }>) {
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
                ) : onEditLink ? (
                    <button type="button" onClick={onEditLink} className="cursor-pointer font-medium hover:underline">
                        {item.title}
                    </button>
                ) : (
                    <span className="font-medium">{item.title}</span>
                )}
                {item.type === 'LINK' ? <LinkAddress link={item} /> : null}
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

function isWebAddress(url: string): boolean {
    try {
        const { protocol } = new URL(url);
        return protocol === 'http:' || protocol === 'https:';
    } catch {
        return false;
    }
}

// Only an http or https address becomes clickable: in another scheme, clicking it in the console could do more than open a page.
function LinkAddress({ link }: Readonly<{ link: PortalNavigationLink }>) {
    if (!isWebAddress(link.url)) {
        return <div className="truncate text-xs text-muted-foreground">{link.url}</div>;
    }
    return (
        <div className="truncate text-xs text-muted-foreground">
            <a href={link.url} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 hover:underline">
                {link.url}
                <ExternalLinkIcon className="size-3 shrink-0" aria-hidden />
                <span className="sr-only"> (opens in a new tab)</span>
            </a>
        </div>
    );
}
