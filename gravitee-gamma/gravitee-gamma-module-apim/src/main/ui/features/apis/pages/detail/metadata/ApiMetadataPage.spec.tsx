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
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiMetadataPage } from './ApiMetadataPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiMetadata, useCreateApiMetadata, useDeleteApiMetadata, useUpdateApiMetadata } from '../../../hooks/useApiMetadata';
import type { ApiMetadata } from '../../../types/metadata';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../../hooks/useApiMetadata', () => ({
    useApiMetadata: jest.fn(),
    useCreateApiMetadata: jest.fn(),
    useUpdateApiMetadata: jest.fn(),
    useDeleteApiMetadata: jest.fn(),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseApiMetadata = useApiMetadata as jest.Mock;
const mockUseCreateApiMetadata = useCreateApiMetadata as jest.Mock;
const mockUseUpdateApiMetadata = useUpdateApiMetadata as jest.Mock;
const mockUseDeleteApiMetadata = useDeleteApiMetadata as jest.Mock;

const TEAM: ApiMetadata = { key: 'team', name: 'Team', format: 'STRING', value: 'Platform Engineering' };

function idleMutation() {
    return { mutateAsync: jest.fn(), isPending: false };
}

function renderPage() {
    render(
        <MemoryRouter initialEntries={['/apis/api-1/metadata']}>
            <Routes>
                <Route path="apis/:apiId/metadata" element={<ApiMetadataPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUseApiDetailContext.mockReturnValue({
        api: { id: 'api-1', definitionContext: { origin: 'MANAGEMENT' } },
        isLoading: false,
        permissionsReady: true,
    });
    mockUseApiMetadata.mockReturnValue({ data: { data: [TEAM], pagination: { totalCount: 1 } }, isLoading: false, isError: false });
    mockUseCreateApiMetadata.mockReturnValue(idleMutation());
    mockUseUpdateApiMetadata.mockReturnValue(idleMutation());
    mockUseDeleteApiMetadata.mockReturnValue(idleMutation());
});

describe('ApiMetadataPage', () => {
    it('renders the classic API metadata heading and create action', () => {
        renderPage();
        expect(screen.queryByRole('heading', { name: 'API metadata' })).not.toBeNull();
        expect(
            screen.queryByText('Set metadata information on the API that can be easily accessed through Markdown templating.'),
        ).not.toBeNull();
        expect(screen.queryByRole('button', { name: /add api metadata/i })).not.toBeNull();
        expect(screen.queryByRole('combobox', { name: 'Filter by source' })).not.toBeNull();
    });

    it('hides the add button when the user cannot create metadata', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-metadata-c'));
        renderPage();
        expect(screen.queryByRole('button', { name: /add api metadata/i })).toBeNull();
    });

    it('does not flash a permission denied message while API permissions are loading', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { id: 'api-1', definitionContext: { origin: 'MANAGEMENT' } },
            isLoading: false,
            permissionsReady: false,
        });
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        expect(screen.queryByText(/don't have permission to view API metadata/i)).toBeNull();
        expect(screen.queryByRole('heading', { name: 'API metadata' })).toBeNull();
        expect(mockUseApiMetadata).toHaveBeenCalledWith('api-1', expect.any(Object), false);
    });

    it('shows a permission denied message when the user lacks api-metadata-r', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-metadata-r'));
        renderPage();
        expect(screen.queryByText(/don't have permission to view API metadata/i)).not.toBeNull();
        expect(screen.queryByRole('button', { name: /add api metadata/i })).toBeNull();
        expect(mockUseApiMetadata).toHaveBeenCalledWith('api-1', expect.any(Object), false);
    });

    it('hides the add button and shows a read-only banner for Kubernetes APIs', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { id: 'api-1', definitionContext: { origin: 'KUBERNETES' } },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        expect(screen.queryByRole('button', { name: /add api metadata/i })).toBeNull();
        expect(screen.queryByText(/managed by the Kubernetes operator/i)).not.toBeNull();
    });

    it('hides the header add button on first-use and keeps the empty-state CTA', () => {
        mockUseApiMetadata.mockReturnValue({ data: { data: [], pagination: { totalCount: 0 } }, isLoading: false, isError: false });
        renderPage();
        expect(screen.queryByText('No API metadata')).not.toBeNull();
        expect(screen.getAllByRole('button', { name: /add api metadata/i })).toHaveLength(1);
        expect(screen.queryByRole('combobox', { name: 'Filter by source' })).toBeNull();
    });

    it('shows an error alert when the metadata query fails', () => {
        mockUseApiMetadata.mockReturnValue({ data: undefined, isLoading: false, isError: true });
        renderPage();
        expect(screen.queryByText('Failed to load metadata. Please refresh the page.')).not.toBeNull();
    });
});
