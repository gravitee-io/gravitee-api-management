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
import { MemoryRouter, useNavigate, useParams } from 'react-router-dom';

import { ApiProductGeneralPage } from './ApiProductGeneralPage';
import { useApiProductDetailContext } from '../features/api-products/context/ApiProductDetailContext';
import { useDeleteApiProduct } from '../features/api-products/hooks/useDeleteApiProduct';
import { useUpdateApiProduct } from '../features/api-products/hooks/useUpdateApiProduct';
import type { ApiProductListItem } from '../features/api-products/types/apiProduct';

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useParams: jest.fn(),
    useNavigate: jest.fn(),
}));
jest.mock('../features/api-products/context/ApiProductDetailContext', () => ({
    useApiProductDetailContext: jest.fn(),
}));
jest.mock('../features/api-products/hooks/useUpdateApiProduct', () => ({
    useUpdateApiProduct: jest.fn(),
}));
jest.mock('../features/api-products/hooks/useDeleteApiProduct', () => ({
    useDeleteApiProduct: jest.fn(),
}));

const mockNavigate = jest.fn();
const mockUpdateProduct = jest.fn();
const mockDeleteProduct = jest.fn();

const mockProduct: ApiProductListItem = {
    id: 'prod-1',
    name: 'Prod1',
    version: '1.0.0',
    description: 'My product description',
    apiIds: ['api-1', 'api-2'],
    deploymentState: 'NEED_REDEPLOY',
};

function renderPage() {
    return render(
        <MemoryRouter>
            <ApiProductGeneralPage />
        </MemoryRouter>,
    );
}

describe('ApiProductGeneralPage', () => {
    beforeEach(() => {
        (useParams as jest.Mock).mockReturnValue({ productId: 'prod-1' });
        (useNavigate as jest.Mock).mockReturnValue(mockNavigate);
        (useApiProductDetailContext as jest.Mock).mockReturnValue({ product: mockProduct, isLoading: false });
        (useUpdateApiProduct as jest.Mock).mockReturnValue({ mutate: mockUpdateProduct, isPending: false, error: null });
        (useDeleteApiProduct as jest.Mock).mockReturnValue({ mutate: mockDeleteProduct, isPending: false });
    });

    afterEach(() => jest.clearAllMocks());

    // ── Form save ──────────────────────────────────────────────────────────────

    describe('form save', () => {
        it('Save button is disabled when the form has not changed from the loaded product', () => {
            renderPage();
            expect(screen.getByRole('button', { name: /Save changes/i })).toBeDisabled();
        });

        it('Save button is enabled once any field differs from the product', () => {
            renderPage();
            fireEvent.change(screen.getByDisplayValue('Prod1'), { target: { value: 'Updated Name' } });
            expect(screen.getByRole('button', { name: /Save changes/i })).not.toBeDisabled();
        });

        it('Save button is disabled when the name is cleared even though the form is dirty', () => {
            renderPage();
            // isDirty becomes true (name ≠ product.name) but name.trim() is ''
            fireEvent.change(screen.getByDisplayValue('Prod1'), { target: { value: '' } });
            expect(screen.getByRole('button', { name: /Save changes/i })).toBeDisabled();
        });

        it('calls updateProduct with trimmed values and preserves existing apiIds', () => {
            renderPage();
            fireEvent.change(screen.getByDisplayValue('Prod1'), { target: { value: '  New Name  ' } });
            fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
            expect(mockUpdateProduct).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: 'New Name',
                    version: '1.0.0',
                    // apiIds must be passed through unchanged — a save must not wipe the API list
                    apiIds: ['api-1', 'api-2'],
                }),
            );
        });

        it('sends description as undefined when the description field is cleared', () => {
            renderPage();
            fireEvent.change(screen.getByDisplayValue('My product description'), { target: { value: '' } });
            fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
            expect(mockUpdateProduct).toHaveBeenCalledWith(expect.objectContaining({ description: undefined }));
        });
    });

    // ── Remove all APIs ────────────────────────────────────────────────────────

    describe('remove all APIs', () => {
        it('"Remove all APIs" button is disabled when the product has no APIs', () => {
            (useApiProductDetailContext as jest.Mock).mockReturnValue({
                product: { ...mockProduct, apiIds: [] },
                isLoading: false,
            });
            renderPage();
            expect(screen.getByRole('button', { name: /Remove all APIs/i })).toBeDisabled();
        });

        it('calls updateProduct with apiIds: [] after confirming remove all', async () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Remove all APIs/i }));
            const confirmBtn = await screen.findByRole('button', { name: 'Remove all' });
            fireEvent.click(confirmBtn);
            expect(mockUpdateProduct).toHaveBeenCalledWith(expect.objectContaining({ apiIds: [] }));
        });

        it('does not call updateProduct when the remove-all dialog is cancelled', async () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Remove all APIs/i }));
            const cancelBtn = await screen.findByRole('button', { name: 'Cancel' });
            fireEvent.click(cancelBtn);
            expect(mockUpdateProduct).not.toHaveBeenCalled();
        });
    });

    // ── Delete product ─────────────────────────────────────────────────────────

    describe('delete product', () => {
        it('"Delete permanently" button is disabled when the confirm field is empty', async () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Delete API product/i }));
            const deleteBtn = await screen.findByRole('button', { name: /Delete permanently/i });
            expect(deleteBtn).toBeDisabled();
        });

        it('"Delete permanently" button is enabled only when confirm text exactly matches the product name', async () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Delete API product/i }));
            const confirmInput = await screen.findByPlaceholderText('Prod1');
            // Partial match — still disabled
            fireEvent.change(confirmInput, { target: { value: 'Prod' } });
            expect(screen.getByRole('button', { name: /Delete permanently/i })).toBeDisabled();
            // Exact match — enabled
            fireEvent.change(confirmInput, { target: { value: 'Prod1' } });
            expect(screen.getByRole('button', { name: /Delete permanently/i })).not.toBeDisabled();
        });

        it('calls deleteProduct with the productId on confirmation', async () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Delete API product/i }));
            fireEvent.change(await screen.findByPlaceholderText('Prod1'), { target: { value: 'Prod1' } });
            fireEvent.click(screen.getByRole('button', { name: /Delete permanently/i }));
            expect(mockDeleteProduct).toHaveBeenCalledWith('prod-1', expect.objectContaining({ onSuccess: expect.any(Function) }));
        });

        it('navigates to "../.." when deleteProduct succeeds', async () => {
            mockDeleteProduct.mockImplementation((_id: string, opts: { onSuccess: () => void }) => opts.onSuccess());
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Delete API product/i }));
            fireEvent.change(await screen.findByPlaceholderText('Prod1'), { target: { value: 'Prod1' } });
            fireEvent.click(screen.getByRole('button', { name: /Delete permanently/i }));
            expect(mockNavigate).toHaveBeenCalledWith('../..');
        });

        it('resets the confirm input when the delete dialog is closed and reopened', async () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Delete API product/i }));
            fireEvent.change(await screen.findByPlaceholderText('Prod1'), { target: { value: 'Prod1' } });
            // Cancel resets deleteConfirm state
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
            // Re-open — the input should be empty again
            fireEvent.click(screen.getByRole('button', { name: /Delete API product/i }));
            await waitFor(() => expect(screen.getByPlaceholderText('Prod1')).toHaveValue(''));
        });
    });
});
