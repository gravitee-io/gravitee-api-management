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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, fireEvent, screen, waitFor, within } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import type { EntityResponse, PolicyResponse, PolicyStatus } from '../../../shared/api/authz-api.types';
import { buildServiceResourceOptions, ServicePolicyPage, type ServicePageConfig } from '../ServicePolicyPage';

// jsdom doesn't implement scrollIntoView; Radix Select calls it on item activation.
beforeAll(() => {
    if (!Element.prototype.scrollIntoView) {
        Element.prototype.scrollIntoView = () => undefined;
    }
});

const listPoliciesSpy = vi.fn();
const deletePolicySpy = vi.fn();

vi.mock('@gravitee/gamma-modules-sdk', async importOriginal => ({
    ...(await importOriginal<object>()),
    useEnvironment: () => ({ id: 'DEFAULT' }),
}));

vi.mock('../../../shared/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        listPolicies: (env: string, params?: unknown) => listPoliciesSpy(env, params),
        deletePolicy: (env: string, id: string) => deletePolicySpy(env, id),
    },
}));

function makePolicy(overrides: Partial<PolicyResponse> = {}): PolicyResponse {
    return {
        id: overrides.id ?? `pol-${Math.random().toString(36).slice(2, 8)}`,
        environmentId: 'DEFAULT',
        name: 'Allow read',
        description: null,
        policyText: '',
        type: 'MCP',
        target: null,
        status: 'DRAFT' as PolicyStatus,
        createdAt: '2026-04-27T10:00:00.000Z',
        updatedAt: '2026-04-27T10:00:00.000Z',
        ...overrides,
    };
}

const mcpConfig: ServicePageConfig = {
    type: 'MCP',
    title: 'MCP Policies',
    description: 'Restrict MCPs.',
    createButtonLabel: 'Create',
    searchPlaceholder: 'Search MCP policies',
    hasTarget: true,
    serviceLabel: 'MCP',
};

function renderPage(config = mcpConfig) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    return render(<ServicePolicyPage config={config} />, { wrapper });
}

beforeEach(() => {
    listPoliciesSpy.mockReset();
    deletePolicySpy.mockReset();
});

describe('ServicePolicyPage', () => {
    it('renders the empty landing when the backend returns zero policies', async () => {
        listPoliciesSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        renderPage();

        await waitFor(() => {
            expect(screen.getByText('No policies yet')).toBeInTheDocument();
        });
    });

    it('renders rows when policies are available and shows KPI counts', async () => {
        listPoliciesSpy.mockResolvedValue({
            data: [makePolicy({ id: '1', name: 'A', status: 'DEPLOYED' }), makePolicy({ id: '2', name: 'B', status: 'DRAFT' })],
            total: 2,
            page: 1,
            perPage: 10,
        });
        renderPage();

        await waitFor(() => {
            expect(screen.getByTestId('row-policy-1-name')).toBeInTheDocument();
            expect(screen.getByTestId('row-policy-2-name')).toBeInTheDocument();
        });
    });

    it('passes the status filter through to the backend query', async () => {
        listPoliciesSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        renderPage();

        await waitFor(() => expect(listPoliciesSpy).toHaveBeenCalled());
        listPoliciesSpy.mockClear();

        const statusFilter = screen.getByLabelText('Filter by status');
        fireEvent.click(statusFilter);
        const option = await screen.findByRole('option', { name: 'Deployed' });
        fireEvent.click(option);

        await waitFor(() => {
            expect(listPoliciesSpy).toHaveBeenCalledWith('DEFAULT', expect.objectContaining({ status: 'DEPLOYED', type: 'MCP' }));
        });
    });

    it('shows the "no matches" empty state when search filters out all rows', async () => {
        listPoliciesSpy.mockResolvedValue({
            data: [makePolicy({ id: '1', name: 'Only-one' })],
            total: 1,
            page: 1,
            perPage: 10,
        });
        renderPage();

        await waitFor(() => expect(screen.getByTestId('row-policy-1-name')).toBeInTheDocument());

        fireEvent.change(screen.getByLabelText('Search policies'), { target: { value: 'no-such-thing' } });

        await waitFor(() => expect(screen.getByText('No policies match your filters')).toBeInTheDocument());
    });

    it('opens the delete confirmation when the delete row action is clicked', async () => {
        const policy = makePolicy({ id: 'p1', name: 'Audit-log' });
        listPoliciesSpy.mockResolvedValue({ data: [policy], total: 1, page: 1, perPage: 10 });
        renderPage();

        await waitFor(() => expect(screen.getByTestId('row-policy-p1-name')).toBeInTheDocument());

        fireEvent.click(screen.getByRole('button', { name: 'Delete Audit-log' }));

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText(/will be permanently removed/)).toBeInTheDocument();
    });

    it('omits the Unique-targets KPI for services without targets (CUSTOM)', async () => {
        listPoliciesSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        renderPage({ ...mcpConfig, type: 'CUSTOM', hasTarget: false });

        await waitFor(() => expect(listPoliciesSpy).toHaveBeenCalled());
        expect(screen.queryByLabelText('Unique targets')).not.toBeInTheDocument();
    });
});

function resourceEntity(uid: string, attributes: Record<string, unknown> = {}): EntityResponse {
    return {
        id: `id-${uid}`,
        environmentId: 'DEFAULT',
        uid,
        attributes,
        parents: [],
        createdAt: new Date(0).toISOString(),
        updatedAt: new Date(0).toISOString(),
    };
}

describe('buildServiceResourceOptions', () => {
    it('excludes api/mcp/model/llm/action prefixes for custom policies', () => {
        const entities = [
            resourceEntity('api.orders'),
            resourceEntity('mcp.files'),
            resourceEntity('model.gpt'),
            resourceEntity('llm.claude'),
            resourceEntity('action.read'),
            resourceEntity('document.contract'),
        ];

        const options = buildServiceResourceOptions(entities, { hasTarget: false, type: 'CUSTOM' });

        expect(options.map(o => o.id)).toEqual(['Resource::"contract"']);
        expect(options[0].group).toBe('Resource');
        expect(options[0].label).toBe('contract');
    });

    it('keeps bare-id resources for custom policies', () => {
        const entities = [resourceEntity('inventory'), resourceEntity('api.orders')];

        const options = buildServiceResourceOptions(entities, { hasTarget: false, type: 'CUSTOM' });

        expect(options).toHaveLength(1);
        expect(options[0].id).toBe('Resource::"inventory"');
        expect(options[0].group).toBe('Resource');
    });

    it('scopes to the service prefix and groups by segment for targeted policies', () => {
        const entities = [
            resourceEntity('mcp.files', { displayName: 'Files MCP' }),
            resourceEntity('mcp.search'),
            resourceEntity('api.orders'),
        ];

        const options = buildServiceResourceOptions(entities, { hasTarget: true, type: 'MCP' });

        expect(options.map(o => o.id)).toEqual(['MCP::"files"', 'MCP::"search"']);
        expect(options.every(o => o.group === 'MCP')).toBe(true);
        expect(options[0].label).toBe('Files MCP');
    });
});
