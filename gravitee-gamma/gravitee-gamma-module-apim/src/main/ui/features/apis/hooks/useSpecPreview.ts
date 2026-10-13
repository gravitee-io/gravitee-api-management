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

import { parseSpecDocument, type SpecContentType, type SpecDocument, type SpecDocumentResult } from '../utils/specDocument';

/** Swagger UI and Redoc rebuild their whole view on each new document, too slow to follow every keystroke. */
export const SPEC_PREVIEW_DELAY_MS = 400;

export type SpecPreviewProblem = Exclude<SpecDocumentResult, { status: 'valid' }>;

export interface SpecPreview {
    /** The document to show: the content's own, or the last valid one while the content does not parse. */
    document: SpecDocument | undefined;
    /** Why the content itself cannot be shown. With a document, that document is out of date. */
    problem: SpecPreviewProblem | undefined;
}

interface PreviewState extends SpecPreview {
    previewedContent: string;
}

/**
 * Follows an OpenAPI or AsyncAPI page's content as it is typed. Content that does not parse keeps the last valid
 * document on screen, so the preview does not flash an error at every keystroke that leaves the YAML incomplete.
 */
export function useSpecPreview(type: SpecContentType, content: string): SpecPreview {
    const [state, setState] = useState<PreviewState>(() => previewOf(type, content, undefined));

    useEffect(() => {
        const timer = setTimeout(
            () => setState(previous => (previous.previewedContent === content ? previous : previewOf(type, content, previous.document))),
            SPEC_PREVIEW_DELAY_MS,
        );
        return () => clearTimeout(timer);
    }, [type, content]);

    return { document: state.document, problem: state.problem };
}

function previewOf(type: SpecContentType, content: string, lastDocument: SpecDocument | undefined): PreviewState {
    const result = parseSpecDocument(type, content);
    switch (result.status) {
        case 'valid':
            return { previewedContent: content, document: result.document, problem: undefined };
        case 'invalid':
            return { previewedContent: content, document: lastDocument, problem: result };
        case 'empty':
            return { previewedContent: content, document: undefined, problem: result };
    }
}
