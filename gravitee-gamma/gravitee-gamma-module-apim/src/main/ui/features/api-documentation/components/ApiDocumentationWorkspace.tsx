/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
    Button,
    Skeleton,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';
import { UploadIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import type { ContentAreaHandle } from '@apim/portal-editor/portal-shell/components/ContentArea';
import { ContentArea } from '@apim/portal-editor/portal-shell/components/ContentArea';
import { DeleteNavItemDialog } from '@apim/portal-editor/portal-shell/components/DeleteNavItemDialog';
import { NavigationTree } from '@apim/portal-editor/portal-shell/components/NavigationTree';
import type { EditorMode } from '@apim/portal-editor/editor/stores/editor.store';
import { useNavigation } from '@apim/portal-editor/portal-shell/hooks/useNavigation';
import { getPortalPages } from '@apim/portal-editor/portal-shell/utils/portal-pages';
import type { Api } from '@apim/portal-editor/editor/entities/api';
import type { DeveloperPortal, PortalNavigationItem, PortalNavigationItemType, PortalNavigationPage } from '@apim/portal-editor/portals/types';
import { seedPortalsIfEmpty } from '@apim/portal-editor/portals/storage/portals.storage';
import { notify } from '@apim/portal-editor/shared/notify/notify';
import {
    ensureApiDocumentationDraft,
    getPublishPreferences,
    savePublishPreferences,
} from '../storage/api-documentation.storage';
import { publishApiDocumentationToPortal } from '../services/publish-to-portal';
import { PublishDialog } from './PublishDialog';
import styles from './ApiDocumentationWorkspace.module.scss';

const API_DOC_ALLOWED_TYPES: PortalNavigationItemType[] = ['FOLDER', 'PAGE', 'LINK'];

export interface ApiDocumentationWorkspaceProps {
    readonly apiId: string;
    readonly apiName: string;
    readonly apiContext?: Api | null;
    readonly standaloneEditorBaseUrl?: string;
}

export function ApiDocumentationWorkspace({
    apiId,
    apiName,
    apiContext = null,
    standaloneEditorBaseUrl = '/portal-editor',
}: ApiDocumentationWorkspaceProps) {
    const contentAreaRef = useRef<ContentAreaHandle>(null);
    const [draftPortalId, setDraftPortalId] = useState<string | null>(null);
    const [initializing, setInitializing] = useState(true);
    const [initError, setInitError] = useState<string | null>(null);
    const [mode, setMode] = useState<EditorMode>('edit');
    const [isSaving, setIsSaving] = useState(false);
    const [isPublishing, setIsPublishing] = useState(false);
    const [publishDialogOpen, setPublishDialogOpen] = useState(false);
    const [portals, setPortals] = useState<DeveloperPortal[]>([]);
    const [selectedPortalId, setSelectedPortalId] = useState('');
    const [deleteTarget, setDeleteTarget] = useState<PortalNavigationItem | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    const navigation = useNavigation(draftPortalId ?? undefined, {
        mode,
        apiContextId: apiId,
    });

    const portalPages = useMemo(() => getPortalPages(navigation.navItems), [navigation.navItems]);
    const publishPreferences = useMemo(() => getPublishPreferences(apiId), [apiId]);

    useEffect(() => {
        let cancelled = false;

        void (async () => {
            try {
                setInitializing(true);
                setInitError(null);
                const [portalId, loadedPortals] = await Promise.all([
                    ensureApiDocumentationDraft(apiId, apiName),
                    seedPortalsIfEmpty(),
                ]);
                if (cancelled) {
                    return;
                }
                setDraftPortalId(portalId);
                setPortals(loadedPortals);
                const prefs = getPublishPreferences(apiId);
                const defaultPortalId = prefs?.portalId ?? loadedPortals[0]?.id ?? '';
                setSelectedPortalId(defaultPortalId);
            } catch (error) {
                if (!cancelled) {
                    setInitError(error instanceof Error ? error.message : 'Failed to load documentation draft.');
                }
            } finally {
                if (!cancelled) {
                    setInitializing(false);
                }
            }
        })();

        return () => {
            cancelled = true;
        };
    }, [apiId, apiName]);

    const handleSave = useCallback(async () => {
        setIsSaving(true);
        try {
            await contentAreaRef.current?.save();
            notify.success('Documentation saved');
        } catch (error) {
            notify.error(error, 'Failed to save documentation');
        } finally {
            setIsSaving(false);
        }
    }, []);

    const handleQuickPublish = useCallback(
        async ({ portalId }: { portalId: string }) => {
            if (!draftPortalId) {
                return;
            }

            setSelectedPortalId(portalId);
            setIsPublishing(true);
            try {
                const result = await publishApiDocumentationToPortal({
                    apiId,
                    apiName,
                    draftPortalId,
                    portalId,
                    parentId: null,
                    mode: 'replace',
                });
                savePublishPreferences(apiId, { portalId, parentId: null });
                setPublishDialogOpen(false);
                notify.success(`Published ${result.publishedPageCount} page(s) to the portal`);
            } catch (error) {
                notify.error(error, 'Failed to publish documentation');
            } finally {
                setIsPublishing(false);
            }
        },
        [apiId, apiName, draftPortalId],
    );

    const handlePublishDialogConfirm = useCallback(
        async ({
            portalId,
            parentId,
            mode: publishMode,
        }: {
            portalId: string;
            parentId: string | null;
            mode: 'replace' | 'merge';
        }) => {
            if (!draftPortalId) {
                return;
            }

            setSelectedPortalId(portalId);
            setIsPublishing(true);
            try {
                const result = await publishApiDocumentationToPortal({
                    apiId,
                    apiName,
                    draftPortalId,
                    portalId,
                    parentId,
                    mode: publishMode,
                });
                savePublishPreferences(apiId, { portalId, parentId });
                setPublishDialogOpen(false);
                notify.success(`Published ${result.publishedPageCount} page(s) to the portal`);
            } catch (error) {
                notify.error(error, 'Failed to publish documentation');
            } finally {
                setIsPublishing(false);
            }
        },
        [apiId, apiName, draftPortalId],
    );

    const handleAddLinkFromPage = useCallback(
        (page: PortalNavigationPage, parentId: string | null) => navigation.addLinkFromPage(page, parentId, 'HEADER'),
        [navigation],
    );

    const handleConfirmDelete = useCallback(async () => {
        if (!deleteTarget) {
            return;
        }
        setIsDeleting(true);
        try {
            await navigation.deleteNavItem(deleteTarget.id);
            setDeleteTarget(null);
        } catch (error) {
            notify.error(error, 'Failed to delete navigation item');
        } finally {
            setIsDeleting(false);
        }
    }, [deleteTarget, navigation]);

    const rootItems = useMemo(
        () => navigation.navItems.filter(item => item.parentId === null),
        [navigation.navItems],
    );

    if (initializing || navigation.loading) {
        return (
            <div className={styles.loading}>
                <Skeleton className="mb-4 h-8 w-72" />
                <Skeleton className="h-[480px] w-full" />
            </div>
        );
    }

    if (initError) {
        return (
            <Alert variant="destructive">
                <AlertDescription>{initError}</AlertDescription>
            </Alert>
        );
    }

    return (
        <div className={styles.workspace}>
            <header className={styles.header}>
                <div className={styles.titleGroup}>
                    <div className={styles.headerText}>
                        <h1 className={styles.title}>Documentation</h1>
                        <p className={styles.subtitle}>Build and publish documentation for {apiName}.</p>
                    </div>
                    <div className={styles.titleSeparator} aria-hidden="true" />
                    <ToggleGroup
                        type="single"
                        variant="outline"
                        size="sm"
                        spacing={0}
                        value={mode}
                        onValueChange={value => value && setMode(value as EditorMode)}
                        aria-label="Editor mode"
                        className={styles.modeToggle}
                    >
                        <ToggleGroupItem value="edit" aria-label="Edit mode">
                            Edit
                        </ToggleGroupItem>
                        <ToggleGroupItem value="preview" aria-label="Preview mode">
                            Preview
                        </ToggleGroupItem>
                    </ToggleGroup>
                </div>

                <div className={styles.controls}>
                    <Button type="button" variant="outline" size="sm" disabled={isSaving} onClick={() => void handleSave()}>
                        {isSaving ? 'Saving…' : 'Save'}
                    </Button>

                    <Button
                        type="button"
                        size="sm"
                        disabled={portals.length === 0 || isPublishing}
                        onClick={() => setPublishDialogOpen(true)}
                    >
                        <UploadIcon className="size-4" aria-hidden="true" />
                        {isPublishing ? 'Publishing…' : 'Publish'}
                    </Button>
                </div>
            </header>

            {portals.length === 0 ? (
                <div className={styles.emptyState}>
                    <Alert>
                        <AlertDescription>
                            No developer portals found. Create a portal in Developer Portals before publishing documentation.
                        </AlertDescription>
                    </Alert>
                </div>
            ) : null}

            <div className={styles.body}>
                <aside className={styles.sidebar}>
                    <h2 className={styles.sidebarTitle}>Navigation</h2>
                    <NavigationTree
                        items={rootItems}
                        allItems={navigation.navItems}
                        selectedNavItemId={navigation.selectedNavItemId}
                        mode={mode}
                        portalId={draftPortalId ?? ''}
                        portalPages={portalPages}
                        showRootAddButton={mode === 'edit'}
                        rootAddParentId={null}
                        onSelectNavItem={navigation.selectNavItem}
                        onAddNavItem={(type, parentId, pageOptions) =>
                            void navigation.addNavItem(type, parentId, 'HEADER', pageOptions)
                        }
                        onAddApiNavItem={async () => undefined}
                        onAddLinkFromPage={handleAddLinkFromPage}
                        onUpdateNavItem={navigation.updateNavItem}
                        onRequestDeleteNavItem={setDeleteTarget}
                        onTogglePublished={item => void navigation.toggleNavItemPublished(item.id)}
                        allowedAddTypes={API_DOC_ALLOWED_TYPES}
                        hideStyleActions
                    />
                </aside>

                <section className={styles.content}>
                    {draftPortalId ? (
                        <ContentArea
                            ref={contentAreaRef}
                            portalId={draftPortalId}
                            selectedNavItemId={navigation.selectedNavItemId}
                            navItems={navigation.navItems}
                            mode={mode}
                            pageWidth="wide"
                            apiContextId={apiId}
                            apiContext={apiContext}
                            onUpdateNavItem={navigation.updateNavItem}
                            onSelectNavItem={navigation.selectNavItem}
                        />
                    ) : null}
                </section>
            </div>

            <DeleteNavItemDialog
                item={deleteTarget}
                allItems={navigation.navItems}
                open={deleteTarget !== null}
                isPending={isDeleting}
                onOpenChange={open => {
                    if (!open) {
                        setDeleteTarget(null);
                    }
                }}
                onConfirm={() => void handleConfirmDelete()}
            />

            <PublishDialog
                open={publishDialogOpen}
                onOpenChange={setPublishDialogOpen}
                portals={portals}
                selectedPortalId={selectedPortalId}
                onSelectedPortalIdChange={setSelectedPortalId}
                initialParentId={publishPreferences?.parentId ?? null}
                apiId={apiId}
                isPublishing={isPublishing}
                standaloneEditorBaseUrl={standaloneEditorBaseUrl}
                onQuickPublish={options => void handleQuickPublish(options)}
                onConfirm={options => void handlePublishDialogConfirm(options)}
            />
        </div>
    );
}
