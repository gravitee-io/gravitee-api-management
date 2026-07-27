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

import { MetadataDeleteSheet } from './MetadataDeleteSheet';
import type { Metadata } from '../types/metadata';

const METADATA: Metadata = { key: 'support-email', name: 'Support Email', format: 'MAIL', value: 'help@example.com' };

function renderSheet(open: boolean, metadata: Metadata | undefined = METADATA) {
    const onClose = jest.fn();
    const onConfirm = jest.fn();
    render(<MetadataDeleteSheet open={open} metadata={metadata} onClose={onClose} onConfirm={onConfirm} isDeleting={false} />);
    return { onClose, onConfirm };
}

describe('MetadataDeleteSheet', () => {
    it('does not show dialog content when closed', () => {
        renderSheet(false);
        expect(screen.queryByRole('heading', { name: 'Delete Metadata' })).toBeNull();
    });

    it('shows the metadata name and key in the confirmation message', () => {
        renderSheet(true);
        expect(screen.getByText(/Support Email/)).not.toBeNull();
        expect(screen.getByText('support-email')).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Delete' })).not.toBeNull();
    });

    it('invokes onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet(true);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('invokes onConfirm when Delete is clicked', () => {
        const { onConfirm } = renderSheet(true);
        fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
        expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it('shows deleting label while the mutation is in progress', () => {
        render(<MetadataDeleteSheet open metadata={METADATA} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting />);

        expect(screen.getByRole('button', { name: 'Deleting…' })).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Delete' })).toBeNull();
        expect((screen.getByRole('button', { name: 'Cancel' }) as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByRole('button', { name: 'Deleting…' }) as HTMLButtonElement).disabled).toBe(true);
    });
});
