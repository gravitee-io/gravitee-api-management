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
import { resetApimClientForTests } from '@gravitee/gamma-ui-shared/api';
import { useObservabilityBaseUrl } from '@gravitee/gamma-ui-shared/observability';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

function renderBaseUrl() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/environments/env-a/platform/environment/metrics-configuration']}>{children}</MemoryRouter>
            </QueryClientProvider>
        );
    }
    return renderHook(() => useObservabilityBaseUrl(), { wrapper: Wrapper });
}

describe('useObservabilityBaseUrl', () => {
    let fetchSpy: jest.SpiedFunction<typeof fetch>;

    beforeEach(() => {
        resetApimClientForTests();
        mockUseEnvironment.mockReturnValue({ id: 'env-a' });
        fetchSpy = jest.spyOn(globalThis, 'fetch');
    });

    afterEach(() => {
        fetchSpy.mockRestore();
    });

    it('builds the environment base URL from the bootstrap, through the dev proxy outside production', async () => {
        fetchSpy.mockImplementation(async input => {
            const url = String(input);
            if (url === '/constants.json') return jsonResponse({ gammaBaseURL: 'https://apim.example.com/gamma' });
            if (url === 'https://apim.example.com/gamma/ui/bootstrap') {
                return jsonResponse({ managementBaseURL: 'https://apim.example.com/management', organizationId: 'org-1' });
            }
            throw new Error(`Unexpected fetch ${url}`);
        });

        const { result } = renderBaseUrl();

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.isError).toBe(false);
        // NODE_ENV is "test" here: the origin is dropped so the browser hits the local dev proxy.
        expect(result.current.baseUrl).toBe('/gamma/organizations/org-1/environments/env-a');
    });

    it('keeps the bootstrap origin in production: no dev proxy there', async () => {
        fetchSpy.mockImplementation(async input => {
            const url = String(input);
            if (url === '/constants.json') return jsonResponse({ gammaBaseURL: 'https://apim.example.com/gamma' });
            if (url === 'https://apim.example.com/gamma/ui/bootstrap') {
                return jsonResponse({ managementBaseURL: 'https://apim.example.com/management', organizationId: 'org-1' });
            }
            throw new Error(`Unexpected fetch ${url}`);
        });
        const nodeEnv = process.env.NODE_ENV;
        process.env.NODE_ENV = 'production';
        try {
            const { result } = renderBaseUrl();

            await waitFor(() => expect(result.current.isLoading).toBe(false));
            expect(result.current.baseUrl).toBe('https://apim.example.com/gamma/organizations/org-1/environments/env-a');
        } finally {
            process.env.NODE_ENV = nodeEnv;
        }
    });

    it('reports a bootstrap that could not be read', async () => {
        fetchSpy.mockResolvedValue(jsonResponse({ message: 'down' }, 503));

        const { result } = renderBaseUrl();

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.baseUrl).toBeUndefined();
        expect(result.current.error).toBeInstanceOf(Error);
    });
});
