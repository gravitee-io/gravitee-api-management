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
        useLayoutConfig: jest.fn((config: Record<string, unknown>, deps: unknown[]) => {
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

jest.mock('./ApiDetailSidebarNav', () => ({
    API_PROXY_NAV_GROUPS: [],
    ApiDetailSidebarNav: () => <div />,
    withTcpRestrictions: (groups: unknown[]) => groups,
    withMetadataPermission: (groups: unknown[]) => groups,
    withResponseTemplatesPermission: (groups: unknown[]) => groups,
    withApiScoreEnabled: (groups: unknown[]) => groups,
    withObservabilityLinks: (groups: unknown[]) => groups,
}));

import { ApiDetailIndexRedirect, ApiDetailLayout } from './ApiDetailLayout';
import { useDetailBasePath } from '../../../../shared/hooks/useDetailBasePath';
import { notify } from '../../../../shared/notify';
import { useApiDetail } from '../../hooks/useApiDetail';
import { useApiPermissions } from '../../hooks/useApiPermissions';
import { useApiReviewEnabled } from '../../hooks/useApiReviewEnabled';
import { deployApi } from '../../services/apis';

const mockUseEnvironment = useEnvironment as jest.Mock;
const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseMutation = useMutation as jest.Mock;
const mockDeployApi = deployApi as jest.Mock;

function renderLayout(apiId = 'abc-123', initialEntry = `/apis/${apiId}/overview`) {
    mockCapturedLayoutConfig = null;
    mockCapturedLayoutDeps = [];
    mockBannerHost = document.createElement('div');
    document.body.appendChild(mockBannerHost);

    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
                <Route path="apis/:apiId" element={<ApiDetailLayout />}>
                    <Route path="overview" element={<div />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );

    const layoutConfig = mockCapturedLayoutConfig as Record<string, unknown> | null;
    if (layoutConfig?.banner) {
        render(layoutConfig.banner as ReactElement, { container: mockBannerHost! });
    }
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
        render(mockCapturedLayoutConfig.contextSidebar as ReactElement);
    }
}

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
        renderLayout('abc-123', '/apis/abc-123/overview?review');
        expect(screen.getByTestId('api-review-sheet')).toBeInTheDocument();
    });

    it('ignores ?review for a user who cannot review', () => {
        grant('api-definition-u');
        mockApi({ workflowState: 'IN_REVIEW' });
        renderLayout('abc-123', '/apis/abc-123/overview?review');
        expect(screen.queryByTestId('api-review-sheet')).not.toBeInTheDocument();
    });

    it('ignores ?review once the review has been accepted', () => {
        mockApi({ workflowState: 'REVIEW_OK' });
        renderLayout('abc-123', '/apis/abc-123/overview?review');
        expect(screen.queryByTestId('api-review-sheet')).not.toBeInTheDocument();
    });
});
