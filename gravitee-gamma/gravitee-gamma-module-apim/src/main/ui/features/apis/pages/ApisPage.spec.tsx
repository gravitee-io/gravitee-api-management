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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { ApisPage } from './ApisPage';
import { useApiList } from '../hooks/useApiList';
import { useApiStats } from '../hooks/useApiStats';
import { useEnvironmentTotalCalls } from '../hooks/useEnvironmentTotalCalls';

jest.mock('../hooks/useApiList');
jest.mock('../hooks/useApiStats');
jest.mock('../hooks/useEnvironmentTotalCalls');

const mockUseApiList = useApiList as jest.Mock;
const mockUseApiStats = useApiStats as jest.Mock;
const mockUseEnvironmentTotalCalls = useEnvironmentTotalCalls as jest.Mock;

const STUB_STATS = { total: 0, private: 0, published: 0, isLoading: false };

function renderPage() {
    return render(
        <MemoryRouter>
            <ApisPage />
        </MemoryRouter>,
    );
}

describe('ApisPage', () => {
    beforeEach(() => {
        mockUseApiStats.mockReturnValue(STUB_STATS);
        mockUseEnvironmentTotalCalls.mockReturnValue({ total: null, isLoading: false });
    });

    afterEach(() => jest.clearAllMocks());

    it('shows the empty landing when there are no APIs and no active search', () => {
        mockUseApiList.mockReturnValue({
            data: { data: [], pagination: { page: 1, perPage: 10, pageCount: 0, totalCount: 0 } },
            isLoading: false,
            isFetching: false,
        });
        renderPage();

        expect(screen.queryByText('Why add an API proxy?')).not.toBeNull();
        expect(screen.queryByPlaceholderText('Search APIs...')).toBeNull();
    });

    it('shows the list view when APIs exist', () => {
        mockUseApiList.mockReturnValue({
            data: {
                data: [{ id: '1', name: 'My API', apiVersion: '1.0', type: 'PROXY', definitionVersion: 'V4' }],
                pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: 1 },
            },
            isLoading: false,
            isFetching: false,
        });
        renderPage();

        expect(screen.queryByText('Why add an API proxy?')).toBeNull();
        expect(screen.queryByPlaceholderText('Search APIs...')).not.toBeNull();
    });

    it('shows the list view while loading — does not flash the empty landing', () => {
        mockUseApiList.mockReturnValue({ data: undefined, isLoading: true, isFetching: false });
        renderPage();

        expect(screen.queryByText('Why add an API proxy?')).toBeNull();
    });

    it('resets page to 1 when the search term changes', async () => {
        mockUseApiList.mockReturnValue({
            data: {
                data: [{ id: '1', name: 'My API', apiVersion: '1.0', type: 'PROXY', definitionVersion: 'V4' }],
                pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: 1 },
            },
            isLoading: false,
            isFetching: false,
        });
        renderPage();

        const input = screen.getByPlaceholderText('Search APIs...');
        fireEvent.change(input, { target: { value: 'new-search' } });

        await waitFor(() => {
            const calls = mockUseApiList.mock.calls;
            const searchCall = calls.find(([params]) => params.query === 'new-search');
            expect(searchCall).not.toBeUndefined();
            expect(searchCall![0].page).toBe(1);
        });
    });
});
