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
import { Component, type ErrorInfo, type ReactNode } from 'react';

import type { SpecPreview } from '../../../hooks/useSpecPreview';
import type { SpecDocument } from '../../../utils/specDocument';

// Redoc, Swagger UI and AsyncAPI set only some of their colors and inherit the rest, expecting a light page as in
// the portal. Graphene cannot scope its light theme to a subtree, so that page is recreated here.
const PORTAL_PAGE_SURFACE = { colorScheme: 'light', backgroundColor: '#ffffff', color: '#333333' } as const;

/**
 * What an OpenAPI or AsyncAPI preview shows around its viewer: why the preview is out of date, or why there is
 * nothing to preview. The viewer is only ever given a document that parsed.
 */
export function SpecPreviewFrame({
    preview,
    children,
}: Readonly<{ preview: SpecPreview; children: (document: SpecDocument) => ReactNode }>) {
    const { document, problem } = preview;

    if (!document) {
        return problem?.status === 'invalid' ? (
            <Alert variant="destructive">
                <AlertDescription>{problem.message}</AlertDescription>
            </Alert>
        ) : (
            <p className="text-sm text-muted-foreground">Nothing to preview yet.</p>
        );
    }

    return (
        <div className="flex flex-col gap-4">
            {problem?.status === 'invalid' ? (
                <Alert variant="warning">
                    <AlertDescription>Preview not updated: {problem.message}</AlertDescription>
                </Alert>
            ) : null}
            <div className={problem ? 'opacity-50' : undefined}>
                <ViewerErrorBoundary document={document}>
                    <div className="rounded-md p-4" style={PORTAL_PAGE_SURFACE}>
                        {children(document)}
                    </div>
                </ViewerErrorBoundary>
            </div>
        </div>
    );
}

export function PreviewLoading() {
    return (
        <div role="status" aria-busy="true" aria-label="Loading preview">
            <Skeleton className="h-64 w-full rounded-lg" />
        </div>
    );
}

export function PreviewLoadError() {
    return (
        <Alert variant="destructive">
            <AlertDescription>The preview could not be loaded.</AlertDescription>
        </Alert>
    );
}

interface ViewerErrorBoundaryProps {
    document: SpecDocument;
    children: ReactNode;
}

/** Keeps a viewer that throws from taking the editor down with it. The next document gets a fresh attempt. */
class ViewerErrorBoundary extends Component<ViewerErrorBoundaryProps, { failed: boolean }> {
    override state = { failed: false };

    static getDerivedStateFromError() {
        return { failed: true };
    }

    override componentDidCatch(error: Error, info: ErrorInfo) {
        console.error('[SpecPreviewFrame] The viewer failed to render the document', error, info.componentStack);
    }

    override componentDidUpdate(previous: ViewerErrorBoundaryProps) {
        if (this.state.failed && previous.document !== this.props.document) {
            this.setState({ failed: false });
        }
    }

    override render() {
        return this.state.failed ? <PreviewLoadError /> : this.props.children;
    }
}
