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

/** Controllable stand-in for schema-driven shared config (real JsonSchemaForm is covered by its unit test). */
jest.mock('./group-form/SharedConfigurationSchemaForm', () => {
    const { useEffect } = jest.requireActual('react');

    const DEFAULT_TCP = {
        connectTimeout: 3000,
        reconnectAttempts: 3,
        reconnectInterval: 1000,
        idleTimeout: 0,
        readIdleTimeout: 0,
        writeIdleTimeout: 0,
    };

    return {
        SharedConfigurationSchemaForm: ({
            endpointType,
            value,
            onChange,
            onValidityChange,
        }: {
            endpointType: string;
            value: Record<string, unknown>;
            onChange: (next: Record<string, unknown>) => void;
            onValidityChange?: (valid: boolean) => void;
        }) => {
            useEffect(() => {
                onValidityChange?.(true);
                if (endpointType === 'tcp-proxy' && !value.tcp) {
                    onChange({
                        ...value,
                        tcp: DEFAULT_TCP,
                        proxy: value.proxy ?? { enabled: false, useSystemProxy: false },
                        ssl: value.ssl ?? { hostnameVerifier: true, trustAll: false },
                    });
                } else if (endpointType === 'http-proxy' && !value.http) {
                    onChange({
                        ...value,
                        http: { version: 'HTTP_1_1' },
                        proxy: value.proxy ?? { enabled: false, useSystemProxy: false },
                        ssl: value.ssl ?? { hostnameVerifier: true, trustAll: false },
                        headers: value.headers ?? [],
                    });
                }
                // Seed defaults once per endpoint type. Including the callbacks would re-seed on every parent render.
                // eslint-disable-next-line react-hooks/exhaustive-deps
            }, [endpointType]);

            if (endpointType !== 'tcp-proxy') {
                return <div data-testid="shared-config-http">HTTP configuration</div>;
            }

            const tcp = (value.tcp ?? DEFAULT_TCP) as typeof DEFAULT_TCP;
            return (
                <div data-testid="shared-config-tcp">
                    <div>TCP Client Options</div>
                    <label htmlFor="tcp-connect-timeout">Connection timeout</label>
                    <input
                        id="tcp-connect-timeout"
                        type="number"
                        aria-label="Connection timeout"
                        value={tcp.connectTimeout}
                        onChange={e =>
                            onChange({
                                ...value,
                                tcp: { ...tcp, connectTimeout: Number(e.target.value) },
                            })
                        }
                    />
                    <label htmlFor="tcp-reconnect-attempts">Reconnect attempts</label>
                    <input
                        id="tcp-reconnect-attempts"
                        type="number"
                        aria-label="Reconnect attempts"
                        value={tcp.reconnectAttempts}
                        readOnly
                    />
                    <div>Proxy Options</div>
                    <div>SSL Options</div>
                    <button type="button">SSL Options</button>
                    <div>Hostname verifier</div>
                    <div>Trust all certificates</div>
                    <div>Truststore</div>
                    <div>Key store</div>
                </div>
            );
        },
        EndpointConfigurationSchemaForm: ({
            endpointType,
            value,
            onChange,
            onValidityChange,
        }: {
            endpointType: string;
            value: Record<string, unknown>;
            onChange: (next: Record<string, unknown>) => void;
            onValidityChange?: (valid: boolean) => void;
        }) => {
            useEffect(() => {
                onValidityChange?.(true);
            }, [endpointType, onValidityChange]);

            if (endpointType === 'tcp-proxy') {
                const target = (value.target ?? {}) as { host?: string; port?: number; secured?: boolean };
                return (
                    <div data-testid="endpoint-config-tcp">
                        <div>Target server</div>
                        <label htmlFor="endpoint-host">Host</label>
                        <input
                            id="endpoint-host"
                            value={target.host ?? ''}
                            onChange={e =>
                                onChange({
                                    ...value,
                                    target: { host: e.target.value, port: target.port ?? 0, secured: target.secured ?? false },
                                })
                            }
                        />
                        <label htmlFor="endpoint-port">Port</label>
                        <input
                            id="endpoint-port"
                            type="number"
                            value={target.port ?? ''}
                            onChange={e =>
                                onChange({
                                    ...value,
                                    target: { host: target.host ?? '', port: Number(e.target.value), secured: target.secured ?? false },
                                })
                            }
                        />
                    </div>
                );
            }

            const target = typeof value.target === 'string' ? value.target : '';
            return (
                <div data-testid="endpoint-config-http">
                    <label htmlFor="endpoint-target">Target URL</label>
                    <input id="endpoint-target" value={target} onChange={e => onChange({ ...value, target: e.target.value })} />
                </div>
            );
        },
    };
});

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

