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
import { clearPortalsDatabase } from './navigation-items.storage.test-utils';
import {
    deletePageContent,
    deletePageContentsForPortal,
    getPageContent,
    savePageContent,
} from './page-contents.storage';
import { buildPageContent } from './page-contents.storage.test-utils';

jest.mock('../../editor/gmd/gmd-content', () => ({
    serializeDocumentToGmd: jest.fn(() => 'upgraded-gmd'),
}));

jest.mock('../../editor/utils/markdown-to-blocks', () => ({
    looksLikeMarkdown: (text: string) => text.includes('##'),
    markdownToBlocks: () => [
        {
            type: 'heading',
            props: { level: 2 },
            content: [{ type: 'text', text: 'Getting started', styles: {} }],
            children: [],
        },
    ],
}));

describe('page-contents.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should save and load page content by navigation item id', async () => {
        const content = buildPageContent();

        await savePageContent(content);

        expect(await getPageContent('nav-test')).toEqual(content);
    });

    it('should delete page content', async () => {
        const content = buildPageContent();

        await savePageContent(content);
        await deletePageContent(content.id);

        expect(await getPageContent('nav-test')).toBeUndefined();
    });

    it('should upgrade legacy markdown paragraphs when loading block page content', async () => {
        const legacyContent = buildPageContent({
            document: [
                {
                    id: 'legacy-paragraph',
                    type: 'paragraph',
                    content: [{ type: 'text', text: '## Getting started\n\n1. Step', styles: {} }],
                    children: [],
                },
            ],
            gmd: 'legacy-gmd',
        });

        await savePageContent(legacyContent);

        const loaded = await getPageContent('nav-test');

        expect(loaded?.document).toHaveLength(1);
        expect(loaded?.document[0]).toMatchObject({ type: 'heading', props: { level: 2 } });
        expect(loaded?.gmd).toBe('upgraded-gmd');
    });

    it('should delete all page contents for a portal', async () => {
        const portalAContent = buildPageContent({
            id: 'page-content-a',
            portalId: 'portal-a',
            navigationItemId: 'nav-a',
        });
        const portalBContent = buildPageContent({
            id: 'page-content-b',
            portalId: 'portal-b',
            navigationItemId: 'nav-b',
        });

        await savePageContent(portalAContent);
        await savePageContent(portalBContent);
        await deletePageContentsForPortal('portal-a');

        expect(await getPageContent('nav-a')).toBeUndefined();
        expect(await getPageContent('nav-b')).toEqual(portalBContent);
    });
});
