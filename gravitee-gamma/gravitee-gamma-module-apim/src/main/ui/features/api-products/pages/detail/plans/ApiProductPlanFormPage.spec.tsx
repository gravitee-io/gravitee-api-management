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
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiProductPlanFormPage } from './ApiProductPlanFormPage';
import { usePlanSecuritySettings } from '../../../../apis/hooks/usePlanSecuritySettings';
import { useApiProductDetail } from '../../../hooks/useApiProductDetail';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('@gravitee/graphene-core', () => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const React = require('react');
    return {
        Button: ({ children, ...props }: { children?: ReactNode }) => <button {...props}>{children}</button>,
        Skeleton: ({ className }: { className?: string }) => <div data-testid="skeleton" className={className} />,
        PageFocused: ({ children }: { children?: ReactNode }) => <>{children}</>,
    };
});

jest.mock('../../../hooks/useApiProductPermissions', () => ({
    useApiProductResourcePermissions: jest.fn(),
}));

jest.mock('../../../hooks/useApiProductDetail', () => ({
    useApiProductDetail: jest.fn(() => ({ data: { tags: [] } })),
}));

jest.mock('../../../../apis/hooks/usePlanSecuritySettings', () => ({
    usePlanSecuritySettings: jest.fn(),
}));

jest.mock('../../../../apis/hooks/usePlans', () => ({
    usePlan: jest.fn(),
}));

jest.mock('../../../../apis/pages/detail/plans/plan-form/PlanFormWizard', () => ({
    PlanFormWizard: () => <div data-testid="plan-form-wizard">Plan form</div>,
}));

const mockPermissions = useApiProductResourcePermissions as jest.Mock;
const mockUseApiProductDetail = useApiProductDetail as jest.Mock;
const mockUsePlanSecuritySettings = usePlanSecuritySettings as jest.Mock;

const ALL_SECURITY_ENABLED = { API_KEY: true, JWT: true, OAUTH2: true, MTLS: true, KEY_LESS: true };

function newQueryClient() {
    return new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
}

function renderCreate(securityType: string) {
    return render(
        <QueryClientProvider client={newQueryClient()}>
            <MemoryRouter initialEntries={[`/api-products/product-1/plans/new/${securityType}`]}>
                <Routes>
                    <Route path="api-products/:productId/plans">
                        <Route index element={<p>Plans list</p>} />
                        <Route path="new/:securityType" element={<ApiProductPlanFormPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ApiProductPlanFormPage plan security settings', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockPermissions.mockReturnValue({
            canRead: true,
            canCreate: true,
            canUpdate: true,
            canDelete: true,
            isLoading: false,
        });
        mockUseApiProductDetail.mockReturnValue({ data: { tags: [] } });
        mockUsePlanSecuritySettings.mockReturnValue({ planSecuritySettings: ALL_SECURITY_ENABLED, isLoading: false });
    });

    it('redirects away from create when the plan security type is disabled in portal settings', async () => {
        mockUsePlanSecuritySettings.mockReturnValue({
            planSecuritySettings: { ...ALL_SECURITY_ENABLED, JWT: false },
            isLoading: false,
        });

        renderCreate('JWT');

        expect(await screen.findByText('Plans list')).toBeInTheDocument();
        expect(screen.queryByTestId('plan-form-wizard')).not.toBeInTheDocument();
    });

    it('renders the create wizard when the plan security type is enabled', async () => {
        renderCreate('JWT');

        expect(await screen.findByTestId('plan-form-wizard')).toBeInTheDocument();
        expect(screen.queryByText('Plans list')).not.toBeInTheDocument();
    });

    it('keeps the create wizard behind a skeleton while portal security settings load', () => {
        mockUsePlanSecuritySettings.mockReturnValue({ planSecuritySettings: ALL_SECURITY_ENABLED, isLoading: true });

        renderCreate('API_KEY');

        expect(screen.getAllByTestId('skeleton').length).toBeGreaterThan(0);
        expect(screen.queryByTestId('plan-form-wizard')).not.toBeInTheDocument();
        expect(screen.queryByText('Plans list')).not.toBeInTheDocument();
    });
});
