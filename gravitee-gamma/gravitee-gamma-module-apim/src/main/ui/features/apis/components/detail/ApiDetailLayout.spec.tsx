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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { useMutation } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
    permissionService: {
        load: jest.fn(),
        clear: jest.fn(),
        hasAllOf: jest.fn(() => true),
        hasAnyOf: jest.fn(() => true),
        getAllPermissions: jest.fn(() => []),
        subscribe: jest.fn(() => () => {}),
        getSnapshot: jest.fn(() => 0),
    },
}));

jest.mock('@tanstack/react-query', () => ({
    useMutation: jest.fn(() => ({ mutate: jest.fn(), isPending: false })),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

jest.mock('../../hooks/useApiDetail', () => ({
    useApiDetail: jest.fn(() => ({ data: null, isLoading: false })),
}));

jest.mock('../../hooks/useApiPermissions', () => ({
    useApiPermissions: jest.fn(() => ({ permissionsReady: false })),
}));

jest.mock('../../services/apis', () => ({
    deployApi: jest.fn(),
}));

jest.mock('../../utils/queryKeys', () => ({
    apiDetailKeys: {
        all: ['api-detail'],
        detail: (envId: string, apiId: string) => ['api-detail', envId, apiId],
    },
}));

jest.mock('@gravitee/graphene-core', () => ({
    Alert: ({ children, className }: { children?: ReactNode; className?: string }) => (
        <div role="alert" className={className}>
            {children}
        </div>
    ),
    AlertDescription: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
    Badge: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
    Button: ({ children, onClick, disabled }: { children?: ReactNode; onClick?: () => void; disabled?: boolean }) => (
        <button type="button" onClick={onClick} disabled={disabled}>
            {children}
        </button>
    ),
    Skeleton: () => <div />,
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('./ApiDetailSidebarNav', () => ({
    API_PROXY_NAV_GROUPS: [],
    ApiDetailSidebarNav: ({ basePath }: { basePath: string }) => <div data-testid="sidebar" data-basepath={basePath} />,
}));

import { ApiDetailIndexRedirect, ApiDetailLayout } from './ApiDetailLayout';
import { useApiDetail } from '../../hooks/useApiDetail';
import { deployApi } from '../../services/apis';

const mockUseEnvironment = useEnvironment as jest.Mock;
const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseMutation = useMutation as jest.Mock;
const mockDeployApi = deployApi as jest.Mock;

function renderAt(path: string, routePattern = 'apis/:apiId') {
    render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path={routePattern} element={<ApiDetailLayout />}>
                    <Route index element={<ApiDetailIndexRedirect />} />
                    <Route path="overview" element={<div />} />
                    <Route path="plans" element={<div />} />
                    <Route path="endpoints/list" element={<div />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
    return screen.getByTestId('sidebar').getAttribute('data-basepath') ?? '';
}

function renderLayout(apiId = 'abc-123') {
    render(
        <MemoryRouter initialEntries={[`/apis/${apiId}/overview`]}>
            <Routes>
                <Route path="apis/:apiId" element={<ApiDetailLayout />}>
                    <Route path="overview" element={<div />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

// ─── useApiBasePath ───────────────────────────────────────────────────────────

describe('useApiBasePath', () => {
    it('strips the sub-page suffix and returns the API root path', () => {
        expect(renderAt('/apis/abc-123/overview')).toBe('/apis/abc-123');
    });

    it('produces the same basePath regardless of which sub-page is active', () => {
        expect(renderAt('/apis/abc-123/plans')).toBe('/apis/abc-123');
    });

    it('handles deeply nested sub-pages', () => {
        expect(renderAt('/apis/abc-123/endpoints/list')).toBe('/apis/abc-123');
    });

    it('handles an MF host prefix — extracts only up to /apis/{id}', () => {
        expect(renderAt('/org/env/apis/abc-123/overview', 'org/env/apis/:apiId')).toBe('/org/env/apis/abc-123');
    });
});

// ─── ApiDetailIndexRedirect ───────────────────────────────────────────────────

describe('ApiDetailIndexRedirect', () => {
    it('redirects the index route to overview', () => {
        render(
            <MemoryRouter initialEntries={['/apis/abc-123']}>
                <Routes>
                    <Route path="apis/:apiId" element={<ApiDetailLayout />}>
                        <Route index element={<ApiDetailIndexRedirect />} />
                        <Route path="overview" element={<div data-testid="overview-page" />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
        expect(screen.getByTestId('overview-page')).toBeInTheDocument();
    });
});

// ─── DeployBanner ─────────────────────────────────────────────────────────────

describe('DeployBanner', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseMutation.mockReturnValue({ mutate: jest.fn(), isPending: false });
    });

    afterEach(() => jest.clearAllMocks());

    it('shows the banner and Deploy API button when NEED_REDEPLOY and user has permission', () => {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', deploymentState: 'NEED_REDEPLOY' },
            isLoading: false,
        });
        renderLayout();
        expect(screen.getByText(/pending changes/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /deploy api/i })).toBeInTheDocument();
    });

    it('calls deployApi with the correct envId and apiId when mutationFn is invoked', async () => {
        let capturedMutationFn: (() => Promise<void>) | undefined;
        mockUseMutation.mockImplementation(({ mutationFn }: { mutationFn: () => Promise<void> }) => {
            capturedMutationFn = mutationFn;
            return { mutate: jest.fn(), isPending: false };
        });
        mockDeployApi.mockResolvedValue(undefined);
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', deploymentState: 'NEED_REDEPLOY' },
            isLoading: false,
        });
        renderLayout();
        await capturedMutationFn!();
        expect(mockDeployApi).toHaveBeenCalledWith('DEFAULT', 'abc-123');
    });

    it('does not call deployApi when env is null (null-env guard)', async () => {
        mockUseEnvironment.mockReturnValue(null);
        let capturedMutationFn: (() => Promise<void>) | undefined;
        mockUseMutation.mockImplementation(({ mutationFn }: { mutationFn: () => Promise<void> }) => {
            capturedMutationFn = mutationFn;
            return { mutate: jest.fn(), isPending: false };
        });
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', deploymentState: 'NEED_REDEPLOY' },
            isLoading: false,
        });
        renderLayout();
        await capturedMutationFn!();
        expect(mockDeployApi).not.toHaveBeenCalled();
    });

    it('shows "Deploying…" and disables button while mutation is pending', () => {
        mockUseMutation.mockReturnValue({ mutate: jest.fn(), isPending: true });
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', deploymentState: 'NEED_REDEPLOY' },
            isLoading: false,
        });
        renderLayout();
        const btn = screen.getByRole('button', { name: /deploying/i });
        expect(btn).toBeDisabled();
    });

    it('hides the banner when deploymentState is DEPLOYED', () => {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', deploymentState: 'DEPLOYED' },
            isLoading: false,
        });
        renderLayout();
        expect(screen.queryByText(/pending changes/i)).not.toBeInTheDocument();
    });

    it('hides the banner when deploymentState is absent', () => {
        (useApiDetail as jest.Mock).mockReturnValue({ data: { id: 'abc-123', name: 'My API' }, isLoading: false });
        renderLayout();
        expect(screen.queryByText(/pending changes/i)).not.toBeInTheDocument();
    });

    it('hides the banner when user lacks api-definition-u permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', deploymentState: 'NEED_REDEPLOY' },
            isLoading: false,
        });
        renderLayout();
        expect(screen.queryByText(/pending changes/i)).not.toBeInTheDocument();
    });
});

// ─── ApiAvatar ────────────────────────────────────────────────────────────────

describe('ApiAvatar', () => {
    afterEach(() => jest.clearAllMocks());

    it('renders an img when _links.pictureUrl is provided', () => {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', _links: { pictureUrl: 'https://cdn.example.com/pic.png' } },
            isLoading: false,
        });
        renderLayout();
        const img = screen.getByRole('img', { name: 'My API' });
        expect(img).toHaveAttribute('src', 'https://cdn.example.com/pic.png');
    });

    it('removes the image when it fires an error event', () => {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', _links: { pictureUrl: 'https://cdn.example.com/broken.png' } },
            isLoading: false,
        });
        renderLayout();
        const img = screen.getByRole('img', { name: 'My API' });
        fireEvent.error(img);
        expect(screen.queryByRole('img', { name: 'My API' })).not.toBeInTheDocument();
    });

    it('renders no avatar when no pictureUrl is present', () => {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'Payment Gateway' },
            isLoading: false,
        });
        renderLayout();
        expect(screen.queryByRole('img', { name: 'Payment Gateway' })).not.toBeInTheDocument();
    });
});
