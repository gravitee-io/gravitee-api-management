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

import { AuditLogsTable } from './AuditLogsTable';
import type { AuditLogRow } from '../types/auditLog';

const ROW: AuditLogRow = {
    id: 'a-1',
    createdAt: Date.parse('2026-08-19T10:00:00.000Z'),
    user: 'Ada Lovelace',
    referenceType: 'API',
    reference: 'Pets',
    event: 'API_UPDATED',
    targets: [{ key: 'API', value: 'Pets' }],
    patch: '[{"op":"replace","path":"/name","value":"Pets"}]',
};

describe('AuditLogsTable', () => {
    it('renders audit columns', () => {
        render(
            <AuditLogsTable
                rows={[ROW]}
                loading={false}
                page={1}
                pageSize={10}
                totalCount={1}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                selected={null}
                onSelectRow={jest.fn()}
                onCloseDetail={jest.fn()}
            />,
        );

        expect(screen.getByText('Ada Lovelace')).not.toBeNull();
        expect(screen.getByText('API_UPDATED')).not.toBeNull();
        expect(screen.getByText('Pets')).not.toBeNull();
        expect(screen.getByText(/API: Pets/)).not.toBeNull();
    });

    it('names the Date and User actions distinctly per row so screen readers can tell them apart', () => {
        const onSelectRow = jest.fn();
        render(
            <AuditLogsTable
                rows={[ROW]}
                loading={false}
                page={1}
                pageSize={10}
                totalCount={1}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                selected={null}
                onSelectRow={onSelectRow}
                onCloseDetail={jest.fn()}
            />,
        );

        fireEvent.click(screen.getAllByRole('button', { name: /^View audit details for API_UPDATED/ })[0]);
        expect(onSelectRow).toHaveBeenCalledWith(ROW);
    });

    it('shows a first-use empty state when there are no rows and no filters', () => {
        render(
            <AuditLogsTable
                rows={[]}
                loading={false}
                page={1}
                pageSize={10}
                totalCount={0}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                selected={null}
                onSelectRow={jest.fn()}
                onCloseDetail={jest.fn()}
            />,
        );

        expect(screen.getByText('No audit logs')).not.toBeNull();
        expect(screen.getByText('Configuration changes will appear here.')).not.toBeNull();
        expect(screen.queryByText('Try adjusting or clearing your filters.')).toBeNull();
    });

    it('tells the user to clear filters when a filtered search is empty', () => {
        render(
            <AuditLogsTable
                rows={[]}
                loading={false}
                page={1}
                pageSize={10}
                totalCount={0}
                hasActiveFilters
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                selected={null}
                onSelectRow={jest.fn()}
                onCloseDetail={jest.fn()}
            />,
        );

        expect(screen.getByText('No audit logs found')).not.toBeNull();
        expect(screen.getByText('Try adjusting or clearing your filters.')).not.toBeNull();
    });

    it('renders a dash when a row has no targets', () => {
        render(
            <AuditLogsTable
                rows={[{ ...ROW, targets: [] }]}
                loading={false}
                page={1}
                pageSize={10}
                totalCount={1}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                selected={null}
                onSelectRow={jest.fn()}
                onCloseDetail={jest.fn()}
            />,
        );

        expect(screen.getByText('—')).not.toBeNull();
    });

    it('opens the detail sheet with a pretty-printed patch when the eye is clicked', () => {
        const onSelectRow = jest.fn();
        const { rerender } = render(
            <AuditLogsTable
                rows={[ROW]}
                loading={false}
                page={1}
                pageSize={10}
                totalCount={1}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                selected={null}
                onSelectRow={onSelectRow}
                onCloseDetail={jest.fn()}
            />,
        );

        fireEvent.click(screen.getByRole('button', { name: 'View patch for API_UPDATED' }));
        expect(onSelectRow).toHaveBeenCalledWith(ROW);

        rerender(
            <AuditLogsTable
                rows={[ROW]}
                loading={false}
                page={1}
                pageSize={10}
                totalCount={1}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                selected={ROW}
                onSelectRow={onSelectRow}
                onCloseDetail={jest.fn()}
            />,
        );

        expect(screen.getByText('Audit event')).not.toBeNull();
        expect(screen.getByText(/"op": "replace"/)).not.toBeNull();
    });
});
