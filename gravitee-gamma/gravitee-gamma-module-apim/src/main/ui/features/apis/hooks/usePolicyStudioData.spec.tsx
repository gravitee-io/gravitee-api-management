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
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { usePolicyStudioData } from './usePolicyStudioData';
import {
    getFullApiDetail,
    listEndpointPlugins,
    listEntrypointPlugins,
    listPolicies,
    listPublishedPlans,
    listSharedPolicyGroupPlugins,
} from '../services/policyStudioService';
import type { PolicyStudioApiDetail } from '../types/policyStudio';
import { apiDetailKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/policyStudioService', () => ({
    getFullApiDetail: jest.fn(),
    listEndpointPlugins: jest.fn(),
    listEntrypointPlugins: jest.fn(),
    listPolicies: jest.fn(),
    listPublishedPlans: jest.fn(),
    listSharedPolicyGroupPlugins: jest.fn(),
}));

const MOCK_ENV = { id: 'env-1', hrids: ['env-1'] };

const API: PolicyStudioApiDetail = { id: 'api-1', name: 'My API', type: 'PROXY', definitionVersion: 'V4' };

function createWrapper(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('usePolicyStudioData', () => {
    beforeEach(() => {
        jest.mocked(useEnvironment).mockReturnValue(MOCK_ENV);
        jest.mocked(listPolicies).mockResolvedValue([]);
        jest.mocked(listEntrypointPlugins).mockResolvedValue([]);
        jest.mocked(listEndpointPlugins).mockResolvedValue([]);
        jest.mocked(listSharedPolicyGroupPlugins).mockResolvedValue([]);
        jest.mocked(listPublishedPlans).mockResolvedValue({ data: [], pagination: { totalCount: 0 } });
    });

    afterEach(() => jest.clearAllMocks());

    it('returns the resources configured on the API, by name and type', async () => {
        jest.mocked(getFullApiDetail).mockResolvedValue({
            ...API,
            resources: [
                { name: 'My Cache Resource', type: 'cache', enabled: true, configuration: { timeToLiveSeconds: 60 } },
                { name: 'Keycloak', type: 'oauth2-keycloak-resource', enabled: true },
            ],
        });

        const { result } = renderHook(() => usePolicyStudioData('api-1'), { wrapper: createWrapper() });

        await waitFor(() =>
            expect(result.current.resources).toEqual([
                { name: 'My Cache Resource', type: 'cache' },
                { name: 'Keycloak', type: 'oauth2-keycloak-resource' },
            ]),
        );
    });

    it('returns no resources when the API has none', async () => {
        jest.mocked(getFullApiDetail).mockResolvedValue(API);

        const { result } = renderHook(() => usePolicyStudioData('api-1'), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.apiDetail).toEqual(API));
        expect(result.current.resources).toEqual([]);
    });

    it('reloads the API and its plans when the API detail is refreshed', async () => {
        jest.mocked(getFullApiDetail).mockResolvedValue(API);
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        const { result } = renderHook(() => usePolicyStudioData('api-1'), { wrapper: createWrapper(queryClient) });
        await waitFor(() => expect(result.current.isLoading).toBe(false));
        jest.mocked(getFullApiDetail).mockResolvedValue({
            ...API,
            resources: [{ name: 'My Cache Resource', type: 'cache', enabled: true }],
        });

        await act(() => queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail('env-1', 'api-1') }));

        await waitFor(() => expect(result.current.resources).toEqual([{ name: 'My Cache Resource', type: 'cache' }]));
        expect(getFullApiDetail).toHaveBeenCalledTimes(2);
        expect(listPublishedPlans).toHaveBeenCalledTimes(2);
    });
});
