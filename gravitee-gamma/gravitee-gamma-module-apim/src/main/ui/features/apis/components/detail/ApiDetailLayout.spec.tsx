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
import { fireEvent, render, renderHook, screen } from '@testing-library/react';
import type { ReactElement, ReactNode } from 'react';
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

jest.mock('../../hooks/useApiScoreEnabled', () => ({
    useApiScoreEnabled: jest.fn(() => ({ enabled: true, isFetched: true })),
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

let mockCapturedLayoutConfig: Record<string, unknown> | null = null;
let mockPublishedLayoutDeps: unknown[] | null = null;
let mockBannerHost: HTMLDivElement | null = null;

jest.mock('@gravitee/graphene-core', () => {
    return {
        Alert: ({ children, ...props }: { children?: ReactNode; role?: string; 'aria-label'?: string }) => (
            <div role={props.role} aria-label={props['aria-label']}>
                {children}
            </div>
        ),
        AlertTitle: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        AlertDescription: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        AlertAction: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        Badge: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
        Button: ({ children, onClick, disabled }: { children?: ReactNode; onClick?: () => void; disabled?: boolean }) => (
            <button type="button" onClick={onClick} disabled={disabled}>
                {children}
            </button>
        ),
        Skeleton: () => <div />,
        cn: (...classes: unknown[]) => classes.filter(Boolean).join(' '),
        TooltipProvider: ({ children }: { children?: ReactNode }) => <>{children}</>,
        Tooltip: ({ children }: { children?: ReactNode }) => <>{children}</>,
        TooltipTrigger: ({ children }: { children?: ReactNode }) => <>{children}</>,
        TooltipContent: ({ children }: { children?: ReactNode }) => <>{children}</>,
        ContextSidebar: ({ children, header }: { children?: ReactNode; header?: ReactNode }) => (
            <div>
                {header}
                {children}
            </div>
        ),
        ContextToggleButton: () => <button />,
        Dialog: ({ children, open }: { children?: ReactNode; open?: boolean }) => (open ? <div>{children}</div> : null),
        DialogClose: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
        DialogContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        DialogDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
        DialogFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        DialogHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        DialogTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        Input: (props: any) => <input {...props} />,
        Label: ({ children, htmlFor }: { children?: ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
        // The real hook publishes from an effect keyed on the caller's dependency list, so a re-render only reaches
        // the host layout when a dependency changed. A fake that captured on every render would stay green with a
        // dependency missing from that list.
        useLayoutConfig: jest.fn((config: Record<string, unknown>, deps: unknown[] = []) => {
            const unchanged =
                mockPublishedLayoutDeps !== null &&
                mockPublishedLayoutDeps.length === deps.length &&
                mockPublishedLayoutDeps.every((dep, index) => Object.is(dep, deps[index]));
            if (unchanged) return;
            mockPublishedLayoutDeps = deps;
            mockCapturedLayoutConfig = { ...(mockCapturedLayoutConfig ?? {}), ...config };
        }),
    };
});

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

import { ApiDetailIndexRedirect, ApiDetailLayout } from './ApiDetailLayout';
import { useDetailBasePath } from '../../../../shared/hooks/useDetailBasePath';
import { useApiDetail } from '../../hooks/useApiDetail';
import { useApiPermissions } from '../../hooks/useApiPermissions';
import { deployApi } from '../../services/apis';

const mockUseEnvironment = useEnvironment as jest.Mock;
const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseMutation = useMutation as jest.Mock;
const mockDeployApi = deployApi as jest.Mock;

function layoutTree(apiId: string) {
    return (
        <MemoryRouter initialEntries={[`/apis/${apiId}/overview`]}>
            <Routes>
                <Route path="apis/:apiId" element={<ApiDetailLayout />}>
                    <Route path="overview" element={<div />} />
                </Route>
            </Routes>
        </MemoryRouter>
    );
}

function renderLayout(apiId = 'abc-123') {
    mockCapturedLayoutConfig = null;
    mockPublishedLayoutDeps = null;
    mockBannerHost = document.createElement('div');
    document.body.appendChild(mockBannerHost);

    const view = render(layoutTree(apiId));

    const layoutConfig = mockCapturedLayoutConfig as Record<string, unknown> | null;
    if (layoutConfig?.banner) {
        render(layoutConfig.banner as ReactElement, { container: mockBannerHost! });
    }
    return view;
}

// ─── useApiBasePath (via useDetailBasePath) ───────────────────────────────────

describe('useApiBasePath', () => {
    function hookAt(path: string, id = 'abc-123') {
        const wrapper = ({ children }: { children: ReactNode }) => <MemoryRouter initialEntries={[path]}>{children}</MemoryRouter>;
        const { result } = renderHook(() => useDetailBasePath('apis', id), { wrapper });
        return result.current;
    }

    it('strips the sub-page suffix and returns the API root path', () => {
        expect(hookAt('/apis/abc-123/overview')).toBe('/apis/abc-123');
    });

    it('produces the same basePath regardless of which sub-page is active', () => {
        expect(hookAt('/apis/abc-123/plans')).toBe('/apis/abc-123');
    });

    it('handles deeply nested sub-pages', () => {
        expect(hookAt('/apis/abc-123/endpoints/list')).toBe('/apis/abc-123');
    });

    it('handles an MF host prefix — extracts only up to /apis/{id}', () => {
        expect(hookAt('/org/env/apis/abc-123/overview')).toBe('/org/env/apis/abc-123');
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

    it('redirects an unknown sub-path to overview without looping', () => {
        render(
            <MemoryRouter initialEntries={['/apis/abc-123/analytics']}>
                <Routes>
                    <Route path="apis/:apiId" element={<ApiDetailLayout />}>
                        <Route path="overview" element={<div data-testid="overview-page" />} />
                        <Route path="*" element={<ApiDetailIndexRedirect />} />
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
        expect(screen.getByText(/out of sync/i)).toBeInTheDocument();
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
        expect(mockDeployApi).toHaveBeenCalledWith('DEFAULT', 'abc-123', undefined);
    });

    it('passes the deployment label to deployApi when the mutationFn receives one', async () => {
        let capturedMutationFn: ((label?: string) => Promise<void>) | undefined;
        mockUseMutation.mockImplementation(({ mutationFn }: { mutationFn: (label?: string) => Promise<void> }) => {
            capturedMutationFn = mutationFn;
            return { mutate: jest.fn(), isPending: false };
        });
        mockDeployApi.mockResolvedValue(undefined);
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', deploymentState: 'NEED_REDEPLOY' },
            isLoading: false,
        });
        renderLayout();
        await capturedMutationFn!('v2.1-release');
        expect(mockDeployApi).toHaveBeenCalledWith('DEFAULT', 'abc-123', 'v2.1-release');
    });

    it('opens the deployment label dialog when "Deploy API" is clicked', () => {
        const mutate = jest.fn();
        mockUseMutation.mockReturnValue({ mutate, isPending: false });
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', deploymentState: 'NEED_REDEPLOY' },
            isLoading: false,
        });
        renderLayout();

        expect(screen.queryByText(/deploy your api/i)).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: /deploy api/i }));
        expect(screen.getByText(/deploy your api/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/deployment label/i)).toBeInTheDocument();
    });

    it('deploys with the entered label from the dialog', () => {
        const mutate = jest.fn();
        mockUseMutation.mockReturnValue({ mutate, isPending: false });
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', deploymentState: 'NEED_REDEPLOY' },
            isLoading: false,
        });
        renderLayout();

        fireEvent.click(screen.getByRole('button', { name: /deploy api/i }));
        fireEvent.change(screen.getByLabelText(/deployment label/i), { target: { value: 'hotfix-cors' } });
        fireEvent.click(screen.getByRole('button', { name: /^deploy$/i }));
        expect(mutate).toHaveBeenCalledWith('hotfix-cors');
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
        expect(screen.queryByText(/out of sync/i)).not.toBeInTheDocument();
    });

    it('hides the banner when deploymentState is absent', () => {
        (useApiDetail as jest.Mock).mockReturnValue({ data: { id: 'abc-123', name: 'My API' }, isLoading: false });
        renderLayout();
        expect(screen.queryByText(/out of sync/i)).not.toBeInTheDocument();
    });

    it('hides the banner when user lacks api-definition-u permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', deploymentState: 'NEED_REDEPLOY' },
            isLoading: false,
        });
        renderLayout();
        expect(screen.queryByText(/out of sync/i)).not.toBeInTheDocument();
    });
});

