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
    ResizableHandle,
    ResizablePanel,
    ResizablePanelGroup,
    Skeleton,
    useLayoutConfig,
} from '@gravitee/graphene-core';
import { CodeEditor } from '@gravitee/graphene-core/code-editor';
import { FolderOpenIcon, PencilIcon, SettingsIcon } from '@gravitee/graphene-core/icons';
import { useDeferredValue, useEffect, useState, type ReactNode } from 'react';
import { useParams } from 'react-router-dom';

import { notify } from '../../../../shared/notify';
import { AsyncApiPreview } from '../../components/detail/documentation/AsyncApiPreview';
import { ItemAccessBadge, ItemPublishedBadge } from '../../components/detail/documentation/DocumentationItemBadges';
import { EditPageDetailsDialog } from '../../components/detail/documentation/EditPageDetailsDialog';
import { GraviteeMarkdownPreview } from '../../components/detail/documentation/GraviteeMarkdownPreview';
import { OpenApiPreview } from '../../components/detail/documentation/OpenApiPreview';
import { OpenApiViewerSettingsDialog } from '../../components/detail/documentation/OpenApiViewerSettingsDialog';
import { SourceFetchStatus } from '../../components/detail/documentation/SourceFetchStatus';
import { useApiDetailContext } from '../../context/ApiDetailContext';
import { useApiDocumentation, useApiDocumentationPageContent, useSaveApiDocumentationPageContent } from '../../hooks/useApiDocumentation';
import { useFetchNow } from '../../hooks/useFetchNow';
import type { ApiDocumentationItem, PortalNavigationFolder, PortalNavigationPage, PortalPageContent } from '../../types/apiDocumentation';
import { allowedChanges, fetchableItemIds, getAncestors, hasSource, type SyncState, syncStateOf } from '../../utils/documentationTree';
import { editorLanguageFor, PAGE_CONTENT_TYPE_LABELS } from '../../utils/pageContentType';

export function ApiDocumentationEditPage() {
    const { apiId = '', pageId = '' } = useParams<{ apiId: string; pageId: string }>();
    const { permissionsReady } = useApiDetailContext();
    const canRead = useHasPermission({ anyOf: ['api-documentation-r'] });
    // The default content area is only as tall as the page; full-bleed makes it as tall as the window, for the editor to fill.
    useLayoutConfig({ contentVariant: 'full-bleed' }, []);

    if (!permissionsReady) {
        return null;
    }

    if (!canRead) {
        return (
            <EditPageContainer>
                <div className="space-y-6">
                    <h1 className="text-2xl font-semibold tracking-tight">Documentation</h1>
                    <p className="text-sm text-muted-foreground">You don&apos;t have permission to view this API&apos;s documentation.</p>
                </div>
            </EditPageContainer>
        );
    }

    return (
        <EditPageContainer>
            <ApiDocumentationEditContent apiId={apiId} pageId={pageId} />
        </EditPageContainer>
    );
}

/**
 * Puts back the width and padding that full-bleed removes. Its height is the window's, not its content's: sized by its
 * content, it would include the editor's last size, which could then never shrink.
 */
function EditPageContainer({ children }: Readonly<{ children: ReactNode }>) {
    return <div className="mx-auto flex h-full w-full max-w-content flex-col px-content pt-4 pb-content">{children}</div>;
}

