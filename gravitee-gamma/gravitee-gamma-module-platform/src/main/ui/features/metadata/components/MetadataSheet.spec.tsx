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

import { MetadataSheet } from './MetadataSheet';
import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';
import type { Metadata } from '../types/metadata';

const EXISTING_METADATA: Metadata = { key: 'support-email', name: 'Support Email', format: 'MAIL', value: 'help@example.com' };

function renderSheet({
    open = true,
    mode,
    metadata,
    isSaving = false,
}: {
    open?: boolean;
    mode: 'create' | 'edit';
    metadata?: Metadata;
    isSaving?: boolean;
}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn();
    render(<MetadataSheet open={open} mode={mode} metadata={metadata} onClose={onClose} onSubmit={onSubmit} isSaving={isSaving} />);
    return { onClose, onSubmit };
}

describe('MetadataSheet', () => {
    describe('visibility', () => {
        it('does not show sheet content when closed', () => {
            renderSheet({ open: false, mode: 'create' });
            expect(querySheetHeading('Add Global Metadata')).toBeNull();
        });

        it('shows create title when mode is create', () => {
            renderSheet({ mode: 'create' });
            expect(screen.getByRole('heading', { name: 'Add Global Metadata' })).not.toBeNull();
        });

        it('shows edit title when mode is edit', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            expect(screen.getByRole('heading', { name: 'Edit Metadata' })).not.toBeNull();
        });
    });

    describe('create mode', () => {
        it('renders an empty name field', () => {
            renderSheet({ mode: 'create' });
            expect((screen.getByLabelText(/Name/i) as HTMLInputElement).value).toBe('');
        });

        it('defaults format to STRING', () => {
            renderSheet({ mode: 'create' });
            expect(screen.queryAllByText('String').length).toBeGreaterThan(0);
        });

        it('keeps Add disabled until name and value are filled', () => {
            renderSheet({ mode: 'create' });
            const addBtn = screen.getByRole('button', { name: 'Add' }) as HTMLButtonElement;
            expect(addBtn.disabled).toBe(true);

            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Metadata' } });
            expect(addBtn.disabled).toBe(true);

            fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: 'some-value' } });
            expect(addBtn.disabled).toBe(false);
        });

        it('submits the form with trimmed name and value and no key', () => {
            const { onSubmit } = renderSheet({ mode: 'create' });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: '  My Metadata  ' } });
            fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: '  some value  ' } });
            fireEvent.click(screen.getByRole('button', { name: 'Add' }));
            expect(onSubmit).toHaveBeenCalledWith({ name: 'My Metadata', format: 'STRING', value: 'some value' });
        });

        it('shows "Adding…" label while saving', () => {
            renderSheet({ mode: 'create', isSaving: true });
            expect(screen.queryByRole('button', { name: 'Adding…' })).not.toBeNull();
        });
    });

    describe('edit mode', () => {
        it('shows the key as a disabled read-only field', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            const keyInput = screen.getByLabelText(/Key/i) as HTMLInputElement;
            expect(keyInput.value).toBe('support-email');
            expect(keyInput.disabled).toBe(true);
        });

        it('does not show the key field in create mode', () => {
            renderSheet({ mode: 'create' });
            expect(screen.queryByLabelText(/Key/i)).toBeNull();
        });

        it('pre-fills name and value from existing metadata', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            expect((screen.getByLabelText(/Name/i) as HTMLInputElement).value).toBe('Support Email');
            expect((screen.getByLabelText(/Value/i) as HTMLInputElement).value).toBe('help@example.com');
        });

        it('shows a hint that format cannot be changed in edit mode', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            expect(screen.queryByText('Format cannot be changed after creation.')).not.toBeNull();
        });

        it('shows Update button', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            expect(screen.queryByRole('button', { name: 'Update' })).not.toBeNull();
        });

        it('disables Update when nothing has changed', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            expect((screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement).disabled).toBe(true);
        });

        it('enables Update after changing the name', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'New Name' } });
            expect((screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement).disabled).toBe(false);
        });

        it('enables Update after changing the value', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: 'new@example.com' } });
            expect((screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement).disabled).toBe(false);
        });

        it('re-disables Update when name is reverted to original', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'New Name' } });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Support Email' } });
            expect((screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement).disabled).toBe(true);
        });

        it('submits updated values including the original key', () => {
            const { onSubmit } = renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Updated Name' } });
            fireEvent.click(screen.getByRole('button', { name: 'Update' }));
            expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ key: 'support-email', name: 'Updated Name' }));
        });

        it('shows "Updating…" label while saving', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA, isSaving: true });
            expect(screen.queryByRole('button', { name: 'Updating…' })).not.toBeNull();
        });
    });

    describe('cancel', () => {
        it('invokes onClose when Cancel is clicked', () => {
            const { onClose } = renderSheet({ mode: 'create' });
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });

    describe('format-specific validation', () => {
        it('renders a Select (not an input) for BOOLEAN format value field', () => {
            const booleanMetadata: Metadata = { key: 'debug', name: 'Debug Mode', format: 'BOOLEAN', value: 'true' };
            renderSheet({ mode: 'edit', metadata: booleanMetadata });
            expect(screen.queryByRole('combobox', { name: /Value/i })).not.toBeNull();
        });

        it('disables Update when NUMERIC value is not a valid number', () => {
            const numericMetadata: Metadata = { key: 'retries', name: 'Max Retries', format: 'NUMERIC', value: '3' };
            renderSheet({ mode: 'edit', metadata: numericMetadata });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Max Retries Updated' } });
            const updateBtn = screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement;
            expect(updateBtn.disabled).toBe(false);

            fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: 'not-a-number' } });
            expect(updateBtn.disabled).toBe(true);
        });

        it('disables Update when MAIL value is not a valid email', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Changed' } });
            const updateBtn = screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement;
            expect(updateBtn.disabled).toBe(false);

            fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: 'not-an-email' } });
            expect(updateBtn.disabled).toBe(true);
        });

        it('disables Update when MAIL value is missing domain extension', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Changed' } });
            fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: 'user@nodomain' } });
            expect((screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement).disabled).toBe(true);
        });

        it('disables Update when URL value is not a valid URL', () => {
            const urlMetadata: Metadata = { key: 'docs', name: 'Docs URL', format: 'URL', value: 'https://docs.example.com' };
            renderSheet({ mode: 'edit', metadata: urlMetadata });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Docs URL Updated' } });
            const updateBtn = screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement;
            expect(updateBtn.disabled).toBe(false);

            fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: 'not a url !!' } });
            expect(updateBtn.disabled).toBe(true);
        });

        it('disables Add when value is empty for STRING format', () => {
            renderSheet({ mode: 'create' });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Metadata' } });
            const addBtn = screen.getByRole('button', { name: 'Add' }) as HTMLButtonElement;
            expect(addBtn.disabled).toBe(true);
        });
    });
});
