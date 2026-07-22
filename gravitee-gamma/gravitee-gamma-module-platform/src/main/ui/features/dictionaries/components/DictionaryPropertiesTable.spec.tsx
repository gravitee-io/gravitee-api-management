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

import { DictionaryPropertiesTable } from './DictionaryPropertiesTable';

const PROPERTIES = [
    { key: 'MUC', value: 'Munich' },
    { key: 'FRA', value: 'Frankfurt' },
];

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
