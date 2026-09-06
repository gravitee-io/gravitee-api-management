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
import { useState } from 'react';

import { ApiMetadataTable } from './ApiMetadataTable';
import type { ApiMetadata, MetadataSource } from '../../../types/metadata';

const API_ONLY: ApiMetadata = { key: 'team', name: 'Team', format: 'STRING', value: 'Platform Engineering' };
const GLOBAL_INHERITED: ApiMetadata = {
    key: 'support-email',
    name: 'Support Email',
    format: 'MAIL',
    defaultValue: 'help@example.com',
};
const GLOBAL_OVERRIDE: ApiMetadata = {
    key: 'docs-url',
    name: 'Documentation URL',
    format: 'URL',
    value: 'https://docs.example.com',
    defaultValue: 'https://docs.acme.io',
};

function renderTable(
    overrides: Partial<{
        metadata: ApiMetadata[];
        canCreate: boolean;
        canEdit: boolean;
        canDelete: boolean;
        readOnly: boolean;
        source: 'GLOBAL' | 'API';
        totalCount: number;
    }> = {},
) {
    const onCreate = jest.fn();
    const onEdit = jest.fn();
    const onDelete = jest.fn();
    const onResetFilters = jest.fn();
    const onSourceChange = jest.fn();
    const metadata = overrides.metadata ?? [API_ONLY, GLOBAL_INHERITED, GLOBAL_OVERRIDE];
    render(
        <ApiMetadataTable
            metadata={metadata}
            totalCount={overrides.totalCount ?? metadata.length}
            page={1}
            pageSize={10}
            sorting={[]}
            source={overrides.source}
            isLoading={false}
            canCreate={overrides.canCreate ?? true}
            canEdit={overrides.canEdit ?? true}
            canDelete={overrides.canDelete ?? true}
            readOnly={overrides.readOnly ?? false}
            isMutating={false}
            onPageChange={jest.fn()}
            onPageSizeChange={jest.fn()}
            onSortingChange={jest.fn()}
            onSourceChange={onSourceChange}
            onResetFilters={onResetFilters}
            onCreate={onCreate}
            onEdit={onEdit}
            onDelete={onDelete}
        />,
    );
    return { onCreate, onEdit, onDelete, onResetFilters, onSourceChange };
}

describe('ApiMetadataTable', () => {
    it('renders key, name, format, and value columns', () => {
        renderTable();
        expect(screen.queryByRole('columnheader', { name: 'Key' })).not.toBeNull();
        expect(screen.queryByRole('columnheader', { name: 'Name' })).not.toBeNull();
        expect(screen.queryByRole('columnheader', { name: 'Format' })).not.toBeNull();
        expect(screen.queryByRole('columnheader', { name: 'Value' })).not.toBeNull();
        expect(screen.queryByText('Team')).not.toBeNull();
        expect(screen.queryByText('Platform Engineering')).not.toBeNull();
    });

    it('shows a Global badge for inherited metadata and falls back to the default value', () => {
        renderTable();
        expect(screen.getAllByText('Global').length).toBe(2);
        expect(screen.queryByText('help@example.com')).not.toBeNull();
    });

    it('renders the source filter without Reset when no source is selected', () => {
        renderTable();
        expect(screen.queryByRole('combobox', { name: 'Filter by source' })).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Reset filters' })).toBeNull();
    });

    it('shows Reset filters only when a source is selected', () => {
        renderTable({ source: 'API' });
        expect(screen.queryByRole('button', { name: 'Reset filters' })).not.toBeNull();
    });

    it('invokes onResetFilters from the toolbar button', () => {
        const { onResetFilters } = renderTable({ source: 'API' });
        fireEvent.click(screen.getByRole('button', { name: 'Reset filters' }));
        expect(onResetFilters).toHaveBeenCalledTimes(1);
    });

    it('clears the selected source when Reset filters is clicked', () => {
        function Harness() {
            const [source, setSource] = useState<MetadataSource | undefined>('API');
            return (
                <ApiMetadataTable
                    metadata={[API_ONLY]}
                    totalCount={1}
                    page={1}
                    pageSize={10}
                    sorting={[]}
                    source={source}
                    isLoading={false}
                    canCreate={false}
                    canEdit={false}
                    canDelete={false}
                    readOnly={false}
                    isMutating={false}
                    onPageChange={jest.fn()}
                    onPageSizeChange={jest.fn()}
                    onSortingChange={jest.fn()}
                    onSourceChange={setSource}
                    onResetFilters={() => setSource(undefined)}
                    onCreate={jest.fn()}
                    onEdit={jest.fn()}
                    onDelete={jest.fn()}
                />
            );
        }

        render(<Harness />);
        expect(screen.getByRole('combobox', { name: 'Filter by source' })).toHaveTextContent('API');
        fireEvent.click(screen.getByRole('button', { name: 'Reset filters' }));
        expect(screen.getByRole('combobox', { name: 'Filter by source' })).toHaveTextContent('All sources');
        expect(screen.queryByRole('button', { name: 'Reset filters' })).toBeNull();
    });

    it('hides mutating actions for inherited rows without an API override', () => {
        renderTable({ metadata: [GLOBAL_INHERITED], canEdit: false, canDelete: true });
        expect(screen.queryByRole('button', { name: /delete/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /reset .* metadata/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /actions for/i })).toBeNull();
    });

    it('uses a reset action for inherited metadata that has an API override', () => {
        renderTable({ metadata: [GLOBAL_OVERRIDE], canEdit: false, canDelete: true });
        expect(screen.queryByRole('button', { name: /reset documentation url metadata/i })).not.toBeNull();
    });

    it('hides the actions column when the user cannot edit or delete', () => {
        renderTable({ canEdit: false, canDelete: false });
        expect(screen.queryByRole('button', { name: /actions for/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /edit/i })).toBeNull();
    });

    it('hides mutating actions when the API is Kubernetes-managed even if the user can edit', () => {
        renderTable({ canEdit: true, canDelete: true, readOnly: true });
        expect(screen.queryByRole('button', { name: /edit .* metadata/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /delete/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /reset .* metadata/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /actions for/i })).toBeNull();
    });

    it('shows a first-use empty state when there is no metadata and no source filter', () => {
        const { onCreate } = renderTable({ metadata: [], totalCount: 0 });
        expect(screen.queryByText('No API metadata')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: /add api metadata/i }));
        expect(onCreate).toHaveBeenCalledTimes(1);
        expect(screen.queryByRole('combobox', { name: 'Filter by source' })).toBeNull();
    });

    it('shows a no-results empty state when a source filter matches nothing', () => {
        renderTable({ metadata: [], source: 'API', totalCount: 0 });
        expect(screen.queryByText('No metadata matches the selected source filter.')).not.toBeNull();
    });
});
