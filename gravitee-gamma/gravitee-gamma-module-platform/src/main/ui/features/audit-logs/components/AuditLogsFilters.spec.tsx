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

import { AuditLogsFilters, ENV_AUDIT_REFERENCE_TYPES, ORG_AUDIT_REFERENCE_TYPES } from './AuditLogsFilters';
import type { AuditLogsFiltersProps } from './AuditLogsFilters';

function renderFilters(overrides: Partial<AuditLogsFiltersProps> = {}) {
    const props: AuditLogsFiltersProps = {
        scope: 'organization',
        eventTypes: ['API_CREATED', 'USER_UPDATED'],
        event: '',
        onEventChange: jest.fn(),
        referenceType: '',
        onReferenceTypeChange: jest.fn(),
        environments: [{ id: 'env-1', name: 'Production' }],
        environmentId: '',
        onEnvironmentIdChange: jest.fn(),
        applications: [],
        applicationId: '',
        onApplicationIdChange: jest.fn(),
        apis: [],
        apiId: '',
        onApiIdChange: jest.fn(),
        datePreset: '',
        onDatePresetChange: jest.fn(),
        customRange: undefined,
        onCustomRangeChange: jest.fn(),
        onReset: jest.fn(),
        onExport: jest.fn(),
        ...overrides,
    };
    return { ...render(<AuditLogsFilters {...props} />), props };
}

describe('AuditLogsFilters', () => {
    it('renders event, type, date, and export controls', () => {
        renderFilters();
        expect(screen.getByLabelText('Filter by event type')).not.toBeNull();
        expect(screen.getByLabelText('Filter by type')).not.toBeNull();
        expect(screen.getByLabelText('Filter by time period')).not.toBeNull();
        expect(screen.getByLabelText('Filter by date range')).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Export' })).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Reset' })).toBeNull();
    });

    it('offers ORGANIZATION and ENVIRONMENT types only on the organization page', () => {
        expect(ORG_AUDIT_REFERENCE_TYPES).toEqual(['ORGANIZATION', 'ENVIRONMENT', 'APPLICATION', 'API']);
        expect(ENV_AUDIT_REFERENCE_TYPES).toEqual(['APPLICATION', 'API']);
    });

    it('shows an environment picker when type is ENVIRONMENT', () => {
        renderFilters({ referenceType: 'ENVIRONMENT' });
        expect(screen.getByLabelText('Filter by environment')).not.toBeNull();
    });

    it('calls onReset and onExport', () => {
        const { props } = renderFilters({ event: 'API_CREATED' });
        fireEvent.click(screen.getByRole('button', { name: 'Reset' }));
        fireEvent.click(screen.getByRole('button', { name: 'Export' }));
        expect(props.onReset).toHaveBeenCalled();
        expect(props.onExport).toHaveBeenCalled();
    });

    it('emits the selected relative time preset', () => {
        const { props } = renderFilters();
        fireEvent.click(screen.getByLabelText('Filter by time period'));
        fireEvent.click(screen.getByRole('option', { name: 'Last 24 hours' }));
        expect(props.onDatePresetChange).toHaveBeenCalledWith('24h');
    });

    it('always shows the date range picker so a calendar range can be applied', () => {
        renderFilters();
        expect(document.querySelector('[data-slot="date-range-picker"]')).not.toBeNull();
    });
});
