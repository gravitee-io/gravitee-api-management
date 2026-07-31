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
import { fireEvent, render, screen, act } from '@testing-library/react';
import type { ReactNode } from 'react';

jest.mock('@gravitee/graphene-core', () => ({
    Badge: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
    Button: ({ children, onClick }: { children?: ReactNode; onClick?: () => void }) => (
        <button type="button" onClick={onClick}>
            {children}
        </button>
    ),
    InputGroup: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    InputGroupAddon: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    InputGroupInput: ({
        value,
        onChange,
        placeholder,
    }: {
        value?: string;
        onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
        placeholder?: string;
    }) => <input value={value} onChange={onChange} placeholder={placeholder} />,
    Skeleton: () => <div data-testid="skeleton" />,
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../hooks/useSubscriptions', () => ({
    useApplicationSearch: jest.fn(() => ({ data: [], isLoading: false })),
}));

import { ApplicationSearchList } from './ApplicationSearchList';
import { useApplicationSearch } from '../../../hooks/useSubscriptions';

const mockUseApplicationSearch = useApplicationSearch as jest.Mock;

function renderList(selected = null as Parameters<typeof ApplicationSearchList>[0]['selected'], onSelect = jest.fn()) {
    render(<ApplicationSearchList selected={selected} onSelect={onSelect} />);
    return { onSelect };
}

function typeQuery(value: string) {
    const input = screen.getByPlaceholderText(/type to search/i);
    fireEvent.change(input, { target: { value } });
    act(() => {
        jest.advanceTimersByTime(300);
    });
}

describe('ApplicationSearchList', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        mockUseApplicationSearch.mockReturnValue({ data: [], isLoading: false });
    });

    afterEach(() => {
        jest.runOnlyPendingTimers();
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    it('shows the prompt to type before any query is entered', () => {
        renderList();
        expect(screen.getByText(/type a name to find applications/i)).toBeInTheDocument();
    });

    it('shows loading skeletons while results are pending', () => {
        mockUseApplicationSearch.mockReturnValue({ data: [], isLoading: true });
        renderList();
        typeQuery('pay');
        expect(screen.getAllByTestId('skeleton').length).toBeGreaterThan(0);
    });

    it('shows empty state when no applications match the query', () => {
        renderList();
        typeQuery('zzz-no-match');
        expect(screen.getByText(/no applications found/i)).toBeInTheDocument();
    });

    it('renders application names from search results', () => {
        mockUseApplicationSearch.mockReturnValue({
            data: [{ id: 'app-1', name: 'Payment Service', primaryOwner: { displayName: 'Alice' } }],
            isLoading: false,
        });
        renderList();
        typeQuery('payment');
        expect(screen.getByText('Payment Service')).toBeInTheDocument();
    });

    it('displays the owner name when primaryOwner.displayName is present', () => {
        mockUseApplicationSearch.mockReturnValue({
            data: [{ id: 'app-1', name: 'Payment Service', primaryOwner: { displayName: 'Alice Smith' } }],
            isLoading: false,
        });
        renderList();
        typeQuery('payment');
        expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });

    it('does not render an owner line when primaryOwner is absent', () => {
        mockUseApplicationSearch.mockReturnValue({
            data: [{ id: 'app-1', name: 'Orphan App' }],
            isLoading: false,
        });
        renderList();
        typeQuery('orphan');
        expect(screen.getByText('Orphan App')).toBeInTheDocument();
        expect(screen.queryByText(/owner/i)).not.toBeInTheDocument();
    });

    it('calls onSelect with the chosen application when clicked', () => {
        const app = { id: 'app-1', name: 'Booking App', primaryOwner: { displayName: 'Bob' } };
        mockUseApplicationSearch.mockReturnValue({ data: [app], isLoading: false });
        const { onSelect } = renderList();
        typeQuery('book');
        fireEvent.click(screen.getByText('Booking App'));
        expect(onSelect).toHaveBeenCalledWith(app);
    });

    it('passes the debounced query string to useApplicationSearch', () => {
        renderList();
        typeQuery('flight');
        expect(mockUseApplicationSearch).toHaveBeenCalledWith('flight');
    });
});

describe('ApplicationSearchList — selected chip', () => {
    const selectedApp = { id: 'app-1', name: 'Payment Service', primaryOwner: { displayName: 'Alice Smith' }, type: 'SIMPLE' };

    it('shows the selected app name instead of the search input when an app is selected', () => {
        renderList(selectedApp);
        expect(screen.getByText('Payment Service')).toBeInTheDocument();
        expect(screen.queryByPlaceholderText(/type to search/i)).not.toBeInTheDocument();
    });

    it('shows the owner name in the chip', () => {
        renderList(selectedApp);
        expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });

    it('calls onSelect(null) when the X button is clicked', () => {
        const { onSelect } = renderList(selectedApp);
        fireEvent.click(screen.getByRole('button', { name: /remove selection/i }));
        expect(onSelect).toHaveBeenCalledWith(null);
    });

    it('hides the chip and shows the search input after deselection', () => {
        let selected: typeof selectedApp | null = selectedApp;
        const onSelect = jest.fn((app: typeof selectedApp | null) => {
            selected = app;
        });
        const { rerender } = render(<ApplicationSearchList selected={selected} onSelect={onSelect} />);
        fireEvent.click(screen.getByRole('button', { name: /remove selection/i }));
        rerender(<ApplicationSearchList selected={null} onSelect={onSelect} />);
        expect(screen.getByPlaceholderText(/type to search/i)).toBeInTheDocument();
        expect(screen.queryByText('Payment Service')).not.toBeInTheDocument();
    });
});
