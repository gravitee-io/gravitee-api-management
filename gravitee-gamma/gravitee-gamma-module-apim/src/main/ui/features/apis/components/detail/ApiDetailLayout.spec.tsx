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
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

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

jest.mock('../../hooks/useApiReviewEnabled', () => ({
    useApiReviewEnabled: jest.fn(() => ({ enabled: false, isFetched: true })),
}));

const mockAskReviewMutate = jest.fn();
jest.mock('../../hooks/useApiReviewMutations', () => ({
    useAskApiReview: jest.fn(() => ({ mutate: mockAskReviewMutate, isPending: false })),
}));

jest.mock('./ApiReviewSheet', () => ({
    ApiReviewSheet: ({ open, apiId }: { open: boolean; apiId: string }) =>
        open ? <div data-testid="api-review-sheet">{apiId}</div> : null,
}));

jest.mock('../../../../shared/components', () => ({
    ConfirmDialog: ({
        open,
        title,
        confirmLabel,
        onConfirm,
    }: {
        open: boolean;
        title: string;
        confirmLabel: string;
        onConfirm: () => void;
    }) =>
        open ? (
            <div role="dialog" aria-label={title}>
                <button type="button" onClick={onConfirm}>
                    {confirmLabel}
                </button>
            </div>
        ) : null,
}));

jest.mock('../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
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
let mockCapturedLayoutDeps: unknown[][] = [];
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
            mockCapturedLayoutDeps.push(deps);
        }),
    };
});

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// The deep-link helpers reach @gravitee/gamma-lib-observability, whose charting bundle does not load
// under jsdom. This suite is about the layout, not about how a deep link is encoded.
jest.mock('../../utils/analyticsDeepLink', () => ({
    buildApiDashboardHref: () => '/env/observe/dashboards/http-proxy-overview',
    buildApiLogsHref: () => '/env/observe/logs',
}));

import { ApiDetailIndexRedirect, ApiDetailLayout } from './ApiDetailLayout';
import { useDetailBasePath } from '../../../../shared/hooks/useDetailBasePath';
import { notify } from '../../../../shared/notify';
import { useApiDetail } from '../../hooks/useApiDetail';
import { useApiPermissions } from '../../hooks/useApiPermissions';
import { useApiReviewEnabled } from '../../hooks/useApiReviewEnabled';
import { useApiScoreEnabled } from '../../hooks/useApiScoreEnabled';
import { deployApi } from '../../services/apis';

const mockUseEnvironment = useEnvironment as jest.Mock;
const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseMutation = useMutation as jest.Mock;
const mockDeployApi = deployApi as jest.Mock;

function layoutTree(apiId: string, landingSegment = 'overview', search = '') {
    return (
        <MemoryRouter initialEntries={[`/apis/${apiId}/${landingSegment}${search}`]}>
            <Routes>
                <Route path="apis/:apiId" element={<ApiDetailLayout />}>
                    <Route path={landingSegment} element={<div data-testid="api-detail-outlet" />} />
                </Route>
            </Routes>
        </MemoryRouter>
    );
}

