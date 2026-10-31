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
    { key: 'MUC', value: 'Munich', encrypted: false, encryptOnSave: false },
    { key: 'FRA', value: 'Frankfurt', encrypted: false, encryptOnSave: false },
];

// The value is deliberately a readable string: a leak would show as this text, which the server's mask could not reveal.
const ENCRYPTED_ROW = { key: 'TOKEN', value: 'leaked-plaintext', encrypted: true, encryptOnSave: false };
const MARKED_ROW = { key: 'MUC', value: 'Munich', encrypted: false, encryptOnSave: true };

function manyProperties(count: number) {
    return Array.from({ length: count }, (_, i) => ({ key: `KEY_${i}`, value: `value-${i}`, encrypted: false, encryptOnSave: false }));
}

function renderTable(props: Partial<ComponentProps<typeof DictionaryPropertiesTable>> = {}) {
    const handlers = { onEdit: jest.fn(), onRenew: jest.fn(), onEncrypt: jest.fn(), onUndoEncrypt: jest.fn(), onDelete: jest.fn() };
    const result = render(
        <DictionaryPropertiesTable
            properties={PROPERTIES}
            canEdit={false}
            dynamic={false}
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
            expect(menuItemNames()).toEqual(['Edit value', 'Encrypt', 'Delete']);

            await user.click(screen.getByRole('menuitem', { name: 'Edit value' }));
            expect(onEdit).toHaveBeenCalledWith(PROPERTIES[0]);

            await user.click(screen.getByRole('button', { name: 'Actions for MUC' }));
            await user.click(screen.getByRole('menuitem', { name: 'Encrypt' }));
            expect(onEncrypt).toHaveBeenCalledWith('MUC');

            await user.click(screen.getByRole('button', { name: 'Actions for FRA' }));
            await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
            expect(onDelete).toHaveBeenCalledWith('FRA');
        });

        it('offers replace and delete, but no edit, on an encrypted row', async () => {
            const { onRenew } = renderTable({ properties: [ENCRYPTED_ROW], canEdit: true });

            const user = await openActions('TOKEN');
            expect(menuItemNames()).toEqual(['Replace value', 'Delete']);

            await user.click(screen.getByRole('menuitem', { name: 'Replace value' }));
            expect(onRenew).toHaveBeenCalledWith(ENCRYPTED_ROW);
        });

        it('offers do not encrypt, replace and delete on a row marked to be encrypted on save', async () => {
            const { onUndoEncrypt, onRenew } = renderTable({ properties: [MARKED_ROW], canEdit: true });
            expect(within(screen.getByRole('row', { name: /MUC/ })).getByText('Encrypted on save')).not.toBeNull();

            const user = await openActions('MUC');
            expect(menuItemNames()).toEqual(['Do not encrypt', 'Replace value', 'Delete']);

            await user.click(screen.getByRole('menuitem', { name: 'Do not encrypt' }));
            expect(onUndoEncrypt).toHaveBeenCalledWith('MUC');

            await user.click(screen.getByRole('button', { name: 'Actions for MUC' }));
            await user.click(screen.getByRole('menuitem', { name: 'Replace value' }));
            expect(onRenew).toHaveBeenCalledWith(MARKED_ROW);
        });

        it('masks an encrypted value with twelve bullets and badges it Encrypted with a tooltip', () => {
            renderTable({ properties: [ENCRYPTED_ROW, ...PROPERTIES] });

            expect(screen.getByText('•'.repeat(12))).not.toBeNull();
            expect(screen.queryByText(ENCRYPTED_ROW.value)).toBeNull();
            const badge = within(screen.getByRole('row', { name: /TOKEN/ })).getByText('Encrypted');
            expect(badge.closest('[title]')?.getAttribute('title')).toBe('This value is encrypted');
            expect(within(screen.getByRole('row', { name: /MUC/ })).getByText('Unencrypted')).not.toBeNull();
        });

        it('keeps a masked value out of reach: it cannot be selected and clicking it opens nothing', async () => {
            const { onEdit, onRenew } = renderTable({ properties: [ENCRYPTED_ROW], canEdit: true });
            const mask = screen.getByText('•'.repeat(12));

            await userEvent.setup().click(mask);

            expect(mask.className).toContain('select-none');
            expect(onEdit).not.toHaveBeenCalled();
            expect(onRenew).not.toHaveBeenCalled();
        });
    });

    describe('dynamic dictionary', () => {
        it("badges every row Dynamic, masks encrypted values and shows each row's encryption state", () => {
            renderTable({ dynamic: true, canEdit: true, properties: [...PROPERTIES, ENCRYPTED_ROW] });

            expect(screen.getAllByText('Dynamic')).toHaveLength(PROPERTIES.length + 1);
            expect(within(screen.getByRole('row', { name: /TOKEN/ })).getByText('Encrypted')).not.toBeNull();
            expect(within(screen.getByRole('row', { name: /MUC/ })).getByText('Unencrypted')).not.toBeNull();
        });

        it('offers no action, even to a user who can edit', () => {
            renderTable({ dynamic: true, canEdit: true, properties: [...PROPERTIES, ENCRYPTED_ROW] });

            expect(screen.queryByRole('button', { name: /Actions for/ })).toBeNull();
            expect(screen.queryByRole('columnheader', { name: 'Actions' })).toBeNull();
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
        const { rerender, onEdit, onRenew, onEncrypt, onUndoEncrypt, onDelete } = renderTable({ properties: manyProperties(15) });

        await user.click(screen.getByRole('button', { name: /next page/i }));
        expect(screen.getByText('KEY_10')).not.toBeNull();

        // Simulate deleting properties down to a single page — page 2 no longer exists.
        rerender(
            <DictionaryPropertiesTable
                properties={manyProperties(5)}
                canEdit={false}
                dynamic={false}
                isMutating={false}
                onEdit={onEdit}
                onRenew={onRenew}
                onEncrypt={onEncrypt}
                onUndoEncrypt={onUndoEncrypt}
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