// Split from the page so nothing is requested once the user is known not to be allowed to read it.
function ApiDocumentationEditContent({ apiId, pageId }: Readonly<{ apiId: string; pageId: string }>) {
    const canEdit = useHasPermission({ anyOf: ['api-documentation-u'] });
    const documentation = useApiDocumentation(apiId);
    const items = documentation.data?.items ?? [];
    const page = items.find((item): item is PortalNavigationPage => item.id === pageId && item.type === 'PAGE');
    const content = useApiDocumentationPageContent(apiId, pageId, page !== undefined);

    if (documentation.isError && !documentation.data) {
        return <PageAlert message="Failed to load the documentation. Refresh the page." />;
    }
    if (documentation.isLoading || content.isLoading) {
        return (
            <div role="status" aria-label="Loading the page" className="flex flex-1 flex-col gap-6">
                <Skeleton className="h-16 w-full" />
                <Skeleton className="w-full flex-1" />
            </div>
        );
    }
    if (!page) {
        return <PageAlert message="This page does not exist in the documentation of this API." />;
    }
    if (!content.data) {
        return <PageAlert message="Failed to load the content of this page. Refresh the page." />;
    }

    const ancestors = getAncestors(items, page);
    const syncState = syncStateOf(page, ancestors);
    const allowed = allowedChanges(syncState);
    const parent = ancestors.at(-1);
    const readOnly = !canEdit || !allowed.content;
    const syncedFolder = syncState === 'BELOW_SYNCED_FOLDER' ? ancestors.find(hasSource) : undefined;

    return (
        <PageEditor
            // A page turning editable starts its draft over from its content, which fetches may have replaced since the
            // draft was copied.
            key={`${page.id}:${readOnly}`}
            apiId={apiId}
            page={page}
            parent={parent?.type === 'FOLDER' ? parent : undefined}
            ancestors={ancestors}
            saved={content.data}
            syncState={syncState}
            readOnly={readOnly}
            canEditDetails={canEdit && allowed.access}
            canFetch={canEdit && syncState === 'OWN_SOURCE'}
            syncedFolder={syncedFolder}
            canFetchSyncedFolder={canEdit && syncedFolder !== undefined && fetchableItemIds(items).has(syncedFolder.id)}
            // As in the portal editor: a synced page can be configured, a page inside a synced folder cannot.
            canConfigureViewer={canEdit && syncState !== 'BELOW_SYNCED_FOLDER' && content.data.type === 'OPENAPI'}
        />
    );
}

function PageAlert({ message }: Readonly<{ message: string }>) {
    return (
        <Alert variant="destructive">
            <AlertDescription>{message}</AlertDescription>
        </Alert>
    );
}

