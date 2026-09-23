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
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

import { useCreateApiProxy } from './useCreateApiProxy';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';
import type { ApiProxyDraft } from '../types/apiCreation';

const DRAFT: ApiProxyDraft = {
    apiName: 'Flights',
    apiVersion: '1.0.0',
    apiDescription: '',
    protocol: 'HTTP',
    contextPath: '/flights',
    virtualHostsEnabled: false,
    virtualHosts: [],
    targetUrl: 'https://backend.example.com',
    tcpHosts: [],
    tcpTargetHost: '',
    tcpTargetPort: '',
    tcpTargetSecured: false,
    authType: 'keyless',
    apiKeyPlanName: 'Default API Key plan',
    jwtPlanName: 'Default JWT plan',
    jwtSignature: 'RS256',
    jwtJwksResolver: 'JWKS_URL',
    jwtResolverParameter: '',
    oauth2PlanName: 'Default OAuth2 plan',
    oauth2ResourceType: '',
    oauth2ResourceConfig: {},
    oauth2ResourceValid: false,
    mtlsPlanName: 'Default mTLS plan',
    deployImmediately: false,
    askForReview: false,
};

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

function mockCreationChain() {
    trackHandler('post', `${TEST_V2_BASE}/apis`, { id: 'api-1', name: 'Flights' });
    trackHandler('post', `${TEST_V2_BASE}/apis/:apiId/plans`, { id: 'plan-1' });
    trackHandler('post', `${TEST_V2_BASE}/apis/:apiId/plans/:planId/_publish`, undefined, 204);
    return {
        start: trackHandler('post', `${TEST_V2_BASE}/apis/:apiId/_start`, undefined, 204),
        ask: trackHandler('post', `${TEST_V2_BASE}/apis/:apiId/reviews/_ask`, undefined, 204),
    };
}

describe('useCreateApiProxy — outcome after creation', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('asks for a review instead of starting when the draft asks for one', async () => {
        const { start, ask } = mockCreationChain();
        const { result } = renderHook(() => useCreateApiProxy(), { wrapper: createWrapper() });

        result.current.mutate({ ...DRAFT, askForReview: true, deployImmediately: true });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(ask.callCount).toBe(1);
        expect(new URL(ask.lastCall!.url).pathname).toContain('/apis/api-1/reviews/_ask');
        expect(start.callCount).toBe(0);
    });

    it('starts the API when deploying immediately without a review', async () => {
        const { start, ask } = mockCreationChain();
        const { result } = renderHook(() => useCreateApiProxy(), { wrapper: createWrapper() });

        result.current.mutate({ ...DRAFT, deployImmediately: true });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(start.callCount).toBe(1);
        expect(ask.callCount).toBe(0);
    });

    it('creates only a draft when neither is requested', async () => {
        const { start, ask } = mockCreationChain();
        const { result } = renderHook(() => useCreateApiProxy(), { wrapper: createWrapper() });

        result.current.mutate(DRAFT);

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(start.callCount).toBe(0);
        expect(ask.callCount).toBe(0);
    });

    it('reports a failed review request with a pointer to the General page', async () => {
        mockCreationChain();
        trackHandler('post', `${TEST_V2_BASE}/apis/:apiId/reviews/_ask`, { message: 'Review is still in progress.' }, 400);
        const { result } = renderHook(() => useCreateApiProxy(), { wrapper: createWrapper() });

        result.current.mutate({ ...DRAFT, askForReview: true });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error?.message).toBe(
            'API "Flights" was created but the review could not be requested. Ask for a review from the API General page.',
        );
    });
});
