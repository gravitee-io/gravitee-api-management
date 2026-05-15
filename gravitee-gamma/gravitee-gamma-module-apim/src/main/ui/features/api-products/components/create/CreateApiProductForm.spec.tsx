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
import { MemoryRouter } from 'react-router-dom';

import { CreateApiProductForm } from './CreateApiProductForm';
import { useCreateApiProduct } from '../../hooks/useCreateApiProduct';
import { useVerifyApiProductName } from '../../hooks/useVerifyApiProductName';

jest.mock('../../hooks/useCreateApiProduct', () => ({ useCreateApiProduct: jest.fn() }));
jest.mock('../../hooks/useVerifyApiProductName', () => ({ useVerifyApiProductName: jest.fn() }));

const mockUseCreateApiProduct = useCreateApiProduct as jest.Mock;
const mockUseVerifyApiProductName = useVerifyApiProductName as jest.Mock;
const mockCreate = jest.fn();

function renderForm(props: Partial<{ onBack: () => void; onCreated: (id: string) => void }> = {}) {
    return render(
        <MemoryRouter>
            <CreateApiProductForm onBack={jest.fn()} onCreated={jest.fn()} {...props} />
        </MemoryRouter>,
    );
}

// Submits the form via the form element directly — fireEvent.click on a submit
// button does not trigger the submit event in jsdom without a full browser event loop.
function submitForm(container: HTMLElement) {
    fireEvent.submit(container.querySelector('form')!);
}

describe('CreateApiProductForm', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        mockUseCreateApiProduct.mockReturnValue({ mutate: mockCreate, isPending: false, error: null });
        mockUseVerifyApiProductName.mockReturnValue({ data: undefined, isChecking: false });
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    it('submit button is disabled when the name field is empty (initial state)', () => {
        renderForm();
        // version pre-fills "1.0.0" but name is empty → canSubmit is false
        expect(screen.getByRole('button', { name: /Create API Product/i })).toBeDisabled();
    });

    it('submit button is disabled while the availability check is in flight', () => {
        mockUseVerifyApiProductName.mockReturnValue({ data: undefined, isChecking: true });
        renderForm();
        fireEvent.change(screen.getByPlaceholderText('My API Product'), { target: { value: 'My Product' } });
        act(() => {
            jest.advanceTimersByTime(400);
        });
        // !isChecking is false → canSubmit is false
        expect(screen.getByRole('button', { name: /Create API Product/i })).toBeDisabled();
    });

    it('submit button is disabled when the name is already taken', () => {
        mockUseVerifyApiProductName.mockReturnValue({ data: { ok: false, reason: 'Already taken.' }, isChecking: false });
        renderForm();
        fireEvent.change(screen.getByPlaceholderText('My API Product'), { target: { value: 'Taken' } });
        act(() => {
            jest.advanceTimersByTime(400);
        });
        // verifyResult.ok is false → canSubmit is false
        expect(screen.getByRole('button', { name: /Create API Product/i })).toBeDisabled();
    });

    it('shows the reason from the server as an error once the debounce settles — no blur required', () => {
        mockUseVerifyApiProductName.mockReturnValue({ data: { ok: false, reason: 'Name already in use.' }, isChecking: false });
        renderForm();
        fireEvent.change(screen.getByPlaceholderText('My API Product'), { target: { value: 'Taken' } });
        act(() => {
            jest.advanceTimersByTime(400);
        });
        expect(screen.getByText('Name already in use.')).toBeInTheDocument();
    });

    it('shows "Name is available." when verify returns ok after debounce has settled', () => {
        mockUseVerifyApiProductName.mockReturnValue({ data: { ok: true }, isChecking: false });
        renderForm();
        fireEvent.change(screen.getByPlaceholderText('My API Product'), { target: { value: 'Fresh Name' } });
        act(() => {
            jest.advanceTimersByTime(400);
        });
        // verifyResult.ok && debouncedName → success hint appears
        expect(screen.getByText('Name is available.')).toBeInTheDocument();
    });

    it('calls createProduct with trimmed name, version, and description on submit', () => {
        mockUseVerifyApiProductName.mockReturnValue({ data: { ok: true }, isChecking: false });
        const { container } = renderForm();
        fireEvent.change(screen.getByPlaceholderText('My API Product'), { target: { value: '  My Product  ' } });
        fireEvent.change(screen.getByPlaceholderText('1.0.0'), { target: { value: '  2.0.0  ' } });
        fireEvent.change(screen.getByPlaceholderText(/Describe what this product offers/i), { target: { value: '  A desc  ' } });
        act(() => {
            jest.advanceTimersByTime(400);
        });
        submitForm(container);
        expect(mockCreate).toHaveBeenCalledWith(
            { name: 'My Product', version: '2.0.0', description: 'A desc' },
            expect.objectContaining({ onSuccess: expect.any(Function) }),
        );
    });

    it('passes description as undefined when the description field is blank', () => {
        mockUseVerifyApiProductName.mockReturnValue({ data: { ok: true }, isChecking: false });
        const { container } = renderForm();
        fireEvent.change(screen.getByPlaceholderText('My API Product'), { target: { value: 'Product' } });
        act(() => {
            jest.advanceTimersByTime(400);
        });
        submitForm(container);
        expect(mockCreate).toHaveBeenCalledWith(expect.objectContaining({ description: undefined }), expect.any(Object));
    });

    it('calls onCreated with the new product id on create success', () => {
        mockUseVerifyApiProductName.mockReturnValue({ data: { ok: true }, isChecking: false });
        const onCreated = jest.fn();
        mockCreate.mockImplementation((_req: unknown, opts: { onSuccess: (p: { id: string }) => void }) => {
            opts.onSuccess({ id: 'new-prod-id' });
        });
        const { container } = renderForm({ onCreated });
        fireEvent.change(screen.getByPlaceholderText('My API Product'), { target: { value: 'Product' } });
        act(() => {
            jest.advanceTimersByTime(400);
        });
        submitForm(container);
        expect(onCreated).toHaveBeenCalledWith('new-prod-id');
    });

    it('shows the server error message when creation fails', () => {
        mockUseCreateApiProduct.mockReturnValue({ mutate: jest.fn(), isPending: false, error: { message: 'Server error.' } });
        renderForm();
        expect(screen.getByText('Server error.')).toBeInTheDocument();
    });
});
