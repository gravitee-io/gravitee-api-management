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
import { isSameSource, sourceChangeOf } from './sourceChange';
import type { PortalNavigationItemSource } from '../types/apiDocumentation';

const STORED: PortalNavigationItemSource = {
    type: 'github-fetcher',
    configuration: { repository: 'petstore', personalAccessToken: '********' },
    useAutoFetch: true,
    fetchCron: '0 0 * * * *',
    lastFetchedAt: '2026-10-12T10:42:00Z',
    subtreeImport: true,
};

describe('isSameSource', () => {
    it('ignores what the server records about the fetches, and the order of the settings', () => {
        const edited: PortalNavigationItemSource = {
            type: 'github-fetcher',
            configuration: { personalAccessToken: '********', repository: 'petstore' },
            useAutoFetch: true,
            fetchCron: '0 0 * * * *',
        };

        expect(isSameSource(STORED, edited)).toBe(true);
    });

    it('sees a change of fetcher, of setting, or of schedule', () => {
        expect(isSameSource(STORED, { ...STORED, type: 'gitlab-fetcher' })).toBe(false);
        expect(isSameSource(STORED, { ...STORED, configuration: { ...STORED.configuration, repository: 'billing' } })).toBe(false);
        expect(isSameSource(STORED, { ...STORED, fetchCron: '0 0 0 * * *' })).toBe(false);
        expect(isSameSource(STORED, { ...STORED, useAutoFetch: false })).toBe(false);
    });

    it('ignores a schedule kept while automatic fetching is off', () => {
        expect(isSameSource({ ...STORED, useAutoFetch: false }, { ...STORED, useAutoFetch: false, fetchCron: undefined })).toBe(true);
    });

    it('treats a switch left off as unset, as the server leaves settings that are false out', () => {
        expect(
            isSameSource(
                { ...STORED, configuration: { repository: 'petstore' } },
                { ...STORED, configuration: { repository: 'petstore', useSystemProxy: false } },
            ),
        ).toBe(true);
        expect(
            isSameSource(
                { ...STORED, configuration: { repository: 'petstore', useSystemProxy: true } },
                { ...STORED, configuration: { repository: 'petstore', useSystemProxy: false } },
            ),
        ).toBe(false);
    });

    it('treats a setting left empty as unset', () => {
        expect(
            isSameSource(
                { ...STORED, configuration: { repository: 'petstore' } },
                { ...STORED, configuration: { repository: 'petstore', branchOrTag: '' } },
            ),
        ).toBe(true);
    });
});

describe('sourceChangeOf', () => {
    const EDITED: PortalNavigationItemSource = { type: 'gitlab-fetcher', configuration: { project: 'acme/sdk' }, useAutoFetch: false };

    it('removes the stored source when the item stops being fetched', () => {
        expect(sourceChangeOf(STORED, false, { source: EDITED, valid: true })).toBeNull();
    });

    it('changes nothing for an item without a source that is not fetched', () => {
        expect(sourceChangeOf(undefined, false, { source: null, valid: false })).toBeUndefined();
    });

    it('adds the edited source to an item without one', () => {
        expect(sourceChangeOf(undefined, true, { source: EDITED, valid: true })).toBe(EDITED);
    });

    it('replaces the stored source by an edited one that differs', () => {
        expect(sourceChangeOf(STORED, true, { source: EDITED, valid: true, unchanged: false })).toBe(EDITED);
    });

    it('keeps the stored source when the edit leaves it as it is, or could not be made', () => {
        expect(sourceChangeOf(STORED, true, { source: STORED, valid: true, unchanged: true })).toBeUndefined();
        expect(sourceChangeOf(STORED, true, { source: null, valid: false })).toBeUndefined();
    });
});
