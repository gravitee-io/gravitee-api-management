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
import { render, screen } from '@testing-library/react';
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
import { getPlan } from '../../../services/plans';
import type { ManagedPlan } from '../../../types/plan';

const mockGetPlan = getPlan as jest.Mock;

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

async function renderPlanEditPage(plan: ManagedPlan) {
    mockGetPlan.mockResolvedValue(plan);
    render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
            <MemoryRouter initialEntries={['/apis/api-1/plans/plan-1']}>
                <Routes>
                    <Route path="apis/:apiId/plans/:planId" element={<ApiPlanFormPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
    await screen.findByLabelText(/^Name/);
}

describe('ApiPlanFormPage edit form', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('opens a federated plan populated with that plan own values', async () => {
        await renderPlanEditPage(FEDERATED_PLAN);

        expect(screen.getByLabelText(/^Name/)).toHaveValue('Provider Gold');
        expect(screen.getByLabelText('Description')).toHaveValue('Plan owned by the upstream provider');
        expect(screen.getByRole('button', { name: 'Remove gold' })).toBeInTheDocument();
        expect(screen.getByRole('switch', { name: /auto validate subscription/i })).toBeChecked();
        expect(screen.queryByText(/failed to load plan/i)).toBeNull();
    });
});
