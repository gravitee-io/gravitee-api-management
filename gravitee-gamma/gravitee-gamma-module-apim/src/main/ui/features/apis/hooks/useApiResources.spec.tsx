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
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useUpdateApiResources } from './useApiResources';
import { policyStudioKeys } from './usePolicyStudioData';
import { updateApiResources } from '../services/resources';
import { apiDetailKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/resources', () => ({ updateApiResources: jest.fn() }));

const MOCK_ENV = { id: 'env-1', hrids: ['env-1'] };

describe('useUpdateApiResources', () => {
    beforeEach(() => {
        jest.mocked(useEnvironment).mockReturnValue(MOCK_ENV);
        jest.mocked(updateApiResources).mockResolvedValue(undefined);
    });

    afterEach(() => jest.clearAllMocks());

    it("marks the API detail and the Policy Studio's copy of that API stale once the resources are saved", async () => {
        const queryClient = new QueryClient();
        queryClient.setQueryData(apiDetailKeys.detail('env-1', 'api-1'), { id: 'api-1' });
        queryClient.setQueryData(policyStudioKeys.api('env-1', 'api-1'), { id: 'api-1' });
        queryClient.setQueryData(policyStudioKeys.api('env-1', 'api-2'), { id: 'api-2' });
        const wrapper = ({ children }: { children: ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        );
        const resources = [{ name: 'My Cache Resource', type: 'cache', enabled: true }];

        const { result } = renderHook(() => useUpdateApiResources('api-1'), { wrapper });
        await act(() => result.current.mutateAsync(resources));

        expect(updateApiResources).toHaveBeenCalledWith('env-1', 'api-1', resources);
        expect(queryClient.getQueryState(apiDetailKeys.detail('env-1', 'api-1'))?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(policyStudioKeys.api('env-1', 'api-1'))?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(policyStudioKeys.api('env-1', 'api-2'))?.isInvalidated).toBe(false);
    });
});
