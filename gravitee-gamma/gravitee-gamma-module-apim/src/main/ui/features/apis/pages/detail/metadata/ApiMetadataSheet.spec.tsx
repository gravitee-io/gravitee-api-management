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

import { ApiMetadataSheet } from './ApiMetadataSheet';
import type { ApiMetadata } from '../../../types/metadata';

const EXISTING: ApiMetadata = { key: 'support-email', name: 'Support Email', format: 'MAIL', value: 'help@example.com' };

function renderSheet({
    open = true,
    mode,
    metadata,
    isSaving = false,
    readOnly = false,
}: {
    open?: boolean;
    mode: 'create' | 'edit';
    metadata?: ApiMetadata;
    isSaving?: boolean;
    readOnly?: boolean;
}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn();
    render(
        <ApiMetadataSheet
            open={open}
            mode={mode}
            metadata={metadata}
            readOnly={readOnly}
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={isSaving}
        />,
    );
    return { onClose, onSubmit };
}

describe('ApiMetadataSheet', () => {
    it('does not show sheet content when closed', () => {
        renderSheet({ open: false, mode: 'create' });
        expect(screen.queryByRole('heading', { name: 'Add API Metadata' })).toBeNull();
    });

    it('shows create title when mode is create', () => {
        renderSheet({ mode: 'create' });
        expect(screen.getByRole('heading', { name: 'Add API Metadata' })).not.toBeNull();
    });

    it('keeps Add disabled until name and value are filled', () => {
        renderSheet({ mode: 'create' });
        const addBtn = screen.getByRole('button', { name: 'Add' }) as HTMLButtonElement;
        expect(addBtn.disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Team' } });
        expect(addBtn.disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: 'Platform' } });
        expect(addBtn.disabled).toBe(false);
    });

    it('submits a create payload', () => {
        const { onSubmit } = renderSheet({ mode: 'create' });
        fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Team' } });
        fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: 'Platform' } });
        fireEvent.click(screen.getByRole('button', { name: 'Add' }));
        expect(onSubmit).toHaveBeenCalledWith({ name: 'Team', format: 'STRING', value: 'Platform' });
    });

    it('shows the key as read-only in edit mode', () => {
        renderSheet({ mode: 'edit', metadata: EXISTING });
        expect((screen.getByLabelText('Key') as HTMLInputElement).value).toBe('support-email');
        expect((screen.getByLabelText('Key') as HTMLInputElement).disabled).toBe(true);
    });

    it('hides the submit button in read-only mode', () => {
        renderSheet({ mode: 'edit', metadata: EXISTING, readOnly: true });
        expect(screen.queryByRole('button', { name: 'Update' })).toBeNull();
        expect(screen.getAllByRole('button', { name: 'Close' }).length).toBeGreaterThan(0);
    });

    it('shows a mail example on the value field', () => {
        renderSheet({ mode: 'edit', metadata: EXISTING });
        expect(screen.getByLabelText(/Value/i)).toHaveAttribute('placeholder', 'e.g. john@doe.com');
        expect(screen.queryByText('e.g. john@doe.com')).toBeNull();
    });

    it('shows a url example on the value field', () => {
        renderSheet({
            mode: 'edit',
            metadata: { key: 'docs', name: 'Docs', format: 'URL', value: 'https://docs.example.com' },
        });
        expect(screen.getByLabelText(/Value/i)).toHaveAttribute('placeholder', 'e.g. https://gravitee.io');
        expect(screen.queryByText('e.g. https://gravitee.io')).toBeNull();
    });
});
