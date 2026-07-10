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
import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { EditorHeader } from '../../editor/components/EditorHeader';
import { PreviewFrame } from '../../editor/components/PreviewFrame';
import { useEditorStore } from '../../editor/stores/editor.store';
import { PortalShell } from '../../portal-shell/components/PortalShell';
import type { PortalShellHandle } from '../../portal-shell/components/PortalShellHandle';
import { usePortalTheme } from '../../theming/hooks/usePortalTheme';
import { resolvePreviewColorMode } from '../../theming/hooks/useDarkMode';
import { ThemeSidebar } from '../../theming/components/ThemeSidebar';
import { CustomizeOverlay } from '../../theming/components/CustomizeOverlay';
import { getPortal, savePortal } from '../storage/portals.storage';
import { seedCatalogDataIfEmpty } from '../storage/seed-catalog-data';
import type { DeveloperPortal } from '../types';
import { notify } from '../../../shared/notify/notify';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { buildStandalonePortalUrl, usePortalApp } from '../../../app/PortalAppContext';
import constants from '../../../constants.json';
import styles from './PortalEditPage.module.scss';

export function PortalEditPage() {
    const { id, slug } = useParams<{ id: string; slug?: string }>();
    const navigate = useNavigate();
    const contentAreaRef = useRef<PortalShellHandle>(null);
    const [portal, setPortal] = useState<DeveloperPortal | undefined>();
    const [loading, setLoading] = useState(true);

    const {
        mode,
        pageWidth,
        previewViewport,
        layout,
        isSaving,
        initialize,
        reset,
        setMode,
        setPageWidth,
        setPreviewViewport,
        setLayout,
        save,
    } = useEditorStore();

    const themeState = usePortalTheme(id ?? '');
    const [previewColorMode, setPreviewColorMode] = useState<'light' | 'dark'>('light');
    const [previewModeInitialized, setPreviewModeInitialized] = useState(false);
    const [themeSidebarOpen, setThemeSidebarOpen] = useState(false);
    const { standaloneEditorBaseUrl } = usePortalApp();

    useEffect(() => {
        setPreviewModeInitialized(false);
    }, [id]);

    useEffect(() => {
        if (!themeState.loading && !previewModeInitialized) {
            setPreviewColorMode(resolvePreviewColorMode(themeState.theme.activeMode));
            setPreviewModeInitialized(true);
        }
    }, [themeState.loading, themeState.theme.activeMode, previewModeInitialized]);

    const isDark = previewColorMode === 'dark';

    useEffect(() => {
        if (!id) {
            setLoading(false);
            return;
        }

        let cancelled = false;

        void (async () => {
            await seedCatalogDataIfEmpty();
            const loadedPortal = await getPortal(id);
            if (cancelled) return;

            if (!loadedPortal) {
                setLoading(false);
                return;
            }

            setPortal(loadedPortal);
            initialize(loadedPortal);

            if (!cancelled) {
                setLoading(false);
            }
        })();

        return () => {
            cancelled = true;
            reset();
        };
    }, [id, initialize, reset]);

    const handleSave = useCallback(async () => {
        if (!portal) return;

        await save(async () => {
            await contentAreaRef.current?.save();
            await themeState.save();

            const screenshotDataUrl =
                (await contentAreaRef.current?.captureScreenshot()) ?? portal.screenshotDataUrl;
            const updatedPortal = {
                ...portal,
                layout,
                pageWidth,
                screenshotDataUrl,
                updatedAt: new Date().toISOString(),
            };

            await savePortal(updatedPortal);
            setPortal(updatedPortal);
        });
    }, [layout, pageWidth, portal, save, themeState]);

    useEffect(() => {
        if (mode !== 'edit' || !portal) {
            return;
        }

        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key !== 's' || !(event.metaKey || event.ctrlKey)) {
                return;
            }

            event.preventDefault();
            if (!isSaving) {
                void handleSave();
            }
        };

        window.addEventListener('keydown', onKeyDown);
        return () => window.removeEventListener('keydown', onKeyDown);
    }, [handleSave, isSaving, mode, portal]);

    const handlePortalChange = useCallback(
        (updated: DeveloperPortal) => {
            const portalToSave = { ...updated, layout, pageWidth, updatedAt: new Date().toISOString() };
            setPortal(portalToSave);
            void savePortal(portalToSave).catch(error => {
                notify.error(error, 'Failed to save portal changes');
            });
        },
        [layout, pageWidth],
    );

    const getPagePath = useCallback(
        (pageSlug: string) => `/portals/${id}/edit/${pageSlug}`,
        [id],
    );

    const handleNavigate = useCallback(
        (path: string, options?: { replace?: boolean }) => {
            navigate(path, options);
        },
        [navigate],
    );

    const handleOpenInNewWindow = useCallback(() => {
        if (!id) return;
        const base = standaloneEditorBaseUrl || constants.appBasePath;
        const viewPath = slug ? `/portals/${id}/${slug}` : `/portals/${id}`;
        window.open(buildStandalonePortalUrl(base, viewPath), '_blank', 'noopener,noreferrer');
    }, [id, slug, standaloneEditorBaseUrl]);

    if (loading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading portal…</p>;
    }

    if (!portal) {
        return (
            <NotFoundPage
                homePath="/"
                homeLabel="Back to dashboards"
                title="Portal not found"
                description="This developer portal does not exist or may have been removed."
                className="min-h-screen"
            />
        );
    }

    const portalShell = (
        <CustomizeOverlay
            themeState={themeState}
            enabled={mode === 'edit'}
            editingMode={previewColorMode}
            getBlockInstanceStyle={blockId => contentAreaRef.current?.getInstanceStyle(blockId) ?? {}}
            onBindBlockInstanceStyle={(blockId, prop, customVarName) => {
                contentAreaRef.current?.bindInstanceStyle(blockId, prop, customVarName);
            }}
            onUnbindBlockInstanceStyle={(blockId, prop) => {
                contentAreaRef.current?.unbindInstanceStyle(blockId, prop);
            }}
        >
            <PortalShell
                ref={contentAreaRef}
                portal={portal}
                layout={layout}
                mode={mode}
                pageWidth={pageWidth}
                onPortalChange={handlePortalChange}
                slug={slug}
                getPagePath={getPagePath}
                onNavigate={handleNavigate}
                theme={themeState.theme}
                themeReady={!themeState.loading}
                isDark={isDark}
            />
        </CustomizeOverlay>
    );

    return (
        <div className="flex h-screen flex-col overflow-hidden">
            <EditorHeader
                portalId={portal.id}
                portalName={portal.name}
                mode={mode}
                pageWidth={pageWidth}
                previewViewport={previewViewport}
                layout={layout}
                isSaving={isSaving}
                onModeChange={setMode}
                onPageWidthChange={setPageWidth}
                onPreviewViewportChange={setPreviewViewport}
                onLayoutChange={setLayout}
                onPortalNameChange={name => handlePortalChange({ ...portal, name })}
                onSave={() => void handleSave()}
                onOpenInNewWindow={handleOpenInNewWindow}
                themeState={themeState}
                themeSidebarOpen={themeSidebarOpen}
                onThemeSidebarToggle={() => setThemeSidebarOpen(open => !open)}
            />

            <div className={styles.editorBody}>
                <div className={styles.portalArea}>
                    <PreviewFrame viewport={previewViewport}>{portalShell}</PreviewFrame>
                </div>
                {themeSidebarOpen && (
                    <ThemeSidebar
                        themeState={themeState}
                        portalName={portal.name}
                        previewColorMode={previewColorMode}
                        onPreviewColorModeChange={setPreviewColorMode}
                        className={styles.themeSidebar}
                    />
                )}
            </div>
        </div>
    );
}
