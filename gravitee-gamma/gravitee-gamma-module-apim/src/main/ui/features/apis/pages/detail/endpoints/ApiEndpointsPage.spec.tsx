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

// ─── All jest.mock() calls must precede imports ───────────────────────────────

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

jest.mock('@gravitee/graphene-core', () => ({
    toast: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
    cn: (...args: unknown[]) => args.filter(Boolean).join(' '),
    Alert: ({ children, variant }: { children?: React.ReactNode; variant?: string }) => (
        <div role="alert" data-variant={variant}>
            {children}
        </div>
    ),
    AlertDescription: ({ children }: { children?: React.ReactNode }) => <p>{children}</p>,
    Badge: ({ children }: { children?: React.ReactNode }) => <span>{children}</span>,
    Button: ({
        children,
        onClick,
        disabled,
        type,
        'aria-label': ariaLabel,
    }: {
        children?: React.ReactNode;
        onClick?: () => void;
        disabled?: boolean;
        type?: string;
        'aria-label'?: string;
    }) => (
        <button type={(type as 'button' | 'submit' | 'reset') ?? 'button'} onClick={onClick} disabled={disabled} aria-label={ariaLabel}>
            {children}
        </button>
    ),
    Card: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    CardContent: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    CardHeader: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    CardTitle: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    Checkbox: ({ checked, onCheckedChange }: { checked?: boolean; onCheckedChange?: (v: boolean) => void }) => (
        <input type="checkbox" checked={checked ?? false} onChange={e => onCheckedChange?.(e.target.checked)} />
    ),
    Dialog: ({ children, open }: { children?: React.ReactNode; open?: boolean }) => (open ? <div role="dialog">{children}</div> : null),
    DialogContent: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    DialogFooter: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    DialogHeader: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    DialogTitle: ({ children }: { children?: React.ReactNode }) => <h2>{children}</h2>,
    Input: ({
        id,
        value,
        onChange,
        placeholder,
        disabled,
        type,
        min,
    }: {
        id?: string;
        value?: string | number;
        onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
        placeholder?: string;
        disabled?: boolean;
        type?: string;
        min?: number;
    }) => <input id={id} type={type ?? 'text'} value={value} onChange={onChange} placeholder={placeholder} disabled={disabled} min={min} />,
    Label: ({ children, htmlFor }: { children?: React.ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
    Popover: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    PopoverContent: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    PopoverTrigger: ({ children, asChild }: { children?: React.ReactNode; asChild?: boolean }) =>
        asChild ? <>{children}</> : <div>{children}</div>,
    Select: ({ children, value, onValueChange }: { children?: React.ReactNode; value?: string; onValueChange?: (v: string) => void }) => (
        <select value={value} onChange={e => onValueChange?.(e.target.value)}>
            {children}
        </select>
    ),
    SelectContent: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
    SelectItem: ({ children, value }: { children?: React.ReactNode; value?: string }) => <option value={value}>{children}</option>,
    SelectTrigger: () => null,
    SelectValue: () => null,
    Skeleton: () => <div data-testid="skeleton" />,
    Switch: ({
        id,
        checked,
        onCheckedChange,
        disabled,
    }: {
        id?: string;
        checked?: boolean;
        onCheckedChange?: (v: boolean) => void;
        disabled?: boolean;
    }) => (
        <input type="checkbox" id={id} checked={checked ?? false} onChange={e => onCheckedChange?.(e.target.checked)} disabled={disabled} />
    ),
    Tooltip: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
    TooltipContent: () => null,
    TooltipProvider: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
    TooltipTrigger: ({ children, asChild }: { children?: React.ReactNode; asChild?: boolean }) =>
        asChild ? <>{children}</> : <div>{children}</div>,
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('@tanstack/react-query', () => ({
    ...jest.requireActual('@tanstack/react-query'),
    useMutation: jest.fn(),
    useQueryClient: jest.fn(),
    useQuery: jest.fn(),
}));

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useParams: jest.fn(),
}));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../../services/apis', () => ({
    updateApiEndpointGroups: jest.fn(),
}));

jest.mock('../../../services/tenants', () => ({
    getTenants: jest.fn(() => Promise.resolve([])),
}));

jest.mock('./group-form/HealthCheckStep', () => ({
    HealthCheckStep: () => <div data-testid="health-check-step" />,
}));

jest.mock('../../../utils/queryKeys', () => ({
    apiDetailKeys: {
        all: ['api-detail'],
        detail: (envId: string, apiId: string) => ['api-detail', envId, apiId],
    },
    tenantKeys: {
        all: ['tenants'],
        list: (envId: string) => ['tenants', envId],
    },
}));

// ─── Actual imports (after mocks) ─────────────────────────────────────────────

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { toast } from '@gravitee/graphene-core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom';