function renderLayout(apiId = 'abc-123', landingSegment?: string, search = '') {
    mockCapturedLayoutConfig = null;
    mockCapturedLayoutDeps = [];
    mockPublishedLayoutDeps = null;
    mockBannerHost = document.createElement('div');
    document.body.appendChild(mockBannerHost);

    const view = render(layoutTree(apiId, landingSegment, search));

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

function renderIndexRedirect() {
    return render(
        <MemoryRouter initialEntries={['/apis/abc-123']}>
            <Routes>
                <Route path="apis/:apiId" element={<ApiDetailLayout />}>
                    <Route index element={<ApiDetailIndexRedirect />} />
                    <Route path="overview" element={<div data-testid="overview-page" />} />
                    <Route path="general" element={<div data-testid="general-page" />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('ApiDetailIndexRedirect', () => {
    // Keeps each case's branch decided by its own mock: the two shipped cases read the module-level
    // default, which the explicitly-mocked cases would otherwise leave overwritten.
    afterEach(() => (useApiDetail as jest.Mock).mockReturnValue({ data: null, isLoading: false }));

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

    it('keeps the query string so a Tasks deep link still opens the review', () => {
        let observedSearch = '';
        function Overview() {
            observedSearch = useLocation().search;
            return <div data-testid="overview-page" />;
        }
        render(
            <MemoryRouter initialEntries={['/apis/abc-123?review']}>
                <Routes>
                    <Route path="apis/:apiId" element={<ApiDetailLayout />}>
                        <Route index element={<ApiDetailIndexRedirect />} />
                        <Route path="overview" element={<Overview />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
        expect(screen.getByTestId('overview-page')).toBeInTheDocument();
        expect(observedSearch).toBe('?review');
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

    it('lands a federated API on general, the route its nav kept, rather than on overview', () => {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'Federated Orders', definitionVersion: 'FEDERATED' },
            isLoading: false,
            isPending: false,
        });
        renderIndexRedirect();

        expect(screen.getByTestId('general-page')).toBeInTheDocument();
        expect(screen.queryByTestId('overview-page')).not.toBeInTheDocument();
    });

    it('lands a V4 API on overview, which stays its shipped landing route', () => {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'Payments Proxy', definitionVersion: 'V4' },
            isLoading: false,
            isPending: false,
        });
        renderIndexRedirect();

        expect(screen.getByTestId('overview-page')).toBeInTheDocument();
        expect(screen.queryByTestId('general-page')).not.toBeInTheDocument();
    });

    it('redirects nowhere while the detail query has produced no result, even when it reports no loading', () => {
        // The window this guards is a query that is disabled rather than in flight — the first render of a
        // host-mounted deep link, before the environment resolves — which reports isLoading: false with no
        // data. A replace redirect fired there cannot be undone once the definition version arrives.
        (useApiDetail as jest.Mock).mockReturnValue({ data: undefined, isLoading: false, isPending: true });
        renderIndexRedirect();

        expect(screen.queryByTestId('overview-page')).not.toBeInTheDocument();
        expect(screen.queryByTestId('general-page')).not.toBeInTheDocument();
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

// ─── API review ───────────────────────────────────────────────────────────────

describe('API review', () => {
    const mockUseApiReviewEnabled = useApiReviewEnabled as jest.Mock;
    const mockUseApiPermissions = useApiPermissions as jest.Mock;

    function grant(...permissions: string[]) {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => anyOf.some(p => permissions.includes(p)));
    }

    function mockApi(overrides: Record<string, unknown>) {
        (useApiDetail as jest.Mock).mockReturnValue({ data: { id: 'abc-123', name: 'My API', ...overrides }, isLoading: false });
    }

    beforeEach(() => {
        mockUseMutation.mockReturnValue({ mutate: jest.fn(), isPending: false });
        mockUseApiReviewEnabled.mockReturnValue({ enabled: true, isFetched: true });
        mockUseApiPermissions.mockReturnValue({ permissionsReady: true });
        grant('api-definition-u', 'api-reviews-u');
    });

    afterEach(() => {
        jest.clearAllMocks();
        mockUseHasPermission.mockReturnValue(true);
    });

    it('shows no review banner while review is disabled for the environment', () => {
        mockUseApiReviewEnabled.mockReturnValue({ enabled: false, isFetched: true });
        mockApi({ workflowState: 'DRAFT' });
        renderLayout();
        expect(screen.queryByRole('region', { name: 'API review status' })).not.toBeInTheDocument();
    });

    it('lets an author ask for a review from the draft banner after confirming', () => {
        grant('api-definition-u');
        mockApi({ workflowState: 'DRAFT' });
        renderLayout();

        expect(screen.getByText('This API is a draft.')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Ask for a review' }));
        expect(screen.getByRole('dialog', { name: 'Review API' })).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Ask for review' }));
        expect(mockAskReviewMutate).toHaveBeenCalledWith(undefined, expect.objectContaining({ onSuccess: expect.any(Function) }));

        const options = mockAskReviewMutate.mock.calls[0]?.[1] as { onSuccess: () => void };
        options.onSuccess();
        expect(notify.success).toHaveBeenCalledWith('Review has been asked.');
    });

    it('hides the ask action from a viewer without api-definition-u', () => {
        grant('api-reviews-u');
        mockApi({ workflowState: 'DRAFT' });
        renderLayout();
        expect(screen.getByText('This API is a draft.')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Ask for a review' })).not.toBeInTheDocument();
    });

    it('opens the review sheet for a reviewer from the in-review banner', () => {
        mockApi({ workflowState: 'IN_REVIEW' });
        renderLayout();

        expect(screen.getByText('This API has changes waiting for your review.')).toBeInTheDocument();
        expect(screen.queryByTestId('api-review-sheet')).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Review changes' }));
        expect(screen.getByTestId('api-review-sheet')).toHaveTextContent('abc-123');
    });

    it('keeps the layout dependencies stable across a rerender so the host layout does not re-register forever', () => {
        mockApi({ workflowState: 'IN_REVIEW' });
        renderLayout();

        // Opening the sheet rerenders the layout without touching anything the host layout depends on.
        fireEvent.click(screen.getByRole('button', { name: 'Review changes' }));

        const [previous, latest] = mockCapturedLayoutDeps.slice(-2);
        expect(latest).toHaveLength(previous.length);
        latest.forEach((dep, index) => expect(Object.is(dep, previous[index])).toBe(true));
    });

    it('tells a non-reviewer the review is pending without offering a decision', () => {
        grant('api-definition-u');
        mockApi({ workflowState: 'IN_REVIEW' });
        renderLayout();
        expect(screen.getByText('The API reviewer has been asked to review the changes.')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Review changes' })).not.toBeInTheDocument();
    });

    it('waits for the API permissions before deciding which wording to show', () => {
        mockUseApiPermissions.mockReturnValue({ permissionsReady: false });
        mockApi({ workflowState: 'IN_REVIEW' });
        renderLayout();
        expect(screen.queryByRole('region', { name: 'API review status' })).not.toBeInTheDocument();
    });

    it('holds back the out-of-sync deploy banner while the review is pending', () => {
        mockApi({ workflowState: 'IN_REVIEW', deploymentState: 'NEED_REDEPLOY' });
        renderLayout();
        expect(screen.queryByText(/out of sync/i)).not.toBeInTheDocument();

        mockApi({ workflowState: 'REVIEW_OK', deploymentState: 'NEED_REDEPLOY' });
        renderLayout();
        expect(screen.getByText(/out of sync/i)).toBeInTheDocument();
    });

    it('opens the review sheet straight away for a reviewer arriving with ?review', () => {
        mockApi({ workflowState: 'IN_REVIEW' });
        renderLayout('abc-123', 'overview', '?review');
        expect(screen.getByTestId('api-review-sheet')).toBeInTheDocument();
    });

    it('ignores ?review for a user who cannot review', () => {
        grant('api-definition-u');
        mockApi({ workflowState: 'IN_REVIEW' });
        renderLayout('abc-123', 'overview', '?review');
        expect(screen.queryByTestId('api-review-sheet')).not.toBeInTheDocument();
    });

    it('ignores ?review once the review has been accepted', () => {
        mockApi({ workflowState: 'REVIEW_OK' });
        renderLayout('abc-123', 'overview', '?review');
        expect(screen.queryByTestId('api-review-sheet')).not.toBeInTheDocument();
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

    // Both names run past 40 characters, the point at which the breadcrumb label deliberately slices and
    // appends an ellipsis — so a header that borrowed that cap would fail on the routed API's own name.
    const ROUTED_FEDERATED_API = {
        id: 'federated-orders',
        name: 'Orders Federated API for the EMEA region and partners',
        definitionVersion: 'FEDERATED',
    };
    const OTHER_FEDERATED_API = {
        id: 'federated-billing',
        name: 'Billing Federated API for the AMER region and partners',
        definitionVersion: 'FEDERATED',
    };

    it('renders the routed federated API name in full on the general route a federated row lands on', () => {
        (useApiDetail as jest.Mock).mockImplementation((apiId: string) => ({
            data: apiId === ROUTED_FEDERATED_API.id ? ROUTED_FEDERATED_API : OTHER_FEDERATED_API,
            isLoading: false,
            isError: false,
        }));
        renderLayout(ROUTED_FEDERATED_API.id, 'general');
        renderSidebar();

        expect(screen.getByText(ROUTED_FEDERATED_API.name)).toBeInTheDocument();
        expect(screen.queryByText(OTHER_FEDERATED_API.name)).not.toBeInTheDocument();
    });

    it('renders no API name once a refetch fails on an already-loaded API', () => {
        // A failed refetch keeps the last successful data, so isError is the only value that changes — and the
        // only arrangement in which a denied detail request still has a name in hand to render.
        const loadedApi = { id: 'abc-123', name: 'Payment Gateway' };
        (useApiDetail as jest.Mock).mockReturnValue({ data: loadedApi, isLoading: false, isError: false });
        const { rerender } = renderLayout();

        (useApiDetail as jest.Mock).mockReturnValue({ data: loadedApi, isLoading: false, isError: true });
        rerender(layoutTree('abc-123'));
        renderSidebar();

        expect(screen.queryByText('Payment Gateway')).not.toBeInTheDocument();
    });
});

// ─── Sidebar navigation ───────────────────────────────────────────────────────

// One label per API_PROXY_NAV_GROUPS group, so a nav missing a whole group cannot pass.
const NAV_ITEM_LABELS = ['Overview', 'Entrypoints', 'Policy Studio', 'Plans', 'User Permissions', 'Audit Logs', 'Sharding Tags'];

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

// ─── Sidebar navigation — federated APIs ──────────────────────────────────────

// Labels, not paths: the label is what a user sees, and a nav rendering the right hrefs with the wrong rows
// would still pass a path-based assertion. Every nav row, link or `comingSoon` button, in one list — a row
// filtered out of the item list and a row still rendered as a disabled one are then told apart.
function renderedNavLabels(): string[] {
    return NAV_ITEM_ROLES.flatMap(role => screen.queryAllByRole(role))
        .map(row => row.textContent?.trim() ?? '')
        .sort();
}

// Asserted as the complete set, mirroring the production allow-list: a nav item added later reaches a federated
// API's sidebar only once someone confirms it applies, and fails this test until they do.
const FEDERATED_SHOWN_LABELS = [
    'Settings',
    'User Permissions',
    'Metadata',
    'Plans',
    'Subscriptions',
    'Broadcasts',
    'Audit Logs',
    'API Score',
    // The two Observability deep links, which survive federation.
    'Dashboard',
    'Logs',
];

// The same enumeration for an API that is not federated: every row of the canonical nav under this suite's mocks
// (permissions granted, API Score enabled, HTTP PROXY, no TCP listeners). 'Response Templates' survives its own
// permission and API-subtype gates here, so federation is what removes it from the list above.
const NON_FEDERATED_SHOWN_LABELS = [
    'Overview',
    'Settings',
    'User Permissions',
    'Authorization',
    'Metadata',
    'Entrypoints',
    'Policy Studio',
    'Endpoints',
    'Failover',
    'Response Templates',
    'Resources',
    'API Properties',
    'CORS',
    'Plans',
    'Subscriptions',
    'Broadcasts',
    'Notifications',
    'Alerts',
    'Audit Logs',
    'Health Check Dashboard',
    'API Score',
    'Dashboard',
    'Logs',
    'Sharding Tags',
    'Deployment History',
    'Reporter Settings',
];

const FEDERATED_EMPTIED_GROUP_HEADINGS = ['Design', 'Operations'];

const FEDERATED_KEPT_LINKS: [label: string, path: string][] = [
    ['Settings', 'general'],
    ['Plans', 'plans'],
    ['Subscriptions', 'consumers'],
    ['Broadcasts', 'broadcasts'],
    ['User Permissions', 'user-permissions'],
    ['Audit Logs', 'audit-logs'],
    // Kept for the api-metadata-r holder the default useHasPermission mock stands in for: federation must not be
    // the thing that removes it, so this entry fails the moment 'metadata' joins the federated omission set.
    ['Metadata', 'metadata'],
];

const API_BASE_PATH = '/apis/abc-123';

describe('ApiDetailSidebarNav in the detail layout — federated API', () => {
    beforeEach(() => {
        (useApiPermissions as jest.Mock).mockReturnValue({ permissionsReady: true });
        (useApiScoreEnabled as jest.Mock).mockReturnValue({ enabled: true, isFetched: true });
        // The viewer holds api-metadata-r and api-response_templates-r: both items survive their own permission
        // gate here, so federation is the only thing left that can remove them. Set explicitly rather than left to
        // the module-level default, which an earlier describe overwrites with false for the rest of the file.
        mockUseHasPermission.mockReturnValue(true);
    });

    afterEach(() => {
        jest.clearAllMocks();
        (useApiPermissions as jest.Mock).mockReturnValue({ permissionsReady: false });
        (useApiScoreEnabled as jest.Mock).mockReturnValue({ enabled: true, isFetched: true });
    });

    function renderSidebarForApiOfDefinitionVersion(definitionVersion: string) {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'My API', definitionVersion, type: 'PROXY' },
            isLoading: false,
            isError: false,
        });
        const view = renderLayout();
        renderSidebar();
        return view;
    }

    function renderSidebarForFederatedApi() {
        return renderSidebarForApiOfDefinitionVersion('FEDERATED');
    }

    it('shows only the sections a federated API applies to, and drops the group headings they empty', () => {
        renderSidebarForFederatedApi();

        expect(renderedNavLabels()).toEqual([...FEDERATED_SHOWN_LABELS].sort());
        for (const heading of FEDERATED_EMPTIED_GROUP_HEADINGS) {
            expect(screen.queryByText(heading)).not.toBeInTheDocument();
        }
    });

    it('keeps the sections a federated API does have backing data for as navigable links', () => {
        renderSidebarForFederatedApi();

        for (const [label, path] of FEDERATED_KEPT_LINKS) {
            expect(screen.getByRole('link', { name: new RegExp(`^${label}$`, 'i') })).toHaveAttribute('href', `${API_BASE_PATH}/${path}`);
        }
    });

    it('keeps API Score as a navigable link when the environment has API Score enabled', () => {
        renderSidebarForFederatedApi();

        expect(screen.getByRole('link', { name: /^api score$/i })).toHaveAttribute('href', `${API_BASE_PATH}/api-score`);
    });

    // The flag is fetched, so an API-Score-enabled environment reports `false` on the first render and `true` only
    // once the portal configuration arrives. The sidebar reaches the host layout through a deps-keyed publish, so
    // this is the only case that fails when `apiScoreEnabled` is dropped from the useLayoutConfig dependency array.
    it('gains the API Score link when the environment flag arrives after the first render', () => {
        (useApiScoreEnabled as jest.Mock).mockReturnValue({ enabled: false, isFetched: false });
        const { rerender } = renderSidebarForFederatedApi();
        expect(screen.queryByRole('link', { name: /^api score$/i })).not.toBeInTheDocument();

        (useApiScoreEnabled as jest.Mock).mockReturnValue({ enabled: true, isFetched: true });
        rerender(layoutTree('abc-123'));
        renderSidebar();

        expect(screen.getByRole('link', { name: /^api score$/i })).toHaveAttribute('href', `${API_BASE_PATH}/api-score`);
    });

    it('removes only the API Score row when the environment has API Score disabled', () => {
        (useApiScoreEnabled as jest.Mock).mockReturnValue({ enabled: false, isFetched: true });
        renderSidebarForFederatedApi();

        expect(screen.queryByRole('link', { name: /^api score$/i })).not.toBeInTheDocument();
        expect(screen.queryByText('API Score')).not.toBeInTheDocument();
        // The group heading is the only 'General' that is not a link, so the selector is what separates it
        // from the General nav item — an emptied-out sidebar would satisfy the two absences above on its own.
        expect(screen.getByText('General', { selector: 'p' })).toBeInTheDocument();
        for (const [label, path] of FEDERATED_KEPT_LINKS) {
            expect(screen.getByRole('link', { name: new RegExp(`^${label}$`, 'i') })).toHaveAttribute('href', `${API_BASE_PATH}/${path}`);
        }
    });

    // These are the only cases that state a definition version explicitly — every other sidebar case leaves the
    // field off entirely — so a federated marker broadened to match any API that names a version, or broadened
    // to anything-but-'V4', is caught here and nowhere else.
    it.each(['V4', 'V4_NATIVE'])('keeps every section for a %s API, which is not federated', definitionVersion => {
        renderSidebarForApiOfDefinitionVersion(definitionVersion);

        expect(renderedNavLabels()).toEqual([...NON_FEDERATED_SHOWN_LABELS].sort());
        for (const heading of FEDERATED_EMPTIED_GROUP_HEADINGS) {
            expect(screen.getByText(heading)).toBeInTheDocument();
        }
    });
});

// ─── Federated agent APIs ─────────────────────────────────────────────────────

describe('ApiDetailLayout — federated agent API', () => {
    const AGENT_NAME = 'Fraud Detection Agent';

    beforeEach(() => {
        (useApiPermissions as jest.Mock).mockReturnValue({ permissionsReady: true });
        // A SUCCESSFUL fetch, not a failure: GET /apis/{id} answers 200 for a federated agent, so the
        // shipped isError branch never fires and only a definition-version check can close this route.
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: AGENT_NAME, definitionVersion: 'FEDERATED_AGENT' },
            isLoading: false,
            isError: false,
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
        (useApiPermissions as jest.Mock).mockReturnValue({ permissionsReady: false });
    });

    it('renders no detail page for an id that resolves to a federated agent', () => {
        renderLayout();
        renderSidebar();

        expect(screen.queryByTestId('api-detail-outlet')).not.toBeInTheDocument();
        for (const role of NAV_ITEM_ROLES) {
            expect(screen.queryAllByRole(role)).toHaveLength(0);
        }
        expect(screen.queryByText(AGENT_NAME)).not.toBeInTheDocument();
        // Pinned as an exact string, not a pattern: the copy is what tells the user why the page is blank,
        // and an empty or reworded message is indistinguishable from the blocked page working correctly.
        expect(screen.getByText('This API type is not available in API Proxies.')).toBeInTheDocument();
        // The load-failure copy would be a false statement about a request that succeeded.
        expect(screen.queryByText(/failed to load api/i)).not.toBeInTheDocument();
    });

    // The counterpart the blocked case needs: nothing else asserts the routed page renders at all, so an
    // agent block widened to every federated API — or to every API — would look exactly like this one working.
    it('still renders the routed detail page for a federated API, which is not an agent', () => {
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: 'Federated Orders', definitionVersion: 'FEDERATED' },
            isLoading: false,
            isError: false,
        });
        renderLayout();

        expect(screen.getByTestId('api-detail-outlet')).toBeInTheDocument();
        expect(screen.queryByText('This API type is not available in API Proxies.')).not.toBeInTheDocument();
    });

    it('shows the load-failure copy rather than the unsupported-type copy when the agent detail request fails', () => {
        // A failed refetch keeps the last successful payload, so both the agent marker and isError are
        // true at once — and a load failure is the more specific thing to tell the user about.
        (useApiDetail as jest.Mock).mockReturnValue({
            data: { id: 'abc-123', name: AGENT_NAME, definitionVersion: 'FEDERATED_AGENT' },
            isLoading: false,
            isError: true,
        });
        renderLayout();

        expect(screen.getByText('Failed to load API. It may have been deleted or you may not have access.')).toBeInTheDocument();
        expect(screen.queryByText('This API type is not available in API Proxies.')).not.toBeInTheDocument();
    });
});
