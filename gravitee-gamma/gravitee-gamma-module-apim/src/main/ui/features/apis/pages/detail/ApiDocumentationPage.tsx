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
    DataTableEmptyState,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    PageHeader,
} from '@gravitee/graphene-core';
import { BookOpenIcon, ChevronDownIcon, FileTextIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ConfirmDialog } from '../../../../shared/components/ConfirmDialog';
import { notify } from '../../../../shared/notify';
import { CreatePageDialog } from '../../components/detail/documentation/CreatePageDialog';
import { DocumentationTree } from '../../components/detail/documentation/DocumentationTree';
import { useApiDetailContext } from '../../context/ApiDetailContext';
import { useApiDocumentation, useDeleteApiDocumentationItem } from '../../hooks/useApiDocumentation';
import { useExpandedDocumentationFolders } from '../../hooks/useExpandedDocumentationFolders';
import type { ApiPortalPublication, PortalNavigationFolder } from '../../types/apiDocumentation';
import { type DocumentationRow, getPublishedSection } from '../../utils/documentationTree';

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
    const { data, isLoading, isError } = useApiDocumentation(apiId);
    const deleteMutation = useDeleteApiDocumentationItem(apiId);
    const expandedFolders = useExpandedDocumentationFolders(apiId);
    const navigate = useNavigate();
    const [toDelete, setToDelete] = useState<DocumentationRow | null>(null);
    // `parent` is absent for a page added at the top level.
    const [pageToAdd, setPageToAdd] = useState<{ parent?: PortalNavigationFolder } | null>(null);

    const items = data?.items ?? [];
    const isFirstUse = !isLoading && !isError && items.length === 0;
    const openDeleteDialog = useCallback((row: DocumentationRow) => setToDelete(row), []);
    const openAddPageDialog = useCallback((parent: PortalNavigationFolder) => setPageToAdd({ parent }), []);

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

    const addMenu = canAdd ? <AddDocumentationMenu onAddPage={() => setPageToAdd({})} /> : null;

    return (
        <div className="flex flex-col gap-6">
            <PageHeader
                title="Documentation"
                description={DESCRIPTION}
                actions={
                    <>
                        {data ? <PublicationBadge publications={data.publications} /> : null}
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
        </div>
    );
}

function AddDocumentationMenu({ onAddPage }: Readonly<{ onAddPage: () => void }>) {
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
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

function PublicationBadge({ publications }: Readonly<{ publications: ApiPortalPublication[] }>) {
    const publishedSection = getPublishedSection(publications);
    return publishedSection ? (
        <Badge variant="success">Published in {publishedSection}</Badge>
    ) : (
        <Badge variant="outline">Not published</Badge>
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
