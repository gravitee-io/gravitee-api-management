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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
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

jest.mock('@gravitee/graphene-core', () => ({
    Button: ({
        children,
        onClick,
        disabled,
        variant: _variant,
        size: _size,
        className: _className,
        asChild,
    }: {
        children?: ReactNode;
        onClick?: () => void;
        disabled?: boolean;
        variant?: string;
        size?: string;
        className?: string;
        asChild?: boolean;
    }) =>
        asChild ? (
            <>{children}</>
        ) : (
            <button type="button" onClick={onClick} disabled={disabled}>
                {children}
            </button>
        ),
    Card: ({ children, className }: { children?: ReactNode; className?: string }) => <div className={className}>{children}</div>,
    CardContent: ({ children, className }: { children?: ReactNode; className?: string }) => <div className={className}>{children}</div>,
    Dialog: ({ children, open }: { children?: ReactNode; open?: boolean }) => (open ? <div role="dialog">{children}</div> : null),
    DialogClose: ({ children }: { children?: ReactNode }) => <>{children}</>,
    DialogContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    DialogFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
    Input: ({
        id,
        value,
        onChange,
        placeholder,
        disabled,
        'aria-label': ariaLabel,
        'aria-invalid': ariaInvalid,
    }: {
        id?: string;
        value?: string;
        onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
        placeholder?: string;
        disabled?: boolean;
        'aria-label'?: string;
        'aria-invalid'?: boolean;
    }) => (
        <input
            id={id}
            value={value}
            onChange={onChange}
            placeholder={placeholder}
            disabled={disabled}
            aria-label={ariaLabel}
            aria-invalid={ariaInvalid}
        />
    ),
    Label: ({ children, htmlFor, className }: { children?: ReactNode; htmlFor?: string; className?: string }) => (
        <label htmlFor={htmlFor} className={className}>
            {children}
        </label>
    ),
    Separator: () => <hr />,
    Skeleton: () => <div data-testid="skeleton" />,
    Switch: ({
        checked,
        onCheckedChange,
        disabled,
        'aria-label': ariaLabel,
    }: {
        checked?: boolean;
        onCheckedChange?: (v: boolean) => void;
        disabled?: boolean;
        'aria-label'?: string;
    }) => (
        <input
            type="checkbox"
            checked={checked}
            onChange={e => onCheckedChange?.(e.target.checked)}
            disabled={disabled}
            aria-label={ariaLabel}
        />
    ),
    Tooltip: ({ children }: { children?: ReactNode }) => <>{children}</>,
    TooltipContent: ({ children }: { children?: ReactNode }) => <div role="tooltip">{children}</div>,
    TooltipProvider: ({ children }: { children?: ReactNode }) => <>{children}</>,
    TooltipTrigger: ({ children, asChild }: { children?: ReactNode; asChild?: boolean }) =>
        asChild ? <>{children}</> : <button type="button">{children}</button>,
    cn: (...args: string[]) => args.filter(Boolean).join(' '),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../../services/entrypoints', () => ({
    getExposedEntrypoints: jest.fn(),
    updateApiListeners: jest.fn(),
}));

jest.mock('../../../utils/queryKeys', () => ({
    apiDetailKeys: {
        all: ['api-detail'],
        detail: (envId: string, apiId: string) => ['api-detail', envId, apiId],
    },
    apiEntrypointKeys: {
        all: ['api-entrypoints'],
        exposed: (envId: string, apiId: string) => ['api-entrypoints', 'exposed', envId, apiId],
    },
}));

import { ApiEntrypointsPage } from './ApiEntrypointsPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { getExposedEntrypoints, updateApiListeners } from '../../../services/entrypoints';

const mockUseEnvironment = useEnvironment as jest.Mock;
const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockGetExposedEntrypoints = getExposedEntrypoints as jest.Mock;
const mockUpdateApiListeners = updateApiListeners as jest.Mock;

const API_WITH_PATHS = {
    id: 'api-1',
    name: 'Passenger Boarding API',
    listeners: [
        {
            type: 'HTTP' as const,
            paths: [{ path: '/boarding' }],
            hosts: [],
            entrypoints: [{ type: 'http-proxy' }],
        },
    ],
};