function PageEditor({
    apiId,
    page,
    parent,
    ancestors,
    saved,
    syncState,
    readOnly,
    canEditDetails,
    canFetch,
    syncedFolder,
    canFetchSyncedFolder,
    canConfigureViewer,
}: Readonly<{
    apiId: string;
    page: PortalNavigationPage;
    parent?: PortalNavigationFolder;
    ancestors: ApiDocumentationItem[];
    saved: PortalPageContent;
    syncState: SyncState;
    readOnly: boolean;
    canEditDetails: boolean;
    /** Whether the page has a source of its own and the user may fetch it. */
    canFetch: boolean;
    syncedFolder?: ApiDocumentationItem;
    canFetchSyncedFolder: boolean;
    canConfigureViewer: boolean;
}>) {
    const saveContent = useSaveApiDocumentationPageContent(apiId);
    const [draft, setDraft] = useState(saved.content);
    const [isSaving, setIsSaving] = useState(false);
    const [isEditingDetails, setIsEditingDetails] = useState(false);
    const [isConfiguringViewer, setIsConfiguringViewer] = useState(false);
    // The draft is copied from the content once. A read-only page has no draft of its own: it shows the content as
    // loaded, which a fetch replaces. Until its first successful fetch, a sourced page only holds the placeholder it was
    // created with, which would read as fetched content.
    const neverFetched = syncState === 'OWN_SOURCE' && !page.source?.lastFetchedAt;
    const loadedContent = neverFetched ? '' : saved.content;
    const shownContent = readOnly ? loadedContent : draft;
    const previewContent = useDeferredValue(shownContent);
    const { fetchNow, isFetching } = useFetchNow(apiId);

    const isDirty = !readOnly && draft !== saved.content;
    useWarnBeforeUnload(isDirty);

    async function handleSave() {
        if (isSaving) return;
        setIsSaving(true);
        try {
            await saveContent.mutateAsync({ navId: page.id, content: draft });
            notify.success(`Page '${page.title}' saved`);
        } catch (error) {
            notify.error(error, `Failed to save '${page.title}'`);
        } finally {
            setIsSaving(false);
        }
    }

    const actions =
        readOnly && !canEditDetails ? null : (
            <div className="flex shrink-0 gap-2">
                {canConfigureViewer ? (
                    <Button variant="outline" onClick={() => setIsConfiguringViewer(true)}>
                        <SettingsIcon className="size-4" aria-hidden />
                        Configure
                    </Button>
                ) : null}
                {canEditDetails ? (
                    <Button variant="outline" onClick={() => setIsEditingDetails(true)}>
                        <PencilIcon className="size-4" aria-hidden />
                        Edit details
                    </Button>
                ) : null}
                {/* Shown even with nothing to save, as in the policy studios, so the page reads as editable. */}
                {readOnly ? null : (
                    <Button onClick={() => void handleSave()} disabled={!isDirty || isSaving}>
                        {isSaving ? 'Saving…' : 'Save changes'}
                    </Button>
                )}
            </div>
        );

    return (
        <div className="flex min-h-0 flex-1 flex-col gap-6">
            {/* Graphene's PageHeader layout, as that header takes text only: the badges follow the title on its line, and
                the folders holding the page take the description's place. */}
            <header className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0 space-y-1 wrap-anywhere">
                    <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
                        <h1 className="min-w-0 text-balance">{page.title}</h1>
                        <div className="flex flex-wrap gap-2">
                            <Badge variant="outline">{PAGE_CONTENT_TYPE_LABELS[saved.type]}</Badge>
                            <ItemPublishedBadge published={page.published} itemType="PAGE" />
                            <ItemAccessBadge visibility={page.visibility} itemType="PAGE" />
                        </div>
                    </div>
                    {ancestors.length > 0 ? (
                        <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
                            <FolderOpenIcon className="size-4 shrink-0" aria-hidden />
                            <span className="truncate">{ancestors.map(folder => folder.title).join(' / ')}</span>
                        </p>
                    ) : null}
                </div>
                {actions}
            </header>

            {syncState === 'NOT_SYNCED' ? null : (
                <SourceFetchStatus
                    source={page.source}
                    syncedFolder={syncedFolder}
                    canFetchSyncedFolder={canFetchSyncedFolder}
                    onFetch={canFetch ? () => void fetchNow(page) : undefined}
                    isFetching={isFetching}
                />
            )}

            {/* A row sized by its content could not shrink below the editor's last size, which the editor then keeps
                chasing as the window shrinks: the row takes the space left, whatever the editor's size. */}
            <ResizablePanelGroup className="min-h-0 flex-1">
                <ResizablePanel defaultSize="50%" minSize="25%" className="h-full">
                    <CodeEditor
                        className="h-full"
                        value={shownContent}
                        onChange={value => setDraft(value ?? '')}
                        language={editorLanguageFor(saved.type, shownContent)}
                        readOnly={readOnly}
                        height="100%"
                    />
                </ResizablePanel>
                <ResizableHandle withHandle aria-label="Resize the editor and the preview" className="mx-2 bg-transparent" />
                <ResizablePanel defaultSize="50%" minSize="25%" className="h-full">
                    <div className="h-full overflow-auto rounded-lg border p-4">
                        {saved.type === 'GRAVITEE_MARKDOWN' ? <GraviteeMarkdownPreview content={previewContent} /> : null}
                        {saved.type === 'OPENAPI' ? <OpenApiPreview content={previewContent} configuration={saved.configuration} /> : null}
                        {saved.type === 'ASYNCAPI' ? <AsyncApiPreview content={previewContent} /> : null}
                    </div>
                </ResizablePanel>
            </ResizablePanelGroup>

            {canEditDetails ? (
                <EditPageDetailsDialog
                    open={isEditingDetails}
                    apiId={apiId}
                    page={page}
                    parent={parent}
                    syncState={syncState}
                    hasUnsavedContent={isDirty}
                    onClose={() => setIsEditingDetails(false)}
                />
            ) : null}

            {canConfigureViewer ? (
                <OpenApiViewerSettingsDialog
                    open={isConfiguringViewer}
                    apiId={apiId}
                    pageId={page.id}
                    configuration={saved.configuration}
                    onClose={() => setIsConfiguringViewer(false)}
                />
            ) : null}
        </div>
    );
}

// Covers closing or reloading the tab only: Gamma has no guard on in-app navigation.
function useWarnBeforeUnload(enabled: boolean) {
    useEffect(() => {
        if (!enabled) return;
        function onBeforeUnload(event: BeforeUnloadEvent) {
            event.preventDefault();
            event.returnValue = '';
        }
        window.addEventListener('beforeunload', onBeforeUnload);
        return () => window.removeEventListener('beforeunload', onBeforeUnload);
    }, [enabled]);
}
