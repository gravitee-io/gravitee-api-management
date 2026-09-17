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
import { cleanup, fireEvent, render, screen } from '@testing-library/react';

import { UserFieldSheet } from './UserFieldSheet';
import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';
import type { UserField } from '../types/userField';

// Radix Switch measures its thumb via ResizeObserver, which jsdom does not implement.
beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

const EXISTING_FIELD: UserField = { key: 'department', label: 'Department', required: true, values: ['Engineering', 'Product'] };

function renderSheet({
    open = true,
    mode,
    field,
    isSaving = false,
}: {
    open?: boolean;
    mode: 'create' | 'edit';
    field?: UserField;
    isSaving?: boolean;
}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn();
    render(<UserFieldSheet open={open} mode={mode} field={field} onClose={onClose} onSubmit={onSubmit} isSaving={isSaving} />);
    return { onClose, onSubmit };
}

function keyInput(): HTMLInputElement {
    return screen.getByLabelText(/^Key/) as HTMLInputElement;
}

function labelInput(): HTMLInputElement {
    return screen.getByLabelText(/^Label/) as HTMLInputElement;
}

function valuesInput(): HTMLInputElement {
    return screen.getByPlaceholderText('Type value and confirm with enter.') as HTMLInputElement;
}

function addValue(value: string) {
    fireEvent.change(valuesInput(), { target: { value } });
    fireEvent.keyDown(valuesInput(), { key: 'Enter' });
}

describe('UserFieldSheet', () => {
    describe('visibility', () => {
        it('renders nothing when closed', () => {
            renderSheet({ open: false, mode: 'create' });
            expect(querySheetHeading('Create user field')).toBeNull();
        });

        it('titles the sheet per mode', () => {
            renderSheet({ mode: 'create' });
            expect(screen.getByRole('heading', { name: 'Create user field' })).not.toBeNull();
        });

        it('titles the edit sheet as an update', () => {
            renderSheet({ mode: 'edit', field: EXISTING_FIELD });
            expect(screen.getByRole('heading', { name: 'Update user field' })).not.toBeNull();
        });
    });

    describe('create mode', () => {
        it('starts empty with the key suggestion list attached', () => {
            renderSheet({ mode: 'create' });
            expect(keyInput().value).toBe('');
            expect(keyInput().disabled).toBe(false);
            const listId = keyInput().getAttribute('list');
            expect(listId).not.toBeNull();
            const options = Array.from(document.getElementById(listId!)!.querySelectorAll('option')).map(option => option.value);
            expect(options).toEqual(['address', 'city', 'country', 'job_position', 'organization', 'telephone_number', 'zip_code']);
        });

        it('shows the Classic validation messages after a submit attempt and does not submit', () => {
            const { onSubmit } = renderSheet({ mode: 'create' });
            fireEvent.click(screen.getByRole('button', { name: 'Create field' }));

            expect(screen.getByText('Key is required.')).not.toBeNull();
            expect(screen.getByText('Label is required.')).not.toBeNull();
            expect(onSubmit).not.toHaveBeenCalled();
        });

        it('rejects a key with characters outside a-zA-Z0-9_-', () => {
            const { onSubmit } = renderSheet({ mode: 'create' });
            fireEvent.change(keyInput(), { target: { value: 'bad key!' } });
            fireEvent.change(labelInput(), { target: { value: 'Bad' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create field' }));

            expect(screen.getByText('Only a-zA-Z0-9_- characters allowed')).not.toBeNull();
            expect(onSubmit).not.toHaveBeenCalled();
        });

        it('caps key and label at 50 characters', () => {
            renderSheet({ mode: 'create' });
            expect(keyInput().maxLength).toBe(50);
            expect(labelInput().maxLength).toBe(50);
            expect(screen.getAllByText('0/50').length).toBe(2);
        });

        it('submits a lowercased key, trimmed label, required flag and de-duplicated values', () => {
            const { onSubmit } = renderSheet({ mode: 'create' });
            fireEvent.change(keyInput(), { target: { value: 'Job_Position' } });
            fireEvent.change(labelInput(), { target: { value: '  Job position  ' } });
            fireEvent.click(screen.getByRole('switch', { name: 'Required' }));
            addValue('Engineer');
            addValue('Manager');
            addValue('Engineer');
            fireEvent.click(screen.getByRole('button', { name: 'Create field' }));

            expect(onSubmit).toHaveBeenCalledWith({
                key: 'job_position',
                label: 'Job position',
                required: true,
                values: ['Engineer', 'Manager'],
            });
        });

        it('shows the saving label and disables the actions while saving', () => {
            renderSheet({ mode: 'create', isSaving: true });
            expect((screen.getByRole('button', { name: 'Saving…' }) as HTMLButtonElement).disabled).toBe(true);
            expect((screen.getByRole('button', { name: 'Cancel' }) as HTMLButtonElement).disabled).toBe(true);
        });

        it('calls onClose when Cancel is clicked', () => {
            const { onClose } = renderSheet({ mode: 'create' });
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('closes on Escape when idle but not while a save is in flight', () => {
            const idle = renderSheet({ mode: 'create' });
            fireEvent.keyDown(labelInput(), { key: 'Escape' });
            expect(idle.onClose).toHaveBeenCalledTimes(1);

            cleanup();

            const saving = renderSheet({ mode: 'create', isSaving: true });
            fireEvent.keyDown(labelInput(), { key: 'Escape' });
            expect(saving.onClose).not.toHaveBeenCalled();
        });
    });

    describe('edit mode', () => {
        it('pre-fills the form and locks the key', () => {
            renderSheet({ mode: 'edit', field: EXISTING_FIELD });
            expect(keyInput().value).toBe('department');
            expect(keyInput().disabled).toBe(true);
            expect(labelInput().value).toBe('Department');
            expect(screen.getByRole('switch', { name: 'Required' }).getAttribute('aria-checked')).toBe('true');
            expect(screen.getByText('Engineering')).not.toBeNull();
            expect(screen.getByText('Product')).not.toBeNull();
        });

        it('keeps Save disabled until something changes', () => {
            renderSheet({ mode: 'edit', field: EXISTING_FIELD });
            const save = screen.getByRole('button', { name: 'Save changes' }) as HTMLButtonElement;
            expect(save.disabled).toBe(true);

            fireEvent.change(labelInput(), { target: { value: 'Team' } });
            expect(save.disabled).toBe(false);
        });

        it('submits the stored key unchanged with the edited label and values', () => {
            const { onSubmit } = renderSheet({ mode: 'edit', field: EXISTING_FIELD });
            fireEvent.change(labelInput(), { target: { value: 'Team' } });
            fireEvent.click(screen.getByRole('button', { name: 'Remove Product' }));
            fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

            expect(onSubmit).toHaveBeenCalledWith({ key: 'department', label: 'Team', required: true, values: ['Engineering'] });
        });

        it('still requires a label', () => {
            const { onSubmit } = renderSheet({ mode: 'edit', field: EXISTING_FIELD });
            fireEvent.change(labelInput(), { target: { value: '   ' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

            expect(screen.getByText('Label is required.')).not.toBeNull();
            expect(onSubmit).not.toHaveBeenCalled();
        });
    });
});
