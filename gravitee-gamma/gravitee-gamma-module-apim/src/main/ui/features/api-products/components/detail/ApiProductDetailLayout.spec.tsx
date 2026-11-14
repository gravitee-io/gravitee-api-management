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
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { type ReactNode, useSyncExternalStore } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiProductDetailLayout } from './ApiProductDetailLayout';
import { resetApimClientForTests } from '../../../../shared/api/apimClient';
import { notify } from '../../../../shared/notify';
import { TEST_V2_BASE } from '../../../../testing/factories';
import { settleOutstandingRequests, trackHandler } from '../../../../testing/helpers';
import { server } from '../../../../testing/server';
import type { ApiProductDeploymentState } from '../../types/apiProduct';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));

jest.mock('../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

// The real hook hands the banner to the host shell from an effect keyed on the caller's dependency list; this stand-in
// does the same, so a banner that only changes with a dependency missing from that list stays stale here too.
let mockBanner: ReactNode = null;
const mockBannerListeners = new Set<() => void>();
jest.mock('@gravitee/graphene-core', () => {
    const { useEffect } = jest.requireActual('react');
    return {
        ...jest.requireActual<object>('@gravitee/graphene-core'),
        useLayoutConfig: (config: { banner?: ReactNode }, deps: unknown[] = []) => {
            useEffect(() => {
                mockBanner = config.banner ?? null;
                mockBannerListeners.forEach(listener => listener());
                // eslint-disable-next-line react-hooks/exhaustive-deps
            }, deps);
        },
    };
});

function BannerHost() {
    const banner = useSyncExternalStore(
        listener => {
            mockBannerListeners.add(listener);
            return () => mockBannerListeners.delete(listener);
        },
        () => mockBanner,
    );
    return <div data-testid="banner-host">{banner}</div>;
}

const PRODUCT_PATH = `${TEST_V2_BASE}/api-products/prod-1`;

function givenProduct(deploymentState: ApiProductDeploymentState | undefined) {
    server.use(http.get(PRODUCT_PATH, () => HttpResponse.json({ id: 'prod-1', name: 'Payments', version: '1.0', deploymentState })));
}

function givenPermissions(permissions: Record<string, string>) {
    server.use(http.get(`${PRODUCT_PATH}/members/permissions`, () => HttpResponse.json(permissions)));
}

function renderLayout() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/api-products/prod-1/overview']}>
                <BannerHost />
                <Routes>
                    <Route path="/api-products/:productId" element={<ApiProductDetailLayout />}>
                        <Route path="overview" element={<div />} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

const banner = () => within(screen.getByTestId('banner-host'));

describe('ApiProductDetailLayout deploy banner', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockBanner = null;
        (useEnvironment as jest.Mock).mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
    });

    afterEach(() => jest.clearAllMocks());

    it('shows the banner with a Deploy button when the product needs a redeploy and the user can update its definition', async () => {
        givenProduct('NEED_REDEPLOY');
        givenPermissions({ DEFINITION: 'RU' });

        renderLayout();

        expect(await banner().findByText(/this api product is out of sync/i)).toBeInTheDocument();
        expect(banner().getByRole('button', { name: /deploy api product/i })).toBeEnabled();
    });

    it('shows no banner when the product is deployed', async () => {
        givenProduct('DEPLOYED');
        givenPermissions({ DEFINITION: 'RU' });

        renderLayout();
        await settleOutstandingRequests();

        expect(banner().queryByText(/out of sync/i)).not.toBeInTheDocument();
    });

    it('shows no banner when the user cannot update the product definition', async () => {
        givenProduct('NEED_REDEPLOY');
        const permissions = trackHandler('get', `${PRODUCT_PATH}/members/permissions`, { DEFINITION: 'R', PLAN: 'RU' });

        renderLayout();
        await settleOutstandingRequests();

        expect(permissions.callCount).toBeGreaterThan(0);
        expect(banner().queryByText(/out of sync/i)).not.toBeInTheDocument();
    });

    it('deploys the product after confirmation, then hides the banner once the refreshed product is deployed', async () => {
        let deploymentState: ApiProductDeploymentState = 'NEED_REDEPLOY';
        server.use(http.get(PRODUCT_PATH, () => HttpResponse.json({ id: 'prod-1', name: 'Payments', version: '1.0', deploymentState })));
        givenPermissions({ DEFINITION: 'RU' });
        const deploys: unknown[] = [];
        server.use(
            http.post(`${PRODUCT_PATH}/deployments`, async ({ request }) => {
                deploys.push(await request.json());
                deploymentState = 'DEPLOYED';
                return HttpResponse.json({ id: 'prod-1', deploymentState }, { status: 202 });
            }),
        );

        renderLayout();
        fireEvent.click(await banner().findByRole('button', { name: /deploy api product/i }));
        const dialog = await screen.findByRole('dialog', { name: /deploy your api product/i });
        // The product deployment endpoint takes no label, so none is asked for.
        expect(within(dialog).queryByLabelText(/deployment label/i)).not.toBeInTheDocument();
        fireEvent.click(within(dialog).getByRole('button', { name: /^deploy$/i }));

        await waitFor(() => expect(deploys).toEqual([{}]));
        await waitFor(() => expect(banner().queryByText(/out of sync/i)).not.toBeInTheDocument());
        expect(screen.queryByRole('dialog', { name: /deploy your api product/i })).not.toBeInTheDocument();
    });

    it('keeps the banner and reports the error when the deployment fails', async () => {
        givenProduct('NEED_REDEPLOY');
        givenPermissions({ DEFINITION: 'RU' });
        server.use(http.post(`${PRODUCT_PATH}/deployments`, () => HttpResponse.json({ message: 'License expired' }, { status: 500 })));

        renderLayout();
        fireEvent.click(await banner().findByRole('button', { name: /deploy api product/i }));
        fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: /^deploy$/i }));

        await waitFor(() =>
            expect(notify.error).toHaveBeenCalledWith(expect.anything(), 'An error occurred while deploying the API Product.'),
        );
        expect(banner().getByText(/this api product is out of sync/i)).toBeInTheDocument();
    });
});
