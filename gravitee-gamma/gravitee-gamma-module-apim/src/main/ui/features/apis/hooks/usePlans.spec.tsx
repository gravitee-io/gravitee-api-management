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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useUpdatePlan } from './usePlans';
import { updatePlan } from '../services/plans';
import { EMPTY_GENERAL, EMPTY_RESTRICTIONS, EMPTY_SECURITY } from '../types/plan';
import type { ManagedPlan, PlanContext, PlanFormValue } from '../types/plan';
import { apiDetailKeys, apiPlanKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/plans', () => ({ updatePlan: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUpdatePlan = jest.mocked(updatePlan);

const CTX: PlanContext = { type: 'api', entityId: 'api-1' };

const RENAMED_PLAN: ManagedPlan = {
    id: 'plan-1',
    name: 'Renamed by operator',
    description: 'Plan owned by the upstream provider',
    status: 'PUBLISHED',
    security: { type: 'API_KEY' },
    order: 1,
    validation: 'AUTO',
    definitionVersion: 'FEDERATED',
};

const FORM: PlanFormValue = {
    securityType: 'API_KEY',
    general: { ...EMPTY_GENERAL, name: 'Renamed by operator' },
    security: { ...EMPTY_SECURITY },
    restrictions: { ...EMPTY_RESTRICTIONS },
};

function renderUpdatePlanHook(ctx: PlanContext = CTX) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries');
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const { result } = renderHook(() => useUpdatePlan(ctx), { wrapper });
    return { result, queryClient, invalidateQueries };
}

describe('useUpdatePlan once the mutation settles', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
        mockUpdatePlan.mockResolvedValue(RENAMED_PLAN);
    });

    it('stores the saved plan under its detail key so the plan page shows the new value', async () => {
        const { result, queryClient } = renderUpdatePlanHook();

        result.current.mutate({ planId: 'plan-1', form: FORM, definitionVersion: 'FEDERATED' });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(queryClient.getQueryData(apiPlanKeys.detail('DEFAULT', CTX, 'plan-1'))).toEqual(RENAMED_PLAN);
    });

    it('invalidates the plan list of the edited API', async () => {
        const { result, invalidateQueries } = renderUpdatePlanHook();

        result.current.mutate({ planId: 'plan-1', form: FORM, definitionVersion: 'FEDERATED' });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['api-plans', 'api', 'api-1'] });
    });

    it.each([
        { ctx: { type: 'api', entityId: 'api-1' } as PlanContext, invalidatesApiDetail: true },
        { ctx: { type: 'api-product', entityId: 'api-1' } as PlanContext, invalidatesApiDetail: false },
    ])('invalidates the API detail only for an API context ($ctx.type)', async ({ ctx, invalidatesApiDetail }) => {
        const { result, invalidateQueries } = renderUpdatePlanHook(ctx);

        result.current.mutate({ planId: 'plan-1', form: FORM, definitionVersion: 'FEDERATED' });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        const apiDetailInvalidation = { queryKey: apiDetailKeys.detail('DEFAULT', 'api-1') };
        if (invalidatesApiDetail) {
            expect(invalidateQueries).toHaveBeenCalledWith(apiDetailInvalidation);
        } else {
            expect(invalidateQueries).not.toHaveBeenCalledWith(apiDetailInvalidation);
        }
    });
});
