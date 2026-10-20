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
    Button,
    DataTableEmptyState,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    PageHeader,
} from '@gravitee/graphene-core';
import {
    BookOpenIcon,
    ChevronDownIcon,
    EyeOffIcon,
    FileTextIcon,
    FolderOpenIcon,
    GlobeIcon,
    Link2Icon,
    PlusIcon,
    Trash2Icon,
} from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ConfirmDialog } from '../../../../shared/components/ConfirmDialog';
import { notify } from '../../../../shared/notify';
import { CreatePageDialog } from '../../components/detail/documentation/CreatePageDialog';
import { ApiPublicationBadge } from '../../components/detail/documentation/DocumentationItemBadges';
import { DocumentationTree } from '../../components/detail/documentation/DocumentationTree';
import { EditPageDetailsDialog } from '../../components/detail/documentation/EditPageDetailsDialog';
import { FolderDialog } from '../../components/detail/documentation/FolderDialog';
import { LinkDialog } from '../../components/detail/documentation/LinkDialog';
import { PublicationDialog } from '../../components/detail/documentation/PublicationDialog';
import { PublishToPortalDialog } from '../../components/detail/documentation/PublishToPortalDialog';
import { useApiDetailContext } from '../../context/ApiDetailContext';
import { useApiDocumentation, useDeleteApiDocumentationItem, useUnpublishApiFromPortal } from '../../hooks/useApiDocumentation';
import { useExpandedDocumentationFolders } from '../../hooks/useExpandedDocumentationFolders';
import { useFetchNow } from '../../hooks/useFetchNow';
import type { ApiDocumentationItem, ApiPortalPublication, PortalNavigationFolder } from '../../types/apiDocumentation';
import { type DocumentationRow, getAncestors, getPublishedSection, syncStateOf } from '../../utils/documentationTree';

const DESCRIPTION = 'Pages, folders and links that describe this API in the developer portal.';

export function ApiDocumentationPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const { permissionsReady } = useApiDetailContext();
    const canRead = useHasPermission({ anyOf: ['api-documentation-r'] });

    if (!permissionsReady) {
        return null;
    }

    if (!canRead) {
        return (
            <div className="space-y-6">
                <h1 className="text-2xl font-semibold tracking-tight">Documentation</h1>
                <p className="text-sm text-muted-foreground">You don&apos;t have permission to view this API&apos;s documentation.</p>
            </div>
        );
    }

    // Keyed so that moving to another API starts from that API's own open folders.
    return <ApiDocumentationContent key={apiId} apiId={apiId ?? ''} />;
}

