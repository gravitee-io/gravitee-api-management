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
import { Alert, AlertDescription, Badge, Button, Separator, Skeleton, useLayoutConfig } from '@gravitee/graphene-core';
import { CodeEditor } from '@gravitee/graphene-core/code-editor';
import { ArrowLeftIcon, FolderOpenIcon } from '@gravitee/graphene-core/icons';
import { useDeferredValue, useEffect, useState, type ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';

import { notify } from '../../../../shared/notify';
import { GraviteeMarkdownPreview } from '../../components/detail/documentation/GraviteeMarkdownPreview';
import { useApiDetailContext } from '../../context/ApiDetailContext';
import { useApiDocumentation, useApiDocumentationPageContent, useSaveApiDocumentationPageContent } from '../../hooks/useApiDocumentation';
import type { ApiDocumentationItem, PortalNavigationPage, PortalPageContent } from '../../types/apiDocumentation';
import { getAncestors, hasSource } from '../../utils/documentationTree';
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
    // The server refuses to change the content of a page with a source, or below a folder with one.
    const synced = [page, ...ancestors].some(hasSource);

    return (
        <PageEditor
            key={page.id}
            apiId={apiId}
            page={page}
            ancestors={ancestors}
            saved={content.data}
            synced={synced}
            readOnly={!canEdit || synced}
        />
    );
}

function PageAlert({ message }: Readonly<{ message: string }>) {
    return (
        <div className="flex flex-col gap-4">
            <div>
                <BackToDocumentation />
            </div>
            <Alert variant="destructive">
                <AlertDescription>{message}</AlertDescription>
            </Alert>
        </div>
    );
}

function BackToDocumentation() {
    return (
        <Button asChild variant="outline">
            <Link to="..">
                <ArrowLeftIcon className="size-4" aria-hidden />
                Documentation
            </Link>
        </Button>
    );
}

function PageEditor({
    apiId,
    page,
    ancestors,
    saved,
    synced,
    readOnly,
}: Readonly<{
    apiId: string;
    page: PortalNavigationPage;
    ancestors: ApiDocumentationItem[];
    saved: PortalPageContent;
    synced: boolean;
    readOnly: boolean;
}>) {
    const saveContent = useSaveApiDocumentationPageContent(apiId);
    const [draft, setDraft] = useState(saved.content);
    const [isSaving, setIsSaving] = useState(false);
    const previewContent = useDeferredValue(draft);

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

    // Shown even with nothing to save, as in the policy studios, so the page reads as editable.
    const saveActions = readOnly ? null : (
        <div className="flex shrink-0 gap-2">
            <Button variant="outline" onClick={() => setDraft(saved.content)} disabled={!isDirty || isSaving}>
                Discard
            </Button>
            <Button onClick={() => void handleSave()} disabled={!isDirty || isSaving}>
                {isSaving ? 'Saving…' : 'Save changes'}
            </Button>
        </div>
    );

    return (
        <div className="flex min-h-0 flex-1 flex-col gap-6">
            <div className="flex flex-col gap-3">
                {/* The buttons share the back button's row, to leave the editor more height. */}
                <div className="flex items-center justify-between gap-4">
                    <div className="flex min-w-0 items-center gap-3">
                        <BackToDocumentation />
                        {ancestors.length > 0 ? (
                            <>
                                <Separator orientation="vertical" className="h-5" style={{ alignSelf: 'center' }} />
                                <span className="flex min-w-0 items-center gap-1.5 text-sm text-muted-foreground">
                                    <FolderOpenIcon className="size-4 shrink-0" aria-hidden />
                                    <span className="truncate">{ancestors.map(folder => folder.title).join(' / ')}</span>
                                </span>
                            </>
                        ) : null}
                    </div>
                    {saveActions}
                </div>
                {/* Not Graphene's PageHeader, whose title is text only: the badges follow the title on its line. */}
                <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
                    <h1 className="min-w-0 text-balance wrap-anywhere">{page.title}</h1>
                    <div className="flex flex-wrap gap-2">
                        <Badge variant="outline">{PAGE_CONTENT_TYPE_LABELS[saved.type]}</Badge>
                        {page.published ? <Badge variant="success">Published</Badge> : <Badge variant="outline">Unpublished</Badge>}
                        {page.visibility === 'PRIVATE' ? (
                            <Badge variant="secondary">Private</Badge>
                        ) : (
                            <Badge variant="outline">Public</Badge>
                        )}
                    </div>
                </div>
            </div>

            {synced ? (
                <Alert>
                    <AlertDescription>This page is synced from an external source, so it can&apos;t be edited here.</AlertDescription>
                </Alert>
            ) : null}

            {/* A row sized by its content could not shrink below the editor's last size, which the editor then keeps
                chasing as the window shrinks: the row takes the space left, whatever the editor's size. */}
            <div className="grid min-h-0 flex-1 grid-cols-2 gap-4" style={{ gridTemplateRows: 'minmax(0, 1fr)' }}>
                <CodeEditor
                    className="min-h-0"
                    value={draft}
                    onChange={value => setDraft(value ?? '')}
                    language={editorLanguageFor(saved.type, draft)}
                    readOnly={readOnly}
                    height="100%"
                />
                {saved.type === 'GRAVITEE_MARKDOWN' ? (
                    <div className="min-h-0 overflow-auto rounded-lg border p-4">
                        <GraviteeMarkdownPreview content={previewContent} />
                    </div>
                ) : (
                    <div className="flex items-center justify-center rounded-lg border p-4">
                        <p className="max-w-sm text-center text-sm text-muted-foreground">
                            {PAGE_CONTENT_TYPE_LABELS[saved.type]} preview isn&apos;t available here yet. The developer portal renders this
                            page once it&apos;s published.
                        </p>
                    </div>
                )}
            </div>
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
