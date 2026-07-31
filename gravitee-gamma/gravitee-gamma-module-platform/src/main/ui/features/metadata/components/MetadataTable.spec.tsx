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

import { MetadataTable } from './MetadataTable';
import type { Metadata } from '../types/metadata';

const METADATA: Metadata[] = [
    { key: 'support-email', name: 'Support Email', format: 'MAIL', value: 'help@example.com' },
    { key: 'api-version', name: 'API Version', format: 'STRING', value: '1.0' },
    { key: 'max-retries', name: 'Max Retries', format: 'NUMERIC', value: '3' },
];

function renderTable(overrides: Partial<{ canEdit: boolean; canDelete: boolean; metadata: Metadata[] }> = {}) {
    const onEdit = jest.fn();
    const onDelete = jest.fn();
    render(
        <MetadataTable
            metadata={overrides.metadata ?? METADATA}
            canEdit={overrides.canEdit ?? true}
            canDelete={overrides.canDelete ?? true}
            onEdit={onEdit}
            onDelete={onDelete}
        />,
    );
    return { onEdit, onDelete };
}

describe('MetadataTable', () => {
    describe('rendering', () => {
        it('renders all metadata rows', () => {
            renderTable();
            expect(screen.queryByText('Support Email')).not.toBeNull();
            expect(screen.queryByText('API Version')).not.toBeNull();
            expect(screen.queryByText('Max Retries')).not.toBeNull();
        });

        it('shows the empty message when there is no metadata', () => {
            renderTable({ metadata: [] });
            expect(screen.queryByText('No global metadata defined for this environment.')).not.toBeNull();
        });

        it('renders "Value" as the column header (not "Default Value")', () => {
            renderTable();
            expect(screen.queryByRole('columnheader', { name: 'Value' })).not.toBeNull();
            expect(screen.queryByRole('columnheader', { name: 'Default Value' })).toBeNull();
        });
    });

    describe('search filtering', () => {
        it('renders a search input with the expected placeholder', () => {
            renderTable();
            const search = screen.getByPlaceholderText('Search by key or name…') as HTMLInputElement;
            expect(search).not.toBeNull();
            expect(search.tagName).toBe('INPUT');
        });

        it('filters rows by name', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by key or name…'), { target: { value: 'version' } });
            expect(screen.queryByText('API Version')).not.toBeNull();
            expect(screen.queryByText('Support Email')).toBeNull();
        });

        it('filters rows by key', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by key or name…'), { target: { value: 'max-retries' } });
            expect(screen.queryByText('Max Retries')).not.toBeNull();
            expect(screen.queryByText('Support Email')).toBeNull();
        });

        it('shows the no-match message when search has no results', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by key or name…'), { target: { value: 'zzznomatch' } });
            expect(screen.queryByText('No metadata matches your search.')).not.toBeNull();
        });

        it('is case-insensitive', () => {
            renderTable();
            fireEvent.change(screen.getByPlaceholderText('Search by key or name…'), { target: { value: 'SUPPORT' } });
            expect(screen.queryByText('Support Email')).not.toBeNull();
        });
    });

    describe('permissions', () => {
        it('hides the actions column when both canEdit and canDelete are false', () => {
            renderTable({ canEdit: false, canDelete: false });
            expect(screen.queryByRole('button', { name: 'Metadata actions' })).toBeNull();
        });

        it('shows one action button per row when canEdit is true', () => {
            renderTable({ canEdit: true, canDelete: false });
            expect(screen.getAllByRole('button', { name: 'Metadata actions' }).length).toBe(METADATA.length);
        });

        it('shows one action button per row when canDelete is true', () => {
            renderTable({ canEdit: false, canDelete: true });
            expect(screen.getAllByRole('button', { name: 'Metadata actions' }).length).toBe(METADATA.length);
        });

        it('shows one action button per row when both canEdit and canDelete are true', () => {
            renderTable();
            expect(screen.getAllByRole('button', { name: 'Metadata actions' }).length).toBe(METADATA.length);
        });
    });
});
