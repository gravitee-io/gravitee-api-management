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
import type { BlockPageContent, PageContent } from '../types';
import { serializeDocumentToGmd } from '../../editor/gmd/gmd-content';
import { upgradeLegacyMarkdownInDocument } from '../../editor/utils/upgrade-legacy-markdown-document';
import { PAGE_CONTENTS_STORE_NAME, runTransaction } from './db';

export const STORE_NAME = PAGE_CONTENTS_STORE_NAME;

function isBlockPageContent(content: PageContent): content is BlockPageContent {
    return 'document' in content;
}

async function maybeUpgradeBlockPageContent(content: BlockPageContent): Promise<BlockPageContent> {
    const upgradedDocument = upgradeLegacyMarkdownInDocument(content.document);
    if (upgradedDocument === content.document) {
        return content;
    }

    const upgradedContent: BlockPageContent = {
        ...content,
        document: upgradedDocument,
        gmd: serializeDocumentToGmd(upgradedDocument),
    };
    await savePageContent(upgradedContent);
    return upgradedContent;
}

export async function getPageContent(navigationItemId: string): Promise<PageContent | undefined> {
    const content = await runTransaction(PAGE_CONTENTS_STORE_NAME, 'readonly', store =>
        store.index('navigationItemId').get(navigationItemId),
    );

    if (!content || !isBlockPageContent(content)) {
        return content;
    }

    return maybeUpgradeBlockPageContent(content);
}

export async function savePageContent(content: PageContent): Promise<void> {
    await runTransaction(PAGE_CONTENTS_STORE_NAME, 'readwrite', store => store.put(content));
}

export async function deletePageContent(id: string): Promise<void> {
    await runTransaction(PAGE_CONTENTS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deletePageContentsForPortal(portalId: string): Promise<void> {
    const contents = await runTransaction<PageContent[]>(PAGE_CONTENTS_STORE_NAME, 'readonly', store =>
        store.index('portalId').getAll(portalId),
    );
    await Promise.all(contents.map(content => deletePageContent(content.id)));
}