import { ApiEndpointsPage } from './ApiEndpointsPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import type { EndpointGroupDto } from '../../../types';

// ─── Typed mock refs ──────────────────────────────────────────────────────────

const mockToast = jest.mocked(toast);
const mockUseEnvironment = useEnvironment as jest.Mock;
const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseMutation = useMutation as jest.Mock;
const mockUseQueryClient = useQueryClient as jest.Mock;
const mockUseQuery = useQuery as jest.Mock;
const mockUseParams = useParams as jest.Mock;

// ─── Stub data ────────────────────────────────────────────────────────────────

const ENDPOINT_A = {
    name: 'ep-a',
    type: 'http-proxy',
    weight: 1,
    configuration: { target: 'https://backend.example.com' },
};

const GROUP_1: EndpointGroupDto = {
    name: 'default-group',
    type: 'http-proxy',
    loadBalancer: { type: 'ROUND_ROBIN' },
    endpoints: [ENDPOINT_A],
};

const GROUP_2: EndpointGroupDto = {
    name: 'second-group',
    type: 'http-proxy',
    loadBalancer: { type: 'RANDOM' },
    endpoints: [],
};

const ENDPOINT_B = {
    name: 'ep-b',
    type: 'http-proxy',
    weight: 2,
    configuration: { target: 'https://backend-b.example.com' },
};

const GROUP_TWO_ENDPOINTS: EndpointGroupDto = {
    name: 'default-group',
    type: 'http-proxy',
    loadBalancer: { type: 'ROUND_ROBIN' },
    endpoints: [ENDPOINT_A, ENDPOINT_B],
};

const HTTP_PROXY_API_BASE = { id: 'api-1', type: 'PROXY' as const, listeners: [{ type: 'HTTP' as const }] };

