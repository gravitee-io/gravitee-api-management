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

import { DictionariesTable } from './DictionariesTable';
import type { DictionaryListItem } from '../types/dictionary';

const DICTIONARIES: DictionaryListItem[] = [
    {
        id: '1',
        key: 'countries',
        name: 'Countries',
        type: 'MANUAL',
        state: 'STOPPED',
        updated_at: '2026-01-01T00:00:00.000Z',
    },
    {
        id: '2',
        key: 'remote-codes',
        name: 'Remote Codes',
        type: 'DYNAMIC',
        state: 'STARTED',
        updated_at: '2026-01-02T00:00:00.000Z',
    },
    {
        id: '3',
        key: 'status-map',
        name: 'Status Map',
        type: 'MANUAL',
        state: 'STOPPED',
    },
];

function renderTable(overrides: Partial<{ canEdit: boolean; canDelete: boolean; dictionaries: DictionaryListItem[] }> = {}) {
    const onEdit = jest.fn();
    const onDelete = jest.fn();
    render(
        <DictionariesTable
            dictionaries={overrides.dictionaries ?? DICTIONARIES}
            canEdit={overrides.canEdit ?? true}
            canDelete={overrides.canDelete ?? true}
            onEdit={onEdit}
            onDelete={onDelete}
        />,
    );
    return { onEdit, onDelete };
}

describe('DictionariesTable', () => {
    describe('rendering', () => {
        it('renders all dictionary rows', () => {
            renderTable();
            expect(screen.queryByText('Countries')).not.toBeNull();
            expect(screen.queryByText('Remote Codes')).not.toBeNull();
            expect(screen.queryByText('Status Map')).not.toBeNull();
        });

        it('shows the empty message when there are no dictionaries', () => {
            renderTable({ dictionaries: [] });
            expect(screen.queryByText('No dictionaries defined for this environment.')).not.toBeNull();
        });
    });

    describe('search filtering', () => {
        it('filters rows by name', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by key or name…'), { target: { value: 'remote' } });
            expect(screen.queryByText('Remote Codes')).not.toBeNull();
            expect(screen.queryByText('Countries')).toBeNull();
        });

        it('filters rows by key', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by key or name…'), { target: { value: 'status-map' } });
            expect(screen.queryByText('Status Map')).not.toBeNull();
            expect(screen.queryByText('Countries')).toBeNull();
        });

        it('shows the no-match message when search has no results', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by key or name…'), { target: { value: 'zzznomatch' } });
            expect(screen.queryByText('No dictionaries match your search.')).not.toBeNull();
        });

        it('is case-insensitive', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by key or name…'), { target: { value: 'COUNTRIES' } });
            expect(screen.queryByText('Countries')).not.toBeNull();
        });
    });

    describe('permissions', () => {
        it('hides the actions column when both canEdit and canDelete are false', () => {
            renderTable({ canEdit: false, canDelete: false });
            expect(screen.queryByRole('button', { name: 'Dictionary actions' })).toBeNull();
        });

        it('shows one action button per row when canEdit is true', () => {
            renderTable({ canEdit: true, canDelete: false });
            expect(screen.getAllByRole('button', { name: 'Dictionary actions' }).length).toBe(DICTIONARIES.length);
        });

        it('shows one action button per row when canDelete is true', () => {
            renderTable({ canEdit: false, canDelete: true });
            expect(screen.getAllByRole('button', { name: 'Dictionary actions' }).length).toBe(DICTIONARIES.length);
        });
    });
});
