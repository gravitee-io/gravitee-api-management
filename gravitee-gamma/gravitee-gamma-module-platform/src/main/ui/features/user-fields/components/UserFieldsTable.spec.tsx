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

import { UserFieldsTable } from './UserFieldsTable';
import type { UserField } from '../types/userField';

const FIELDS: UserField[] = [
    { key: 'department', label: 'Department', required: true, values: ['Engineering', 'Product'] },
    { key: 'job_position', label: 'Job position', required: false, values: [] },
    { key: 'country', label: 'Country', required: false },
];

function renderTable(overrides: Partial<{ canEdit: boolean; canDelete: boolean; fields: UserField[] }> = {}) {
    const onEdit = jest.fn();
    const onDelete = jest.fn();
    render(
        <UserFieldsTable
            fields={overrides.fields ?? FIELDS}
            canEdit={overrides.canEdit ?? true}
            canDelete={overrides.canDelete ?? true}
            onEdit={onEdit}
            onDelete={onDelete}
        />,
    );
    return { onEdit, onDelete };
}

const SEARCH_PLACEHOLDER = 'Search by key, label, or value…';

describe('UserFieldsTable', () => {
    describe('rendering', () => {
        it('renders one row per field with key and label', () => {
            renderTable();
            expect(screen.getByText('department')).not.toBeNull();
            expect(screen.getByText('Department')).not.toBeNull();
            expect(screen.getByText('job_position')).not.toBeNull();
            expect(screen.getByText('Country')).not.toBeNull();
        });

        it('marks required fields with a badge on the key cell', () => {
            renderTable();
            expect(screen.getAllByText('Required').length).toBe(1);
        });

        it('lists each value on its own line and a dash when the field is free text', () => {
            renderTable();
            expect(screen.getByText('Engineering')).not.toBeNull();
            expect(screen.getByText('Product')).not.toBeNull();
            expect(screen.getAllByText('—').length).toBe(2);
        });

        it('renders duplicate values the API may return without collapsing them', () => {
            renderTable({ fields: [{ key: 'dup', label: 'Dup', required: false, values: ['Same', 'Same'] }] });
            expect(screen.getAllByText('Same').length).toBe(2);
        });

        it('shows the Classic empty message when there are no fields', () => {
            renderTable({ fields: [] });
            expect(screen.getByText('There are no custom user fields')).not.toBeNull();
        });
    });

    describe('search filtering', () => {
        it('filters rows by key', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), { target: { value: 'job_' } });
            expect(screen.queryByText('Job position')).not.toBeNull();
            expect(screen.queryByText('Department')).toBeNull();
        });

        it('filters rows by label, case-insensitively', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), { target: { value: 'COUNTRY' } });
            expect(screen.queryByText('Country')).not.toBeNull();
            expect(screen.queryByText('Department')).toBeNull();
        });

        it('filters rows by one of the allowed values', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), { target: { value: 'product' } });
            expect(screen.queryByText('Department')).not.toBeNull();
            expect(screen.queryByText('Country')).toBeNull();
        });

        it('shows the no-match message when nothing matches', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), { target: { value: 'zzz' } });
            expect(screen.getByText('No user fields match your search.')).not.toBeNull();
        });
    });

    describe('pagination', () => {
        it('falls back to the last available page when a search shrinks the result set', () => {
            const many: UserField[] = Array.from({ length: 12 }, (_, index) => ({
                key: `field_${String(index + 1).padStart(2, '0')}`,
                label: `Field ${index + 1}`,
                required: false,
                values: index === 11 ? ['only-on-last'] : [],
            }));
            renderTable({ fields: many });
            fireEvent.click(screen.getByRole('button', { name: /next page/i }));
            expect(screen.queryByText('field_12')).not.toBeNull();
            expect(screen.queryByText('field_01')).toBeNull();

            fireEvent.change(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), { target: { value: 'only-on-last' } });

            expect(screen.queryByText('field_12')).not.toBeNull();
            expect(screen.queryByText('No user fields match your search.')).toBeNull();
        });
    });

    describe('sorting', () => {
        it('offers sorting on key and label only', () => {
            renderTable();
            expect(screen.queryByRole('button', { name: 'Key' })).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Label' })).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Values' })).toBeNull();
            expect(screen.queryByRole('button', { name: 'Required' })).toBeNull();
        });

        it('sorts by key descending after two header clicks', () => {
            renderTable();
            fireEvent.click(screen.getByRole('button', { name: 'Key' }));
            fireEvent.click(screen.getByRole('button', { name: 'Key' }));
            const keys = Array.from(document.querySelectorAll('tbody tr td:first-child')).map(cell =>
                cell.textContent?.replace('Required', ''),
            );
            expect(keys).toEqual(['job_position', 'department', 'country']);
        });
    });

    describe('permissions', () => {
        it('hides the actions column when the user can neither edit nor delete', () => {
            renderTable({ canEdit: false, canDelete: false });
            expect(screen.queryByRole('button', { name: 'User field actions' })).toBeNull();
        });

        it('shows one actions button per row when the user can edit', () => {
            renderTable({ canEdit: true, canDelete: false });
            expect(screen.getAllByRole('button', { name: 'User field actions' }).length).toBe(FIELDS.length);
        });

        it('shows one actions button per row when the user can only delete', () => {
            renderTable({ canEdit: false, canDelete: true });
            expect(screen.getAllByRole('button', { name: 'User field actions' }).length).toBe(FIELDS.length);
        });
    });
});
