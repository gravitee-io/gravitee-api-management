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

import { Alert, AlertDescription, Skeleton } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

type ViewerStatus = 'loading' | 'ready' | 'failed';

interface GraviteeMarkdownPreviewProps {
    readonly content: string;
}

/**
 * Renders Gravitee Markdown with the same viewer the portal uses, so the preview matches what is
 * published. Form field components (`gmd-input` and the like) are not supported by the viewer
 * element and render nothing.
 */
export function GraviteeMarkdownPreview({ content }: GraviteeMarkdownPreviewProps) {
    const [status, setStatus] = useState<ViewerStatus>('loading');

    useEffect(() => {
        let isCancelled = false;

        // A dynamic import keeps the viewer, which ships its own copy of Angular, out of every
        // screen that does not render a preview.
        import('@gravitee/gravitee-markdown-element')
            .then(({ registerGmdViewerElement }) => registerGmdViewerElement())
            .then(() => {
                if (!isCancelled) {
                    setStatus('ready');
                }
            })
            .catch((error: unknown) => {
                console.error('[GraviteeMarkdownPreview] Failed to load the Gravitee Markdown viewer', error);
                if (!isCancelled) {
                    setStatus('failed');
                }
            });

        return () => {
            isCancelled = true;
        };
    }, []);

    if (status === 'failed') {
        return (
            <Alert variant="destructive">
                <AlertDescription>The preview could not be loaded.</AlertDescription>
            </Alert>
        );
    }
    if (status === 'loading') {
        return (
            <div role="status" aria-busy="true" aria-label="Loading preview">
                <Skeleton className="h-64 w-full rounded-lg" />
            </div>
        );
    }
    // The element is registered before it renders, so React 19 sets `content` as a property rather
    // than an attribute: markdown is too large and too newline-heavy for an attribute.
    return <gmd-viewer content={content} />;
}
