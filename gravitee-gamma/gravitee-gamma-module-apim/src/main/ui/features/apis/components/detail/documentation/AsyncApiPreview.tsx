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
import { useEffect, useState } from 'react';

import { PreviewLoadError, PreviewLoading, SpecPreviewFrame } from './SpecPreviewFrame';
import { type AsyncApiViewer, loadAsyncApiViewer } from './specViewers';
import { useSpecPreview } from '../../../hooks/useSpecPreview';
import type { SpecDocument } from '../../../utils/specDocument';

/** Renders an AsyncAPI document with the AsyncAPI viewer the portal uses. */
export function AsyncApiPreview({ content }: Readonly<{ content: string }>) {
    const preview = useSpecPreview('ASYNCAPI', content);

    return <SpecPreviewFrame preview={preview}>{document => <AsyncApiDocumentViewer specDocument={document} />}</SpecPreviewFrame>;
}

function AsyncApiDocumentViewer({ specDocument }: Readonly<{ specDocument: SpecDocument }>) {
    const [viewer, setViewer] = useState<{ Viewer: AsyncApiViewer } | 'failed'>();

    useEffect(() => {
        let isCancelled = false;
        loadAsyncApiViewer()
            .then(Viewer => {
                if (!isCancelled) setViewer({ Viewer });
            })
            .catch((error: unknown) => {
                console.error('[AsyncApiPreview] Failed to load the AsyncAPI viewer', error);
                if (!isCancelled) setViewer('failed');
            });
        return () => {
            isCancelled = true;
        };
    }, []);

    if (viewer === 'failed') {
        return <PreviewLoadError />;
    }
    if (!viewer) {
        return <PreviewLoading />;
    }
    return <viewer.Viewer schema={specDocument} />;
}
