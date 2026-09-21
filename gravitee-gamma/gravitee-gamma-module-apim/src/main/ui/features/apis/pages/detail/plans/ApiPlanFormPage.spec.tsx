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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../services/plans', () => ({
    getPlan: jest.fn(),
    updatePlan: jest.fn(),
    createPlan: jest.fn(),
    deletePlan: jest.fn(),
    listPlans: jest.fn(),
    transitionPlan: jest.fn(),
}));

jest.mock('../../../hooks/useApiDetail', () => ({ useApiDetail: jest.fn(() => ({ data: undefined })) }));
jest.mock('../../../hooks/useGroups', () => ({ useGroups: jest.fn(() => ({ data: undefined })) }));
jest.mock('../../../hooks/useOrgTags', () => ({ useOrgTags: jest.fn(() => ({ data: [] })) }));
jest.mock('../../../hooks/useUserTags', () => ({ useUserTags: jest.fn(() => ({ data: [] })) }));
jest.mock('../../../hooks/useApiResources', () => ({ useResourcePlugins: jest.fn(() => ({ data: [] })) }));

import { ApiPlanFormPage } from './ApiPlanFormPage';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { useGroups } from '../../../hooks/useGroups';
import { useOrgTags } from '../../../hooks/useOrgTags';
import { useUserTags } from '../../../hooks/useUserTags';
import { getPlan, updatePlan } from '../../../services/plans';
import type { ManagedPlan } from '../../../types/plan';

const mockGetPlan = getPlan as jest.Mock;
const mockUpdatePlan = updatePlan as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUseGroups = useGroups as jest.Mock;
const mockUseOrgTags = useOrgTags as jest.Mock;
const mockUseUserTags = useUserTags as jest.Mock;

const FEDERATED_PLAN: ManagedPlan = {
    id: 'plan-1',
    name: 'Provider Gold',
    description: 'Plan owned by the upstream provider',
    status: 'PUBLISHED',
    security: { type: 'API_KEY' },
    order: 1,
    validation: 'AUTO',
    characteristics: ['gold'],
    definitionVersion: 'FEDERATED',
};

const MANUALLY_VALIDATED_PLAN: ManagedPlan = {
    ...FEDERATED_PLAN,
    validation: 'MANUAL',
    commentRequired: true,
    commentMessage: 'Tell us about your use case',
};

const PARTNER_GROUP = { id: 'group-1', name: 'Partners' };
const EUROPE_TAG = { id: 'tag-1', key: 'eu', name: 'Europe' };

function newQueryClient() {
    return new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
}

