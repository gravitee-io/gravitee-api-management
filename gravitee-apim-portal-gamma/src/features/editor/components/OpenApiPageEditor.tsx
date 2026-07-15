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
    Button,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Switch,
} from '@gravitee/graphene-core';
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
import { buildGitSpecUrl } from '../utils/build-git-spec-url';
import { getOpenApiEditorValidationState } from '../utils/openapi-editor-validation';
import { resolveOpenApiSpecContent } from '../utils/resolve-openapi-spec';
import {
    SPEC_SOURCE_LABELS,
    SPEC_SOURCE_TYPES,
    getSpecSourceIcon,
    isRemoteSpecSource,
    isRemoteSpecSourceType,
} from '../utils/spec-source-options';
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

function isRemoteSpecSynced(specSource: OpenApiSpecSource): boolean {
    return isRemoteSpecSource(specSource) && Boolean(specSource.lastSyncedAt);
}

function readRemoteOptions(specSource: OpenApiSpecSource): { useSystemProxy: boolean; autoFetch: boolean } {
    if (!isRemoteSpecSource(specSource)) {
        return { useSystemProxy: false, autoFetch: false };
    }
    return {
        useSystemProxy: specSource.useSystemProxy ?? false,
        autoFetch: specSource.autoFetch ?? false,
    };
}

interface RemoteSourceTogglesProps {
    readonly useSystemProxy: boolean;
    readonly autoFetch: boolean;
    readonly onUseSystemProxyChange: (checked: boolean) => void;
    readonly onAutoFetchChange: (checked: boolean) => void;
}

function RemoteSourceToggles({
    useSystemProxy,
    autoFetch,
    onUseSystemProxyChange,
    onAutoFetchChange,
}: RemoteSourceTogglesProps) {
    return (
        <div className={styles.toggleGroup}>
            <div className={styles.toggleRow}>
                <div className={styles.toggleCopy}>
                    <Label htmlFor="openapi-use-system-proxy">Use system proxy</Label>
                    <p className={styles.toggleDescription}>
                        Use the system proxy configured by your administrator
                    </p>
                </div>
                <Switch
                    id="openapi-use-system-proxy"
                    checked={useSystemProxy}
                    onCheckedChange={onUseSystemProxyChange}
                    className={styles.toggleSwitch}
                />
            </div>
            <div className={styles.toggleRow}>
                <div className={styles.toggleCopy}>
                    <Label htmlFor="openapi-auto-fetch">Enable Auto Fetch</Label>
                    <p className={styles.toggleDescription}>
                        Enable a periodic update of this documentation page
                    </p>
                </div>
                <Switch
                    id="openapi-auto-fetch"
                    checked={autoFetch}
                    onCheckedChange={onAutoFetchChange}
                    className={styles.toggleSwitch}
                />
            </div>
        </div>
    );
}