// Split from the page so the list is only requested once the user is known to be allowed to read it.
function ApiDocumentationContent({ apiId }: Readonly<{ apiId: string }>) {
    const canDelete = useHasPermission({ anyOf: ['api-documentation-d'] });
    // A page is created, then its content is saved: with create alone it would stay empty.
    const canAdd = useHasPermission({ allOf: ['api-documentation-c', 'api-documentation-u'] });
    const canEdit = useHasPermission({ anyOf: ['api-documentation-u'] });
    // Enough for the Overview page, which the server creates with its content.
    const canCreate = useHasPermission({ anyOf: ['api-documentation-c'] });
    const { api } = useApiDetailContext();
    const { data, isLoading, isError } = useApiDocumentation(apiId);
    const deleteMutation = useDeleteApiDocumentationItem(apiId);
    const unpublishMutation = useUnpublishApiFromPortal(apiId);
    const { fetchNow, isFetching } = useFetchNow(apiId);
    const expandedFolders = useExpandedDocumentationFolders(apiId);
    const navigate = useNavigate();
    const [toDelete, setToDelete] = useState<DocumentationRow | null>(null);
    const [toChangePublication, setToChangePublication] = useState<DocumentationRow | null>(null);
    // `parent` is absent for an item added at the top level.
    const [pageToAdd, setPageToAdd] = useState<{ parent?: PortalNavigationFolder } | null>(null);
    const [folderToAdd, setFolderToAdd] = useState<{ parent?: PortalNavigationFolder } | null>(null);
    const [linkToAdd, setLinkToAdd] = useState<{ parent?: PortalNavigationFolder } | null>(null);
    const [itemToEdit, setItemToEdit] = useState<ApiDocumentationItem | null>(null);
    const [isPublishing, setIsPublishing] = useState(false);
    const [isUnpublishing, setIsUnpublishing] = useState(false);

    const items = data?.items ?? [];
    const isFirstUse = !isLoading && !isError && items.length === 0;
    const openDeleteDialog = useCallback((row: DocumentationRow) => setToDelete(row), []);
    const openPublicationDialog = useCallback((row: DocumentationRow) => setToChangePublication(row), []);
    const openAddPageDialog = useCallback((parent: PortalNavigationFolder) => setPageToAdd({ parent }), []);
    const openAddFolderDialog = useCallback((parent: PortalNavigationFolder) => setFolderToAdd({ parent }), []);
    const openAddLinkDialog = useCallback((parent: PortalNavigationFolder) => setLinkToAdd({ parent }), []);
    const openEditDetailsDialog = useCallback((item: ApiDocumentationItem) => setItemToEdit(item), []);
    const parentOfItemToEdit = items.find(
        (item): item is PortalNavigationFolder => item.type === 'FOLDER' && item.id === itemToEdit?.parentId,
    );

    async function handleDelete() {
        if (!toDelete) return;
        const { title, id } = toDelete.item;
        try {
            await deleteMutation.mutateAsync(id);
            notify.success(`'${title}' deleted`);
            setToDelete(null);
        } catch (error) {
            notify.error(error, `Failed to delete '${title}'`);
        }
    }

    async function handleUnpublish() {
        try {
            await unpublishMutation.mutateAsync();
            notify.success('Unpublished from the developer portal');
            setIsUnpublishing(false);
        } catch (error) {
            notify.error(error, 'Failed to unpublish the API');
        }
    }

    const addMenu = canAdd ? (
        <AddDocumentationMenu
            onAddPage={() => setPageToAdd({})}
            onAddFolder={() => setFolderToAdd({})}
            onAddLink={() => setLinkToAdd({})}
        />
    ) : null;

    return (
        <div className="flex flex-col gap-6">
            <PageHeader
                title="Documentation"
                description={DESCRIPTION}
                actions={
                    <>
                        {data ? <ApiPublicationBadge publications={data.publications} /> : null}
                        {data && canEdit ? (
                            <PublicationAction
                                publications={data.publications}
                                onPublish={() => setIsPublishing(true)}
                                onUnpublish={() => setIsUnpublishing(true)}
                            />
                        ) : null}
                        {addMenu}
                    </>
                }
            />

            {isError ? (
                <Alert variant="destructive">
                    <AlertDescription>Failed to load the documentation. Refresh the page.</AlertDescription>
                </Alert>
            ) : isFirstUse ? (
                <div className="rounded-lg border">
                    <DataTableEmptyState
                        variant="first-use"
                        icon={<BookOpenIcon />}
                        title="No documentation yet"
                        description="Pages, folders and links you add here appear in the developer portal once the API is published."
                        primaryAction={addMenu}
                    />
                </div>
            ) : (
                <DocumentationTree
                    items={items}
                    isLoading={isLoading}
                    expandedIds={expandedFolders.expandedIds}
                    onToggle={expandedFolders.toggle}
                    canDelete={canDelete}
                    onDelete={openDeleteDialog}
                    canAdd={canAdd}
                    onAddPage={openAddPageDialog}
                    onAddFolder={openAddFolderDialog}
                    onAddLink={openAddLinkDialog}
                    canEdit={canEdit}
                    onEditDetails={openEditDetailsDialog}
                    isApiPublished={getPublishedSection(data?.publications ?? []) !== undefined}
                    onChangePublication={openPublicationDialog}
                    isFetching={isFetching}
                    onFetch={fetchNow}
                />
            )}

            <ConfirmDialog
                open={toDelete !== null}
                onOpenChange={open => !open && setToDelete(null)}
                title={`Delete "${toDelete?.item.title}"?`}
                description={toDelete ? <DeleteDescription row={toDelete} /> : null}
                confirmLabel="Delete"
                pendingLabel="Deleting…"
                destructive
                icon={<Trash2Icon className="size-4" aria-hidden />}
                isPending={deleteMutation.isPending}
                onConfirm={handleDelete}
            />

            {toChangePublication ? (
                <PublicationDialog
                    open
                    apiId={apiId}
                    item={toChangePublication.item}
                    itemsInside={toChangePublication.descendantCount}
                    onClose={() => setToChangePublication(null)}
                />
            ) : null}

            <CreatePageDialog
                open={pageToAdd !== null}
                apiId={apiId}
                parent={pageToAdd?.parent}
                onClose={() => setPageToAdd(null)}
                onCreated={pageId => {
                    // So the new page shows when coming back from it.
                    if (pageToAdd?.parent) expandedFolders.expand(pageToAdd.parent.id);
                    navigate(`${pageId}/edit`);
                }}
            />

            <FolderDialog
                open={folderToAdd !== null}
                apiId={apiId}
                parent={folderToAdd?.parent}
                onClose={() => setFolderToAdd(null)}
                onCreated={() => {
                    // So the new folder shows.
                    if (folderToAdd?.parent) expandedFolders.expand(folderToAdd.parent.id);
                }}
            />

            <LinkDialog
                open={linkToAdd !== null}
                apiId={apiId}
                parent={linkToAdd?.parent}
                onClose={() => setLinkToAdd(null)}
                onCreated={() => {
                    // So the new link shows.
                    if (linkToAdd?.parent) expandedFolders.expand(linkToAdd.parent.id);
                }}
            />

            <PublishToPortalDialog
                open={isPublishing}
                apiId={apiId}
                publications={data?.publications ?? []}
                hasPage={items.some(item => item.type === 'PAGE')}
                canCreatePage={canCreate}
                onOverviewPageCreated={pageId => {
                    setIsPublishing(false);
                    navigate(`${pageId}/edit`);
                }}
                onClose={() => setIsPublishing(false)}
            />

            <ConfirmDialog
                open={isUnpublishing}
                onOpenChange={setIsUnpublishing}
                title={`Unpublish "${api?.name ?? 'this API'}"?`}
                description="The API and its documentation will become invisible in the developer portal."
                confirmLabel="Unpublish"
                pendingLabel="Unpublishing…"
                icon={<EyeOffIcon className="size-4" aria-hidden />}
                isPending={unpublishMutation.isPending}
                onConfirm={handleUnpublish}
            />

            {itemToEdit?.type === 'PAGE' ? (
                <EditPageDetailsDialog
                    open
                    apiId={apiId}
                    page={itemToEdit}
                    syncState={syncStateOf(itemToEdit, getAncestors(items, itemToEdit))}
                    parent={parentOfItemToEdit}
                    onClose={() => setItemToEdit(null)}
                />
            ) : null}

            {itemToEdit?.type === 'FOLDER' ? (
                <FolderDialog
                    open
                    apiId={apiId}
                    folder={itemToEdit}
                    syncState={syncStateOf(itemToEdit, getAncestors(items, itemToEdit))}
                    itemsInside={items.filter(item => getAncestors(items, item).some(ancestor => ancestor.id === itemToEdit.id)).length}
                    parent={parentOfItemToEdit}
                    onClose={() => setItemToEdit(null)}
                />
            ) : null}

            {itemToEdit?.type === 'LINK' ? (
                <LinkDialog open apiId={apiId} link={itemToEdit} parent={parentOfItemToEdit} onClose={() => setItemToEdit(null)} />
            ) : null}
        </div>
    );
}

