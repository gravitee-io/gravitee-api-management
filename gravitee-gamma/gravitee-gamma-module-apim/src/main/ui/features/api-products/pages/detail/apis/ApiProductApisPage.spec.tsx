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
import type { ReactNode } from 'react';
import { MemoryRouter, useParams } from 'react-router-dom';

// Render DropdownMenu inline (no portal) with open/close state so only the
// triggered row's items are visible at a time.
// All React APIs are accessed via require() to satisfy jest.mock hoisting rules.
jest.mock('@gravitee/graphene-core', () => {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
    const { useState, createContext, useContext } = jest.requireActual('react');
    const MenuCtx = createContext<{ open: boolean; setOpen: (v: boolean) => void }>({ open: false, setOpen: () => {} });
    return {
        ...jest.requireActual<object>('@gravitee/graphene-core'),
        DropdownMenu: ({ children }: { children?: ReactNode }) => {
            const [open, setOpen] = useState(false);
            return <MenuCtx.Provider value={{ open, setOpen }}>{children}</MenuCtx.Provider>;
        },
        DropdownMenuTrigger: ({ children, asChild: _ }: { children?: ReactNode; asChild?: boolean }) => {
            const { setOpen } = useContext(MenuCtx);
            return (
                <span role="button" tabIndex={0} onKeyDown={e => e.key === 'Enter' && setOpen(true)} onClick={() => setOpen(true)}>
                    {children}
                </span>
            );
        },
        DropdownMenuContent: ({ children }: { children?: ReactNode }) => {
            const { open } = useContext(MenuCtx);
            return open ? <div>{children}</div> : null;
        },
        DropdownMenuItem: ({ children, onSelect }: { children?: ReactNode; onSelect?: () => void }) => (
            <button role="menuitem" onClick={onSelect}>
                {children}
            </button>
        ),
    };
});

import { ApiProductApisPage } from './ApiProductApisPage';
import { useApiProductDetailContext } from '../../../context/ApiProductDetailContext';
import { useApiProductApis } from '../../../hooks/useApiProductApis';
import { useUpdateApiProduct } from '../../../hooks/useUpdateApiProduct';
import type { ApiProductListItem } from '../../../types/apiProduct';

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useParams: jest.fn(),
}));
jest.mock('../../../context/ApiProductDetailContext', () => ({
    useApiProductDetailContext: jest.fn(),
}));
jest.mock('../../../hooks/useApiProductApis', () => ({
    useApiProductApis: jest.fn(),
    useApisAvailableForProduct: jest.fn(() => ({ data: undefined, isLoading: false })),
}));
jest.mock('../../../hooks/useUpdateApiProduct', () => ({
    useUpdateApiProduct: jest.fn(),
}));
// Simplified stand-in — calls onAdd with a preset list so we can test the page's merge logic
// without exercising the dialog's own internals.
jest.mock('../../../components/apis/AddApiToProduct', () => ({
    AddApiToProduct: ({
        onAdd,
        open,
    }: {
        onAdd: (ids: string[]) => void;
        open: boolean;
        existingApiIds: string[];
        onClose: () => void;
        isAdding: boolean;
    }) =>
        open ? (
            <button data-testid="mock-dialog-add" onClick={() => onAdd(['api-2', 'api-3'])}>
                Add Selected
            </button>
        ) : null,
}));

const mockUseApiProductApis = useApiProductApis as jest.Mock;
const mockUpdateProduct = jest.fn();

const mockProduct: ApiProductListItem = {
    id: 'prod-1',
    name: 'Prod1',
    version: '1.0.0',
    apiIds: ['api-1', 'api-2'],
};

const TWO_APIS = {
    data: [
        { id: 'api-1', name: 'API One', apiVersion: '1.0', type: 'PROXY' as const, definitionVersion: 'V4' as const },
        { id: 'api-2', name: 'API Two', apiVersion: '2.0', type: 'PROXY' as const, definitionVersion: 'V4' as const },
    ],
    pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: 2 },
};

function renderPage() {
    return render(
        <MemoryRouter>
            <ApiProductApisPage />
        </MemoryRouter>,
    );
}