// ─── ApiAvatar ────────────────────────────────────────────────────────────────

function renderSidebar() {
    if (mockCapturedLayoutConfig?.contextSidebar) {
        render(<MemoryRouter>{mockCapturedLayoutConfig.contextSidebar as ReactElement}</MemoryRouter>);
    }
}

describe('ApiInfoHeader proxy type badge', () => {
    afterEach(() => jest.clearAllMocks());

    function renderSidebarForApi(api: Record<string, unknown>) {
        (useApiDetail as jest.Mock).mockReturnValue({ data: api, isLoading: false });
        renderLayout();
        renderSidebar();
    }

    it('shows TCP Proxy for an API with TCP listeners', () => {
        renderSidebarForApi({
            id: 'abc-123',
            name: 'TCP API',
            type: 'PROXY',
            listeners: [{ type: 'TCP', hosts: ['tcp.example.com'] }],
        });
        expect(screen.getByText('TCP Proxy')).toBeInTheDocument();
    });

    it('shows HTTP Proxy for an API without TCP listeners', () => {
        renderSidebarForApi({
            id: 'abc-123',
            name: 'HTTP API',
            type: 'PROXY',
            listeners: [{ type: 'HTTP', paths: [{ path: '/foo' }] }],
        });
        expect(screen.getByText('HTTP Proxy')).toBeInTheDocument();
    });

    it('detects TCP Proxy from tcp-proxy endpoint groups when listeners are absent', () => {
        renderSidebarForApi({
            id: 'abc-123',
            name: 'TCP API',
            type: 'PROXY',
            listeners: [],
            endpointGroups: [{ type: 'tcp-proxy', name: 'Default TCP Proxy group' }],
        });
        expect(screen.getByText('TCP Proxy')).toBeInTheDocument();
    });
});