const API_WITH_GROUPS = { ...HTTP_PROXY_API_BASE, endpointGroups: [GROUP_1] };
const API_TWO_GROUPS = { ...HTTP_PROXY_API_BASE, endpointGroups: [GROUP_1, GROUP_2] };
const API_NO_GROUPS = { ...HTTP_PROXY_API_BASE, endpointGroups: [] };
const API_TWO_ENDPOINTS = { ...HTTP_PROXY_API_BASE, endpointGroups: [GROUP_TWO_ENDPOINTS] };

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockMutate = jest.fn();
const mockReset = jest.fn();

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/apis/api-1/endpoints/list']}>
            <Routes>
                <Route path="apis/:apiId/endpoints/list" element={<ApiEndpointsPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

function advanceGroupWizardPastGeneral() {
    fireEvent.click(screen.getByRole('button', { name: /validate general information/i }));
}

/** Advance past Configuration (fill target on create before calling). HTTP proxy APIs then land on Health-check. */
function advanceGroupWizardPastConfiguration(targetUrl?: string) {
    advanceGroupWizardPastGeneral();
    if (targetUrl) {
        fireEvent.change(screen.getByLabelText(/^target url/i), { target: { value: targetUrl } });
    }
    fireEvent.click(screen.getByRole('button', { name: /^next$/i }));
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('ApiEndpointsPage', () => {
    beforeEach(() => {
        mockUseParams.mockReturnValue({ apiId: 'api-1' });
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' });
        mockUseHasPermission.mockReturnValue(true);
        mockUseApiDetailContext.mockReturnValue({ api: API_WITH_GROUPS, isLoading: false });
        mockUseMutation.mockReturnValue({
            mutate: mockMutate,
            isPending: false,
            isError: false,
            error: null,
            reset: mockReset,
        });
        mockUseQueryClient.mockReturnValue({ invalidateQueries: jest.fn() });
        mockUseQuery.mockReturnValue({ data: [], isLoading: false });
    });

    afterEach(() => jest.clearAllMocks());

    // ── Loading ────────────────────────────────────────────────────────────────

    it('shows loading skeletons while the API context is loading', () => {
        mockUseApiDetailContext.mockReturnValue({ api: null, isLoading: true });
        renderPage();
        expect(screen.getAllByTestId('skeleton').length).toBeGreaterThan(0);
    });

    // ── List view ──────────────────────────────────────────────────────────────

    it('shows "Endpoints" as the page heading in the list view', () => {
        renderPage();
        expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Endpoints');
    });

    it('renders endpoint group names in the list view', () => {
        renderPage();
        expect(screen.getByText('default-group')).toBeInTheDocument();
    });

    it('shows landing content when the API has no endpoint groups', () => {
        mockUseApiDetailContext.mockReturnValue({ api: API_NO_GROUPS, isLoading: false });
        renderPage();
        expect(screen.getByText(/why configure upstream endpoints/i)).toBeInTheDocument();
    });

    it('does not show the landing content when the API has endpoint groups', () => {
        renderPage();
        expect(screen.queryByText(/why configure upstream endpoints/i)).not.toBeInTheDocument();
    });

    // ── Add endpoint group ─────────────────────────────────────────────────────

    describe('add endpoint group', () => {
        it('switches to the group form when "Add endpoint group" is clicked', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /add endpoint group/i }));
            expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Add endpoint group');
        });

        it('returns to the list view when Cancel is clicked in the group form', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /add endpoint group/i }));
            fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
            expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Endpoints');
        });

        it('calls mutation.mutate with the new group after completing the wizard', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /add endpoint group/i }));

            fireEvent.change(screen.getByPlaceholderText('default-group'), { target: { value: 'my-new-group' } });
            advanceGroupWizardPastConfiguration('https://backend.example.com');

            fireEvent.click(screen.getByRole('button', { name: /save endpoint group/i }));

            expect(mockMutate).toHaveBeenCalledTimes(1);
            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            expect(savedGroups).toContainEqual(
                expect.objectContaining({
                    name: 'my-new-group',
                    sharedConfiguration: expect.objectContaining({
                        proxy: { enabled: false, useSystemProxy: false },
                        http: expect.objectContaining({
                            version: 'HTTP_1_1',
                            propagateClientHost: false,
                        }),
                    }),
                    endpoints: [
                        expect.objectContaining({
                            name: 'my-new-group default endpoint',
                            type: 'http-proxy',
                            inheritConfiguration: true,
                            configuration: { target: 'https://backend.example.com' },
                        }),
                    ],
                }),
            );
        });

        it('preserves the existing groups when adding a new one', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /add endpoint group/i }));
            fireEvent.change(screen.getByPlaceholderText('default-group'), { target: { value: 'another-group' } });
            advanceGroupWizardPastConfiguration('https://backend.example.com');
            fireEvent.click(screen.getByRole('button', { name: /save endpoint group/i }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            expect(savedGroups).toContainEqual(expect.objectContaining({ name: 'default-group' }));
            expect(savedGroups).toContainEqual(expect.objectContaining({ name: 'another-group' }));
        });

        it('disables the Next button on the General step when the name is empty', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /add endpoint group/i }));
            // Name input is empty by default
            expect(screen.getByRole('button', { name: /validate general information/i })).toBeDisabled();
        });

        it('shows Validate general information on the General step (classic console parity)', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /add endpoint group/i }));
            expect(screen.getByRole('button', { name: /validate general information/i })).toBeInTheDocument();
        });
    });

    // ── Edit endpoint group ────────────────────────────────────────────────────

    describe('edit endpoint group', () => {
        it('switches to the group form with "Edit default endpoint group" heading when the default group is edited', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group default-group' }));
            expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Edit default endpoint group');
        });

        it('returns to the list view when Cancel is clicked in the edit form', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group default-group' }));
            fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
            expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Endpoints');
        });

        it('preserves existing endpoints when saving a group edit', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group default-group' }));
            advanceGroupWizardPastConfiguration();
            fireEvent.click(screen.getByRole('button', { name: /save endpoint group/i }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            const savedGroup = savedGroups.find(g => g.name === 'default-group');
            expect(savedGroup?.endpoints).toContainEqual(expect.objectContaining({ name: 'ep-a' }));
        });
    });

    // ── Delete endpoint group ──────────────────────────────────────────────────

    describe('delete endpoint group', () => {
        it('calls mutation.mutate without the deleted group after confirming', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TWO_GROUPS, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete group default-group' }));
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            expect(savedGroups).not.toContainEqual(expect.objectContaining({ name: 'default-group' }));
            expect(savedGroups).toContainEqual(expect.objectContaining({ name: 'second-group' }));
        });

        it('does not call mutation.mutate when deletion is cancelled', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TWO_GROUPS, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete group default-group' }));
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

            expect(mockMutate).not.toHaveBeenCalled();
        });
    });

    // ── Add endpoint ───────────────────────────────────────────────────────────

    describe('add endpoint', () => {
        it('switches to the endpoint form when "Add endpoint" is clicked', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Add endpoint' }));
            expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Add endpoint');
        });

        it('returns to the list view when Cancel is clicked in the endpoint form', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Add endpoint' }));
            fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
            expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Endpoints');
        });
    });

    // ── Edit endpoint ──────────────────────────────────────────────────────────

    describe('edit endpoint', () => {
        it('switches to the endpoint form with "Edit endpoint" heading when Edit is clicked', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit endpoint ep-a' }));
            expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Edit endpoint');
        });
    });

    // ── Reorder endpoint ───────────────────────────────────────────────────────

    describe('reorder endpoint', () => {
        it('calls mutation.mutate with endpoints swapped when Move up is clicked', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TWO_ENDPOINTS, isLoading: false });
            renderPage();

            // ep-b is at index 1 — Move up swaps it with ep-a
            fireEvent.click(screen.getByRole('button', { name: 'Move ep-b up' }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            const endpoints = savedGroups[0].endpoints ?? [];
            expect(endpoints[0].name).toBe('ep-b');
            expect(endpoints[1].name).toBe('ep-a');
        });

        it('calls mutation.mutate with endpoints swapped when Move down is clicked', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TWO_ENDPOINTS, isLoading: false });
            renderPage();

            // ep-a is at index 0 — Move down swaps it with ep-b
            fireEvent.click(screen.getByRole('button', { name: 'Move ep-a down' }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            const endpoints = savedGroups[0].endpoints ?? [];
            expect(endpoints[0].name).toBe('ep-b');
            expect(endpoints[1].name).toBe('ep-a');
        });

        it('disables Move up for the first endpoint', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TWO_ENDPOINTS, isLoading: false });
            renderPage();
            expect(screen.getByRole('button', { name: 'Move ep-a up' })).toBeDisabled();
        });

        it('disables Move down for the last endpoint', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TWO_ENDPOINTS, isLoading: false });
            renderPage();
            expect(screen.getByRole('button', { name: 'Move ep-b down' })).toBeDisabled();
        });
    });

    // ── Delete endpoint ────────────────────────────────────────────────────────

    describe('delete endpoint', () => {
        it('calls mutation.mutate without the deleted endpoint after confirming', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TWO_ENDPOINTS, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete endpoint ep-a' }));
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            const endpoints = savedGroups[0].endpoints ?? [];
            expect(endpoints).not.toContainEqual(expect.objectContaining({ name: 'ep-a' }));
            expect(endpoints).toContainEqual(expect.objectContaining({ name: 'ep-b' }));
        });

        it('does not call mutation.mutate when endpoint deletion is cancelled', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TWO_ENDPOINTS, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete endpoint ep-a' }));
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

            expect(mockMutate).not.toHaveBeenCalled();
        });

        it('disables the delete endpoint button when a group has only one endpoint', () => {
            renderPage(); // API_WITH_GROUPS has one endpoint in GROUP_1
            expect(screen.getByRole('button', { name: 'Delete endpoint ep-a' })).toBeDisabled();
        });
    });

    // ── Save button disabled while pending ─────────────────────────────────────

    it('shows "Saving…" and disables the Save button while mutation is pending', () => {
        mockUseMutation.mockReturnValue({
            mutate: mockMutate,
            isPending: true,
            isError: false,
            error: null,
            reset: mockReset,
        });
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /add endpoint group/i }));
        fireEvent.change(screen.getByPlaceholderText('default-group'), { target: { value: 'test-group' } });
        advanceGroupWizardPastConfiguration('https://backend.example.com');

        const saveBtn = screen.getByRole('button', { name: /saving/i });
        expect(saveBtn).toBeDisabled();
    });

    // ── Error feedback ─────────────────────────────────────────────────────────

    it('surfaces a toast when a list-view action (delete group) fails', () => {
        mockUseMutation.mockReturnValue({
            mutate: (_groups: unknown, opts?: { onError?: (e: unknown) => void }) => opts?.onError?.(new Error('Network error')),
            isPending: false,
            isError: false,
            error: null,
            reset: mockReset,
        });
        mockUseApiDetailContext.mockReturnValue({ api: API_TWO_GROUPS, isLoading: false });
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: 'Delete group default-group' }));
        fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

        expect(mockToast.error).toHaveBeenCalled();
    });

    // ── Read-only mode ─────────────────────────────────────────────────────────

    describe('read-only mode', () => {
        it('hides "Add endpoint group" and group edit/delete controls when user lacks api-definition-u', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();
            expect(screen.queryByRole('button', { name: /add endpoint group/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /edit group/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /delete group/i })).not.toBeInTheDocument();
        });

        it('shows the Kubernetes read-only banner for Kubernetes-managed APIs', () => {
            const k8sApi = { ...API_WITH_GROUPS, definitionContext: { origin: 'KUBERNETES' as const } };
            mockUseApiDetailContext.mockReturnValue({ api: k8sApi, isLoading: false });
            renderPage();
            expect(screen.getByRole('alert')).toBeInTheDocument();
            expect(screen.getByText(/kubernetes operator/i)).toBeInTheDocument();
        });

        it('hides edit controls for Kubernetes-managed APIs even with full permissions', () => {
            const k8sApi = { ...API_WITH_GROUPS, definitionContext: { origin: 'KUBERNETES' as const } };
            mockUseApiDetailContext.mockReturnValue({ api: k8sApi, isLoading: false });
            renderPage();
            expect(screen.queryByRole('button', { name: /add endpoint group/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /edit group/i })).not.toBeInTheDocument();
        });
    });
});
