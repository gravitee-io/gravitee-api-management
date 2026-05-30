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
import { act, fireEvent, render, screen } from '@testing-library/react';

import { AddApiToProduct } from './AddApiToProduct';
import { useApisAvailableForProduct } from '../../hooks/useApiProductApis';

jest.mock('../../hooks/useApiProductApis', () => ({
    useApisAvailableForProduct: jest.fn(),
}));

const mockUseApisAvailableForProduct = useApisAvailableForProduct as jest.Mock;

const AVAILABLE_APIS = [
    { id: 'api-1', name: 'API One', apiVersion: '1.0', type: 'PROXY' as const, definitionVersion: 'V4' as const },
    { id: 'api-2', name: 'API Two', apiVersion: '2.0', type: 'PROXY' as const, definitionVersion: 'V4' as const },
    { id: 'api-3', name: 'API Three', apiVersion: '3.0', type: 'PROXY' as const, definitionVersion: 'V4' as const },
];

const MOCK_API_RESPONSE = {
    data: AVAILABLE_APIS,
    pagination: { page: 1, perPage: 50, pageCount: 1, totalCount: 3 },
};

interface RenderProps {
    open?: boolean;
    existingApiIds?: string[];
    onClose?: () => void;
    onAdd?: jest.Mock;
    isAdding?: boolean;
}

function renderDialog({ open = true, existingApiIds = [], onClose = jest.fn(), onAdd = jest.fn(), isAdding = false }: RenderProps = {}) {
    return render(<AddApiToProduct open={open} existingApiIds={existingApiIds} onClose={onClose} onAdd={onAdd} isAdding={isAdding} />);
}

// Types in the search box and advances past the 300 ms debounce.
function typeSearch(text: string) {
    fireEvent.change(screen.getByPlaceholderText('Filter by name or context path...'), { target: { value: text } });
    act(() => {
        jest.advanceTimersByTime(300);
    });
}

describe('AddApiToProduct', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        // Default: returns results so tests that need a list don't have to re-mock
        mockUseApisAvailableForProduct.mockReturnValue({ data: MOCK_API_RESPONSE, isLoading: false });
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    it('shows the empty-search placeholder before any search term is entered', () => {
        renderDialog();
        // hasSearch = false → no list rendered, only the prompt
        expect(screen.queryByText('Type a name or path to find APIs')).not.toBeNull();
    });

    it('filters out APIs that are already in the product (existingApiIds)', () => {
        renderDialog({ existingApiIds: ['api-1'] });
        typeSearch('api');
        // api-1 must be excluded; api-2 and api-3 must appear
        expect(screen.queryByRole('button', { name: /API One/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /API Two/i })).not.toBeNull();
        expect(screen.queryByRole('button', { name: /API Three/i })).not.toBeNull();
    });

    it('toggles selection on click — second click deselects and resets the count', () => {
        renderDialog();
        typeSearch('api');
        const apiTwoBtn = screen.getByRole('button', { name: /API Two/i });
        // Select
        fireEvent.click(apiTwoBtn);
        expect(screen.queryByRole('button', { name: /Add \(1\)/i })).not.toBeNull();
        // Deselect
        fireEvent.click(apiTwoBtn);
        expect(screen.queryByRole('button', { name: /Add \(1\)/i })).toBeNull();
        // Button reverts to plain "Add"
        expect(screen.queryByRole('button', { name: /^Add$/i })).not.toBeNull();
    });

    it('calls onAdd with the selected API ids when the Add button is clicked', () => {
        const onAdd = jest.fn();
        renderDialog({ onAdd });
        typeSearch('api');
        fireEvent.click(screen.getByRole('button', { name: /API Two/i }));
        fireEvent.click(screen.getByRole('button', { name: /API Three/i }));
        fireEvent.click(screen.getByRole('button', { name: /Add \(2\)/i }));
        expect(onAdd).toHaveBeenCalledWith(expect.arrayContaining(['api-2', 'api-3']));
        expect(onAdd.mock.calls[0][0]).toHaveLength(2);
    });

    it('resets search and selections when the dialog is closed and reopened', () => {
        const { rerender } = renderDialog();
        typeSearch('api');
        fireEvent.click(screen.getByRole('button', { name: /API Two/i }));
        expect(screen.queryByRole('button', { name: /Add \(1\)/i })).not.toBeNull();

        // Close → useEffect clears state
        rerender(<AddApiToProduct open={false} existingApiIds={[]} onClose={jest.fn()} onAdd={jest.fn()} isAdding={false} />);
        // Reopen
        rerender(<AddApiToProduct open={true} existingApiIds={[]} onClose={jest.fn()} onAdd={jest.fn()} isAdding={false} />);
        // Selection is gone — Add shows no count
        expect(screen.queryByRole('button', { name: /Add \(1\)/i })).toBeNull();
        // Search is reset — the placeholder prompt is visible again
        expect(screen.queryByText('Type a name or path to find APIs')).not.toBeNull();
    });
});
