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
import { ApimApiError } from '@gravitee/gamma-ui-shared/api';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { ApisPage } from './ApisPage';
import { useApiList } from '../hooks/useApiList';
import { useApiStats } from '../hooks/useApiStats';

jest.mock('@gravitee/gamma-lib-observability', () => ({
    DEFAULT_TIME_RANGE: { type: 'relative', period: '5m' },
    encodeObservabilityState: () => ({ q: 'ENCODED_Q', v: '1' }),
}));

jest.mock('../hooks/useApiList');
jest.mock('../hooks/useApiStats');

const mockUseApiList = useApiList as jest.Mock;
const mockUseApiStats = useApiStats as jest.Mock;

const STUB_STATS = { total: 0, private: 0, published: 0, isLoading: false };

const NATIVE_PROXY_NAME = 'Payments Proxy';
const FEDERATED_API_NAME = 'Federated Orders';

const API_ROWS = [
    { id: 'native-1', name: NATIVE_PROXY_NAME, apiVersion: '1.0', type: 'PROXY', definitionVersion: 'V4' },
    { id: 'federated-1', name: FEDERATED_API_NAME, apiVersion: '1.0', type: 'PROXY', definitionVersion: 'V4' },
];

const FEDERATED_PROVIDERS = [
    'aws-api-gateway',
    'solace',
    'apigee',
    'azure-api-management',
    'ibm-api-connect',
    'confluent-platform',
    'mulesoft',
    'edge-stack',
];

function federatedRow(provider?: string) {
    return {
        id: `federated-${provider ?? 'unattributed'}`,
        name: `Orders via ${provider ?? 'an unnamed integration'}`,
        apiVersion: '1.0',
        type: 'PROXY',
        definitionVersion: 'FEDERATED',
        originContext: { origin: 'INTEGRATION', provider },
    };
}

const NATIVE_SORTABLE_ROW = {
    id: 'native-1',
    name: NATIVE_PROXY_NAME,
    apiVersion: '1.0',
    type: 'PROXY',
    definitionVersion: 'V4',
    state: 'STARTED',
    listeners: [{ type: 'HTTP', paths: [{ path: '/payments' }] }],
    tags: ['eu-west'],
};

// The federated row carries none of state/listeners/tags — the values the three sortable columns read.
const FEDERATED_SORTABLE_ROW = federatedRow('solace');

const MIXED_ROWS = [NATIVE_SORTABLE_ROW, FEDERATED_SORTABLE_ROW];

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

    it('renders an API row of either kind when the search succeeds', () => {
        mockUseApiList.mockReturnValue({
            data: { data: API_ROWS, pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: API_ROWS.length } },
            isLoading: false,
            isFetching: false,
        });
        renderPage();

        expect(screen.queryByText(NATIVE_PROXY_NAME)).not.toBeNull();
        expect(screen.queryByText(FEDERATED_API_NAME)).not.toBeNull();
    });

    it.each<[string, ReturnType<typeof federatedRow>[]]>([
        ['carries no origin provider', [federatedRow()]],
        ['carries any of the federated origin providers', FEDERATED_PROVIDERS.map(provider => federatedRow(provider))],
        // `kong` has no display-name entry; the mapped row alongside it would catch an inverted filter.
        ['carries an origin provider with no display name, alongside one that has', [federatedRow('kong'), federatedRow('solace')]],
    ])('renders exactly one row per federated API when it %s', (_case, rows) => {
        mockUseApiList.mockReturnValue({
            data: { data: rows, pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: rows.length } },
            isLoading: false,
            isFetching: false,
        });
        renderPage();

        rows.forEach(row => expect(screen.getAllByText(row.name)).toHaveLength(1));
    });

    it('renders no API row of either kind when the search is refused with 403', () => {
        mockUseApiList.mockReturnValue({
            data: undefined,
            isLoading: false,
            isFetching: false,
            isPlaceholderData: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        });
        renderPage();

        expect(screen.queryByText(NATIVE_PROXY_NAME)).toBeNull();
        expect(screen.queryByText(FEDERATED_API_NAME)).toBeNull();
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

    function lastRequest() {
        const calls = mockUseApiList.mock.calls;
        return calls[calls.length - 1][0];
    }

    function lastRequestedSortBy() {
        return lastRequest().sortBy;
    }

    function clickColumnHeader(title: string) {
        fireEvent.click(screen.getByRole('button', { name: title }));
    }

    function expectBothRowsRendered() {
        expect(screen.queryByText(NATIVE_PROXY_NAME)).not.toBeNull();
        expect(screen.queryByText(FEDERATED_SORTABLE_ROW.name)).not.toBeNull();
    }

    it.each<[string, string, string]>([
        ['Runtime Status', 'status', '-status'],
        ['Access', 'paths', '-paths'],
        ['Sharding Tags', 'tags_asc', '-tags_desc'],
    ])('sorts by %s both ways with a federated row in the list', (columnTitle, ascending, descending) => {
        mockUseApiList.mockReturnValue({
            data: { data: MIXED_ROWS, pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: MIXED_ROWS.length } },
            isLoading: false,
            isFetching: false,
        });
        renderPage();

        clickColumnHeader(columnTitle);

        expect(lastRequestedSortBy()).toBe(ascending);
        expectBothRowsRendered();

        clickColumnHeader(columnTitle);

        expect(lastRequestedSortBy()).toBe(descending);
        expectBothRowsRendered();
    });

    it('sorts by the newly clicked column instead of the one sorted before it', () => {
        mockUseApiList.mockReturnValue({
            data: { data: MIXED_ROWS, pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: MIXED_ROWS.length } },
            isLoading: false,
            isFetching: false,
        });
        renderPage();

        clickColumnHeader('Runtime Status');
        clickColumnHeader('Access');

        expect(lastRequestedSortBy()).toBe('paths');
        expectBothRowsRendered();
    });

    it('returns to page 1 when a column is sorted from a later page', () => {
        mockUseApiList.mockReturnValue({
            data: { data: MIXED_ROWS, pagination: { page: 1, perPage: 10, pageCount: 3, totalCount: 25 } },
            isLoading: false,
            isFetching: false,
        });
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /next page/i }));
        expect(lastRequest().page).toBe(2);

        clickColumnHeader('Runtime Status');

        expect(lastRequest().page).toBe(1);
        expect(lastRequest().sortBy).toBe('status');
        expectBothRowsRendered();
    });

    it('adds the sort to the outbound request without dropping the active search term', async () => {
        mockUseApiList.mockReturnValue({
            data: { data: MIXED_ROWS, pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: MIXED_ROWS.length } },
            isLoading: false,
            isFetching: false,
        });
        renderPage();

        fireEvent.change(screen.getByPlaceholderText('Search APIs...'), { target: { value: 'payments' } });
        await waitFor(() => expect(lastRequest().query).toBe('payments'));

        clickColumnHeader('Runtime Status');

        expect(lastRequest()).toEqual({ query: 'payments', page: 1, perPage: 10, sortBy: 'status' });
        expectBothRowsRendered();
    });
});
