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

import { dataTableHarness } from '@gravitee/graphene-core/testing';
import { render, screen } from '@testing-library/react';
import type { ComponentProps } from 'react';

import { IntegrationsTable } from './IntegrationsTable';
import type { Integration, IntegrationAgentStatus } from '../types/integration';
import { TABLE_PAGE_SIZE_OPTIONS } from '../utils/paginationConstants';

const INTEGRATIONS: Integration[] = [
    { id: 'int-1', name: 'Acme Gateway', provider: 'aws-api-gateway' },
    { id: 'int-2', name: 'Broker North', provider: 'solace' },
    { id: 'int-3', name: 'Partner Apigee', provider: 'apigee' },
];

const PROVIDER_LABEL_BY_TOKEN: [token: string, label: string][] = [
    ['A2A', 'A2A Protocol'],
    ['aws-api-gateway', 'AWS API Gateway'],
    ['solace', 'Solace'],
    ['apigee', 'Apigee'],
    ['azure-api-management', 'Azure API Management'],
    ['ibm-api-connect', 'IBM API Connect'],
    ['confluent-platform', 'Confluent Platform'],
    ['mulesoft', 'MuleSoft'],
    ['edge-stack', 'Edge Stack'],
];
const SUPPORTED_PROVIDER_TOKENS = PROVIDER_LABEL_BY_TOKEN.map(([token]) => token);
const UNMAPPED_PROVIDER_TOKEN = 'kong';
const OBJECT_PROTOTYPE_MEMBER_PROVIDER_TOKEN = 'constructor';
// The v2 DTO declares agentStatus nullable, so the wire can send an explicit null that the optional field type cannot express.
const WIRE_NULL_AGENT_STATUS = null as unknown as IntegrationAgentStatus;

const A2A_INTEGRATIONS_WITHOUT_AGENT_STATUS: [description: string, integration: Integration][] = [
    ['omits the agentStatus key', { id: 'int-a2a', name: 'Agent Bridge', provider: 'A2A' }],
    ['sets agentStatus to undefined', { id: 'int-a2a', name: 'Agent Bridge', provider: 'A2A', agentStatus: undefined }],
    ['sets agentStatus to null', { id: 'int-a2a', name: 'Agent Bridge', provider: 'A2A', agentStatus: WIRE_NULL_AGENT_STATUS }],
];

const PAGINATED_TOTAL_COUNT = 23;
const PAGE_SIZES_AT_OR_ABOVE_TOTAL_COUNT = TABLE_PAGE_SIZE_OPTIONS.filter(size => size >= PAGINATED_TOTAL_COUNT);

function renderTable(overrides: Partial<ComponentProps<typeof IntegrationsTable>> = {}) {
    return render(
        <IntegrationsTable
            integrations={INTEGRATIONS}
            totalCount={INTEGRATIONS.length}
            page={1}
            pageSize={10}
            loading={false}
            onPageChange={jest.fn()}
            onPageSizeChange={jest.fn()}
            {...overrides}
        />,
    );
}

function integrationsTable() {
    return dataTableHarness({ within: screen.getByRole('region', { name: 'Integrations' }) });
}

