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
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { ApisListView } from './ApisListView';
import { useApiStats } from '../../hooks/useApiStats';
import { useEnvironmentTotalCalls } from '../../hooks/useEnvironmentTotalCalls';

jest.mock('../../hooks/useApiStats');
jest.mock('../../hooks/useEnvironmentTotalCalls');

const mockUseApiStats = useApiStats as jest.Mock;
const mockUseEnvironmentTotalCalls = useEnvironmentTotalCalls as jest.Mock;

const STUB_STATS = { total: 5, private: 2, published: 3, isLoading: false };

const DEFAULT_PROPS = {
    apis: [],
    totalCount: 0,
    isLoading: false,
    isFetching: false,
    search: '',
    page: 1,
    perPage: 10,
    onSearchChange: jest.fn(),
    onPageChange: jest.fn(),
    onPerPageChange: jest.fn(),
    onCreateProxy: jest.fn(),
    canCreate: true,
};

function renderView(overrides: Partial<typeof DEFAULT_PROPS> = {}) {
    const props = { ...DEFAULT_PROPS, ...overrides };
    return render(
        <MemoryRouter>
            <ApisListView {...props} />
        </MemoryRouter>,
    );
}

describe('ApisListView', () => {
    beforeEach(() => {
        mockUseApiStats.mockReturnValue(STUB_STATS);
        mockUseEnvironmentTotalCalls.mockReturnValue({ total: null, isLoading: false });
    });

    afterEach(() => jest.clearAllMocks());

    it('renders the page heading', () => {
        renderView();
        expect(screen.queryByText('API Proxies')).not.toBeNull();
    });

    it('calls onSearchChange with the input value when the user types', () => {
        const onSearchChange = jest.fn();
        renderView({ onSearchChange });
        const input = screen.getByPlaceholderText('Search APIs...');
        fireEvent.change(input, { target: { value: 'my-service' } });
        expect(onSearchChange).toHaveBeenCalledWith('my-service');
    });

    it('shows the Create New Proxy button when canCreate is true', () => {
        renderView({ canCreate: true });
        expect(screen.queryByRole('button', { name: /Create New Proxy/i })).not.toBeNull();
    });

    it('hides the Create New Proxy button when canCreate is false', () => {
        renderView({ canCreate: false });
        expect(screen.queryByRole('button', { name: /Create New Proxy/i })).toBeNull();
    });

    it('calls onCreateProxy when the create button is clicked', () => {
        const onCreateProxy = jest.fn();
        renderView({ canCreate: true, onCreateProxy });
        fireEvent.click(screen.getByRole('button', { name: /Create New Proxy/i }));
        expect(onCreateProxy).toHaveBeenCalled();
    });
});