// ─── TCP stub data ────────────────────────────────────────────────────────────

const TCP_ENDPOINT = {
    name: 'default-tcp',
    type: 'tcp-proxy',
    weight: 1,
    inheritConfiguration: false,
    configuration: { target: { host: 'backend.example.com', port: 9090, secured: false } },
    sharedConfigurationOverride: {
        tcp: {
            connectTimeout: 4000,
            reconnectAttempts: 2,
            reconnectInterval: 500,
            idleTimeout: 0,
            readIdleTimeout: 0,
            writeIdleTimeout: 0,
        },
        proxy: { enabled: true, useSystemProxy: true },
        ssl: { hostnameVerifier: true, trustAll: false },
    },
};

const TCP_GROUP: EndpointGroupDto = {
    name: 'tcp-group',
    type: 'tcp-proxy',
    loadBalancer: { type: 'ROUND_ROBIN' },
    sharedConfiguration: {
        tcp: {
            connectTimeout: 3000,
            reconnectAttempts: 3,
            reconnectInterval: 1000,
            idleTimeout: 0,
            readIdleTimeout: 0,
            writeIdleTimeout: 0,
        },
        proxy: { enabled: true, useSystemProxy: true },
        ssl: { hostnameVerifier: true, trustAll: false },
    },
    endpoints: [TCP_ENDPOINT],
};

const TCP_API_BASE = { id: 'api-2', type: 'PROXY' as const, listeners: [{ type: 'TCP' as const, hosts: ['tcp.example.com'] }] };
const API_TCP = { ...TCP_API_BASE, endpointGroups: [TCP_GROUP] };

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockMutate = jest.fn();
const mockReset = jest.fn();