describe('IntegrationsTable', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    it('renders one row per integration', () => {
        renderTable();

        expect(integrationsTable().getRowCount()).toBe(INTEGRATIONS.length);
    });

    it('renders a row for every provider token, including one it has no label for', () => {
        const integrations = [...SUPPORTED_PROVIDER_TOKENS, UNMAPPED_PROVIDER_TOKEN].map(provider => ({
            id: `int-${provider}`,
            name: `Integration ${provider}`,
            provider,
        }));

        renderTable({ integrations, totalCount: integrations.length });

        const names = integrationsTable()
            .getRows()
            .map(row => row.getCellText('Name'));

        expect(names).toEqual(integrations.map(integration => integration.name));
    });

    it('renders exactly the Name, Provider and Status columns', () => {
        renderTable();

        expect(integrationsTable().getHeaders()).toEqual(['Name', 'Provider', 'Status']);
    });

    it("renders each integration's name in its own Name cell", () => {
        renderTable();

        const names = integrationsTable()
            .getRows()
            .map(row => row.getCellText('Name'));

        expect(names).toEqual(['Acme Gateway', 'Broker North', 'Partner Apigee']);
    });

    it('renders every name as plain text, offering nothing to navigate into', () => {
        renderTable();

        const nameCells = integrationsTable()
            .getRows()
            .map(row => row.getCellElement('Name'));

        expect(nameCells).toHaveLength(INTEGRATIONS.length);
        nameCells.forEach(nameCell => {
            expect(nameCell.querySelector('a, button, [role="link"], [role="button"]')).toBeNull();
        });
    });

    it("renders each supported provider's display label in its own Provider cell", () => {
        const integrations = PROVIDER_LABEL_BY_TOKEN.map(([provider]) => ({
            id: `int-${provider}`,
            name: `Integration ${provider}`,
            provider,
        }));

        renderTable({ integrations, totalCount: integrations.length });

        const labels = integrations.map(({ name }) => integrationsTable().getRow(name).getCellText('Provider'));

        expect(labels).toEqual(PROVIDER_LABEL_BY_TOKEN.map(([, label]) => label));
    });

    it('renders the mapped label rather than the bare token in the Provider cell of an A2A integration', () => {
        const integrations = [{ id: 'int-a2a', name: 'Agent Bridge', provider: 'A2A' }];

        renderTable({ integrations, totalCount: integrations.length });

        expect(integrationsTable().getRow('Agent Bridge').getCellText('Provider')).toBe('A2A Protocol');
    });

    it.each([UNMAPPED_PROVIDER_TOKEN, OBJECT_PROTOTYPE_MEMBER_PROVIDER_TOKEN])(
        'falls back to the raw token in the Provider cell of %s, a token it has no label for',
        provider => {
            const integrations = [{ id: 'int-unmapped', name: 'Legacy Gateway', provider }];

            renderTable({ integrations, totalCount: integrations.length });

            expect(integrationsTable().getRow('Legacy Gateway').getCellText('Provider')).toBe(provider);
        },
    );

    it.each([UNMAPPED_PROVIDER_TOKEN, OBJECT_PROTOTYPE_MEMBER_PROVIDER_TOKEN])(
        'warns once, naming %s, rather than falling back to the raw token in the Provider cell in silence',
        provider => {
            const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
            const integrations = [{ id: 'int-unmapped', name: 'Legacy Gateway', provider }];

            renderTable({ integrations, totalCount: integrations.length });

            expect(warn).toHaveBeenCalledTimes(1);
            expect(warn).toHaveBeenCalledWith(expect.stringContaining(provider));
            warn.mockRestore();
        },
    );

    it('stays quiet while every provider on screen is one it has a label for', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const integrations = SUPPORTED_PROVIDER_TOKENS.map(provider => ({
            id: `int-${provider}`,
            name: `Integration ${provider}`,
            provider,
        }));

        renderTable({ integrations, totalCount: integrations.length });

        expect(warn).not.toHaveBeenCalled();
        warn.mockRestore();
    });

    it.each([
        ['CONNECTED', 'Connected'],
        ['DISCONNECTED', 'Disconnected'],
    ] as const)("renders agent status %s as a badge reading '%s' in that integration's Status cell", (agentStatus, label) => {
        const integrations = [{ id: 'int-status', name: 'Status Bearer', provider: 'solace', agentStatus }];

        renderTable({ integrations, totalCount: integrations.length });

        const row = integrationsTable().getRow('Status Bearer');

        expect(row.getCellElement('Status').querySelector('[data-slot="badge"]')?.textContent).toBe(label);
        expect(row.getCellText('Status')).toBe(label);
    });

    it("derives each row's Status cell from that row's own agent status when both statuses are on screen", () => {
        const integrations: Integration[] = [
            { id: 'int-connected', name: 'Connected Bearer', provider: 'solace', agentStatus: 'CONNECTED' },
            { id: 'int-disconnected', name: 'Disconnected Bearer', provider: 'apigee', agentStatus: 'DISCONNECTED' },
        ];

        renderTable({ integrations, totalCount: integrations.length });

        expect(integrationsTable().getRow('Disconnected Bearer').getCellText('Status')).not.toContain('Connected');
        expect(integrationsTable().getRow('Connected Bearer').getCellText('Status')).toBe('Connected');
    });

    it.each(A2A_INTEGRATIONS_WITHOUT_AGENT_STATUS)(
        'renders an empty Status cell, and a row that is otherwise intact, for an A2A integration that %s',
        (_description, integration) => {
            renderTable({ integrations: [integration], totalCount: 1 });

            const row = integrationsTable().getRow('Agent Bridge');

            expect(row.getCellText('Status')).toBe('');
            expect(row.getCellElement('Status').querySelector('[data-slot="badge"]')).toBeNull();
            expect(row.getCellText('Name')).toBe('Agent Bridge');
            expect(row.getCellText('Provider')).toBe('A2A Protocol');
        },
    );

    it("leaves a status-less row's Status cell empty while its neighbors on both sides render badges", () => {
        const integrations: Integration[] = [
            { id: 'int-connected', name: 'Connected Bearer', provider: 'solace', agentStatus: 'CONNECTED' },
            { id: 'int-a2a', name: 'Agent Bridge', provider: 'A2A' },
            { id: 'int-disconnected', name: 'Disconnected Bearer', provider: 'apigee', agentStatus: 'DISCONNECTED' },
        ];

        renderTable({ integrations, totalCount: integrations.length });

        const statusLessRow = integrationsTable().getRow('Agent Bridge');

        expect(statusLessRow.getCellText('Status')).toBe('');
        expect(statusLessRow.getCellElement('Status').querySelector('[data-slot="badge"]')).toBeNull();
        expect(integrationsTable().getRow('Connected Bearer').getCellText('Status')).toBe('Connected');
        expect(integrationsTable().getRow('Disconnected Bearer').getCellText('Status')).toBe('Disconnected');
    });

    it.each([
        [3, 10],
        [10, 10],
    ])('hides pagination when totalCount %i fits within page size %i', (totalCount, pageSize) => {
        renderTable({ totalCount, pageSize });

        expect(screen.queryByRole('combobox', { name: 'Items per page' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Previous page' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Next page' })).toBeNull();
    });

    it.each(PAGE_SIZES_AT_OR_ABOVE_TOTAL_COUNT)(
        'keeps the pagination footer reachable after page size %i is enlarged to at least the total count',
        pageSize => {
            renderTable({ totalCount: PAGINATED_TOTAL_COUNT, pageSize });

            expect(screen.getByRole('combobox', { name: 'Items per page' }).textContent).toBe(String(pageSize));
            expect(screen.getByRole('button', { name: 'Previous page' })).not.toBeNull();
            expect(screen.getByRole('button', { name: 'Next page' })).not.toBeNull();
        },
    );

    it('shows the current page size in the items per page control', () => {
        renderTable({ totalCount: 23, pageSize: 10 });

        expect(screen.getByRole('combobox', { name: 'Items per page' }).textContent).toBe('10');
    });

    it('shows the server-reported total in the result-count indicator', () => {
        renderTable({ page: 1, pageSize: 10, totalCount: 23 });

        expect(screen.getByText('1-10 of 23')).not.toBeNull();
    });

    it('navigates pages through the shared previous and next arrow buttons', () => {
        renderTable({ page: 1, pageSize: 10, totalCount: 23 });

        expect(screen.getByRole('button', { name: 'Previous page' }).hasAttribute('disabled')).toBe(true);
        expect(screen.getByRole('button', { name: 'Next page' }).hasAttribute('disabled')).toBe(false);
    });
});
