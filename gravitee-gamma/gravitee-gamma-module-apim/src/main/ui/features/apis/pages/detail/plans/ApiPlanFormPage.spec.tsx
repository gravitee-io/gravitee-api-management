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

const KEYLESS_PLAN: ManagedPlan = {
    id: 'plan-1',
    name: 'Open access',
    description: 'No key required',
    status: 'PUBLISHED',
    security: { type: 'KEY_LESS' },
    order: 1,
    validation: 'AUTO',
    definitionVersion: 'V4',
};

describe('ApiPlanFormPage edit form', () => {
    it('does not show subscription settings for a keyless plan, which has no subscriptions', async () => {
        mockGetPlan.mockResolvedValue(KEYLESS_PLAN);
        render(
            <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
                <MemoryRouter initialEntries={['/apis/api-1/plans/plan-1']}>
                    <Routes>
                        <Route path="apis/:apiId/plans/:planId" element={<ApiPlanFormPage />} />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>,
        );
        await screen.findByLabelText(/^Name/);

        expect(screen.queryByText('Subscriptions')).toBeNull();
        expect(screen.queryByRole('switch', { name: /auto validate subscription/i })).toBeNull();
    });
});
