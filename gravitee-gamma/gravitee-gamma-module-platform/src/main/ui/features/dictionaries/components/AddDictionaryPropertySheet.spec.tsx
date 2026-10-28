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

import { AddDictionaryPropertySheet } from './AddDictionaryPropertySheet';

jest.mock('../../../shared/notify', () => ({
    notify: { error: jest.fn(), success: jest.fn() },
}));

// jsdom ships no ResizeObserver, which the switch mounts.
globalThis.ResizeObserver ??= class {
    observe() {}
    unobserve() {}
    disconnect() {}
};

function renderSheet(existingKeys: string[] = ['MUC']) {
    const onSubmit = jest.fn().mockResolvedValue(undefined);
    render(<AddDictionaryPropertySheet open onClose={jest.fn()} onSubmit={onSubmit} isSaving={false} existingKeys={existingKeys} />);
    return { onSubmit };
}

describe('AddDictionaryPropertySheet', () => {
    it('submits a plaintext property by default', async () => {
        const { onSubmit } = renderSheet();
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'FRA' } });
        fireEvent.change(screen.getByLabelText(/^Value/), { target: { value: 'Frankfurt' } });
        fireEvent.click(screen.getByRole('button', { name: 'Add' }));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({ key: 'FRA', value: 'Frankfurt', encrypt: false }));
    });

    it('masks the value and submits it for encryption when Encrypt on save is on', async () => {
        const { onSubmit } = renderSheet();
        const value = screen.getByLabelText(/^Value/) as HTMLInputElement;
        expect(value.type).toBe('text');

        fireEvent.click(screen.getByRole('switch', { name: 'Encrypt on save' }));
        expect(value.type).toBe('password');
        expect(screen.getByText('The value will be encrypted on save, and cannot be retrieved later on.')).not.toBeNull();

        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'TOKEN' } });
        fireEvent.change(value, { target: { value: 's3cret' } });
        fireEvent.click(screen.getByRole('button', { name: 'Add' }));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({ key: 'TOKEN', value: 's3cret', encrypt: true }));
    });
});
