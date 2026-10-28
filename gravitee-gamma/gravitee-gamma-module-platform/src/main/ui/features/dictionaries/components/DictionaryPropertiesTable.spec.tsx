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

import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ComponentProps } from 'react';

import { DictionaryPropertiesTable } from './DictionaryPropertiesTable';

const PROPERTIES = [
    { key: 'MUC', value: 'Munich', encrypted: false },
    { key: 'FRA', value: 'Frankfurt', encrypted: false },
];

const ENCRYPTED_ROW = { key: 'TOKEN', value: '••••••••••••', encrypted: true };

function manyProperties(count: number) {
    return Array.from({ length: count }, (_, i) => ({ key: `KEY_${i}`, value: `value-${i}`, encrypted: false }));
}

function renderTable(props: Partial<ComponentProps<typeof DictionaryPropertiesTable>> = {}) {
    const handlers = { onEdit: jest.fn(), onRenew: jest.fn(), onEncrypt: jest.fn(), onDelete: jest.fn() };
    const result = render(
        <DictionaryPropertiesTable
            properties={PROPERTIES}
            canEdit={false}
            isMutating={false}
            emptyMessage="No properties yet."
            {...handlers}
            {...props}
        />,
    );
    return { ...result, ...handlers };
}

async function openActions(key: string) {
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: `Actions for ${key}` }));
    return user;
}

function menuItemNames() {
    return screen.getAllByRole('menuitem').map(item => item.textContent);
}

describe('DictionaryPropertiesTable', () => {
    it('renders property rows without actions when the user cannot edit', () => {
        renderTable();

        expect(screen.getByText('MUC')).not.toBeNull();
        expect(screen.getByText('Munich')).not.toBeNull();
        expect(screen.queryByRole('button', { name: /Actions for/ })).toBeNull();
    });

    describe('manual dictionary', () => {
        it('offers edit, encrypt and delete on a plaintext row', async () => {
            const { onEdit, onEncrypt, onDelete } = renderTable({ canEdit: true });

            const user = await openActions('MUC');
            expect(menuItemNames()).toEqual(['Edit value', 'Encrypt value', 'Delete']);

            await user.click(screen.getByRole('menuitem', { name: 'Edit value' }));
            expect(onEdit).toHaveBeenCalledWith(PROPERTIES[0]);

            await user.click(screen.getByRole('button', { name: 'Actions for MUC' }));
            await user.click(screen.getByRole('menuitem', { name: 'Encrypt value' }));
            expect(onEncrypt).toHaveBeenCalledWith('MUC');

            await user.click(screen.getByRole('button', { name: 'Actions for FRA' }));
            await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
            expect(onDelete).toHaveBeenCalledWith('FRA');
        });

        it('offers renew and delete, but no edit, on an encrypted row', async () => {
            const { onRenew } = renderTable({ properties: [ENCRYPTED_ROW], canEdit: true });

            const user = await openActions('TOKEN');
            expect(menuItemNames()).toEqual(['Renew encryption', 'Delete']);

            await user.click(screen.getByRole('menuitem', { name: 'Renew encryption' }));
            expect(onRenew).toHaveBeenCalledWith(ENCRYPTED_ROW);
        });

        it('masks an encrypted value with eight bullets and badges it Encrypted', () => {
            renderTable({ properties: [ENCRYPTED_ROW, ...PROPERTIES] });

            expect(screen.getByText('•'.repeat(8))).not.toBeNull();
            expect(screen.queryByText(ENCRYPTED_ROW.value)).toBeNull();
            expect(within(screen.getByRole('row', { name: /TOKEN/ })).getByText('Encrypted')).not.toBeNull();
            expect(within(screen.getByRole('row', { name: /MUC/ })).getByText('Unencrypted')).not.toBeNull();
        });
    });

    it('disables the row actions while a save is in flight', () => {
        renderTable({ canEdit: true, isMutating: true });

        expect(screen.getByRole('button', { name: 'Actions for MUC' }).hasAttribute('disabled')).toBe(true);
    });

    it('paginates properties, showing only the first page by default', () => {
        renderTable({ properties: manyProperties(12) });

        expect(screen.getByText('KEY_0')).not.toBeNull();
        expect(screen.getByText('KEY_9')).not.toBeNull();
        expect(screen.queryByText('KEY_10')).toBeNull();
    });

    it('shows the next page of properties after paging forward', async () => {
        const user = userEvent.setup();
        renderTable({ properties: manyProperties(12) });

        await user.click(screen.getByRole('button', { name: /next page/i }));

        expect(screen.getByText('KEY_10')).not.toBeNull();
        expect(screen.getByText('KEY_11')).not.toBeNull();
        expect(screen.queryByText('KEY_0')).toBeNull();
    });

    it('clamps back to the last valid page when properties shrink out from under the current page', async () => {
        const user = userEvent.setup();
        const { rerender, onEdit, onRenew, onEncrypt, onDelete } = renderTable({ properties: manyProperties(15) });

        await user.click(screen.getByRole('button', { name: /next page/i }));
        expect(screen.getByText('KEY_10')).not.toBeNull();

        // Simulate deleting properties down to a single page — page 2 no longer exists.
        rerender(
            <DictionaryPropertiesTable
                properties={manyProperties(5)}
                canEdit={false}
                isMutating={false}
                onEdit={onEdit}
                onRenew={onRenew}
                onEncrypt={onEncrypt}
                onDelete={onDelete}
                emptyMessage="No properties yet."
            />,
        );

        expect(await screen.findByText('KEY_0')).not.toBeNull();
        expect(screen.queryByText('No properties yet.')).toBeNull();
    });

    it('shows empty message when there are no properties', () => {
        renderTable({ properties: [], canEdit: true });

        expect(screen.getByText('No properties yet.')).not.toBeNull();
    });
});
