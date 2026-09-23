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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    Badge,
    Button,
    Card,
    CardContent,
    Checkbox,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
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
    TooltipProvider,
    TooltipTrigger,
    cn,
} from '@gravitee/graphene-core';
import {
    ChevronDownIcon,
    ChevronRightIcon,
    EyeOffIcon,
    FolderOpenIcon,
    GlobeIcon,
    GripVerticalIcon,
    MoreHorizontalIcon,
    PencilIcon,
    PlusIcon,
    Trash2Icon,
} from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { DocumentationFolderDialog } from './DocumentationFolderDialog';
import { DocumentationPublishApiDialog } from './DocumentationPublishApiDialog';
import { DocumentationPublishDialog } from './DocumentationPublishDialog';
import { DocumentationEmptyLanding, PAGE_TYPES, TypeIcon, typeLabel } from './documentation-shared';
import { ConfirmDialog } from '../../../../../shared/components';
import { notify } from '../../../../../shared/notify';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import {
    useApiDocumentationTree,
    useCreateDocumentationPage,
    useDeleteDocumentationPage,
    usePortalDocumentationFolders,
    usePublishApiWithDefaultOverview,
    usePublishDocumentationPage,
    useSyncDocumentationToPortal,
    useUnpublishApiFromPortal,
    useUnpublishDocumentationFromPortal,
    useUnpublishDocumentationPage,
    useUpdateDocumentationPage,
} from '../../../hooks/useApiDocumentation';
import type { DocumentationPage, PageType } from '../../../types/documentation';
import { formatUpdatedAt, normalizeParentId, toApiParentId } from '../../../utils/documentationFormatters';

type FolderDialogState = { open: false } | { open: true; folder?: DocumentationPage; parentId: string | null };

interface TreeRow {
    item: DocumentationPage;
    depth: number;
    childCount: number;
}

function siblingsOf(items: DocumentationPage[], parentId: string | null): DocumentationPage[] {
    return items
        .filter(item => normalizeParentId(item.parentId) === parentId)
        .sort((a, b) => (a.order ?? 0) - (b.order ?? 0) || (a.name ?? '').localeCompare(b.name ?? ''));
}

function moveById(items: DocumentationPage[], fromId: string, toId: string): DocumentationPage[] {
    const from = items.findIndex(item => item.id === fromId);
    const to = items.findIndex(item => item.id === toId);
    if (from < 0 || to < 0 || from === to) return items;
    const next = [...items];
    const [removed] = next.splice(from, 1);
    if (!removed) return items;
    next.splice(to, 0, removed);
    return next;
}

function flattenTree(items: DocumentationPage[], parentId: string | null, depth: number, expanded: Set<string>): TreeRow[] {
    const rows: TreeRow[] = [];
    siblingsOf(items, parentId).forEach(item => {
        const childCount = item.type === 'FOLDER' && item.id ? siblingsOf(items, item.id).length : 0;
        rows.push({ item, depth, childCount });
        if (item.type === 'FOLDER' && item.id && expanded.has(item.id)) {
            rows.push(...flattenTree(items, item.id, depth + 1, expanded));
        }
    });
    return rows;
}

function folderIds(items: DocumentationPage[]): string[] {
    return items.filter(item => item.type === 'FOLDER' && item.id).map(item => item.id!);
}

function isPublishable(item: DocumentationPage): boolean {
    return normalizeParentId(item.parentId) === null;
}

function collectDescendantIds(items: DocumentationPage[], rootId: string): string[] {
    const ids = [rootId];
    siblingsOf(items, rootId).forEach(child => {
        if (child.id) ids.push(...collectDescendantIds(items, child.id));
    });
    return ids;
}

function PageTypeMenuItems({ onSelect }: { onSelect: (pageType: (typeof PAGE_TYPES)[number]) => void }) {
    return PAGE_TYPES.map(pageType => (
        <DropdownMenuItem key={pageType} className="gap-2" onSelect={() => onSelect(pageType)}>
            <TypeIcon type={pageType} className="size-4 shrink-0" />
            {typeLabel(pageType)}
        </DropdownMenuItem>
    ));
}