describe('ApiProductApisPage', () => {
    beforeEach(() => {
        (useParams as jest.Mock).mockReturnValue({ productId: 'prod-1' });
        (useApiProductDetailContext as jest.Mock).mockReturnValue({ product: mockProduct, isLoading: false });
        mockUseApiProductApis.mockReturnValue({ data: TWO_APIS, isLoading: false, isError: false });
        (useUpdateApiProduct as jest.Mock).mockReturnValue({ mutate: mockUpdateProduct, isPending: false, error: null });
    });

    afterEach(() => jest.clearAllMocks());

    // ── Adding APIs ────────────────────────────────────────────────────────────

    describe('adding APIs', () => {
        it('merges new IDs with existing ones and deduplicates before calling updateProduct', () => {
            // product.apiIds = ['api-1', 'api-2']; mock dialog sends ['api-2', 'api-3']
            // merged Set: ['api-1', 'api-2', 'api-3']  — 'api-2' must not appear twice
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Add API/i }));
            fireEvent.click(screen.getByTestId('mock-dialog-add'));
            expect(mockUpdateProduct).toHaveBeenCalledWith(
                expect.objectContaining({ apiIds: ['api-1', 'api-2', 'api-3'] }),
                expect.objectContaining({ onSuccess: expect.any(Function) }),
            );
        });

        it('closes the dialog on successful add', () => {
            mockUpdateProduct.mockImplementation((_req: unknown, opts: { onSuccess: () => void }) => opts.onSuccess());
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Add API/i }));
            expect(screen.getByTestId('mock-dialog-add')).toBeInTheDocument();
            fireEvent.click(screen.getByTestId('mock-dialog-add'));
            // onSuccess fires → setDialogOpen(false) → mock dialog is unmounted
            expect(screen.queryByTestId('mock-dialog-add')).toBeNull();
        });
    });

    // ── Removing a single API ──────────────────────────────────────────────────

    describe('removing a single API', () => {
        it('opens a confirmation dialog when the remove button is clicked', async () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Actions for API One' }));
            fireEvent.click(await screen.findByRole('menuitem', { name: 'Remove from product' }));
            expect(await screen.findByText('Remove API')).toBeInTheDocument();
        });

        it('calls updateProduct with only the target API filtered out of apiIds', async () => {
            renderPage();
            // Remove api-1; product.apiIds was ['api-1', 'api-2'] → should become ['api-2']
            fireEvent.click(screen.getByRole('button', { name: 'Actions for API One' }));
            fireEvent.click(await screen.findByRole('menuitem', { name: 'Remove from product' }));
            fireEvent.click(await screen.findByRole('button', { name: 'Remove' }));
            expect(mockUpdateProduct).toHaveBeenCalledWith(
                expect.objectContaining({ apiIds: ['api-2'] }),
                expect.objectContaining({ onSuccess: expect.any(Function) }),
            );
        });

        it('does not call updateProduct when the remove dialog is cancelled', async () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Actions for API One' }));
            fireEvent.click(await screen.findByRole('menuitem', { name: 'Remove from product' }));
            fireEvent.click(await screen.findByRole('button', { name: 'Cancel' }));
            expect(mockUpdateProduct).not.toHaveBeenCalled();
        });
    });

    // ── Empty and error states ─────────────────────────────────────────────────

    describe('empty and error states', () => {
        it('shows the empty landing when there are no APIs and no active search', () => {
            mockUseApiProductApis.mockReturnValue({
                data: { data: [], pagination: { page: 1, perPage: 10, pageCount: 0, totalCount: 0 } },
                isLoading: false,
                isError: false,
            });
            renderPage();
            expect(screen.queryByText('No APIs in this product yet')).not.toBeNull();
        });

        it('does not flash the empty landing while the initial fetch is in progress', () => {
            mockUseApiProductApis.mockReturnValue({ data: undefined, isLoading: true, isError: false });
            renderPage();
            expect(screen.queryByText('No APIs in this product yet')).toBeNull();
        });

        it('shows an error message when the API fetch fails', () => {
            mockUseApiProductApis.mockReturnValue({ data: undefined, isLoading: false, isError: true });
            renderPage();
            expect(screen.queryByText(/Failed to load APIs for this product/i)).not.toBeNull();
        });

        it('resets page to 1 when the search term changes', async () => {
            renderPage();
            const input = screen.getByPlaceholderText('Search by name');
            fireEvent.change(input, { target: { value: 'test' } });
            await waitFor(() => {
                const calls = mockUseApiProductApis.mock.calls;
                const searchCall = calls.find(([, , , q]) => q === 'test');
                expect(searchCall).not.toBeUndefined();
                expect(searchCall![1]).toBe(1); // page arg is always 1 after reset
            });
        });
    });
});
