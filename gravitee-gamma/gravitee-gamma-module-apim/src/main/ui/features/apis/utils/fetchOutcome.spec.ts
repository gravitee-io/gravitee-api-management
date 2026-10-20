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
import { fetchOutcome, fetchSummaryOutcome } from './fetchOutcome';
import type { PortalNavigationPage } from '../types/apiDocumentation';

const PAGE: PortalNavigationPage = {
    id: 'nav-1',
    organizationId: 'DEFAULT',
    environmentId: 'env-1',
    title: 'Petstore API',
    type: 'PAGE',
    area: 'TOP_NAVBAR',
    rootId: 'nav-1',
    order: 0,
    published: false,
    visibility: 'PUBLIC',
    portalPageContentId: 'content-1',
    source: { type: 'github-fetcher', configuration: {} },
};

describe('fetchOutcome', () => {
    it('reports a page whose content was fetched', () => {
        expect(fetchOutcome('Petstore API', { item: PAGE })).toEqual({ level: 'success', message: "Content of 'Petstore API' fetched" });
    });

    it("reports a page whose fetch failed, with the server's reason", () => {
        const failed = { ...PAGE, source: { ...PAGE.source!, lastFetchError: '404 Not Found' } };

        expect(fetchOutcome('Petstore API', { item: failed })).toEqual({
            level: 'error',
            message: "'Petstore API' could not be fetched: 404 Not Found",
        });
    });

    it('reports a folder by its summary', () => {
        expect(fetchOutcome('SDK reference', { summary: { succeeded: 2, failed: 0, results: [] } })).toEqual({
            level: 'success',
            message: '2 pages fetched',
        });
    });
});

describe('fetchSummaryOutcome', () => {
    it('warns when no page was fetched at all, rather than reporting a success', () => {
        expect(fetchSummaryOutcome({ succeeded: 0, failed: 0, results: [] })).toEqual({
            level: 'warning',
            message: 'No pages were fetched.',
        });
    });

    it('still reports the count when the server names no failed file', () => {
        expect(fetchSummaryOutcome({ succeeded: 1, failed: 1, results: [] })).toEqual({
            level: 'warning',
            message: '1 of 2 pages fetched.',
        });
    });

    it('counts a single page in the singular', () => {
        expect(fetchSummaryOutcome({ succeeded: 1, failed: 0, results: [] }).message).toBe('1 page fetched');
    });

    it('names each failed file with its reason', () => {
        expect(
            fetchSummaryOutcome({
                succeeded: 12,
                failed: 2,
                results: [
                    { title: 'guide.md', success: true },
                    { title: 'errors.md', success: false, error: '404 Not Found' },
                    { title: 'webhooks.md', success: false, error: '404 Not Found' },
                ],
            }),
        ).toEqual({
            level: 'warning',
            message: '12 of 14 pages fetched. Failed: errors.md (404 Not Found), webhooks.md (404 Not Found).',
        });
    });

    it('names a failed file without a reason on its own', () => {
        expect(fetchSummaryOutcome({ succeeded: 0, failed: 1, results: [{ title: 'errors.md', success: false }] }).message).toBe(
            '0 of 1 page fetched. Failed: errors.md.',
        );
    });
});
