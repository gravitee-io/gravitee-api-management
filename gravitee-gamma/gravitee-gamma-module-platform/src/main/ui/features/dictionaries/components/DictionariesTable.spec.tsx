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

import { DictionariesTable } from './DictionariesTable';
import type { DictionaryListItem } from '../types/dictionary';

const DICTIONARIES: DictionaryListItem[] = [
    {
        id: '1',
        name: 'Countries',
        description: 'ISO country lookup',
        type: 'MANUAL',
        state: 'STOPPED',
        properties: 4,
        updated_at: '2026-01-01T00:00:00.000Z',
    },
    {
        id: '2',
        name: 'Remote Codes',
        type: 'DYNAMIC',
        state: 'STARTED',
        properties: 3,
        updated_at: '2026-01-02T00:00:00.000Z',
    },
    {
        id: 'status-map',
        name: 'Status Map',
        type: 'MANUAL',
        state: 'STOPPED',
        properties: 0,
    },
];

function renderTable(overrides: Partial<{ canEdit: boolean; canDelete: boolean; dictionaries: DictionaryListItem[] }> = {}) {
    const onOpen = jest.fn();
    const onEdit = jest.fn();
    const onDelete = jest.fn();
    render(
        <DictionariesTable
            dictionaries={overrides.dictionaries ?? DICTIONARIES}
            canEdit={overrides.canEdit ?? true}
            canDelete={overrides.canDelete ?? true}
            onOpen={onOpen}
            onEdit={onEdit}
            onDelete={onDelete}
        />,
    );
    return { onOpen, onEdit, onDelete };
}

describe('DictionariesTable', () => {
    describe('rendering', () => {
        it('renders all dictionary rows', () => {
            renderTable();
            expect(screen.queryByText('Countries')).not.toBeNull();
            expect(screen.queryByText('Remote Codes')).not.toBeNull();
            expect(screen.queryByText('Status Map')).not.toBeNull();
        });

        it('shows description under name, or nothing when missing', () => {
            renderTable();
            expect(screen.queryByText('ISO country lookup')).not.toBeNull();

            const nameButton = screen.getByText('Remote Codes').closest('button');
            expect(nameButton?.textContent).toBe('Remote Codes');
        });

        it('truncates long descriptions with an ellipsis on one line', () => {
            const longDescription = 'IATA codes for airports referenced by Lufthansa flight operations and partner systems across Europe';
            renderTable({
                dictionaries: [
                    {
                        id: '1',
                        name: 'Airport IATA Codes',
                        description: longDescription,
                        type: 'MANUAL',
                        properties: 4,
                    },
                ],
            });
            expect(screen.queryByText(longDescription)).toBeNull();
            expect(screen.getByText(/IATA codes for airports referenced by Lufthansa/))
                .textContent?.endsWith('…')
                .toBe(true);
        });

        it('shows Dynamic status inline with type and has no State column', () => {
            renderTable();
            expect(screen.queryByText('Started')).not.toBeNull();
            expect(screen.queryByRole('columnheader', { name: 'State' })).toBeNull();
            expect(screen.queryByRole('columnheader', { name: 'Type' })).not.toBeNull();
            expect(screen.queryByRole('columnheader', { name: 'Properties' })).not.toBeNull();
        });

        it('shows the empty message when there are no dictionaries', () => {
            renderTable({ dictionaries: [] });
            expect(screen.queryByText('No dictionaries defined for this environment.')).not.toBeNull();
        });

        it('shows — for Last Deployed on DYNAMIC dictionaries, even when deployed_at is set', () => {
            renderTable({
                dictionaries: [
                    {
                        id: '2',
                        name: 'Remote Codes',
                        type: 'DYNAMIC',
                        state: 'STARTED',
                        properties: 3,
                        updated_at: '2026-01-02T00:00:00.000Z',
                        deployed_at: '2026-01-05T00:00:00.000Z',
                    },
                ],
            });
            expect(screen.queryByText('—')).not.toBeNull();
            expect(screen.queryByText(/Jan 5, 2026/)).toBeNull();
        });
    });

    describe('name open', () => {
        it('calls onOpen when the name cell is clicked', () => {
            const { onOpen } = renderTable();
            fireEvent.click(screen.getByRole('button', { name: /Countries/ }));
            expect(onOpen).toHaveBeenCalledWith(expect.objectContaining({ id: '1', name: 'Countries' }));
        });
    });

    describe('row actions', () => {
        it('calls onEdit when Edit is selected from the actions menu', async () => {
            const user = userEvent.setup();
            const { onEdit } = renderTable();
            await user.click(screen.getAllByRole('button', { name: 'Dictionary actions' })[0]);
            await user.click(await screen.findByRole('menuitem', { name: /^Edit$/ }));
            expect(onEdit).toHaveBeenCalledWith(expect.objectContaining({ id: '1', name: 'Countries' }));
        });
    });

    describe('search filtering', () => {
        it('filters rows by name', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by name, id, or description…'), { target: { value: 'remote' } });
            expect(screen.queryByText('Remote Codes')).not.toBeNull();
            expect(screen.queryByText('Countries')).toBeNull();
        });

        it('filters rows by id', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by name, id, or description…'), { target: { value: 'status-map' } });
            expect(screen.queryByText('Status Map')).not.toBeNull();
            expect(screen.queryByText('Countries')).toBeNull();
        });

        it('filters rows by description', () => {
            renderTable({
                dictionaries: [
                    { id: '1', name: 'Countries', description: 'ISO country lookup', type: 'MANUAL', state: 'STOPPED' },
                    { id: '2', name: 'Remote Codes', description: 'Partner status codes', type: 'DYNAMIC', state: 'STARTED' },
                ],
            });
            fireEvent.change(screen.getByPlaceholderText('Search by name, id, or description…'), { target: { value: 'partner' } });
            expect(screen.queryByText('Remote Codes')).not.toBeNull();
            expect(screen.queryByText('Countries')).toBeNull();
        });

        it('shows the no-match message when search has no results', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by name, id, or description…'), { target: { value: 'zzznomatch' } });
            expect(screen.queryByText('No dictionaries match your search.')).not.toBeNull();
        });

        it('is case-insensitive', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by name, id, or description…'), { target: { value: 'COUNTRIES' } });
            expect(screen.queryByText('Countries')).not.toBeNull();
        });
    });

    describe('permissions', () => {
        it('always shows the actions menu so View Details remains available', () => {
            renderTable({ canEdit: false, canDelete: false });
            expect(screen.getAllByRole('button', { name: 'Dictionary actions' }).length).toBe(DICTIONARIES.length);
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
