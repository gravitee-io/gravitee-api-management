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
import { toApiListSortBy } from './ApiListTable';

describe('toApiListSortBy', () => {
    it('returns undefined when no sort is active', () => {
        expect(toApiListSortBy(undefined)).toBeUndefined();
        expect(toApiListSortBy([])).toBeUndefined();
    });

    it('maps a regular column with a `-` prefix for descending', () => {
        expect(toApiListSortBy([{ id: 'API Name', desc: false }])).toBe('name');
        expect(toApiListSortBy([{ id: 'API Name', desc: true }])).toBe('-name');
        expect(toApiListSortBy([{ id: 'access', desc: false }])).toBe('paths');
    });

    it('maps the Sharding Tags column to the backend asymmetric values (tags_asc / -tags_desc)', () => {
        // The backend ApiSortByParam only accepts these exact strings; `tags`/`-tags` would 400.
        expect(toApiListSortBy([{ id: 'Sharding Tags', desc: false }])).toBe('tags_asc');
        expect(toApiListSortBy([{ id: 'Sharding Tags', desc: true }])).toBe('-tags_desc');
    });

    it('returns undefined for a non-server-sortable column', () => {
        expect(toApiListSortBy([{ id: 'Sync Status', desc: false }])).toBeUndefined();
    });
});
