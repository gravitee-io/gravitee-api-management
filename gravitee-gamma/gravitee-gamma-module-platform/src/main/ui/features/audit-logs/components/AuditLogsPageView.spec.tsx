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

import { render, screen } from '@testing-library/react';

import { AuditLogsPageView, type AuditLogsPageViewProps } from './AuditLogsPageView';

function stubState(): AuditLogsPageViewProps['state'] {
    return {
        params: { page: 1, size: 10 },
        page: 1,
        setPage: jest.fn(),
        pageSize: 10,
        setPageSize: jest.fn(),
        onPageSizeChange: jest.fn(),
        event: '',
        onEventChange: jest.fn(),
        referenceType: '',
        onReferenceTypeChange: jest.fn(),
        environmentId: '',
        onEnvironmentIdChange: jest.fn(),
        applicationId: '',
        onApplicationIdChange: jest.fn(),
        apiId: '',
        onApiIdChange: jest.fn(),
        datePreset: '',
        onDatePresetChange: jest.fn(),
        customRange: undefined,
        onCustomRangeChange: jest.fn(),
        selected: null,
        setSelected: jest.fn(),
        exportOpen: false,
        setExportOpen: jest.fn(),
        exporting: false,
        handleReset: jest.fn(),
        handleExport: jest.fn(),
        hasActiveFilters: false,
    };
}

function renderView(overrides: Partial<AuditLogsPageViewProps> = {}) {
    return render(
        <AuditLogsPageView
            scope="organization"
            description="Search configuration changes across the organization."
            state={stubState()}
            rows={[]}
            totalCount={0}
            loading={false}
            isError={false}
            eventTypes={['API_UPDATED']}
            applications={[]}
            apis={[]}
            {...overrides}
        />,
    );
}

describe('AuditLogsPageView', () => {
    it('keeps heading, filters, and table chrome when the query fails', () => {
        renderView({ isError: true });

        expect(screen.getByRole('heading', { name: 'Audit' })).not.toBeNull();
        expect(screen.getByText('Search configuration changes across the organization.')).not.toBeNull();
        expect(screen.getByText('Failed to load audit logs. Please try again.')).not.toBeNull();
        expect(screen.getByLabelText('Filter by event type')).not.toBeNull();
        expect(screen.getByLabelText('Filter by type')).not.toBeNull();
        expect(screen.getByLabelText('Filter by time period')).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Export' })).not.toBeNull();
        expect(screen.queryByText('No audit logs')).toBeNull();
        expect(screen.queryByText('Try adjusting or clearing your filters.')).toBeNull();
    });
});