function renderPage(initialEntry = '/apis/api-1/endpoints/list') {
    return render(
        <MemoryRouter initialEntries={[initialEntry]}>
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
                        http: expect.objectContaining({ version: 'HTTP_1_1' }),
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

        it('shows Validate general information on the General step', () => {
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

        it('enables Save when the health-check step is opened before configuration', () => {
            renderPage('/apis/api-1/endpoints/list?editGroup=0&step=health-check');

            expect(screen.getByRole('button', { name: /save endpoint group/i })).toBeEnabled();
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

    describe('secondary endpoint', () => {
        function advanceEndpointFormToSave() {
            fireEvent.click(screen.getByRole('button', { name: /^next$/i }));
            fireEvent.click(screen.getByRole('button', { name: /^next$/i }));
        }

        it('persists secondary=true when toggled while adding an endpoint', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Add endpoint' }));

            fireEvent.change(screen.getByPlaceholderText('my-endpoint'), { target: { value: 'fallback' } });
            fireEvent.change(screen.getByLabelText(/^target url/i), { target: { value: 'https://fallback.example.com' } });
            fireEvent.click(screen.getByLabelText(/^secondary endpoint$/i));
            expect(screen.getByLabelText(/^secondary endpoint$/i)).toBeChecked();

            advanceEndpointFormToSave();
            fireEvent.click(screen.getByRole('button', { name: /^add endpoint$/i }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            const newEndpoint = savedGroups[0].endpoints?.find(e => e.name === 'fallback');
            expect(newEndpoint?.secondary).toBe(true);
            expect(newEndpoint?.configuration?.target).toBe('https://fallback.example.com');
        });

        it('loads secondary=true from the DTO and persists secondary=false when cleared', () => {
            const secondaryEndpoint = {
                ...ENDPOINT_A,
                secondary: true,
            };
            mockUseApiDetailContext.mockReturnValue({
                api: {
                    ...HTTP_PROXY_API_BASE,
                    endpointGroups: [{ ...GROUP_1, endpoints: [secondaryEndpoint] }],
                },
                isLoading: false,
            });
            renderPage();

            expect(screen.getByText('Secondary')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', { name: 'Edit endpoint ep-a' }));
            const secondarySwitch = screen.getByLabelText(/^secondary endpoint$/i);
            expect(secondarySwitch).toBeChecked();

            fireEvent.click(secondarySwitch);
            expect(secondarySwitch).not.toBeChecked();

            advanceEndpointFormToSave();
            fireEvent.click(screen.getByRole('button', { name: /save endpoint/i }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            expect(savedGroups[0].endpoints?.[0]?.secondary).toBe(false);
        });

        it('sends an explicit secondary=false for a newly added primary endpoint', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Add endpoint' }));

            fireEvent.change(screen.getByPlaceholderText('my-endpoint'), { target: { value: 'primary-ep' } });
            fireEvent.change(screen.getByLabelText(/^target url/i), { target: { value: 'https://primary.example.com' } });
            expect(screen.getByLabelText(/^secondary endpoint$/i)).not.toBeChecked();

            advanceEndpointFormToSave();
            fireEvent.click(screen.getByRole('button', { name: /^add endpoint$/i }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            const newEndpoint = savedGroups[0].endpoints?.find(e => e.name === 'primary-ep');
            expect(newEndpoint?.secondary).toBe(false);
        });
    });

    // ── TCP endpoint groups ────────────────────────────────────────────────────

    describe('tcp-proxy endpoint group editing', () => {
        it('loads the tcp-proxy shared-configuration schema form when editing a tcp-proxy group', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TCP, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit group tcp-group' }));
            advanceGroupWizardPastGeneral();

            expect(screen.getByTestId('shared-config-tcp')).toBeInTheDocument();
            expect(screen.getByText('TCP Client Options')).toBeInTheDocument();
            expect(screen.getByLabelText(/connection timeout/i)).toHaveValue(3000);
            expect(screen.getByLabelText(/reconnect attempts/i)).toHaveValue(3);
            expect(screen.queryByTestId('shared-config-http')).not.toBeInTheDocument();
            expect(screen.queryByText('HTTP headers')).not.toBeInTheDocument();
        });

        it('saves an http-proxy group with a target URL when adding a group on a TCP API', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TCP, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: /add endpoint group/i }));
            fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'second-group' } });
            advanceGroupWizardPastGeneral();

            expect(screen.getByLabelText(/^target url/i)).toBeInTheDocument();
            expect(screen.getByTestId('shared-config-http')).toBeInTheDocument();
            expect(screen.queryByText('Target server')).not.toBeInTheDocument();
            expect(screen.queryByText('TCP Client Options')).not.toBeInTheDocument();

            fireEvent.change(screen.getByLabelText(/^target url/i), { target: { value: 'https://db.example.com' } });
            fireEvent.click(screen.getByRole('button', { name: /save endpoint group/i }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            const newGroup = savedGroups.find(g => g.name === 'second-group');
            expect(newGroup?.type).toBe('http-proxy');
            expect(newGroup?.endpoints?.[0]).toEqual(
                expect.objectContaining({
                    type: 'http-proxy',
                    configuration: { target: 'https://db.example.com' },
                }),
            );
            expect(newGroup?.sharedConfiguration).toEqual(
                expect.objectContaining({
                    http: expect.objectContaining({ version: 'HTTP_1_1' }),
                }),
            );
        });

        it('persists tcp sharedConfiguration when saving a tcp-proxy group edit', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TCP, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit group tcp-group' }));
            advanceGroupWizardPastGeneral();
            fireEvent.change(screen.getByLabelText(/connection timeout/i), { target: { value: '5000' } });
            fireEvent.click(screen.getByRole('button', { name: /save endpoint group/i }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            const savedGroup = savedGroups.find(g => g.name === 'tcp-group');
            expect(savedGroup?.type).toBe('tcp-proxy');
            expect(savedGroup?.sharedConfiguration?.tcp).toEqual({
                connectTimeout: 5000,
                reconnectAttempts: 3,
                reconnectInterval: 1000,
                idleTimeout: 0,
                readIdleTimeout: 0,
                writeIdleTimeout: 0,
            });
            expect(savedGroup?.sharedConfiguration).not.toHaveProperty('http');
            expect(savedGroup?.sharedConfiguration?.proxy).toEqual({ enabled: true, useSystemProxy: true });
        });
    });

    // ── TCP endpoints ──────────────────────────────────────────────────────────

    describe('tcp-proxy endpoint editing', () => {
        it('shows Host/Port/Secured fields instead of Target URL when editing a tcp-proxy endpoint', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TCP, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit endpoint default-tcp' }));

            expect(screen.getByLabelText(/^host/i)).toHaveValue('backend.example.com');
            expect(screen.getByLabelText(/^port/i)).toHaveAttribute('type', 'number');
            expect(screen.getByLabelText(/^port/i)).toHaveValue(9090);
            expect(screen.queryByLabelText(/^target url/i)).not.toBeInTheDocument();
        });

        it('shows Configuration but not Health-check for a tcp-proxy endpoint', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TCP, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit endpoint default-tcp' }));

            expect(screen.getByRole('button', { name: /^next$/i })).toBeInTheDocument();
            expect(screen.getByRole('button', { name: /2 configuration/i })).toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /health-check/i })).not.toBeInTheDocument();
        });

        it('saves a tcp-proxy endpoint with its target and TCP-shaped configuration override', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TCP, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit endpoint default-tcp' }));
            fireEvent.change(screen.getByLabelText(/^host/i), { target: { value: 'new-backend.example.com' } });
            fireEvent.change(screen.getByLabelText(/^port/i), { target: { value: '5432' } });
            fireEvent.click(screen.getByRole('button', { name: /^next$/i }));
            fireEvent.change(screen.getByLabelText(/connection timeout/i), { target: { value: '6000' } });
            fireEvent.click(screen.getByRole('button', { name: /save endpoint/i }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            const savedEndpoint = savedGroups[0].endpoints?.[0];
            expect(savedEndpoint?.configuration?.target).toEqual({ host: 'new-backend.example.com', port: 5432, secured: false });
            expect(savedEndpoint?.sharedConfigurationOverride).toEqual({
                tcp: {
                    connectTimeout: 6000,
                    reconnectAttempts: 2,
                    reconnectInterval: 500,
                    idleTimeout: 0,
                    readIdleTimeout: 0,
                    writeIdleTimeout: 0,
                },
                proxy: { enabled: true, useSystemProxy: true },
                ssl: { hostnameVerifier: true, trustAll: false },
            });
            expect(savedEndpoint?.sharedConfigurationOverride).not.toHaveProperty('http');
            expect(savedEndpoint?.sharedConfigurationOverride).not.toHaveProperty('headers');
        });

        it('defaults a newly-added endpoint in an existing tcp-proxy group to type tcp-proxy', () => {
            mockUseApiDetailContext.mockReturnValue({ api: API_TCP, isLoading: false });
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Add endpoint' }));
            fireEvent.change(screen.getByPlaceholderText('my-endpoint'), { target: { value: 'second-tcp' } });
            fireEvent.change(screen.getByLabelText(/^host/i), { target: { value: 'db2.example.com' } });
            fireEvent.change(screen.getByLabelText(/^port/i), { target: { value: '5433' } });
            fireEvent.click(screen.getByRole('button', { name: /^next$/i }));
            fireEvent.click(screen.getByRole('button', { name: /add endpoint/i }));

            const savedGroups: EndpointGroupDto[] = mockMutate.mock.calls[0][0];
            const newEndpoint = savedGroups[0].endpoints?.find(e => e.name === 'second-tcp');
            expect(newEndpoint?.type).toBe('tcp-proxy');
            expect(newEndpoint?.configuration?.target).toEqual({ host: 'db2.example.com', port: 5433, secured: false });
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