const API_NO_ENTRYPOINTS = {
    id: 'api-1',
    name: 'New API',
    listeners: [],
};

const API_WITH_VIRTUAL_HOSTS = {
    id: 'api-1',
    name: 'VH API',
    listeners: [
        {
            type: 'HTTP' as const,
            paths: [],
            hosts: [{ host: 'api.company.com', path: '/v1', overrideAccess: false }],
            entrypoints: [{ type: 'http-proxy' }],
        },
    ],
};

function makeClient() {
    return new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
}

function renderPage(apiId = 'api-1') {
    const client = makeClient();
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={[`/apis/${apiId}/entrypoints`]}>
                <Routes>
                    <Route path="apis/:apiId/entrypoints" element={<ApiEntrypointsPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ApiEntrypointsPage', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' });
        // Default: full permissions granted, permissions loaded
        mockUseHasPermission.mockReturnValue(true);
        mockUseApiDetailContext.mockReturnValue({ api: API_WITH_PATHS, isLoading: false, permissionsReady: true });
        mockGetExposedEntrypoints.mockResolvedValue([{ value: 'http://localhost:8080/boarding' }]);
        mockUpdateApiListeners.mockResolvedValue(API_WITH_PATHS);
    });

    afterEach(() => jest.clearAllMocks());

    it('shows loading skeleton when API context is loading', () => {
        mockUseApiDetailContext.mockReturnValue({ api: null, isLoading: true, permissionsReady: false });
        renderPage();
        expect(screen.getAllByTestId('skeleton').length).toBeGreaterThan(0);
    });

    it('shows landing page when API has no entrypoints configured', () => {
        mockUseApiDetailContext.mockReturnValue({ api: API_NO_ENTRYPOINTS, isLoading: false, permissionsReady: true });
        renderPage();
        expect(screen.getByText(/why configure entrypoints/i)).toBeInTheDocument();
        expect(screen.queryByText(/entrypoint context-paths/i)).not.toBeInTheDocument();
    });

    it('shows config view when API already has context paths', () => {
        renderPage();
        expect(screen.queryByText(/why configure entrypoints/i)).not.toBeInTheDocument();
        expect(screen.getByText(/entrypoint context-paths/i)).toBeInTheDocument();
    });

    it('transitions from landing to config when Add context path is clicked', () => {
        mockUseApiDetailContext.mockReturnValue({ api: API_NO_ENTRYPOINTS, isLoading: false, permissionsReady: true });
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /add context path/i }));
        expect(screen.getByText(/entrypoint context-paths/i)).toBeInTheDocument();
        expect(screen.queryByText(/why configure entrypoints/i)).not.toBeInTheDocument();
    });

    it('seeds path input from API data', () => {
        renderPage();
        const inputs = screen.getAllByRole('textbox', { name: /context path/i });
        expect((inputs[0] as HTMLInputElement).value).toBe('/boarding');
    });

    it('disables Save button when form is clean', () => {
        renderPage();
        expect(screen.getByRole('button', { name: /save changes/i })).toBeDisabled();
    });

    it('enables Save button after editing a path', () => {
        renderPage();
        const input = screen.getAllByRole('textbox', { name: /context path/i })[0];
        fireEvent.change(input, { target: { value: '/new-path' } });
        expect(screen.getByRole('button', { name: /save changes/i })).not.toBeDisabled();
    });

    it('disables delete button when only one context path remains', () => {
        renderPage();
        const deleteBtn = screen.getByRole('button', { name: /delete context path/i });
        expect(deleteBtn).toBeDisabled();
    });

    it('enables deleting extra context paths when more than one exists', () => {
        renderPage();
        // Add a second path via the in-card button (last "Add context path" button rendered)
        const addBtns = screen.getAllByRole('button', { name: /add context path/i });
        fireEvent.click(addBtns[addBtns.length - 1]);
        const deleteBtns = screen.getAllByRole('button', { name: /delete context path/i });
        expect(deleteBtns.length).toBe(2);
        expect(deleteBtns[0]).not.toBeDisabled();
    });

    it('calls updateApiListeners with correct listeners payload on Save', async () => {
        renderPage();

        const input = screen.getAllByRole('textbox', { name: /context path/i })[0];
        fireEvent.change(input, { target: { value: '/updated' } });
        fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

        await waitFor(() => expect(mockUpdateApiListeners).toHaveBeenCalledTimes(1));

        // Signature is now (envId, apiId, current, listeners)
        const [envId, apiId, _current, listeners] = mockUpdateApiListeners.mock.calls[0];
        expect(envId).toBe('DEFAULT');
        expect(apiId).toBe('api-1');
        expect(listeners).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    type: 'HTTP',
                    paths: [{ path: '/updated' }],
                    hosts: [],
                }),
            ]),
        );
    });

    it('shows virtual hosts card when API is in virtual host mode', () => {
        mockUseApiDetailContext.mockReturnValue({ api: API_WITH_VIRTUAL_HOSTS, isLoading: false, permissionsReady: true });
        renderPage();
        // The card heading "Virtual hosts" (not the toggle label)
        expect(screen.getByRole('textbox', { name: /virtual host/i })).toBeInTheDocument();
        expect(screen.queryByText(/entrypoint context-paths/i)).not.toBeInTheDocument();
    });

    it('opens switch-mode dialog when virtual host toggle is turned off', () => {
        mockUseApiDetailContext.mockReturnValue({ api: API_WITH_VIRTUAL_HOSTS, isLoading: false, permissionsReady: true });
        renderPage();
        const toggle = screen.getByRole('checkbox', { name: /enable virtual hosts/i });
        fireEvent.click(toggle);
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText(/switch to context-path mode/i)).toBeInTheDocument();
    });

    it('switches to context-path mode after confirming the dialog', () => {
        mockUseApiDetailContext.mockReturnValue({ api: API_WITH_VIRTUAL_HOSTS, isLoading: false, permissionsReady: true });
        renderPage();
        fireEvent.click(screen.getByRole('checkbox', { name: /enable virtual hosts/i }));
        fireEvent.click(screen.getByRole('button', { name: /switch/i }));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(screen.getByText(/entrypoint context-paths/i)).toBeInTheDocument();
    });

    // ── permission guard tests ──────────────────────────────────────────────────

    it('hides Save and mutating controls when user lacks api-definition-u permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /add context path/i })).not.toBeInTheDocument();
        // Inputs still visible but disabled
        const inputs = screen.getAllByRole('textbox', { name: /context path/i });
        expect(inputs[0]).toBeDisabled();
    });

    it('hides Save and mutating controls when permissions are not yet loaded', () => {
        mockUseApiDetailContext.mockReturnValue({ api: API_WITH_PATHS, isLoading: false, permissionsReady: false });
        renderPage();
        expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /add context path/i })).not.toBeInTheDocument();
    });

    it('is read-only when only one of the two required permissions is held', () => {
        // Having api-definition-u but NOT api-gateway_definition-u must still block edits
        mockUseHasPermission.mockImplementation(({ allOf }: { allOf?: string[] }) => {
            if (allOf) {
                // allOf permission check: requires BOTH; return false if any is missing
                return allOf.every(p => p === 'api-definition-u');
            }
            return true;
        });
        renderPage();
        expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /add context path/i })).not.toBeInTheDocument();
        const inputs = screen.getAllByRole('textbox', { name: /context path/i });
        expect(inputs[0]).toBeDisabled();
    });

    it('forces read-only mode for Kubernetes-managed APIs regardless of permissions', () => {
        const kubernetesApi = {
            ...API_WITH_PATHS,
            definitionContext: { origin: 'KUBERNETES' as const },
        };
        mockUseApiDetailContext.mockReturnValue({ api: kubernetesApi, isLoading: false, permissionsReady: true });
        renderPage();
        expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument();
        const inputs = screen.getAllByRole('textbox', { name: /context path/i });
        expect(inputs[0]).toBeDisabled();
    });
});
