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
jest.mock('@gravitee/gamma-modules-sdk/routing', () => jest.requireActual('../testing/buildModuleNavPathForTests'));

import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { UserInheritedPermissionsSection } from './UserInheritedPermissionsSection';
import { useOrganizationUserApiProducts, useOrganizationUserApis, useOrganizationUserApplications } from '../hooks/useOrganizationUser';

jest.mock('../hooks/useOrganizationUser', () => ({
    useOrganizationUserApis: jest.fn(),
    useOrganizationUserApiProducts: jest.fn(),
    useOrganizationUserApplications: jest.fn(),
}));

const mockUseOrganizationUserApis = jest.mocked(useOrganizationUserApis);
const mockUseOrganizationUserApiProducts = jest.mocked(useOrganizationUserApiProducts);
const mockUseOrganizationUserApplications = jest.mocked(useOrganizationUserApplications);

const APIS = [
    { id: 'api-1', name: 'Orders API', version: '1', visibility: 'PRIVATE', environmentId: 'DEFAULT' },
    { id: 'api-2', name: 'Payments API', version: '2', visibility: 'PUBLIC', environmentId: 'DEFAULT' },
];
const API_PRODUCTS = [{ id: 'product-1', name: 'Orders Product', version: '1', visibility: 'PRIVATE', environmentId: 'DEFAULT' }];
const APPLICATIONS = [
    { id: 'app-1', name: 'Mobile App', environmentId: 'DEFAULT' },
    { id: 'app-2', name: 'Partner Portal', environmentId: 'DEFAULT' },
];

const ENVIRONMENTS = [
    { id: 'DEFAULT', name: 'Default environment', hrids: ['default'] },
    { id: 'ENV_A', name: 'Environment A', hrids: ['env-a'] },
];

beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn(),
        })),
    });
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
    Element.prototype.scrollIntoView = jest.fn();
});

function renderSection(pathname = '/environments/default/platform/users/user-1') {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[pathname]}>
                <UserInheritedPermissionsSection userId="user-1" environmentId="DEFAULT" environments={ENVIRONMENTS} />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('UserInheritedPermissionsSection', () => {
    beforeEach(() => {
        mockUseOrganizationUserApis.mockReturnValue({
            data: { data: APIS, pagination: { page: 1, perPage: 9999, pageCount: 1, pageItemsCount: 2, totalCount: 2 } },
            isLoading: false,
        } as ReturnType<typeof useOrganizationUserApis>);
        mockUseOrganizationUserApiProducts.mockReturnValue({
            data: { data: API_PRODUCTS, pagination: { page: 1, perPage: 9999, pageCount: 1, pageItemsCount: 1, totalCount: 1 } },
            isLoading: false,
        } as ReturnType<typeof useOrganizationUserApiProducts>);
        mockUseOrganizationUserApplications.mockReturnValue({
            data: { data: APPLICATIONS, pagination: { page: 1, perPage: 9999, pageCount: 1, pageItemsCount: 2, totalCount: 2 } },
            isLoading: false,
        } as ReturnType<typeof useOrganizationUserApplications>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders inherited APIs, API products, and applications with top pagination controls', async () => {
        renderSection();

        const apisSection = await screen.findByRole('region', { name: 'Inherited APIs table' });
        expect(within(apisSection).getByText('Orders API')).toBeTruthy();
        expect(within(apisSection).getByText('Private')).toBeTruthy();
        expect(within(apisSection).getByText('Items per page')).toBeTruthy();
        expect(within(apisSection).getByText('1-2 of 2')).toBeTruthy();

        const apiProductsSection = screen.getByRole('region', { name: 'Inherited API Products table' });
        expect(within(apiProductsSection).getByText('Orders Product')).toBeTruthy();

        const applicationsSection = screen.getByRole('region', { name: 'Inherited Applications table' });
        expect(within(applicationsSection).getByText('Mobile App')).toBeTruthy();
    });

    it('filters APIs client-side and resets pagination when the search changes', async () => {
        const user = userEvent.setup();
        renderSection();

        const apisSection = await screen.findByRole('region', { name: 'Inherited APIs table' });
        await user.type(within(apisSection).getByPlaceholderText('Search APIs…'), 'payment');

        expect(within(apisSection).queryByText('Orders API')).toBeNull();
        expect(within(apisSection).getByText('Payments API')).toBeTruthy();
        expect(within(apisSection).getByText('1-1 of 1')).toBeTruthy();
    });

    it('shows a no-results message when the search matches nothing', async () => {
        const user = userEvent.setup();
        renderSection();

        const applicationsSection = await screen.findByRole('region', { name: 'Inherited Applications table' });
        await user.type(within(applicationsSection).getByPlaceholderText('Search applications…'), 'missing-app');

        expect(within(applicationsSection).getByText('No applications match your search.')).toBeTruthy();
    });

    it('links inherited resources to their gamma console detail routes', async () => {
        renderSection();

        expect((await screen.findByRole('link', { name: 'Orders API' })).getAttribute('href')).toBe(
            '/environments/default/apim/apis/api-1',
        );
        expect(screen.getByRole('link', { name: 'Orders Product' }).getAttribute('href')).toBe(
            '/environments/default/apim/api-products/product-1/configuration/general',
        );
        expect(screen.getByRole('link', { name: 'Mobile App' }).getAttribute('href')).toBe(
            '/environments/default/platform/applications/app-1',
        );
    });

    it('disables next-page navigation when all rows fit on one page', async () => {
        renderSection();

        const applicationsSection = await screen.findByRole('region', { name: 'Inherited Applications table' });
        expect(within(applicationsSection).getByText('1-2 of 2')).toBeTruthy();
        expect(within(applicationsSection).getByRole('button', { name: 'Next page' })).toHaveProperty('disabled', true);
    });

    it('paginates APIs when more rows exist than the default page size', async () => {
        const manyApis = Array.from({ length: 12 }, (_, index) => ({
            id: `api-${index + 1}`,
            name: `API ${index + 1}`,
            version: '1',
            visibility: 'PRIVATE',
            environmentId: 'DEFAULT',
        }));
        mockUseOrganizationUserApis.mockReturnValue({
            data: { data: manyApis, pagination: { page: 1, perPage: 9999, pageCount: 1, pageItemsCount: 12, totalCount: 12 } },
            isLoading: false,
        } as ReturnType<typeof useOrganizationUserApis>);

        const user = userEvent.setup();
        renderSection();

        const apisSection = await screen.findByRole('region', { name: 'Inherited APIs table' });
        expect(within(apisSection).getByText('1-10 of 12')).toBeTruthy();
        expect(within(apisSection).queryByText('API 11')).toBeNull();

        await user.click(within(apisSection).getByRole('button', { name: 'Next page' }));

        expect(within(apisSection).getByText('11-12 of 12')).toBeTruthy();
        expect(within(apisSection).getByText('API 11')).toBeTruthy();
        expect(within(apisSection).queryByText('API 1')).toBeNull();
    });
});
