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
import { useEffect, useMemo, useRef, useState } from 'react';
import type { SwaggerUIOptions } from 'swagger-ui';

import { PreviewLoadError, PreviewLoading, SpecPreviewFrame } from './SpecPreviewFrame';
import { loadRedoc, loadSwaggerUi, type SwaggerUiInstance } from './specViewers';
import { useSpecPreview } from '../../../hooks/useSpecPreview';
import type { PortalPageOpenApiConfiguration, PortalPageSwaggerConfiguration } from '../../../types/apiDocumentation';
import type { SpecDocument } from '../../../utils/specDocument';

type ViewerStatus = 'loading' | 'ready' | 'failed';

// The portal editor's Redoc preview options: a single column, without search, to fit half the screen.
const REDOC_OPTIONS = {
    hideDownloadButton: true,
    hideLoading: true,
    disableSearch: true,
    onlyRequiredInSamples: true,
    nativeScrollbars: true,
    theme: {
        sidebar: { width: '150px' },
        breakpoints: { medium: '599rem', large: '600rem' },
    },
};

interface OpenApiPreviewProps {
    readonly content: string;
    readonly configuration?: PortalPageOpenApiConfiguration;
}

/**
 * Renders an OpenAPI document with the viewer the portal will use for the page: Redoc, or Swagger UI, which is also
 * what the portal falls back to without a configuration.
 */
export function OpenApiPreview({ content, configuration }: OpenApiPreviewProps) {
    const preview = useSpecPreview('OPENAPI', content);

    return (
        <SpecPreviewFrame preview={preview}>
            {document =>
                configuration?.viewer === 'REDOC' ? (
                    <RedocViewer specDocument={document} serverUrl={configuration.tryItURL} />
                ) : (
                    // Swagger UI takes its options once: a new configuration gets a new viewer.
                    <SwaggerUiViewer key={JSON.stringify(configuration ?? {})} specDocument={document} configuration={configuration} />
                )
            }
        </SpecPreviewFrame>
    );
}

function SwaggerUiViewer({
    specDocument,
    configuration,
}: Readonly<{ specDocument: SpecDocument; configuration?: PortalPageSwaggerConfiguration }>) {
    const spec = useMemo(() => withServer(specDocument, swaggerServerUrl(configuration)), [specDocument, configuration]);
    const options = useMemo(() => swaggerOptions(configuration), [configuration]);
    const nodeRef = useRef<HTMLDivElement>(null);
    const instanceRef = useRef<SwaggerUiInstance | null>(null);
    const specRef = useRef(spec);
    const [status, setStatus] = useState<ViewerStatus>('loading');

    useEffect(() => {
        specRef.current = spec;
        instanceRef.current?.specActions.updateSpec(JSON.stringify(spec));
    }, [spec]);

    useEffect(() => {
        let isCancelled = false;
        loadSwaggerUi()
            .then(SwaggerUi => {
                if (isCancelled || !nodeRef.current) return;
                instanceRef.current = SwaggerUi({ ...options, domNode: nodeRef.current, spec: specRef.current });
                setStatus('ready');
            })
            .catch((error: unknown) => {
                console.error('[OpenApiPreview] Failed to load Swagger UI', error);
                if (!isCancelled) setStatus('failed');
            });
        return () => {
            isCancelled = true;
            instanceRef.current = null;
        };
    }, [options]);

    if (status === 'failed') {
        return <PreviewLoadError />;
    }
    return (
        <>
            {status === 'loading' ? <PreviewLoading /> : null}
            <div ref={nodeRef} />
        </>
    );
}

function RedocViewer({ specDocument, serverUrl }: Readonly<{ specDocument: SpecDocument; serverUrl?: string }>) {
    const spec = useMemo(() => withServer(specDocument, serverUrl), [specDocument, serverUrl]);
    const hostRef = useRef<HTMLDivElement>(null);
    const [status, setStatus] = useState<ViewerStatus>('loading');

    useEffect(() => {
        let isCancelled = false;
        loadRedoc()
            .then(Redoc => {
                const host = hostRef.current;
                if (isCancelled || !host) return;
                // Redoc renders once into the element it is given: each document gets a fresh one.
                const element = document.createElement('div');
                host.replaceChildren(element);
                Redoc.init(spec, REDOC_OPTIONS, element, (error?: unknown) => {
                    if (isCancelled) return;
                    if (error) {
                        console.error('[OpenApiPreview] Redoc could not render the document', error);
                        setStatus('failed');
                    } else {
                        setStatus('ready');
                    }
                });
            })
            .catch((error: unknown) => {
                console.error('[OpenApiPreview] Failed to load Redoc', error);
                if (!isCancelled) setStatus('failed');
            });
        return () => {
            isCancelled = true;
        };
    }, [spec]);

    return (
        <>
            {status === 'loading' ? <PreviewLoading /> : null}
            {status === 'failed' ? <PreviewLoadError /> : null}
            {/* Kept while failed, so that the next document can be rendered into it. */}
            <div ref={hostRef} hidden={status === 'failed'} />
        </>
    );
}

/** The portal editor's rule: a base URL replaces the document's servers, unless Swagger UI uses the API entrypoints. */
function swaggerServerUrl(configuration?: PortalPageSwaggerConfiguration): string | undefined {
    return configuration?.entrypointsAsServers ? undefined : configuration?.tryItURL;
}

function withServer(specDocument: SpecDocument, serverUrl?: string): SpecDocument {
    return serverUrl ? { ...specDocument, servers: [{ url: serverUrl }] } : specDocument;
}

/** The options the portal editor's Swagger UI preview reads from the configuration, with its defaults. */
function swaggerOptions(configuration?: PortalPageSwaggerConfiguration): SwaggerUIOptions {
    const maxDisplayedTags = configuration?.maxDisplayedTags;
    return {
        docExpansion: configuration?.docExpansion ?? 'none',
        displayOperationId: configuration?.displayOperationId ?? false,
        filter: configuration?.enableFiltering ?? false,
        showExtensions: configuration?.showExtensions ?? false,
        showCommonExtensions: configuration?.showCommonExtensions ?? false,
        maxDisplayedTags: maxDisplayedTags !== undefined && maxDisplayedTags >= 0 ? maxDisplayedTags : undefined,
    };
}
