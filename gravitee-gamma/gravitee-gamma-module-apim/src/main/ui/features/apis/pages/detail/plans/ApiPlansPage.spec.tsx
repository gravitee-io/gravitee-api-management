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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core', () => ({
    Button: ({ children, ...props }: { children?: ReactNode }) => <button {...props}>{children}</button>,
    DropdownMenu: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DropdownMenuTrigger: ({ children }: { children?: ReactNode }) => <>{children}</>,
    DropdownMenuContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DropdownMenuItem: ({ children, onClick }: { children?: ReactNode; onClick?: () => void }) => (
        <button role="menuitem" onClick={onClick}>
            {children}
        </button>
    ),
    Dialog: ({ children, open }: { children?: ReactNode; open?: boolean }) => (open ? <div role="dialog">{children}</div> : null),
    DialogClose: ({ children }: { children?: ReactNode }) => <>{children}</>,
    DialogContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    DialogFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
    Input: ({ id }: { id?: string }) => <input id={id} readOnly />,
    Label: ({ children, htmlFor }: { children?: ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
    Skeleton: () => <div data-testid="skeleton" />,
    Switch: ({ checked, disabled }: { checked?: boolean; disabled?: boolean }) => (
        <input type="checkbox" checked={checked} disabled={disabled} readOnly />
    ),
    toast: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('./PlansListPage', () => ({
    PlansListPage: () => <div data-testid="plans-list" />,
}));

jest.mock('./PlansLearningPage', () => ({
    PlansLearningPage: () => <div data-testid="plans-learning" />,
}));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../../hooks/useApiDetail', () => ({
    useApiDetail: jest.fn(),
}));

jest.mock('../../../hooks/usePlans', () => ({
    usePlanStatusCounts: jest.fn(),
}));

import { ApiPlansPage } from './ApiPlansPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { usePlanStatusCounts } from '../../../hooks/usePlans';

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUsePlanStatusCounts = usePlanStatusCounts as jest.Mock;

const HTTP_PROXY_API = {
    id: 'api-1',
    name: 'Orders API',
    definitionVersion: 'V4',
    type: 'PROXY',
    listeners: [{ type: 'HTTP', paths: [{ path: '/orders' }] }],
};

const TCP_PROXY_API = {
    id: 'api-1',
    name: 'Orders TCP API',
    definitionVersion: 'V4',
    listeners: [{ type: 'TCP', hosts: ['orders.example.com'] }],
};

const FEDERATED_API = {
    id: 'api-1',
    name: 'Federated Orders API',
    definitionVersion: 'FEDERATED',
    listeners: undefined,
};

const PLANS_LISTED = { staging: 0, published: 1, deprecated: 0, closed: 0, total: 1, isLoading: false };
const NO_PLANS_YET = { staging: 0, published: 0, deprecated: 0, closed: 0, total: 0, isLoading: false };

function grantPermissions(granted: string[]) {
    mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => anyOf.some(permission => granted.includes(permission)));
}

function renderPlansPage(api: object | null) {
    mockUseApiDetailContext.mockReturnValue({ api, isLoading: false, permissionsReady: true });
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
            <MemoryRouter initialEntries={['/apis/api-1/plans']}>
                <Routes>
                    <Route path="apis/:apiId/plans" element={<ApiPlansPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ApiPlansPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseHasPermission.mockReturnValue(true);
        mockUseApiDetail.mockReturnValue({ data: undefined });
        mockUsePlanStatusCounts.mockReturnValue(PLANS_LISTED);
    });

    it.each<[string, object | null, typeof PLANS_LISTED]>([
        ['a federated API whose plans come from the provider', FEDERATED_API, PLANS_LISTED],
        ['a federated API that has no plans yet', FEDERATED_API, NO_PLANS_YET],
        ['an API whose detail payload has not resolved', null, PLANS_LISTED],
        ['an API whose detail payload has not resolved and has no plans yet', null, NO_PLANS_YET],
    ])('offers no plan creation control on %s, to a viewer who holds plan create permission', (_case, api, counts) => {
        grantPermissions(['api-plan-r', 'api-plan-c']);
        mockUsePlanStatusCounts.mockReturnValue(counts);

        renderPlansPage(api);

        expect(screen.queryByRole('button', { name: /create plan/i })).toBeNull();
        expect(screen.queryAllByRole('menuitem')).toHaveLength(0);
    });

    it('keeps the plan creation control and its security types on a natively managed V4 proxy API', () => {
        grantPermissions(['api-plan-r', 'api-plan-c']);

        renderPlansPage(HTTP_PROXY_API);

        expect(screen.getByRole('button', { name: /create plan/i })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: /api key/i })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: /oauth2/i })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: /keyless/i })).toBeInTheDocument();
    });

    it('offers no plan creation control on a natively managed API to a viewer without plan create permission', () => {
        grantPermissions(['api-plan-r']);

        renderPlansPage(HTTP_PROXY_API);

        expect(screen.queryByRole('button', { name: /create plan/i })).toBeNull();
        expect(screen.queryAllByRole('menuitem')).toHaveLength(0);
    });

    it('keeps narrowing a natively managed TCP API to the only security type it supports', () => {
        renderPlansPage(TCP_PROXY_API);

        expect(screen.getByRole('button', { name: /create plan/i })).toBeInTheDocument();
        expect(screen.getAllByRole('menuitem')).toHaveLength(1);
        expect(screen.getByRole('menuitem', { name: /keyless/i })).toBeInTheDocument();
    });

    it.each<[string, object | null]>([
        ['a federated API, whose subscriptions the provider owns', FEDERATED_API],
        ['an API whose detail payload has not resolved', null],
    ])('offers no multi JWT/OAuth2 subscription toggle on %s', (_case, api) => {
        grantPermissions(['api-plan-r', 'api-plan-u']);

        renderPlansPage(api);

        expect(screen.queryByText(/allow multi jwt\/oauth2 subscriptions per application/i)).toBeNull();
    });

    it('keeps the multi JWT/OAuth2 subscription toggle on a natively managed V4 API', () => {
        grantPermissions(['api-plan-r', 'api-plan-u']);

        renderPlansPage(HTTP_PROXY_API);

        expect(screen.getByText(/allow multi jwt\/oauth2 subscriptions per application/i)).toBeInTheDocument();
    });

    it('keeps offering the plan tutorial when a natively managed API has none', () => {
        mockUsePlanStatusCounts.mockReturnValue(NO_PLANS_YET);

        renderPlansPage(HTTP_PROXY_API);

        expect(screen.getByTestId('plans-learning')).toBeInTheDocument();
    });

    it('keeps offering the plan tutorial on a federated API that has none', () => {
        mockUsePlanStatusCounts.mockReturnValue(NO_PLANS_YET);

        renderPlansPage(FEDERATED_API);

        expect(screen.getByTestId('plans-learning')).toBeInTheDocument();
    });

    it('keeps offering the plan tutorial while the API type is still unknown', () => {
        mockUsePlanStatusCounts.mockReturnValue(NO_PLANS_YET);

        renderPlansPage(null);

        expect(screen.getByTestId('plans-learning')).toBeInTheDocument();
    });
});
