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

import { RenewDictionaryPropertySheet } from './RenewDictionaryPropertySheet';

function renderSheet(overrides: Partial<{ open: boolean; isSaving: boolean }> = {}) {
    const onSubmit = jest.fn().mockResolvedValue(undefined);
    render(
        <RenewDictionaryPropertySheet
            open={overrides.open ?? true}
            propertyKey="TOKEN"
            onClose={jest.fn()}
            onSubmit={onSubmit}
            isSaving={overrides.isSaving ?? false}
        />,
    );
    return { onSubmit };
}

describe('RenewDictionaryPropertySheet', () => {
    it('shows the property key read-only and asks for a new plaintext value', () => {
        renderSheet();

        expect(screen.getByRole('heading', { name: 'Renew Encryption' })).not.toBeNull();
        expect(screen.getByText('TOKEN')).not.toBeNull();
        expect(screen.getByText('Enter a new plaintext value. It will be re-encrypted on save.')).not.toBeNull();
        const value = screen.getByLabelText(/^Value/) as HTMLInputElement;
        expect(value.value).toBe('');
        expect(value.type).toBe('password');
    });

    it('cannot be submitted without a value', () => {
        renderSheet();

        expect(screen.getByRole('button', { name: 'Renew' }).hasAttribute('disabled')).toBe(true);
    });

    it('submits the new plaintext for the key', async () => {
        const { onSubmit } = renderSheet();
        fireEvent.change(screen.getByLabelText(/^Value/), { target: { value: 'n3w-s3cret' } });
        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({ key: 'TOKEN', value: 'n3w-s3cret' }));
    });
});