function renderPlanRoute(queryClient: QueryClient) {
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/apis/api-1/plans/plan-1']}>
                <Routes>
                    <Route path="apis/:apiId/plans/:planId" element={<ApiPlanFormPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

async function renderPlanEditPage(plan: ManagedPlan) {
    mockGetPlan.mockResolvedValue(plan);
    renderPlanRoute(newQueryClient());
    await screen.findByLabelText(/^Name/);
}

// Stands in for the server: only a PUT that actually carried the new name makes the next GET return it.
function servePlanFromStore(initial: ManagedPlan) {
    let stored = initial;
    mockGetPlan.mockImplementation(() => Promise.resolve(stored));
    mockUpdatePlan.mockImplementation((_envId, _ctx, _planId, payload: Partial<ManagedPlan>) => {
        stored = { ...stored, ...payload };
        return Promise.resolve(stored);
    });
}

async function goToSecurityStep(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await screen.findByText('API Key authentication configuration');
}

async function renamePlanAndSave(user: ReturnType<typeof userEvent.setup>, newName: string) {
    const nameInput = await screen.findByLabelText(/^Name/);
    await user.clear(nameInput);
    await user.type(nameInput, newName);
    let next = screen.queryByRole('button', { name: 'Next' });
    while (next) {
        await user.click(next);
        next = screen.queryByRole('button', { name: 'Next' });
    }
    await user.click(screen.getByRole('button', { name: /save changes/i }));
}

describe('ApiPlanFormPage edit form', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseApiDetail.mockReturnValue({ data: undefined });
        mockUseGroups.mockReturnValue({ data: undefined });
        mockUseOrgTags.mockReturnValue({ data: [] });
        mockUseUserTags.mockReturnValue({ data: [] });
    });

    it('opens a federated plan populated with that plan own values', async () => {
        await renderPlanEditPage(FEDERATED_PLAN);

        expect(screen.getByLabelText(/^Name/)).toHaveValue('Provider Gold');
        expect(screen.getByLabelText('Description')).toHaveValue('Plan owned by the upstream provider');
        expect(screen.getByRole('button', { name: 'Remove gold' })).toBeInTheDocument();
        expect(screen.getByRole('switch', { name: /auto validate subscription/i })).toBeChecked();
        expect(screen.queryByText(/failed to load plan/i)).toBeNull();
    });

    it('opens a federated plan that validates subscriptions manually with its own subscription settings', async () => {
        await renderPlanEditPage(MANUALLY_VALIDATED_PLAN);

        expect(screen.getByRole('switch', { name: /auto validate subscription/i })).not.toBeChecked();
        expect(screen.getByRole('switch', { name: /require comment on subscription/i })).toBeChecked();
        expect(screen.getByLabelText('Custom message to display to consumer')).toHaveValue('Tell us about your use case');
    });

    it('opens a federated plan showing the groups it excludes from subscribing', async () => {
        mockUseGroups.mockReturnValue({ data: { data: [PARTNER_GROUP] } });

        await renderPlanEditPage({ ...FEDERATED_PLAN, excludedGroups: [PARTNER_GROUP.id] });

        expect(screen.getByText(PARTNER_GROUP.name)).toBeInTheDocument();
    });

    it('opens a federated plan showing the sharding tags it is deployed to', async () => {
        mockUseOrgTags.mockReturnValue({ data: [EUROPE_TAG] });
        mockUseUserTags.mockReturnValue({ data: [EUROPE_TAG.key] });
        mockUseApiDetail.mockReturnValue({ data: { tags: [EUROPE_TAG.key] } });

        await renderPlanEditPage({ ...FEDERATED_PLAN, tags: [EUROPE_TAG.key] });

        expect(screen.getByText(EUROPE_TAG.name)).toBeInTheDocument();
    });

    it('opens a federated plan showing its stored API key security configuration', async () => {
        const user = userEvent.setup();
        await renderPlanEditPage({
            ...FEDERATED_PLAN,
            security: {
                type: 'API_KEY',
                configuration: { propagateApiKey: true, enableCustomApiKeyHeader: true, apiKeyHeader: 'X-Provider-Key' },
            },
        });

        await goToSecurityStep(user);

        expect(screen.getByRole('switch', { name: /propagate api key to upstream api/i })).toBeChecked();
        expect(screen.getByLabelText('API Key header name')).toHaveValue('X-Provider-Key');
    });

    it('opens a federated plan showing its additional selection rule', async () => {
        const user = userEvent.setup();
        const selectionRule = "{#context.attributes['tier'] == 'gold'}";
        await renderPlanEditPage({ ...FEDERATED_PLAN, selectionRule });

        await goToSecurityStep(user);
        await user.click(screen.getByText('Additional selection rule'));

        expect(screen.getByLabelText('Selection Rule')).toHaveValue(selectionRule);
    });

    it('shows the saved name when a renamed federated plan is reopened', async () => {
        const user = userEvent.setup();
        servePlanFromStore(FEDERATED_PLAN);
        const queryClient = newQueryClient();

        const firstVisit = renderPlanRoute(queryClient);
        await renamePlanAndSave(user, 'Renamed by operator');
        await waitFor(() => expect(mockUpdatePlan).toHaveBeenCalled());
        firstVisit.unmount();

        renderPlanRoute(queryClient);

        expect(await screen.findByLabelText(/^Name/)).toHaveValue('Renamed by operator');
    });
});
