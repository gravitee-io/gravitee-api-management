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
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';

import { useApiProductDetail } from './useApiProductDetail';
import { useUpdateApiProductResources } from './useUpdateApiProductResources';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { server } from '../../../testing/server';
import type { ApiResource } from '../../apis/types/resource';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const PRODUCT_PATH = `${TEST_V2_BASE}/api-products/product-1`;

const BASE_PRODUCT = {
    id: 'product-1',
    name: 'Payments',
    version: '1.0',
    apiIds: ['api-1'],
    deploymentState: 'SYNCED',
    resources: [{ name: 'cache-1', type: 'cache', enabled: true, configuration: {} }] as ApiResource[],
};

function renderHooks() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;

    return renderHook(
        () => ({
            detail: useApiProductDetail('product-1'),
            mutation: useUpdateApiProductResources('product-1'),
        }),
        { wrapper },
    );
}

describe('useUpdateApiProductResources (MSW)', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
    });

    it('refetches product detail after save so deploymentState can update', async () => {
        let getCount = 0;
        server.use(
            http.get(PRODUCT_PATH, () => {
                getCount += 1;
                if (getCount <= 2) {
                    return HttpResponse.json(BASE_PRODUCT);
                }
                return HttpResponse.json({ ...BASE_PRODUCT, deploymentState: 'NEED_REDEPLOY' });
            }),
            http.put(PRODUCT_PATH, async ({ request }) => {
                const body = await request.json();
                return HttpResponse.json({ ...(body as object) });
            }),
        );

        const { result } = renderHooks();

        await waitFor(() => expect(result.current.detail.data?.deploymentState).toBe('SYNCED'));

        await act(async () => {
            await result.current.mutation.mutateAsync(current => [...current, { name: 'new', type: 'cache', enabled: true, configuration: {} }]);
        });

        await waitFor(() => expect(result.current.detail.data?.deploymentState).toBe('NEED_REDEPLOY'));
        expect(getCount).toBeGreaterThanOrEqual(3);
    });
});
