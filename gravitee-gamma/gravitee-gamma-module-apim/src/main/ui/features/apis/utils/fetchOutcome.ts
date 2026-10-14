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
import type { FetchPortalNavigationItemResponse, PortalNavigationItemsFetchSummary } from '../types/apiDocumentation';

export interface FetchOutcome {
    level: 'success' | 'warning' | 'error';
    message: string;
}

/** What to tell the user after a manual fetch: the page's own outcome, or the summary of the pages below a folder. */
export function fetchOutcome(title: string, response: FetchPortalNavigationItemResponse): FetchOutcome {
    if (response.summary) return fetchSummaryOutcome(response.summary);
    const error = response.item && response.item.type !== 'LINK' ? response.item.source?.lastFetchError : undefined;
    return error
        ? { level: 'error', message: `'${title}' could not be fetched: ${error}` }
        : { level: 'success', message: `Content of '${title}' fetched` };
}

export function fetchSummaryOutcome({ succeeded, failed, results }: PortalNavigationItemsFetchSummary): FetchOutcome {
    if (succeeded + failed === 0) {
        return { level: 'warning', message: 'No pages were fetched.' };
    }
    if (failed === 0) {
        return { level: 'success', message: `${pages(succeeded)} fetched` };
    }
    const failures = results
        .filter(result => !result.success)
        .map(result => (result.error ? `${result.title} (${result.error})` : result.title))
        .join(', ');
    const count = `${succeeded} of ${pages(succeeded + failed)} fetched.`;
    return { level: 'warning', message: failures ? `${count} Failed: ${failures}.` : count };
}

function pages(count: number): string {
    return count === 1 ? '1 page' : `${count} pages`;
}
