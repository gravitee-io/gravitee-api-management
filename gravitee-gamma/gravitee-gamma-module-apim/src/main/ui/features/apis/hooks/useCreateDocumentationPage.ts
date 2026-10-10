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
import { useState } from 'react';

import { useCreateApiDocumentationItem, useSaveApiDocumentationPageContent } from './useApiDocumentation';
import { notify } from '../../../shared/notify';
import { extractErrorMessage } from '../../../shared/notify/extractErrorMessage';
import type { PortalPageContentType, PortalVisibility } from '../types/apiDocumentation';

export interface NewDocumentationPage {
    title: string;
    visibility: PortalVisibility;
    contentType: PortalPageContentType;
    /** Empty for a page to be written in the editor. */
    content: string;
    /** A folder owned by the API; without it, the page is created at the top level. */
    parentId?: string;
}

/**
 * Creates an unpublished page, then saves its content, and tells the user how it went. Resolves to the page's id once
 * the page exists, even when its content could not be saved, and to null when the page could not be created.
 */
export function useCreateDocumentationPage(apiId: string) {
    const createItem = useCreateApiDocumentationItem(apiId);
    const saveContent = useSaveApiDocumentationPageContent(apiId);
    // Stays true once the page exists: the caller moves on to it.
    const [isCreating, setIsCreating] = useState(false);

    async function createPage({ title, visibility, contentType, content, parentId }: NewDocumentationPage): Promise<string | null> {
        setIsCreating(true);
        let pageId: string;
        try {
            const page = await createItem.mutateAsync({ type: 'PAGE', title, contentType, area: 'TOP_NAVBAR', visibility, parentId });
            pageId = page.id;
        } catch (error) {
            notify.error(error, 'Failed to create the page');
            setIsCreating(false);
            return null;
        }

        // A page is created empty: its text can only be written through its content, once the page exists.
        if (content !== '') {
            try {
                await saveContent.mutateAsync({ navId: pageId, content });
            } catch (error) {
                notify.warning(`Page '${title}' was created, but its content could not be saved: ${extractErrorMessage(error)}`);
                return pageId;
            }
        }

        notify.success(`Page '${title}' created`);
        return pageId;
    }

    return { createPage, isCreating };
}