export function ApiDocumentationPagesPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const navigate = useNavigate();
    const { api } = useApiDetailContext();

    const canCreate = useHasPermission({ anyOf: ['api-documentation-c'] });
    const canUpdate = useHasPermission({ anyOf: ['api-documentation-u'] });
    const canDelete = useHasPermission({ anyOf: ['api-documentation-d'] });
    const isKubernetes = api?.definitionContext?.origin === 'KUBERNETES';
    const canModify = (canCreate || canUpdate) && !isKubernetes;
    const canReorder = canUpdate && !isKubernetes;
    const canPublish = canUpdate && !isKubernetes;

    const { pages, isLoading, isError, refetch } = useApiDocumentationTree(apiId);
    const createMutation = useCreateDocumentationPage(apiId ?? '');
    const updateMutation = useUpdateDocumentationPage(apiId ?? '');
    const publishMutation = usePublishDocumentationPage(apiId ?? '');
    const unpublishMutation = useUnpublishDocumentationPage(apiId ?? '');
    const deleteMutation = useDeleteDocumentationPage(apiId ?? '');
    const portalSyncMutation = useSyncDocumentationToPortal(apiId ?? '');
    const portalUnpublishMutation = useUnpublishDocumentationFromPortal(apiId ?? '');
    const unpublishApiMutation = useUnpublishApiFromPortal(apiId ?? '');
    const publishEmptyApiMutation = usePublishApiWithDefaultOverview(apiId ?? '');

    const [folderDialog, setFolderDialog] = useState<FolderDialogState>({ open: false });
    const [pendingDelete, setPendingDelete] = useState<DocumentationPage | null>(null);
    const [dragId, setDragId] = useState<string | null>(null);
    const [overId, setOverId] = useState<string | null>(null);
    const [expanded, setExpanded] = useState<Set<string>>(new Set());
    const [expandedReady, setExpandedReady] = useState(false);
    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
    const [pendingPublishIds, setPendingPublishIds] = useState<string[] | null>(null);
    const [pendingUnpublishIds, setPendingUnpublishIds] = useState<string[] | null>(null);
    const [publishApiOpen, setPublishApiOpen] = useState(false);
    const [unpublishApiOpen, setUnpublishApiOpen] = useState(false);
    const [isPublishing, setIsPublishing] = useState(false);

    const portalQuery = usePortalDocumentationFolders(apiId, canPublish);
    const folders = portalQuery.data?.folders ?? [];
    const placement = portalQuery.data?.placement ?? null;

    useEffect(() => {
        if (expandedReady || pages.length === 0) return;
        setExpanded(new Set(folderIds(pages)));
        setExpandedReady(true);
    }, [expandedReady, pages]);

    const rows = useMemo(() => flattenTree(pages, null, 0, expanded), [expanded, pages]);
    const publishableItems = useMemo(() => pages.filter(isPublishable), [pages]);
    const selectedPublishable = useMemo(
        () => publishableItems.filter(item => item.id && selectedIds.has(item.id)),
        [publishableItems, selectedIds],
    );
    const selectedUnpublished = selectedPublishable.filter(item => !item.published);
    const selectedPublished = selectedPublishable.filter(item => item.published);
    const allSelected = publishableItems.length > 0 && selectedPublishable.length === publishableItems.length;
    const someSelected = selectedPublishable.length > 0 && !allSelected;

    const folderExistingNames = useMemo(() => {
        if (!folderDialog.open) return [];
        const parentId = folderDialog.folder ? normalizeParentId(folderDialog.folder.parentId) : folderDialog.parentId;
        const editingId = folderDialog.folder?.id;
        return siblingsOf(pages, parentId)
            .filter(item => item.id !== editingId)
            .map(item => (item.name ?? '').toLowerCase().trim());
    }, [folderDialog, pages]);

    function toggleFolder(id: string) {
        setExpanded(current => {
            const next = new Set(current);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    }

    function toggleSelected(id: string) {
        setSelectedIds(current => {
            const next = new Set(current);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    }

    function toggleSelectAll() {
        if (allSelected) {
            setSelectedIds(new Set());
            return;
        }
        setSelectedIds(new Set(publishableItems.map(item => item.id).filter((id): id is string => Boolean(id))));
    }

    function openNewPage(pageType: PageType, parentId?: string | null) {
        const params = new URLSearchParams({ pageType });
        if (parentId) params.set('parentId', parentId);
        navigate({ pathname: 'new', search: `?${params}` });
    }

    function openPage(item: DocumentationPage) {
        if (item.type === 'FOLDER') {
            if (item.id) toggleFolder(item.id);
            return;
        }
        if (item.id) navigate(item.id);
    }

    const saveFolder = useCallback(
        ({ name }: { name: string }) => {
            if (!apiId || !folderDialog.open) return;
            const editing = folderDialog.folder;
            const parentId = editing ? toApiParentId(editing.parentId) : toApiParentId(folderDialog.parentId);
            if (editing?.id) {
                updateMutation.mutate(
                    { pageId: editing.id, payload: { ...editing, name, type: 'FOLDER' } },
                    {
                        onSuccess: () => {
                            notify.success('Folder updated');
                            setFolderDialog({ open: false });
                        },
                        onError: error => notify.error(error, 'Could not update folder.'),
                    },
                );
                return;
            }
            createMutation.mutate(
                { name, type: 'FOLDER', parentId },
                {
                    onSuccess: created => {
                        if (folderDialog.parentId) {
                            setExpanded(current => new Set(current).add(folderDialog.parentId!));
                        }
                        notify.success('Folder created');
                        setFolderDialog({ open: false });
                        if (created.id) void refetch();
                    },
                    onError: error => notify.error(error, 'Could not create folder.'),
                },
            );
        },
        [apiId, createMutation, folderDialog, refetch, updateMutation],
    );

    function persistOrder(parentId: string | null, nextSiblings: DocumentationPage[]) {
        nextSiblings.forEach((item, index) => {
            if (!item.id || item.order === index) return;
            updateMutation.mutate({ pageId: item.id, payload: { ...item, order: index } });
        });
    }

    function handleDrop(targetId: string) {
        const sourceId = dragId;
        setDragId(null);
        setOverId(null);
        if (!sourceId || sourceId === targetId) return;
        const source = pages.find(item => item.id === sourceId);
        const target = pages.find(item => item.id === targetId);
        if (!source || !target || normalizeParentId(source.parentId) !== normalizeParentId(target.parentId)) return;
        const next = moveById(siblingsOf(pages, normalizeParentId(source.parentId)), sourceId, targetId);
        persistOrder(normalizeParentId(source.parentId), next);
    }

    function openPublish(ids: string[]) {
        setSelectedIds(new Set(ids));
        setPendingPublishIds(ids);
        void portalQuery.refetch();
    }

    async function publishToPortal(folderId: string) {
        if (!pendingPublishIds?.length) return;

        const availableFolders = portalQuery.data?.folders ?? folders;
        const currentPlacement = portalQuery.data?.placement ?? placement;
        const folder = availableFolders.find(entry => entry.id === folderId);

        if (!folder) {
            notify.error(undefined, 'Select a Navigation folder to publish this API and its documentation.');
            return;
        }

        setIsPublishing(true);
        try {
            const idsToPublish = [...new Set(pendingPublishIds.flatMap(id => collectDescendantIds(pages, id)))];
            const classicIds = idsToPublish.filter(id => {
                const page = pages.find(item => item.id === id);
                return Boolean(page?.id && page.id !== page.portalNavId);
            });
            await Promise.all(classicIds.map(id => publishMutation.mutateAsync(id)));
            await portalSyncMutation.mutateAsync({
                title: api?.name ?? 'API',
                folder,
                placement: currentPlacement,
                pages,
                pageIds: idsToPublish,
            });
            setSelectedIds(new Set());
            setPendingPublishIds(null);
            const moved = Boolean(currentPlacement && currentPlacement.folderId !== folder.id);
            notify.success(
                moved
                    ? 'API documentation moved to the selected Navigation folder.'
                    : pendingPublishIds.length === 1
                      ? 'Documentation published to the Next Gen Portal.'
                      : `${pendingPublishIds.length} items published to the Next Gen Portal.`,
            );
            void refetch();
            void portalQuery.refetch();
        } catch (error) {
            notify.error(error, 'Could not publish documentation.');
        } finally {
            setIsPublishing(false);
        }
    }

    async function confirmUnpublish() {
        if (!pendingUnpublishIds?.length) return;
        const ids = [...new Set(pendingUnpublishIds.flatMap(id => collectDescendantIds(pages, id)))];
        setIsPublishing(true);
        try {
            const classicIds = ids.filter(id => {
                const page = pages.find(item => item.id === id);
                return Boolean(page?.id && page.id !== page.portalNavId);
            });
            await Promise.all(classicIds.map(id => unpublishMutation.mutateAsync(id)));
            await portalUnpublishMutation.mutateAsync({ placement, pages, pageIds: ids });
            setSelectedIds(new Set());
            setPendingUnpublishIds(null);
            notify.success(
                pendingUnpublishIds.length === 1
                    ? 'Documentation unpublished from the Next Gen Portal.'
                    : `${pendingUnpublishIds.length} items unpublished from the Next Gen Portal.`,
            );
            void refetch();
            void portalQuery.refetch();
        } catch (error) {
            notify.error(error, 'Could not unpublish documentation.');
        } finally {
            setIsPublishing(false);
        }
    }

    function openPublishApi() {
        setPublishApiOpen(true);
        void portalQuery.refetch();
    }

    async function publishApiToPortal(folderId: string, pageIds: string[]) {
        const availableFolders = portalQuery.data?.folders ?? folders;
        const currentPlacement = portalQuery.data?.placement ?? placement;
        const folder = availableFolders.find(entry => entry.id === folderId);

        if (!folder) {
            notify.error(undefined, 'Select a Navigation folder to publish this API.');
            return;
        }

        if (pages.length > 0 && pageIds.length === 0) {
            notify.error(undefined, 'Select at least one documentation item to publish.');
            return;
        }

        setIsPublishing(true);
        try {
            if (pages.length === 0) {
                // Classic Overview first, then sync publishes the API parent under the selected
                // folder before creating/publishing the Overview child — avoids duplicate apiId.
                await publishEmptyApiMutation.mutateAsync({
                    title: api?.name ?? 'API',
                    folder,
                    placement: currentPlacement,
                });
                setSelectedIds(new Set());
                setPublishApiOpen(false);
                notify.success('Default Overview created and API published to the Next Gen Portal.');
                void refetch();
                void portalQuery.refetch();
                return;
            }

            let idsToPublish = [...new Set(pageIds.flatMap(id => collectDescendantIds(pages, id)))];
            const classicIds = idsToPublish.filter(id => {
                const page = pages.find(item => item.id === id);
                return Boolean(page?.id && page.id !== page.portalNavId);
            });
            await Promise.all(classicIds.map(id => publishMutation.mutateAsync(id)));
            await portalSyncMutation.mutateAsync({
                title: api?.name ?? 'API',
                folder,
                placement: currentPlacement,
                pages,
                pageIds: idsToPublish,
            });
            setSelectedIds(new Set());
            setPublishApiOpen(false);
            notify.success('API published to the Next Gen Portal.');
            void refetch();
            void portalQuery.refetch();
        } catch (error) {
            notify.error(error, 'Could not publish the API.');
        } finally {
            setIsPublishing(false);
        }
    }

    async function confirmUnpublishApi() {
        setIsPublishing(true);
        try {
            await unpublishApiMutation.mutateAsync({ placement, pages });
            setSelectedIds(new Set());
            setUnpublishApiOpen(false);
            notify.success('API unpublished from the Next Gen Portal.');
            void refetch();
            void portalQuery.refetch();
        } catch (error) {
            notify.error(error, 'Could not unpublish the API.');
        } finally {
            setIsPublishing(false);
        }
    }

    const showRootEmpty = !isLoading && !isError && pages.length === 0;
    const isSavingFolder = createMutation.isPending || updateMutation.isPending;
    const apiPublishedOnPortal = Boolean(placement?.published);

    const headerActions = (
        <div className="flex flex-wrap items-center justify-end gap-2">
            {canPublish && !apiPublishedOnPortal ? (
                <Button
                    size="sm"
                    variant={selectedUnpublished.length > 0 || selectedPublished.length > 0 ? 'outline' : 'default'}
                    disabled={isPublishing || portalQuery.isFetching}
                    onClick={openPublishApi}
                >
                    <GlobeIcon className="size-4" />
                    Publish API
                </Button>
            ) : null}
            {canPublish && apiPublishedOnPortal ? (
                <Button
                    size="sm"
                    variant={selectedUnpublished.length > 0 || selectedPublished.length > 0 ? 'outline' : 'default'}
                    disabled={isPublishing}
                    onClick={() => setUnpublishApiOpen(true)}
                >
                    <EyeOffIcon className="size-4" />
                    Unpublish API
                </Button>
            ) : null}
            {canPublish && selectedUnpublished.length > 0 ? (
                <Button
                    size="sm"
                    variant="outline"
                    disabled={isPublishing || portalQuery.isFetching}
                    onClick={() => openPublish(selectedUnpublished.map(item => item.id!).filter(Boolean))}
                >
                    <GlobeIcon className="size-4" />
                    Publish
                </Button>
            ) : null}
            {canPublish && selectedPublished.length > 0 ? (
                <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setPendingUnpublishIds(selectedPublished.map(item => item.id!).filter(Boolean))}
                >
                    <EyeOffIcon className="size-4" />
                    Unpublish
                </Button>
            ) : null}
            {canModify && canCreate ? (
                <Button size="sm" variant="outline" onClick={() => setFolderDialog({ open: true, parentId: null })}>
                    <FolderOpenIcon className="size-4" />
                    Add folder
                </Button>
            ) : null}
            {canModify && canCreate ? (
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="sm" variant="outline">
                            <PlusIcon className="size-4" />
                            Add page
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-max min-w-44">
                        <PageTypeMenuItems onSelect={pageType => openNewPage(pageType)} />
                    </DropdownMenuContent>
                </DropdownMenu>
            ) : null}
        </div>
    );

    if (isLoading) {
        return (
            <div className="space-y-6">
                <div className="space-y-2">
                    <Skeleton className="h-8 w-48 rounded" />
                    <Skeleton className="h-4 w-80 rounded" />
                </div>
                <Skeleton className="h-64 w-full rounded-xl" />
            </div>
        );
    }

    if (isError) {
        return (
            <Alert variant="destructive">
                <AlertDescription>Failed to load documentation. Please try again.</AlertDescription>
            </Alert>
        );
    }

    return (
        <TooltipProvider>
            <div className="space-y-6">
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1">
                        <h1 className="text-2xl font-semibold tracking-tight">Documentation</h1>
                        <p className="text-sm text-muted-foreground">
                            Create folders and pages for this API, then publish them into a Next Gen Portal Navigation folder.
                        </p>
                    </div>
                    {headerActions}
                </div>

                {isKubernetes ? (
                    <Alert>
                        <AlertDescription>This API is managed by the Kubernetes operator. Documentation is read-only.</AlertDescription>
                    </Alert>
                ) : null}

                {showRootEmpty ? <DocumentationEmptyLanding /> : null}

                {pages.length > 0 ? (
                    <Card>
                        <CardContent className="p-0">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        {canPublish ? (
                                            <TableHead className="w-10">
                                                <Checkbox
                                                    checked={allSelected ? true : someSelected ? 'indeterminate' : false}
                                                    aria-label={allSelected ? 'Deselect all documentation' : 'Select all documentation'}
                                                    onCheckedChange={() => toggleSelectAll()}
                                                />
                                            </TableHead>
                                        ) : null}
                                        {canReorder ? (
                                            <TableHead className="w-10">
                                                <span className="sr-only">Reorder</span>
                                            </TableHead>
                                        ) : null}
                                        <TableHead>Name</TableHead>
                                        <TableHead>Type</TableHead>
                                        <TableHead>Status</TableHead>
                                        <TableHead>Last updated</TableHead>
                                        <TableHead className="w-12 text-right">
                                            <span className="sr-only">Actions</span>
                                        </TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {rows.map(({ item, depth, childCount }) => {
                                        const isFolder = item.type === 'FOLDER';
                                        const isExpanded = Boolean(item.id && expanded.has(item.id));
                                        const publishable = isPublishable(item);
                                        const dragParent = dragId
                                            ? normalizeParentId(pages.find(entry => entry.id === dragId)?.parentId)
                                            : undefined;
                                        const dropOk = Boolean(dragId && dragParent === normalizeParentId(item.parentId) && dragId !== item.id);
                                        return (
                                            <TableRow
                                                key={item.id ?? item.name}
                                                className={cn(overId === item.id && dropOk && 'bg-muted/70')}
                                                onDragOver={event => {
                                                    if (!canReorder || !dropOk) return;
                                                    event.preventDefault();
                                                    if (item.id) setOverId(item.id);
                                                }}
                                                onDrop={event => {
                                                    event.preventDefault();
                                                    if (item.id) handleDrop(item.id);
                                                }}
                                            >
                                                {canPublish ? (
                                                    <TableCell className="w-10">
                                                        {publishable && item.id ? (
                                                            <Checkbox
                                                                checked={selectedIds.has(item.id)}
                                                                aria-label={`Select ${item.name}`}
                                                                onCheckedChange={() => item.id && toggleSelected(item.id)}
                                                            />
                                                        ) : (
                                                            <Tooltip>
                                                                <TooltipTrigger asChild>
                                                                    <span className="inline-flex">
                                                                        <Checkbox disabled aria-label={`${item.name} publishes with its folder`} />
                                                                    </span>
                                                                </TooltipTrigger>
                                                                <TooltipContent>Pages inside a folder publish with the folder.</TooltipContent>
                                                            </Tooltip>
                                                        )}
                                                    </TableCell>
                                                ) : null}
                                                {canReorder ? (
                                                    <TableCell className="w-10">
                                                        <button
                                                            type="button"
                                                            draggable
                                                            aria-label={`Drag to reorder ${item.name}`}
                                                            className={cn(
                                                                'inline-flex cursor-grab items-center rounded p-1 text-muted-foreground hover:text-foreground active:cursor-grabbing',
                                                                dragId === item.id && 'opacity-40',
                                                            )}
                                                            onDragStart={event => {
                                                                event.dataTransfer.effectAllowed = 'move';
                                                                event.dataTransfer.setData('text/plain', item.id ?? '');
                                                                if (item.id) setDragId(item.id);
                                                            }}
                                                            onDragEnd={() => {
                                                                setDragId(null);
                                                                setOverId(null);
                                                            }}
                                                        >
                                                            <GripVerticalIcon className="size-4" aria-hidden />
                                                        </button>
                                                    </TableCell>
                                                ) : null}
                                                <TableCell>
                                                    <div className="flex min-w-0 items-center" style={{ paddingLeft: depth * 20 }}>
                                                        {isFolder ? (
                                                            <button
                                                                type="button"
                                                                className="mr-1 inline-flex size-6 shrink-0 items-center justify-center rounded text-muted-foreground hover:text-foreground"
                                                                aria-expanded={isExpanded}
                                                                aria-label={isExpanded ? `Collapse ${item.name}` : `Expand ${item.name}`}
                                                                onClick={() => item.id && toggleFolder(item.id)}
                                                            >
                                                                {isExpanded ? (
                                                                    <ChevronDownIcon className="size-4" aria-hidden />
                                                                ) : (
                                                                    <ChevronRightIcon className="size-4" aria-hidden />
                                                                )}
                                                            </button>
                                                        ) : (
                                                            <span className="mr-1 inline-block size-6 shrink-0" aria-hidden />
                                                        )}
                                                        <button
                                                            type="button"
                                                            className="flex min-w-0 items-center gap-2 text-left font-medium hover:text-primary"
                                                            onClick={() => openPage(item)}
                                                        >
                                                            <TypeIcon type={item.type} className="size-6 shrink-0" />
                                                            <span className="truncate">{item.name}</span>
                                                            {isFolder ? (
                                                                <span className="text-xs font-normal text-muted-foreground" aria-hidden>
                                                                    {childCount === 0 ? 'Empty' : `${childCount}`}
                                                                </span>
                                                            ) : null}
                                                        </button>
                                                    </div>
                                                </TableCell>
                                                <TableCell>
                                                    <Badge variant="secondary">{typeLabel(item.type)}</Badge>
                                                </TableCell>
                                                <TableCell>
                                                    <Badge
                                                        variant={item.published ? 'outline' : 'secondary'}
                                                        className={item.published ? 'border-success/20 text-success' : undefined}
                                                    >
                                                        {item.published ? 'Published' : 'Unpublished'}
                                                    </Badge>
                                                </TableCell>
                                                <TableCell className="text-sm text-muted-foreground">
                                                    {formatUpdatedAt(item.updatedAt)}
                                                </TableCell>
                                                <TableCell className="text-right">
                                                    <DropdownMenu>
                                                        <DropdownMenuTrigger asChild>
                                                            <Button variant="ghost" size="sm" aria-label={`Actions for ${item.name}`}>
                                                                <MoreHorizontalIcon />
                                                            </Button>
                                                        </DropdownMenuTrigger>
                                                        <DropdownMenuContent align="end" className="w-max min-w-52">
                                                            <DropdownMenuItem
                                                                onSelect={() =>
                                                                    isFolder
                                                                        ? setFolderDialog({
                                                                              open: true,
                                                                              folder: item,
                                                                              parentId: normalizeParentId(item.parentId),
                                                                          })
                                                                        : item.id && navigate(item.id)
                                                                }
                                                            >
                                                                <PencilIcon className="size-4" aria-hidden />
                                                                {canUpdate && !isKubernetes
                                                                    ? isFolder
                                                                        ? 'Edit folder'
                                                                        : 'Edit page'
                                                                    : isFolder
                                                                      ? 'View folder'
                                                                      : 'View page'}
                                                            </DropdownMenuItem>
                                                            {canPublish && publishable && item.id ? (
                                                                item.published ? (
                                                                    <DropdownMenuItem onSelect={() => setPendingUnpublishIds([item.id!])}>
                                                                        <EyeOffIcon className="size-4" aria-hidden />
                                                                        Unpublish
                                                                    </DropdownMenuItem>
                                                                ) : (
                                                                    <DropdownMenuItem onSelect={() => openPublish([item.id!])}>
                                                                        <GlobeIcon className="size-4" aria-hidden />
                                                                        Publish
                                                                    </DropdownMenuItem>
                                                                )
                                                            ) : null}
                                                            {canCreate && !isKubernetes && isFolder && item.id ? (
                                                                <>
                                                                    <DropdownMenuSeparator />
                                                                    <DropdownMenuLabel>Add to folder</DropdownMenuLabel>
                                                                    <PageTypeMenuItems onSelect={pageType => openNewPage(pageType, item.id)} />
                                                                    <DropdownMenuItem
                                                                        onSelect={() => setFolderDialog({ open: true, parentId: item.id ?? null })}
                                                                    >
                                                                        <FolderOpenIcon className="size-4" aria-hidden />
                                                                        Folder
                                                                    </DropdownMenuItem>
                                                                </>
                                                            ) : null}
                                                            {canDelete && !isKubernetes ? (
                                                                <>
                                                                    <DropdownMenuSeparator />
                                                                    <DropdownMenuItem
                                                                        className="text-destructive focus:text-destructive"
                                                                        onSelect={() => setPendingDelete(item)}
                                                                    >
                                                                        <Trash2Icon className="size-4" aria-hidden />
                                                                        {isFolder ? 'Delete folder' : 'Delete page'}
                                                                    </DropdownMenuItem>
                                                                </>
                                                            ) : null}
                                                        </DropdownMenuContent>
                                                    </DropdownMenu>
                                                </TableCell>
                                            </TableRow>
                                        );
                                    })}
                                </TableBody>
                            </Table>
                        </CardContent>
                    </Card>
                ) : null}

                <DocumentationFolderDialog
                    open={folderDialog.open}
                    folderName={folderDialog.open ? folderDialog.folder?.name : undefined}
                    existingNames={folderExistingNames}
                    readOnly={isKubernetes || !canModify}
                    isSaving={isSavingFolder}
                    onClose={() => setFolderDialog({ open: false })}
                    onSubmit={saveFolder}
                />

                <DocumentationPublishDialog
                    open={pendingPublishIds !== null}
                    itemCount={pendingPublishIds?.length ?? 0}
                    folders={folders}
                    currentPlacement={placement}
                    isSaving={isPublishing}
                    onClose={() => {
                        if (!isPublishing) setPendingPublishIds(null);
                    }}
                    onSubmit={folderId => void publishToPortal(folderId)}
                />

                <DocumentationPublishApiDialog
                    open={publishApiOpen}
                    folders={folders}
                    currentPlacement={placement}
                    pages={publishableItems}
                    isSaving={isPublishing}
                    onClose={() => {
                        if (!isPublishing) setPublishApiOpen(false);
                    }}
                    onSubmit={(folderId, pageIds) => void publishApiToPortal(folderId, pageIds)}
                />

                <ConfirmDialog
                    open={pendingUnpublishIds !== null}
                    onOpenChange={open => {
                        if (!open) setPendingUnpublishIds(null);
                    }}
                    title={pendingUnpublishIds?.length === 1 ? 'Unpublish documentation' : 'Unpublish selected documentation'}
                    description={
                        pendingUnpublishIds?.length === 1
                            ? 'This removes the page or folder from the Next Gen Developer Portal. If it is the last published documentation under this API, the API is unpublished in Navigation as well.'
                            : 'This removes the selected pages and folders from the Next Gen Developer Portal. If nothing remains published under this API, the API is unpublished in Navigation as well.'
                    }
                    confirmLabel="Unpublish"
                    isPending={isPublishing}
                    onConfirm={() => void confirmUnpublish()}
                />

                <ConfirmDialog
                    open={unpublishApiOpen}
                    onOpenChange={open => {
                        if (!open && !isPublishing) setUnpublishApiOpen(false);
                    }}
                    title="Unpublish API"
                    description="This unpublishes the API and all of its documentation from the Next Gen Developer Portal Navigation. Classic documentation pages are marked unpublished as well."
                    confirmLabel="Unpublish API"
                    isPending={isPublishing}
                    onConfirm={() => void confirmUnpublishApi()}
                />

                <ConfirmDialog
                    open={pendingDelete !== null}
                    onOpenChange={open => {
                        if (!open) setPendingDelete(null);
                    }}
                    title={pendingDelete?.type === 'FOLDER' ? 'Delete folder' : 'Delete page'}
                    description={
                        pendingDelete?.type === 'FOLDER'
                            ? 'Only empty folders can be deleted. The matching Navigation item is removed too. If nothing remains under this API, the API is removed from Navigation. This cannot be undone.'
                            : `Delete “${pendingDelete?.name}”? The matching Navigation item is removed too. If it is the last documentation under this API, the API is removed from Navigation. This cannot be undone.`
                    }
                    confirmLabel="Delete"
                    destructive
                    isPending={deleteMutation.isPending}
                    onConfirm={() => {
                        if (!pendingDelete?.id) return;
                        deleteMutation.mutate(pendingDelete.id, {
                            onSuccess: () => {
                                notify.success(pendingDelete.type === 'FOLDER' ? 'Folder deleted' : 'Page deleted');
                                setSelectedIds(current => {
                                    const next = new Set(current);
                                    next.delete(pendingDelete.id!);
                                    return next;
                                });
                                setPendingDelete(null);
                            },
                            onError: error => notify.error(error, 'Could not delete.'),
                        });
                    }}
                />
            </div>
        </TooltipProvider>
    );
}
