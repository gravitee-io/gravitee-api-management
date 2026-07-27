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
import userEvent from '@testing-library/user-event';

import { DictionaryPropertiesTable } from './DictionaryPropertiesTable';

const PROPERTIES = [
    { key: 'MUC', value: 'Munich' },
    { key: 'FRA', value: 'Frankfurt' },
];

function manyProperties(count: number) {
    return Array.from({ length: count }, (_, i) => ({ key: `KEY_${i}`, value: `value-${i}` }));
}

describe('DictionaryPropertiesTable', () => {
    it('renders property rows', () => {
        render(
            <DictionaryPropertiesTable
                properties={PROPERTIES}
                canEdit={false}
                isMutating={false}
                onEdit={jest.fn()}
                onDelete={jest.fn()}
                emptyMessage="No properties yet."
            />,
        );
        expect(screen.getByText('MUC')).not.toBeNull();
        expect(screen.getByText('Munich')).not.toBeNull();
        expect(screen.queryByRole('button', { name: /Edit property/ })).toBeNull();
    });

    it('calls edit and delete handlers when permitted', () => {
        const onEdit = jest.fn();
        const onDelete = jest.fn();
        render(
            <DictionaryPropertiesTable
                properties={PROPERTIES}
                canEdit
                isMutating={false}
                onEdit={onEdit}
                onDelete={onDelete}
                emptyMessage="No properties yet."
            />,
        );

        fireEvent.click(screen.getByRole('button', { name: 'Edit property MUC' }));
        expect(onEdit).toHaveBeenCalledWith({ key: 'MUC', value: 'Munich' });

        fireEvent.click(screen.getByRole('button', { name: 'Delete property FRA' }));
        expect(onDelete).toHaveBeenCalledWith('FRA');
    });

    it('paginates properties, showing only the first page by default', () => {
        render(
            <DictionaryPropertiesTable
                properties={manyProperties(12)}
                canEdit={false}
                isMutating={false}
                onEdit={jest.fn()}
                onDelete={jest.fn()}
                emptyMessage="No properties yet."
            />,
        );

        expect(screen.getByText('KEY_0')).not.toBeNull();
        expect(screen.getByText('KEY_9')).not.toBeNull();
        expect(screen.queryByText('KEY_10')).toBeNull();
    });

    it('shows the next page of properties after paging forward', async () => {
        const user = userEvent.setup();
        render(
            <DictionaryPropertiesTable
                properties={manyProperties(12)}
                canEdit={false}
                isMutating={false}
                onEdit={jest.fn()}
                onDelete={jest.fn()}
                emptyMessage="No properties yet."
            />,
        );

        await user.click(screen.getByRole('button', { name: /next page/i }));

        expect(screen.getByText('KEY_10')).not.toBeNull();
        expect(screen.getByText('KEY_11')).not.toBeNull();
        expect(screen.queryByText('KEY_0')).toBeNull();
    });

    it('clamps back to the last valid page when properties shrink out from under the current page', async () => {
        const user = userEvent.setup();
        const onEdit = jest.fn();
        const onDelete = jest.fn();
        const { rerender } = render(
            <DictionaryPropertiesTable
                properties={manyProperties(15)}
                canEdit={false}
                isMutating={false}
                onEdit={onEdit}
                onDelete={onDelete}
                emptyMessage="No properties yet."
            />,
        );

        await user.click(screen.getByRole('button', { name: /next page/i }));
        expect(screen.getByText('KEY_10')).not.toBeNull();

        // Simulate deleting properties down to a single page — page 2 no longer exists.
        rerender(
            <DictionaryPropertiesTable
                properties={manyProperties(5)}
                canEdit={false}
                isMutating={false}
                onEdit={onEdit}
                onDelete={onDelete}
                emptyMessage="No properties yet."
            />,
        );

        expect(await screen.findByText('KEY_0')).not.toBeNull();
        expect(screen.queryByText('No properties yet.')).toBeNull();
    });

    it('shows empty message when there are no properties', () => {
        render(
            <DictionaryPropertiesTable
                properties={[]}
                canEdit
                isMutating={false}
                onEdit={jest.fn()}
                onDelete={jest.fn()}
                emptyMessage="No properties yet."
            />,
        );
        expect(screen.getByText('No properties yet.')).not.toBeNull();
    });
});
