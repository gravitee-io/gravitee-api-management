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
import { Button, Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';
import { forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useState } from 'react';

import { OpenApiRendererView } from '../../../blocks/OpenApiBlock/OpenApiRendererView';
import type {
    OpenApiPageContent,
    OpenApiRenderer,
    OpenApiSpecSource,
    PortalNavigationItem,
    PortalNavigationOpenApiPage,
} from '../../portals/types';
import { findApiAncestor } from '../../portal-shell/utils/find-api-ancestor';
import { OPENAPI_RENDERER_LABELS, normalizeOpenApiRenderer } from '../../portal-shell/utils/page-type-options';
import { fetchOpenApiSpecFromUrl } from '../services/openapi.service';
import { getOpenApiEditorValidationState } from '../utils/openapi-editor-validation';
import { resolveOpenApiSpecContent } from '../utils/resolve-openapi-spec';
import styles from './OpenApiPageEditor.module.scss';

export interface OpenApiPageEditorHandle {
    save: () => Promise<void>;
}

interface OpenApiPageEditorProps {
    readonly page: PortalNavigationOpenApiPage;
    readonly content: OpenApiPageContent;
    readonly navItems: readonly PortalNavigationItem[];
    readonly onSave: (content: OpenApiPageContent, pagePatch?: Partial<PortalNavigationOpenApiPage>) => Promise<void>;
}

type SourceType = OpenApiSpecSource['type'];

const TOOLBAR_SELECT_CONTENT_PROPS = {
    position: 'popper' as const,
    side: 'bottom' as const,
    sideOffset: 4,
    align: 'start' as const,
    avoidCollisions: false,
};

export const OpenApiPageEditor = forwardRef<OpenApiPageEditorHandle, OpenApiPageEditorProps>(function OpenApiPageEditor(
    { page, content, navItems, onSave },
    ref,
) {
    const [renderer, setRenderer] = useState<OpenApiRenderer>(normalizeOpenApiRenderer(content.renderer));
    const [sourceType, setSourceType] = useState<SourceType>(page.specSource.type);
    const [url, setUrl] = useState(page.specSource.type === 'URL' ? page.specSource.url : '');
    const [inlineContent, setInlineContent] = useState(
        page.specSource.type === 'INLINE' ? page.specSource.content : content.specContent,
    );
    const [specContent, setSpecContent] = useState(content.specContent);
    const [syncStatus, setSyncStatus] = useState<string | null>(null);
    const [loadingSpec, setLoadingSpec] = useState(false);
    const [apiSpecResolved, setApiSpecResolved] = useState(page.specSource.type === 'API');
    const [urlSpecSynced, setUrlSpecSynced] = useState(
        page.specSource.type === 'URL' && Boolean(page.specSource.lastSyncedAt),
    );

    const apiAncestor = useMemo(() => findApiAncestor(navItems, page.id), [navItems, page.id]);

    const { showValidationStatus, validationLoading, validation } = useMemo(
        () =>
            getOpenApiEditorValidationState({
                sourceType,
                inlineContent,
                specContent,
                loadingSpec,
                hasApiAncestor: Boolean(apiAncestor),
                apiSpecResolved,
                urlSpecSynced,
            }),
        [apiAncestor, apiSpecResolved, inlineContent, loadingSpec, sourceType, specContent, urlSpecSynced],
    );

    const buildSpecSource = useCallback((): OpenApiSpecSource => {
        switch (sourceType) {
            case 'URL':
                return { type: 'URL', url };
            case 'API':
                return { type: 'API', apiId: apiAncestor?.apiId ?? '' };
            case 'INLINE':
            default:
                return { type: 'INLINE', content: inlineContent };
        }
    }, [apiAncestor?.apiId, inlineContent, sourceType, url]);

    const persist = useCallback(async () => {
        const nextSpecSource = buildSpecSource();
        const nextContent: OpenApiPageContent = {
            ...content,
            renderer,
            specContent: sourceType === 'INLINE' ? inlineContent : specContent,
        };
        const pagePatch: Partial<PortalNavigationOpenApiPage> = {
            renderer,
            specSource: nextSpecSource,
        };
        await onSave(nextContent, pagePatch);
    }, [buildSpecSource, content, inlineContent, onSave, renderer, sourceType, specContent]);

    useImperativeHandle(ref, () => ({
        save: persist,
    }));

    useEffect(() => {
        if (sourceType === 'INLINE') {
            setSpecContent(inlineContent);
            setLoadingSpec(false);
            return;
        }

        if (sourceType !== 'API') {
            return;
        }

        let cancelled = false;
        setLoadingSpec(true);
        setApiSpecResolved(false);

        void (async () => {
            const resolved = await resolveOpenApiSpecContent(
                buildSpecSource(),
                navItems,
                page.id,
                content.specContent,
            );
            if (!cancelled) {
                setSpecContent(resolved);
                setLoadingSpec(false);
                setApiSpecResolved(true);
            }
        })();

        return () => {
            cancelled = true;
        };
    }, [buildSpecSource, content.specContent, inlineContent, navItems, page.id, sourceType]);

    const handleFetchUrl = async () => {
        setLoadingSpec(true);
        setSyncStatus(null);
        try {
            const spec = await fetchOpenApiSpecFromUrl(url);
            setSpecContent(spec.content);
            setUrlSpecSynced(true);
            setSyncStatus(`Synced ${new Date().toLocaleTimeString()}`);
            await onSave(
                { ...content, renderer, specContent: spec.content },
                { renderer, specSource: { type: 'URL', url, lastSyncedAt: Date.now() } },
            );
        } catch {
            setSyncStatus('Failed to fetch spec');
        } finally {
            setLoadingSpec(false);
        }
    };

    const handleInlineChange = (value: string) => {
        setInlineContent(value);
        setSpecContent(value);
    };

    const handleRendererChange = (nextRenderer: OpenApiRenderer) => {
        setRenderer(nextRenderer);
    };

    const handleSourceTypeChange = (value: SourceType) => {
        setSourceType(value);
        setSyncStatus(null);

        if (value === 'API') {
            setApiSpecResolved(false);
            setUrlSpecSynced(false);
            setLoadingSpec(true);
            return;
        }

        setApiSpecResolved(false);
        setLoadingSpec(false);

        if (value === 'URL') {
            setUrlSpecSynced(page.specSource.type === 'URL' && Boolean(page.specSource.lastSyncedAt));
            return;
        }

        setUrlSpecSynced(false);
    };

    return (
        <div className={styles.editor}>
            <div className={styles.toolbar}>
                <div className={styles.toolbarGroup}>
                    <Label htmlFor="openapi-source">Source</Label>
                    <Select value={sourceType} onValueChange={value => handleSourceTypeChange(value as SourceType)}>
                        <SelectTrigger id="openapi-source" className={styles.select}>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent {...TOOLBAR_SELECT_CONTENT_PROPS}>
                            <SelectItem value="INLINE">Inline</SelectItem>
                            <SelectItem value="API">API</SelectItem>
                            <SelectItem value="URL">URL</SelectItem>
                        </SelectContent>
                    </Select>
                </div>

                <div className={styles.toolbarGroup}>
                    <Label htmlFor="openapi-renderer">Renderer</Label>
                    <Select value={renderer} onValueChange={value => handleRendererChange(value as OpenApiRenderer)}>
                        <SelectTrigger id="openapi-renderer" className={styles.select}>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent {...TOOLBAR_SELECT_CONTENT_PROPS}>
                            {(Object.keys(OPENAPI_RENDERER_LABELS) as OpenApiRenderer[]).map(option => (
                                <SelectItem key={option} value={option}>
                                    {OPENAPI_RENDERER_LABELS[option]}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
            </div>

            <div className={styles.split}>
                <div
                    className={`${styles.inputPane} ${sourceType === 'INLINE' ? styles.inputPaneFill : ''}`}
                >
                    {showValidationStatus && (
                        <div
                            className={validation.valid ? styles.statusBar : `${styles.statusBar} ${styles.statusBarInvalid}`}
                            role="status"
                            aria-live="polite"
                        >
                            {validationLoading ? 'Loading spec…' : validation.valid ? 'Valid OpenAPI spec' : validation.error ?? 'Invalid spec'}
                        </div>
                    )}

                    {sourceType === 'API' && (
                        <div className={styles.apiSource}>
                            {apiAncestor ? (
                                <p>
                                    Using spec from <strong>{apiAncestor.title}</strong>
                                </p>
                            ) : (
                                <p className={styles.warning}>Place this page under an API nav item to resolve its spec.</p>
                            )}
                        </div>
                    )}

                    {sourceType === 'URL' && (
                        <div className={styles.urlSource}>
                            <Input
                                aria-label="OpenAPI spec URL"
                                value={url}
                                onChange={event => setUrl(event.target.value)}
                                placeholder="https://petstore.swagger.io/v2/swagger.json"
                            />
                            <Button type="button" onClick={() => void handleFetchUrl()} disabled={!url.trim()}>
                                Fetch
                            </Button>
                            {syncStatus && <span className={styles.syncStatus}>{syncStatus}</span>}
                        </div>
                    )}

                    {sourceType === 'INLINE' && (
                        <textarea
                            aria-label="OpenAPI spec"
                            className={styles.codeEditor}
                            value={inlineContent}
                            onChange={event => handleInlineChange(event.target.value)}
                            placeholder="Paste an OpenAPI or Swagger spec (YAML or JSON)"
                            spellCheck={false}
                        />
                    )}
                </div>

                <div className={styles.previewPane}>
                    <OpenApiRendererView renderer={renderer} specContent={specContent} />
                </div>
            </div>
        </div>
    );
});
