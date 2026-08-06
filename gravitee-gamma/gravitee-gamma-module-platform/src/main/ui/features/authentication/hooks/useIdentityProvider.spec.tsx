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

import { useIdentityProviderMappingCatalog } from './useIdentityProvider';
import { listOrganizationGroups } from '../../../shared/services/organizationGroups';
import { listEnvironmentRoles, listOrganizationEnvironments, listOrganizationRoles } from '../../users/services/organizationUsers';

jest.mock('../../../shared/services/organizationGroups', () => ({
    listOrganizationGroups: jest.fn(),
}));

jest.mock('../../users/services/organizationUsers', () => ({
    ...jest.requireActual('../../users/services/organizationUsers'),
    listOrganizationEnvironments: jest.fn(),
    listOrganizationRoles: jest.fn(),
    listEnvironmentRoles: jest.fn(),
}));

const mockListOrganizationGroups = jest.mocked(listOrganizationGroups);
const mockListOrganizationEnvironments = jest.mocked(listOrganizationEnvironments);
const mockListOrganizationRoles = jest.mocked(listOrganizationRoles);
const mockListEnvironmentRoles = jest.mocked(listEnvironmentRoles);

describe('useIdentityProviderMappingCatalog', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('refetches groups, environments, and both role catalogs', async () => {
        mockListOrganizationGroups.mockResolvedValue([]);
        mockListOrganizationEnvironments.mockResolvedValue([]);
        mockListOrganizationRoles.mockResolvedValue([]);
        mockListEnvironmentRoles.mockResolvedValue([]);

        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

        function Wrapper({ children }: { children: ReactNode }) {
            return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
        }

        const { result } = renderHook(() => useIdentityProviderMappingCatalog(), { wrapper: Wrapper });

        await waitFor(() => {
            expect(result.current.groupsQuery.isSuccess).toBe(true);
            expect(result.current.environmentsQuery.isSuccess).toBe(true);
            expect(result.current.organizationRolesQuery.isSuccess).toBe(true);
            expect(result.current.environmentRolesQuery.isSuccess).toBe(true);
        });
        expect(mockListOrganizationGroups).toHaveBeenCalledTimes(1);
        expect(mockListOrganizationEnvironments).toHaveBeenCalledTimes(1);
        expect(mockListOrganizationRoles).toHaveBeenCalledTimes(1);
        expect(mockListEnvironmentRoles).toHaveBeenCalledTimes(1);

        result.current.refetchCatalogs();

        await waitFor(() => {
            expect(mockListOrganizationGroups).toHaveBeenCalledTimes(2);
            expect(mockListOrganizationEnvironments).toHaveBeenCalledTimes(2);
            expect(mockListOrganizationRoles).toHaveBeenCalledTimes(2);
            expect(mockListEnvironmentRoles).toHaveBeenCalledTimes(2);
        });
    });
});
