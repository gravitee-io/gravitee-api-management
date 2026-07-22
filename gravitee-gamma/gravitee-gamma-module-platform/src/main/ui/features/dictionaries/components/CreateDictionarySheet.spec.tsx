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

import { CreateDictionarySheet } from './CreateDictionarySheet';
import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';

function renderSheet({
    open = true,
    isSaving = false,
    onSubmit = jest.fn().mockResolvedValue(undefined),
}: {
    open?: boolean;
    isSaving?: boolean;
    onSubmit?: jest.Mock;
} = {}) {
    const onClose = jest.fn();
    render(<CreateDictionarySheet open={open} onClose={onClose} onSubmit={onSubmit} isSaving={isSaving} />);
    return { onClose, onSubmit };
}

describe('CreateDictionarySheet', () => {
    it('does not show sheet content when closed', () => {
        renderSheet({ open: false });
        expect(querySheetHeading('Create Dictionary')).toBeNull();
    });

    it('shows create title and no Key field', () => {
        renderSheet();
        expect(screen.getByRole('heading', { name: 'Create Dictionary' })).not.toBeNull();
        expect(screen.queryByText('Define a new manual dictionary with a name and description.')).not.toBeNull();
        expect(screen.queryByLabelText(/^Key/)).toBeNull();
        expect(screen.getByLabelText(/^Type/)).not.toBeNull();
    });

    it('keeps Create disabled until name is valid', () => {
        renderSheet();
        const createBtn = screen.getByRole('button', { name: 'Create' }) as HTMLButtonElement;
        expect(createBtn.disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'ab' } });
        expect(createBtn.disabled).toBe(true);
        expect(screen.queryByText('Name must be at least 3 characters')).not.toBeNull();

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Airport IATA Codes' } });
        expect((screen.getByRole('button', { name: 'Create' }) as HTMLButtonElement).disabled).toBe(false);
    });

    it('submits a MANUAL payload without key or properties', async () => {
        const { onSubmit } = renderSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Airport IATA Codes' } });
        fireEvent.change(screen.getByLabelText(/^Description/), {
            target: { value: 'IATA codes for airports' },
        });
        fireEvent.click(screen.getByRole('button', { name: 'Create' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith({
                name: 'Airport IATA Codes',
                description: 'IATA codes for airports',
                type: 'MANUAL',
            });
        });
    });

    it('shows inline API error when create fails', async () => {
        const onSubmit = jest
            .fn()
            .mockRejectedValue(new Error('A dictionary with name [Airport IATA Codes] already exists in this environment.'));
        renderSheet({ onSubmit });
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Airport IATA Codes' } });
        fireEvent.click(screen.getByRole('button', { name: 'Create' }));

        await waitFor(() => {
            expect(screen.queryByText('A dictionary with name [Airport IATA Codes] already exists in this environment.')).not.toBeNull();
        });
    });

    it('shows Creating… while saving', () => {
        renderSheet({ isSaving: true });
        expect(screen.queryByRole('button', { name: 'Creating…' })).not.toBeNull();
    });
});