function AddDocumentationMenu({
    onAddPage,
    onAddFolder,
    onAddLink,
}: Readonly<{ onAddPage: () => void; onAddFolder: () => void; onAddLink: () => void }>) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button type="button">
                    <PlusIcon className="size-4" aria-hidden />
                    Add documentation
                    <ChevronDownIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                <DropdownMenuItem onSelect={onAddPage}>
                    <FileTextIcon className="size-4" aria-hidden />
                    Page
                </DropdownMenuItem>
                <DropdownMenuItem onSelect={onAddFolder}>
                    <FolderOpenIcon className="size-4" aria-hidden />
                    Folder
                </DropdownMenuItem>
                <DropdownMenuItem onSelect={onAddLink}>
                    <Link2Icon className="size-4" aria-hidden />
                    Link
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

// A hidden listing does not make the API published, so it gets Publish, which reuses that listing.
function PublicationAction({
    publications,
    onPublish,
    onUnpublish,
}: Readonly<{ publications: ApiPortalPublication[]; onPublish: () => void; onUnpublish: () => void }>) {
    return getPublishedSection(publications) ? (
        <Button type="button" variant="outline" onClick={onUnpublish}>
            <EyeOffIcon className="size-4" aria-hidden />
            Unpublish
        </Button>
    ) : (
        <Button type="button" variant="outline" onClick={onPublish}>
            <GlobeIcon className="size-4" aria-hidden />
            Publish
        </Button>
    );
}

function DeleteDescription({ row }: Readonly<{ row: DocumentationRow }>) {
    const count = row.descendantCount;
    return (
        <>
            {count > 0
                ? count === 1
                    ? 'The 1 item inside this folder is deleted too. '
                    : `The ${count} items inside this folder are deleted too. `
                : null}
            It disappears from every portal listing this API.
        </>
    );
}