describe('ApiAvatar', () => {
    afterEach(() => jest.clearAllMocks());

    it('renders an img when _links.pictureUrl is provided', () => {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', _links: { pictureUrl: 'https://cdn.example.com/pic.png' } },
            isLoading: false,
        });
        renderLayout();
        renderSidebar();
        const img = screen.getByRole('img', { name: 'My API' });
        expect(img).toHaveAttribute('src', 'https://cdn.example.com/pic.png');
    });

    it('removes the image when it fires an error event', () => {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', _links: { pictureUrl: 'https://cdn.example.com/broken.png' } },
            isLoading: false,
        });
        renderLayout();
        renderSidebar();
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

// ─── ApiInfoHeader ────────────────────────────────────────────────────────────

describe('ApiInfoHeader', () => {
    afterEach(() => jest.clearAllMocks());

    it('renders no API name when the API detail request failed', () => {
        (useApiDetail as jest.Mock).mockReturnValue({ data: undefined, isLoading: false, isError: true });
        renderLayout();
        renderSidebar();
        expect(screen.queryByText('Payment Gateway')).not.toBeInTheDocument();
    });

    it('renders the API name when the API detail request succeeded', () => {
        (useApiDetail as jest.Mock).mockReturnValue({ data: { id: 'abc-123', name: 'Payment Gateway' }, isLoading: false });
        renderLayout();
        renderSidebar();
        expect(screen.getByText('Payment Gateway')).toBeInTheDocument();
    });
});

// ─── Sidebar navigation ───────────────────────────────────────────────────────

// One label per API_PROXY_NAV_GROUPS group, so a nav missing a whole group cannot pass.
const NAV_ITEM_LABELS = ['Overview', 'Entrypoints', 'Policy Studio', 'Plans', 'User Permissions', 'Audit Logs', 'Deployment'];

// Every API_PROXY_NAV_GROUPS item renders as exactly one of these: a NavLink, a collapsible parent button, or a
// coming-soon row carrying role="button" — so an empty result for both roles means no nav item rendered at all.
const NAV_ITEM_ROLES = ['link', 'button'] as const;

describe('ApiDetailSidebarNav in the detail layout', () => {
    beforeEach(() => {
        (useApiPermissions as jest.Mock).mockReturnValue({ permissionsReady: true });
    });

    afterEach(() => {
        jest.clearAllMocks();
        (useApiPermissions as jest.Mock).mockReturnValue({ permissionsReady: false });
    });

    it('renders no navigation item when the API detail request failed', () => {
        (useApiDetail as jest.Mock).mockReturnValue({ data: undefined, isLoading: false, isError: true });
        renderLayout();
        renderSidebar();

        for (const role of NAV_ITEM_ROLES) {
            expect(screen.queryAllByRole(role)).toHaveLength(0);
        }
    });

    it('renders every navigation item when the API detail request succeeded', () => {
        (useApiDetail as jest.Mock).mockReturnValue({ data: { id: 'abc-123', name: 'Payment Gateway' }, isLoading: false, isError: false });
        renderLayout();
        renderSidebar();

        for (const label of NAV_ITEM_LABELS) {
            expect(screen.getByText(label)).toBeInTheDocument();
        }
    });

    it('renders no navigation item once a refetch fails on an already-loaded API', () => {
        // A failed refetch keeps the last successful data, so isError is the only value that changes.
        const loadedApi = { id: 'abc-123', name: 'Payment Gateway' };
        (useApiDetail as jest.Mock).mockReturnValue({ data: loadedApi, isLoading: false, isError: false });
        const { rerender } = renderLayout();

        (useApiDetail as jest.Mock).mockReturnValue({ data: loadedApi, isLoading: false, isError: true });
        rerender(layoutTree('abc-123'));
        renderSidebar();

        for (const role of NAV_ITEM_ROLES) {
            expect(screen.queryAllByRole(role)).toHaveLength(0);
        }
    });
});
