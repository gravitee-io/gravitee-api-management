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

import { EncryptDictionaryPropertySheet } from './EncryptDictionaryPropertySheet';

function renderSheet({ open = true, propertyKey = 'TOKEN', isEncrypting = false } = {}) {
    const onClose = jest.fn();
    const onConfirm = jest.fn();
    render(
        <EncryptDictionaryPropertySheet
            open={open}
            propertyKey={propertyKey}
            onClose={onClose}
            onConfirm={onConfirm}
            isEncrypting={isEncrypting}
        />,
    );
    return { onClose, onConfirm };
}

describe('EncryptDictionaryPropertySheet', () => {
    it('shows nothing when closed', () => {
        renderSheet({ open: false });

        expect(screen.queryByRole('heading', { name: 'Encrypt Property' })).toBeNull();
    });

    it('names the property and warns that encryption cannot be undone', () => {
        renderSheet();

        expect(screen.getByRole('heading', { name: 'Encrypt Property' })).not.toBeNull();
        expect(screen.getByText('TOKEN')).not.toBeNull();
        expect(screen.getByText(/cannot be undone/)).not.toBeNull();
    });

    it('confirms only when Encrypt is clicked', () => {
        const { onConfirm, onClose } = renderSheet();

        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onConfirm).not.toHaveBeenCalled();
        expect(onClose).toHaveBeenCalledTimes(1);

        fireEvent.click(screen.getByRole('button', { name: 'Encrypt' }));
        expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it('disables both actions while encrypting', () => {
        renderSheet({ isEncrypting: true });

        expect((screen.getByRole('button', { name: 'Encrypting…' }) as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByRole('button', { name: 'Cancel' }) as HTMLButtonElement).disabled).toBe(true);
    });
});
