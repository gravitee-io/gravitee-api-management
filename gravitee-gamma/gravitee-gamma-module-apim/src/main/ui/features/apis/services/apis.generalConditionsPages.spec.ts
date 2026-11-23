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
import { listApiGeneralConditionsPages } from './apis';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => {
    const actual = jest.requireActual('../../../shared/api/apimClient');
    return {
        ...actual,
        apimFetchJsonV2: jest.fn(),
    };
});

const mockFetch = jest.mocked(apimFetchJsonV2);

describe('listApiGeneralConditionsPages', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('returns only the published MARKDOWN pages of the API', async () => {
        mockFetch.mockResolvedValueOnce({
            pages: [
                { id: 'md-1', name: 'Terms of use', type: 'MARKDOWN', published: true },
                { id: 'md-2', name: 'Draft terms', type: 'MARKDOWN', published: false },
                { id: 'md-3', name: '', type: 'MARKDOWN', published: true },
                { id: 'sw-1', name: 'Petstore', type: 'SWAGGER', published: true },
                { id: 'ad-1', name: 'Guide', type: 'ASCIIDOC', published: true },
                { id: 'folder-1', name: 'Legal', type: 'FOLDER', published: true },
                { name: 'No id', type: 'MARKDOWN', published: true },
            ],
        });

        await expect(listApiGeneralConditionsPages('env-1', 'api-1')).resolves.toEqual([
            { id: 'md-1', name: 'Terms of use' },
            { id: 'md-3', name: 'md-3' },
        ]);
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api-1/pages');
    });

    it('returns no page when the API has no documentation', async () => {
        mockFetch.mockResolvedValueOnce({});

        await expect(listApiGeneralConditionsPages('env-1', 'api-1')).resolves.toEqual([]);
    });
});
