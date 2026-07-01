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
import type { Metadata } from '../types/metadata';
import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';

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
        it('renders an empty form', () => {
            renderSheet({ mode: 'create' });
            expect((screen.getByLabelText(/Key/i) as HTMLInputElement).value).toBe('');
            expect((screen.getByLabelText(/Name/i) as HTMLInputElement).value).toBe('');
        });

        it('enables the key field', () => {
            renderSheet({ mode: 'create' });
            expect((screen.getByLabelText(/Key/i) as HTMLInputElement).disabled).toBe(false);
        });

        it('keeps Add disabled until both key and name are filled', () => {
            renderSheet({ mode: 'create' });
            const addBtn = screen.getByRole('button', { name: 'Add' }) as HTMLButtonElement;
            expect(addBtn.disabled).toBe(true);

            fireEvent.change(screen.getByLabelText(/Key/i), { target: { value: 'my-key' } });
            expect(addBtn.disabled).toBe(true);

            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Key' } });
            expect(addBtn.disabled).toBe(false);
        });

        it('submits the form with trimmed values', () => {
            const { onSubmit } = renderSheet({ mode: 'create' });
            fireEvent.change(screen.getByLabelText(/Key/i), { target: { value: '  my-key  ' } });
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: '  My Key  ' } });
            fireEvent.click(screen.getByRole('button', { name: 'Add' }));
            expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ key: 'my-key', name: 'My Key' }));
        });

        it('shows "Adding…" label while saving', () => {
            renderSheet({ mode: 'create', isSaving: true });
            expect(screen.queryByRole('button', { name: 'Adding…' })).not.toBeNull();
        });
    });

    describe('edit mode', () => {
        it('pre-fills the form with existing metadata', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            expect((screen.getByLabelText(/Key/i) as HTMLInputElement).value).toBe('support-email');
            expect((screen.getByLabelText(/Name/i) as HTMLInputElement).value).toBe('Support Email');
        });

        it('disables the key field', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            expect((screen.getByLabelText(/Key/i) as HTMLInputElement).disabled).toBe(true);
        });

        it('shows Update button', () => {
            renderSheet({ mode: 'edit', metadata: EXISTING_METADATA });
            expect(screen.queryByRole('button', { name: 'Update' })).not.toBeNull();
        });

        it('submits updated values', () => {
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
});
