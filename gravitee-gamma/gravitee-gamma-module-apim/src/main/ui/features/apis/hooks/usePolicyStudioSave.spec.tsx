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
import type { SaveOutput } from '@gravitee/graphene-policy-studio';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useApiDetail } from './useApiDetail';
import { usePolicyStudioSave } from './usePolicyStudioSave';
import { getApiV4 } from '../services/apis';
import { getAndUpdatePlanFlows, getFullApiDetail, updateApi } from '../services/policyStudioService';
import type { ApiDetailDto } from '../types';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../services/apis', () => ({
    getApiV4: jest.fn(),
}));

jest.mock('../services/policyStudioService', () => ({
    getFullApiDetail: jest.fn(),
    updateApi: jest.fn(),
    getAndUpdatePlanFlows: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetApiV4 = jest.mocked(getApiV4);
const mockGetFullApiDetail = jest.mocked(getFullApiDetail);
const mockUpdateApi = jest.mocked(updateApi);
const mockGetAndUpdatePlanFlows = jest.mocked(getAndUpdatePlanFlows);

const API_ID = 'api-1';

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

/** Mounts the save hook next to the observer that feeds the deploy banner, as ApiDetailLayout does. */
function renderSaveWithDetailObserver() {
    return renderHook(() => ({ onSave: usePolicyStudioSave(API_ID), detail: useApiDetail(API_ID) }), { wrapper: createWrapper() });
}

describe('usePolicyStudioSave', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
        mockGetApiV4.mockResolvedValue({ id: API_ID, deploymentState: 'DEPLOYED' } as ApiDetailDto);
        mockGetFullApiDetail.mockResolvedValue({ id: API_ID });
        mockUpdateApi.mockResolvedValue(undefined);
        mockGetAndUpdatePlanFlows.mockResolvedValue(undefined);
    });

    it('refreshes the API detail so the deploy banner reflects a saved common flow', async () => {
        const { result } = renderSaveWithDetailObserver();
        await waitFor(() => expect(result.current.detail.data?.deploymentState).toBe('DEPLOYED'));
        mockGetApiV4.mockResolvedValue({ id: API_ID, deploymentState: 'NEED_REDEPLOY' } as ApiDetailDto);

        await act(() => result.current.onSave({ commonFlows: [] } as unknown as SaveOutput));

        await waitFor(() => expect(result.current.detail.data?.deploymentState).toBe('NEED_REDEPLOY'));
        expect(mockUpdateApi).toHaveBeenCalledTimes(1);
    });

    it('refreshes the API detail so the deploy banner reflects a saved plan flow', async () => {
        const { result } = renderSaveWithDetailObserver();
        await waitFor(() => expect(result.current.detail.data?.deploymentState).toBe('DEPLOYED'));
        mockGetApiV4.mockResolvedValue({ id: API_ID, deploymentState: 'NEED_REDEPLOY' } as ApiDetailDto);

        await act(() => result.current.onSave({ plansToUpdate: [{ id: 'plan-1', flows: [] }] } as unknown as SaveOutput));

        await waitFor(() => expect(result.current.detail.data?.deploymentState).toBe('NEED_REDEPLOY'));
        expect(mockGetAndUpdatePlanFlows).toHaveBeenCalledWith('DEFAULT', API_ID, 'plan-1', []);
    });

    it('leaves the API detail alone when the save fails', async () => {
        const consoleError = jest.spyOn(console, 'error').mockImplementation(() => undefined);
        mockUpdateApi.mockRejectedValue(new Error('boom'));
        const { result } = renderSaveWithDetailObserver();
        await waitFor(() => expect(result.current.detail.data?.deploymentState).toBe('DEPLOYED'));
        mockGetApiV4.mockClear();

        await act(async () => {
            await expect(result.current.onSave({ commonFlows: [] } as unknown as SaveOutput)).rejects.toThrow('boom');
        });

        expect(mockGetApiV4).not.toHaveBeenCalled();
        consoleError.mockRestore();
    });
});
