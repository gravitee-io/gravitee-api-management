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
    Alert,
    AlertDescription,
    Badge,
    Button,
    Checkbox,
    Field,
    FieldDescription,
    FieldLabel,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { ChevronDownIcon, ChevronRightIcon, ExternalLinkIcon, FileTextIcon, FolderOpenIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type {
    DeveloperPortalOption,
    DocumentationPage,
    PortalPublishSelection,
} from '../../../types/documentation';
import { typeLabel } from './documentation-shared';

function normalizeParentId(parentId?: string | null): string | null {
    if (!parentId || parentId === 'ROOT') return null;
    return parentId;
}

function siblingsOf(items: DocumentationPage[], parentId: string | null): DocumentationPage[] {
    return items
        .filter(item => normalizeParentId(item.parentId) === parentId)
        .slice()
        .sort((a, b) => (a.order ?? 0) - (b.order ?? 0) || (a.name ?? '').localeCompare(b.name ?? ''));
}

function collectDocumentIds(items: DocumentationPage[], rootId: string | null): string[] {
    const docs: string[] = [];
    for (const item of siblingsOf(items, rootId)) {
        if (!item.id) continue;
        if (item.type === 'FOLDER') {
            docs.push(...collectDocumentIds(items, item.id));
        } else {
            docs.push(item.id);
        }
    }
    return docs;
}

function portalInitial(name: string): string {
    const trimmed = name.trim();
    return trimmed ? trimmed.charAt(0).toUpperCase() : '?';
}

const PORTAL_AVATAR_COLORS = [
    'bg-violet-600 text-white',
    'bg-sky-600 text-white',
    'bg-emerald-600 text-white',
    'bg-amber-600 text-white',
    'bg-rose-600 text-white',
    'bg-indigo-600 text-white',
];

function portalAvatarClass(id: string): string {
    let hash = 0;
    for (let i = 0; i < id.length; i += 1) hash = (hash + id.charCodeAt(i) * (i + 1)) % PORTAL_AVATAR_COLORS.length;
    return PORTAL_AVATAR_COLORS[hash] ?? PORTAL_AVATAR_COLORS[0]!;
}

type PageTreeRow =
    | { kind: 'folder'; page: DocumentationPage; depth: number; documentIds: string[]; selectedCount: number }
    | { kind: 'page'; page: DocumentationPage; depth: number };

function buildPageTreeRows(
    items: DocumentationPage[],
    parentId: string | null,
    depth: number,
    expanded: Set<string>,
    selectedIds: Set<string>,
): PageTreeRow[] {
    const rows: PageTreeRow[] = [];
    for (const item of siblingsOf(items, parentId)) {
        if (!item.id) continue;
        if (item.type === 'FOLDER') {
            const documentIds = collectDocumentIds(items, item.id);
            const selectedCount = documentIds.filter(id => selectedIds.has(id)).length;
            rows.push({ kind: 'folder', page: item, depth, documentIds, selectedCount });
            if (expanded.has(item.id)) {
                rows.push(...buildPageTreeRows(items, item.id, depth + 1, expanded, selectedIds));
            }
            continue;
        }
        rows.push({ kind: 'page', page: item, depth });
    }
    return rows;
}

export function DocumentationPublishApiDialog({
    open,
    portals,
    pages,
    isSaving,
    onClose,
    onSubmit,
}: {
    open: boolean;
    portals: DeveloperPortalOption[];
    /** Documentation items available for selection (tree via parentId). Empty means create Overview. */
    pages: DocumentationPage[];
    isSaving: boolean;
    onClose: () => void;
    onSubmit: (selections: PortalPublishSelection[]) => void;
}) {
    const isEmpty = pages.length === 0;
    const allDocumentIds = useMemo(() => collectDocumentIds(pages, null), [pages]);

    const [portalSearch, setPortalSearch] = useState('');
    const [selectedPortalIds, setSelectedPortalIds] = useState<Set<string>>(new Set());
    const [folderByPortal, setFolderByPortal] = useState<Record<string, string>>({});
    const [pageIdsByPortal, setPageIdsByPortal] = useState<Record<string, Set<string>>>({});
    const [pagesPortalId, setPagesPortalId] = useState('');
    const [expandedFolders, setExpandedFolders] = useState<Set<string>>(new Set());

    const filteredPortals = useMemo(() => {
        const query = portalSearch.trim().toLowerCase();
        if (!query) return portals;
        return portals.filter(portal => {
            const haystack = `${portal.name} ${portal.url ?? ''}`.toLowerCase();
            return haystack.includes(query);
        });
    }, [portalSearch, portals]);

    const selectedPortals = useMemo(
        () => portals.filter(portal => selectedPortalIds.has(portal.id)),
        [portals, selectedPortalIds],
    );

    const activePagesPortalId = useMemo(() => {
        if (pagesPortalId && selectedPortalIds.has(pagesPortalId)) return pagesPortalId;
        return selectedPortals[0]?.id ?? '';
    }, [pagesPortalId, selectedPortalIds, selectedPortals]);

    const activeSelectedIds = pageIdsByPortal[activePagesPortalId] ?? new Set<string>();
    const pageTreeRows = useMemo(
        () => buildPageTreeRows(pages, null, 0, expandedFolders, activeSelectedIds),
        [pages, expandedFolders, activeSelectedIds],
    );

    const allPortalsSelected = portals.length > 0 && portals.every(portal => selectedPortalIds.has(portal.id));
    const somePortalsSelected = selectedPortalIds.size > 0 && !allPortalsSelected;
    const activeDocumentCount = allDocumentIds.length;
    const activeSelectedCount = allDocumentIds.filter(id => activeSelectedIds.has(id)).length;
    const allPagesSelected = activeDocumentCount > 0 && activeSelectedCount === activeDocumentCount;
    const somePagesSelected = activeSelectedCount > 0 && !allPagesSelected;

    useEffect(() => {
        if (!open) return;

        const initialSelected = new Set(
            portals.filter(portal => portal.published).map(portal => portal.id),
        );
        if (initialSelected.size === 0 && portals.length === 1) {
            initialSelected.add(portals[0]!.id);
        }

        const nextFolders: Record<string, string> = {};
        const nextPages: Record<string, Set<string>> = {};
        const defaultPageIds = (() => {
            if (pages.length === 0) return new Set<string>();
            const unpublished = pages
                .filter(page => page.id && page.type !== 'FOLDER' && !page.published)
                .map(page => page.id!);
            if (unpublished.length > 0) return new Set(unpublished);
            return new Set(allDocumentIds);
        })();

        for (const portal of portals) {
            const preferred =
                (portal.defaultFolderId && portal.folders.some(folder => folder.id === portal.defaultFolderId)
                    ? portal.defaultFolderId
                    : undefined) ??
                (portal.folders.length === 1 ? portal.folders[0]!.id : (portal.folders[0]?.id ?? ''));
            nextFolders[portal.id] = preferred;
            nextPages[portal.id] = new Set(defaultPageIds);
        }

        setPortalSearch('');
        setSelectedPortalIds(initialSelected);
        setFolderByPortal(nextFolders);
        setPageIdsByPortal(nextPages);
        setPagesPortalId([...initialSelected][0] ?? portals[0]?.id ?? '');
        setExpandedFolders(
            new Set(pages.filter(page => page.type === 'FOLDER' && page.id).map(page => page.id!)),
        );
    }, [open, portals, pages, allDocumentIds]);

    const noPortals = portals.length === 0;
    const canSubmit =
        !isSaving &&
        !noPortals &&
        selectedPortals.length > 0 &&
        selectedPortals.every(portal => {
            const folderId = folderByPortal[portal.id];
            if (!folderId || portal.folders.length === 0) return false;
            if (isEmpty) return true;
            return (pageIdsByPortal[portal.id]?.size ?? 0) > 0;
        });

    function togglePortal(id: string) {
        setSelectedPortalIds(current => {
            const next = new Set(current);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    }

    function toggleSelectAllPortals() {
        if (allPortalsSelected) {
            setSelectedPortalIds(new Set());
            return;
        }
        setSelectedPortalIds(new Set(portals.map(portal => portal.id)));
    }

    function setPortalFolder(portalId: string, folderId: string) {
        setFolderByPortal(current => ({ ...current, [portalId]: folderId }));
    }

    function updateActivePageIds(updater: (current: Set<string>) => Set<string>) {
        if (!activePagesPortalId) return;
        setPageIdsByPortal(current => ({
            ...current,
            [activePagesPortalId]: updater(new Set(current[activePagesPortalId] ?? [])),
        }));
    }

    function togglePage(id: string) {
        updateActivePageIds(current => {
            if (current.has(id)) current.delete(id);
            else current.add(id);
            return current;
        });
    }

    function toggleFolder(documentIds: string[], currentlyAllSelected: boolean) {
        updateActivePageIds(current => {
            if (currentlyAllSelected) {
                for (const id of documentIds) current.delete(id);
            } else {
                for (const id of documentIds) current.add(id);
            }
            return current;
        });
    }

    function toggleSelectAllPages() {
        updateActivePageIds(current => {
            if (allPagesSelected) return new Set();
            return new Set(allDocumentIds);
        });
    }

    function toggleFolderExpanded(id: string) {
        setExpandedFolders(current => {
            const next = new Set(current);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    }

    function buildSelections(): PortalPublishSelection[] {
        return selectedPortals.map(portal => ({
            portalId: portal.id,
            folderId: folderByPortal[portal.id] ?? '',
            pageIds: isEmpty ? [] : [...(pageIdsByPortal[portal.id] ?? [])],
        }));
    }

    return (
        <Sheet
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen) onClose();
            }}
        >
            <SheetContent side="right" className="flex h-full w-full flex-col overflow-hidden sm:max-w-2xl" showCloseButton>
                <SheetHeader>
                    <SheetTitle>Publish API to Developer Portals</SheetTitle>
                    <SheetDescription>
                        Select portals and documentation pages to publish. Each selected portal needs a Navigation folder
                        and at least one document.
                    </SheetDescription>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-6 overflow-y-auto px-4 py-2">
                    {noPortals ? (
                        <Alert>
                            <AlertDescription>
                                No Developer Portals are available yet. Create a portal and a Navigation folder before
                                publishing this API.
                            </AlertDescription>
                        </Alert>
                    ) : null}

                    {isEmpty ? (
                        <Alert>
                            <AlertDescription>
                                No documentation exists for this API yet. A default Overview page will be created and
                                published under each selected portal’s Navigation folder.
                            </AlertDescription>
                        </Alert>
                    ) : null}

                    {/* Section 1 — Developer Portals */}
                    <section className="space-y-3" aria-labelledby="publish-portals-heading">
                        <div className="flex items-start justify-between gap-3">
                            <div>
                                <h3 id="publish-portals-heading" className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                    Developer Portals
                                </h3>
                                <p className="mt-1 text-sm text-muted-foreground">
                                    Select portals and choose where the API appears in each navigation.
                                </p>
                            </div>
                            <Badge variant="secondary" className="shrink-0 tabular-nums">
                                {selectedPortalIds.size} selected
                            </Badge>
                        </div>

                        <div className="flex flex-wrap items-center gap-2">
                            <div className="relative min-w-[12rem] flex-1">
                                <SearchIcon className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
                                <Input
                                    value={portalSearch}
                                    onChange={event => setPortalSearch(event.target.value)}
                                    placeholder="Search portals..."
                                    className="pl-8"
                                    disabled={isSaving || noPortals}
                                    aria-label="Search portals"
                                />
                            </div>
                            <label className="flex items-center gap-2 cursor-pointer text-sm whitespace-nowrap">
                                <Checkbox
                                    checked={allPortalsSelected ? true : somePortalsSelected ? 'indeterminate' : false}
                                    disabled={isSaving || portals.length === 0}
                                    onCheckedChange={() => toggleSelectAllPortals()}
                                    aria-label={allPortalsSelected ? 'Deselect all portals' : 'Select all portals'}
                                />
                                Select all
                            </label>
                        </div>

                        <div className="overflow-hidden rounded-md border">
                            <div className="grid grid-cols-[auto_minmax(0,1fr)_12rem] gap-3 border-b bg-muted/40 px-3 py-2 text-[11px] font-semibold tracking-wide text-muted-foreground uppercase">
                                <span className="w-8">Select</span>
                                <span>Portal info</span>
                                <span>Navigation folder</span>
                            </div>
                            <ul className="divide-y">
                                {filteredPortals.map(portal => {
                                    const checked = selectedPortalIds.has(portal.id);
                                    const folderId = folderByPortal[portal.id] ?? '';
                                    return (
                                        <li
                                            key={portal.id}
                                            className="grid grid-cols-[auto_minmax(0,1fr)_12rem] items-center gap-3 px-3 py-3"
                                        >
                                            <Checkbox
                                                checked={checked}
                                                disabled={isSaving}
                                                onCheckedChange={() => togglePortal(portal.id)}
                                                aria-label={`Select ${portal.name}`}
                                            />
                                            <div className="flex min-w-0 items-start gap-3">
                                                <span
                                                    className={`flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold ${portalAvatarClass(portal.id)}`}
                                                    aria-hidden
                                                >
                                                    {portalInitial(portal.name)}
                                                </span>
                                                <div className="min-w-0 space-y-1">
                                                    <div className="flex flex-wrap items-center gap-2">
                                                        <span className="truncate font-medium">{portal.name}</span>
                                                        <Badge variant={portal.published ? 'default' : 'secondary'}>
                                                            {portal.published ? 'Published' : 'Not published'}
                                                        </Badge>
                                                    </div>
                                                    {portal.url ? (
                                                        <a
                                                            href={portal.url.startsWith('http') ? portal.url : `https://${portal.url}`}
                                                            target="_blank"
                                                            rel="noopener noreferrer"
                                                            className="inline-flex max-w-full items-center gap-1 truncate text-sm text-muted-foreground hover:underline"
                                                            onClick={event => event.stopPropagation()}
                                                        >
                                                            <span className="truncate">{portal.url.replace(/^https?:\/\//, '')}</span>
                                                            <ExternalLinkIcon className="size-3.5 shrink-0" aria-hidden />
                                                        </a>
                                                    ) : (
                                                        <span className="text-sm text-muted-foreground">Portal URL not configured</span>
                                                    )}
                                                </div>
                                            </div>
                                            <Select
                                                value={folderId || undefined}
                                                disabled={isSaving || portal.folders.length === 0 || !checked}
                                                onValueChange={value => setPortalFolder(portal.id, value)}
                                            >
                                                <SelectTrigger className="w-full" aria-label={`Navigation folder for ${portal.name}`}>
                                                    <SelectValue placeholder="Select folder" />
                                                </SelectTrigger>
                                                <SelectContent
                                                    position="popper"
                                                    align="end"
                                                    className="max-h-60 overflow-y-auto"
                                                    style={{ width: 'var(--radix-select-trigger-width)', minWidth: 'unset' }}
                                                >
                                                    {portal.folders.map(folder => (
                                                        <SelectItem key={folder.id} value={folder.id}>
                                                            {folder.path}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        </li>
                                    );
                                })}
                                {filteredPortals.length === 0 ? (
                                    <li className="px-3 py-6 text-center text-sm text-muted-foreground">
                                        No portals match your search.
                                    </li>
                                ) : null}
                            </ul>
                        </div>
                    </section>

                    {/* Section 2 — Pages */}
                    <section className="space-y-3" aria-labelledby="publish-pages-heading">
                        <div className="flex items-start justify-between gap-3">
                            <div>
                                <h3 id="publish-pages-heading" className="text-sm font-semibold">
                                    Pages
                                </h3>
                                <p className="mt-1 text-sm text-muted-foreground">
                                    Choose pages independently for each selected portal.
                                </p>
                            </div>
                            {!isEmpty && selectedPortals.length > 0 ? (
                                <span className="shrink-0 text-sm tabular-nums text-muted-foreground">
                                    {activeSelectedCount} of {activeDocumentCount} selected
                                </span>
                            ) : null}
                        </div>

                        {selectedPortals.length === 0 ? (
                            <Alert>
                                <AlertDescription>Select at least one Developer Portal to configure pages.</AlertDescription>
                            </Alert>
                        ) : isEmpty ? (
                            <Alert>
                                <AlertDescription>
                                    A default Overview page will be created for each selected portal.
                                </AlertDescription>
                            </Alert>
                        ) : (
                            <>
                                <Field>
                                    <div className="flex flex-wrap items-center justify-between gap-3">
                                        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2">
                                            <FieldLabel className="shrink-0">Configure pages for</FieldLabel>
                                            <Select
                                                value={activePagesPortalId || undefined}
                                                disabled={isSaving || selectedPortals.length === 0}
                                                onValueChange={setPagesPortalId}
                                            >
                                                <SelectTrigger className="max-w-xs min-w-[14rem]" aria-label="Configure pages for portal">
                                                    <SelectValue placeholder="Select a portal" />
                                                </SelectTrigger>
                                                <SelectContent position="popper" align="start" className="max-h-60 overflow-y-auto">
                                                    {selectedPortals.map(portal => {
                                                        const count = pageIdsByPortal[portal.id]?.size ?? 0;
                                                        return (
                                                            <SelectItem key={portal.id} value={portal.id}>
                                                                {portal.name} · {count} page{count === 1 ? '' : 's'}
                                                            </SelectItem>
                                                        );
                                                    })}
                                                </SelectContent>
                                            </Select>
                                        </div>
                                        <label className="flex items-center gap-2 cursor-pointer text-sm text-muted-foreground">
                                            <Checkbox
                                                checked={allPagesSelected ? true : somePagesSelected ? 'indeterminate' : false}
                                                disabled={isSaving || activeDocumentCount === 0}
                                                onCheckedChange={() => toggleSelectAllPages()}
                                                aria-label={allPagesSelected ? 'Deselect all pages' : 'Select all pages'}
                                            />
                                            Select all
                                        </label>
                                    </div>
                                </Field>

                                <div className="max-h-72 min-h-0 space-y-1 overflow-y-auto overscroll-contain rounded-md border p-2">
                                    {pageTreeRows.map(row => {
                                        const page = row.page;
                                        if (!page.id) return null;
                                        const paddingLeft = 8 + row.depth * 16;

                                        if (row.kind === 'folder') {
                                            const folderAllSelected =
                                                row.documentIds.length > 0 && row.selectedCount === row.documentIds.length;
                                            const folderSomeSelected = row.selectedCount > 0 && !folderAllSelected;
                                            const isExpanded = expandedFolders.has(page.id);
                                            return (
                                                <div
                                                    key={page.id}
                                                    className="flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-muted/50"
                                                    style={{ paddingLeft }}
                                                >
                                                    <button
                                                        type="button"
                                                        className="flex size-6 shrink-0 items-center justify-center rounded-sm text-muted-foreground hover:bg-muted"
                                                        onClick={() => toggleFolderExpanded(page.id!)}
                                                        aria-label={isExpanded ? `Collapse ${page.name}` : `Expand ${page.name}`}
                                                    >
                                                        {isExpanded ? (
                                                            <ChevronDownIcon className="size-4" aria-hidden />
                                                        ) : (
                                                            <ChevronRightIcon className="size-4" aria-hidden />
                                                        )}
                                                    </button>
                                                    <Checkbox
                                                        checked={
                                                            folderAllSelected ? true : folderSomeSelected ? 'indeterminate' : false
                                                        }
                                                        disabled={isSaving || row.documentIds.length === 0}
                                                        onCheckedChange={() =>
                                                            toggleFolder(row.documentIds, folderAllSelected)
                                                        }
                                                        aria-label={`Select folder ${page.name ?? 'Untitled'}`}
                                                    />
                                                    <FolderOpenIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                                                    <span className="min-w-0 flex-1 truncate text-sm font-medium">
                                                        {page.name ?? 'Untitled'}
                                                    </span>
                                                    <span className="shrink-0 text-xs tabular-nums text-muted-foreground">
                                                        {row.selectedCount}/{row.documentIds.length}
                                                    </span>
                                                </div>
                                            );
                                        }

                                        const checked = activeSelectedIds.has(page.id);
                                        return (
                                            <label
                                                key={page.id}
                                                className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 hover:bg-muted/50"
                                                style={{ paddingLeft: paddingLeft + 24 }}
                                            >
                                                <Checkbox
                                                    checked={checked}
                                                    disabled={isSaving}
                                                    onCheckedChange={() => togglePage(page.id!)}
                                                    aria-label={`Select ${page.name ?? 'Untitled'}`}
                                                />
                                                <FileTextIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                                                <span className="min-w-0 flex-1 truncate text-sm">
                                                    <span className="font-medium">{page.name ?? 'Untitled'}</span>
                                                    <span className="text-muted-foreground"> · {typeLabel(page.type)}</span>
                                                </span>
                                            </label>
                                        );
                                    })}
                                    {pageTreeRows.length === 0 ? (
                                        <p className="px-2 py-4 text-center text-sm text-muted-foreground">
                                            No documentation pages available.
                                        </p>
                                    ) : null}
                                </div>
                                <FieldDescription>
                                    Select at least one document for each selected portal. Nested pages inside a folder
                                    publish with that folder.
                                </FieldDescription>
                            </>
                        )}
                    </section>
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={!canSubmit} onClick={() => onSubmit(buildSelections())}>
                        {isSaving ? 'Publishing…' : 'Publish'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