export const OpenApiPageEditor = forwardRef<OpenApiPageEditorHandle, OpenApiPageEditorProps>(function OpenApiPageEditor(
    { page, content, navItems, onSave },
    ref,
) {
    const initialRemoteOptions = readRemoteOptions(page.specSource);

    const [renderer, setRenderer] = useState<OpenApiRenderer>(normalizeOpenApiRenderer(content.renderer));
    const [sourceType, setSourceType] = useState<SourceType>(page.specSource.type);
    const [url, setUrl] = useState(page.specSource.type === 'HTTP' ? page.specSource.url : '');
    const [repositoryUrl, setRepositoryUrl] = useState(
        page.specSource.type === 'GITHUB' || page.specSource.type === 'GITLAB' ? page.specSource.repositoryUrl : '',
    );
    const [branch, setBranch] = useState(
        page.specSource.type === 'GITHUB' || page.specSource.type === 'GITLAB' ? page.specSource.branch : 'main',
    );
    const [filepath, setFilepath] = useState(
        page.specSource.type === 'GITHUB' || page.specSource.type === 'GITLAB' ? page.specSource.filepath : '',
    );
    const [useSystemProxy, setUseSystemProxy] = useState(initialRemoteOptions.useSystemProxy);
    const [autoFetch, setAutoFetch] = useState(initialRemoteOptions.autoFetch);
    const [inlineContent, setInlineContent] = useState(
        page.specSource.type === 'INLINE' ? page.specSource.content : content.specContent,
    );
    const [specContent, setSpecContent] = useState(content.specContent);
    const [syncStatus, setSyncStatus] = useState<string | null>(null);
    const [loadingSpec, setLoadingSpec] = useState(false);
    const [apiSpecResolved, setApiSpecResolved] = useState(page.specSource.type === 'API');
    const [remoteSpecSynced, setRemoteSpecSynced] = useState(isRemoteSpecSynced(page.specSource));

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
                remoteSpecSynced,
            }),
        [apiAncestor, apiSpecResolved, inlineContent, loadingSpec, remoteSpecSynced, sourceType, specContent],
    );

    const buildSpecSource = useCallback((): OpenApiSpecSource => {
        switch (sourceType) {
            case 'HTTP':
                return { type: 'HTTP', url, useSystemProxy, autoFetch };
            case 'GITHUB':
            case 'GITLAB':
                return { type: sourceType, repositoryUrl, branch, filepath, useSystemProxy, autoFetch };
            case 'API':
                return { type: 'API', apiId: apiAncestor?.apiId ?? '' };
            case 'INLINE':
            default:
                return { type: 'INLINE', content: inlineContent };
        }
    }, [
        apiAncestor?.apiId,
        autoFetch,
        branch,
        filepath,
        inlineContent,
        repositoryUrl,
        sourceType,
        url,
        useSystemProxy,
    ]);

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

    const handleFetchRemote = async () => {
        setLoadingSpec(true);
        setSyncStatus(null);

        const nextSpecSource = buildSpecSource();
        const fetchUrl =
            nextSpecSource.type === 'HTTP'
                ? nextSpecSource.url
                : nextSpecSource.type === 'GITHUB' || nextSpecSource.type === 'GITLAB'
                  ? buildGitSpecUrl(nextSpecSource.type, nextSpecSource)
                  : '';

        try {
            const spec = await fetchOpenApiSpecFromUrl(fetchUrl);
            const syncedSpecSource: OpenApiSpecSource = {
                ...nextSpecSource,
                lastSyncedAt: Date.now(),
            } as OpenApiSpecSource;

            setSpecContent(spec.content);
            setRemoteSpecSynced(true);
            setSyncStatus(`Synced ${new Date().toLocaleTimeString()}`);
            await onSave(
                { ...content, renderer, specContent: spec.content },
                { renderer, specSource: syncedSpecSource },
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
            setRemoteSpecSynced(false);
            setLoadingSpec(true);
            return;
        }

        setApiSpecResolved(false);
        setLoadingSpec(false);

        if (isRemoteSpecSourceType(value)) {
            const remoteOptions =
                page.specSource.type === value ? readRemoteOptions(page.specSource) : { useSystemProxy: false, autoFetch: false };
            setUseSystemProxy(remoteOptions.useSystemProxy);
            setAutoFetch(remoteOptions.autoFetch);
            setRemoteSpecSynced(page.specSource.type === value && isRemoteSpecSynced(page.specSource));
            return;
        }

        setRemoteSpecSynced(false);
    };

    const canFetchRemote =
        sourceType === 'HTTP'
            ? Boolean(url.trim())
            : sourceType === 'GITHUB' || sourceType === 'GITLAB'
              ? Boolean(repositoryUrl.trim() && branch.trim() && filepath.trim())
              : false;

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
                            {SPEC_SOURCE_TYPES.map(option => (
                                <SelectItem key={option} value={option} className="gap-2">
                                    <span className="text-muted-foreground" aria-hidden="true">
                                        {getSpecSourceIcon(option)}
                                    </span>
                                    {SPEC_SOURCE_LABELS[option]}
                                </SelectItem>
                            ))}
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

                    {sourceType === 'HTTP' && (
                        <div className={styles.remoteSource}>
                            <Input
                                aria-label="OpenAPI spec URL"
                                value={url}
                                onChange={event => setUrl(event.target.value)}
                                placeholder="https://petstore.swagger.io/v2/swagger.json"
                            />
                            <Button type="button" onClick={() => void handleFetchRemote()} disabled={!canFetchRemote}>
                                Fetch
                            </Button>
                            {syncStatus && <span className={styles.syncStatus}>{syncStatus}</span>}
                            <RemoteSourceToggles
                                useSystemProxy={useSystemProxy}
                                autoFetch={autoFetch}
                                onUseSystemProxyChange={setUseSystemProxy}
                                onAutoFetchChange={setAutoFetch}
                            />
                        </div>
                    )}

                    {(sourceType === 'GITHUB' || sourceType === 'GITLAB') && (
                        <div className={styles.remoteSource}>
                            <div className={styles.fieldGrid}>
                                <div className={`${styles.field} ${styles.fieldFullWidth}`}>
                                    <Label htmlFor="git-repository-url">Repository URL</Label>
                                    <Input
                                        id="git-repository-url"
                                        value={repositoryUrl}
                                        onChange={event => setRepositoryUrl(event.target.value)}
                                        placeholder={
                                            sourceType === 'GITHUB'
                                                ? 'https://github.com/gravitee-io/gravitee-api-management'
                                                : 'https://gitlab.com/gravitee-io/gravitee-api-management'
                                        }
                                    />
                                </div>
                                <div className={styles.field}>
                                    <Label htmlFor="git-branch">Branch</Label>
                                    <Input
                                        id="git-branch"
                                        value={branch}
                                        onChange={event => setBranch(event.target.value)}
                                        placeholder="main"
                                    />
                                </div>
                                <div className={styles.field}>
                                    <Label htmlFor="git-filepath">File path</Label>
                                    <Input
                                        id="git-filepath"
                                        value={filepath}
                                        onChange={event => setFilepath(event.target.value)}
                                        placeholder="openapi/openapi.yaml"
                                    />
                                </div>
                            </div>
                            <Button type="button" onClick={() => void handleFetchRemote()} disabled={!canFetchRemote}>
                                Fetch
                            </Button>
                            {syncStatus && <span className={styles.syncStatus}>{syncStatus}</span>}
                            <RemoteSourceToggles
                                useSystemProxy={useSystemProxy}
                                autoFetch={autoFetch}
                                onUseSystemProxyChange={setUseSystemProxy}
                                onAutoFetchChange={setAutoFetch}
                            />
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
