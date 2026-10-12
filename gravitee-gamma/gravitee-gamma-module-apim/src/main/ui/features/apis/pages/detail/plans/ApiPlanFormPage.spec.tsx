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
jest.mock('../../../hooks/usePlanSecuritySettings', () => ({
    usePlanSecuritySettings: jest.fn(() => ({
        planSecuritySettings: { API_KEY: true, JWT: true, OAUTH2: true, MTLS: true, KEY_LESS: true },
        isLoading: false,
    })),
}));

import { ApiPlanFormPage } from './ApiPlanFormPage';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { useGroups } from '../../../hooks/useGroups';
import { useOrgTags } from '../../../hooks/useOrgTags';
import { usePlanSecuritySettings } from '../../../hooks/usePlanSecuritySettings';
import { useUserTags } from '../../../hooks/useUserTags';
import { getPlan } from '../../../services/plans';
import type { ManagedPlan } from '../../../types/plan';

const mockGetPlan = getPlan as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUseGroups = useGroups as jest.Mock;
const mockUseOrgTags = useOrgTags as jest.Mock;
const mockUseUserTags = useUserTags as jest.Mock;
const mockUsePlanSecuritySettings = usePlanSecuritySettings as jest.Mock;

const ALL_SECURITY_ENABLED = { API_KEY: true, JWT: true, OAUTH2: true, MTLS: true, KEY_LESS: true };

function newQueryClient() {
    return new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
}

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

describe('ApiPlanFormPage plan security settings', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseApiDetail.mockReturnValue({ data: { listeners: [{ type: 'HTTP' }] }, isLoading: false });
        mockUseGroups.mockReturnValue({ data: undefined });
        mockUseOrgTags.mockReturnValue({ data: [] });
        mockUseUserTags.mockReturnValue({ data: [] });
        mockUsePlanSecuritySettings.mockReturnValue({ planSecuritySettings: ALL_SECURITY_ENABLED, isLoading: false });
    });

    it('redirects away from create when the plan security type is disabled in portal settings', async () => {
        mockUsePlanSecuritySettings.mockReturnValue({
            planSecuritySettings: { ...ALL_SECURITY_ENABLED, API_KEY: false },
            isLoading: false,
        });

        render(
            <QueryClientProvider client={newQueryClient()}>
                <MemoryRouter initialEntries={['/apis/api-1/plans/new/API_KEY']}>
                    <Routes>
                        <Route path="apis/:apiId/plans">
                            <Route index element={<p>Plans list</p>} />
                            <Route path="new/:securityType" element={<ApiPlanFormPage />} />
                        </Route>
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>,
        );

        expect(await screen.findByText('Plans list')).toBeInTheDocument();
        expect(screen.queryByLabelText(/^Name/)).not.toBeInTheDocument();
    });

    it('keeps the create wizard closed while portal security settings load', () => {
        mockUsePlanSecuritySettings.mockReturnValue({ planSecuritySettings: ALL_SECURITY_ENABLED, isLoading: true });

        render(
            <QueryClientProvider client={newQueryClient()}>
                <MemoryRouter initialEntries={['/apis/api-1/plans/new/API_KEY']}>
                    <Routes>
                        <Route path="apis/:apiId/plans">
                            <Route index element={<p>Plans list</p>} />
                            <Route path="new/:securityType" element={<ApiPlanFormPage />} />
                        </Route>
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>,
        );

        expect(screen.queryByText('Plans list')).not.toBeInTheDocument();
        expect(screen.queryByLabelText(/^Name/)).not.toBeInTheDocument();
        expect(screen.queryByRole('navigation', { name: 'Plan creation steps' })).not.toBeInTheDocument();
    });
});
