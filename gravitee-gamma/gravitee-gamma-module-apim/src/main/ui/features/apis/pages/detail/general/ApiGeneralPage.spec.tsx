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
    Badge: ({ children, className }: { children?: ReactNode; className?: string }) => <span className={className}>{children}</span>,
    Checkbox: ({
        checked,
        onCheckedChange,
        disabled,
    }: {
        checked?: boolean;
        onCheckedChange?: (v: boolean) => void;
        disabled?: boolean;
    }) => (
        <input
            type="checkbox"
            checked={checked}
            onChange={e => onCheckedChange?.(e.target.checked)}
            disabled={disabled}
            readOnly={!onCheckedChange}
        />
    ),
    Popover: ({ children }: { children?: ReactNode }) => <>{children}</>,
    PopoverContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    PopoverTrigger: ({ children }: { children?: ReactNode }) => <>{children}</>,
    Button: ({
        children,
        onClick,
        disabled,
        type,
    }: {
        children?: ReactNode;
        onClick?: () => void;
        disabled?: boolean;
        type?: 'button' | 'submit' | 'reset';
    }) => (
        <button type={type ?? 'button'} onClick={onClick} disabled={disabled}>
            {children}
        </button>
    ),
    Card: ({ children, className }: { children?: ReactNode; className?: string }) => <div className={className}>{children}</div>,
    CardContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
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
    }: {
        id?: string;
        value?: string;
        onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
        placeholder?: string;
        disabled?: boolean;
    }) => <input id={id} value={value} onChange={onChange} placeholder={placeholder} disabled={disabled} />,
    Label: ({ children, htmlFor }: { children?: ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
    Separator: () => <hr />,
    Skeleton: () => <div data-testid="skeleton" />,
    Switch: ({ checked, onCheckedChange, disabled }: { checked?: boolean; onCheckedChange?: (v: boolean) => void; disabled?: boolean }) => (
        <input type="checkbox" checked={checked} onChange={e => onCheckedChange?.(e.target.checked)} disabled={disabled} />
    ),
    Textarea: ({
        id,
        value,
        onChange,
        placeholder,
        disabled,
    }: {
        id?: string;
        value?: string;
        onChange?: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
        placeholder?: string;
        disabled?: boolean;
    }) => <textarea id={id} value={value} onChange={onChange} placeholder={placeholder} disabled={disabled} />,
    cn: (...args: string[]) => args.filter(Boolean).join(' '),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../../services/apis', () => ({
    updateApiGeneral: jest.fn(),
    startApi: jest.fn(),
    stopApi: jest.fn(),
    deleteApi: jest.fn(),
    duplicateApi: jest.fn(),
    exportApiDefinition: jest.fn(),
    updateApiFromDefinition: jest.fn(),
    updateApiPicture: jest.fn(),
    deleteApiPicture: jest.fn(),
    updateApiBackground: jest.fn(),
    deleteApiBackground: jest.fn(),
}));

jest.mock('../../../utils/queryKeys', () => ({
    apiDetailKeys: {
        all: ['api-detail'],
        detail: (envId: string, apiId: string) => ['api-detail', envId, apiId],
    },
    envCategoryKeys: {
        all: ['env-categories'],
        list: (envId: string) => ['env-categories', envId],
    },
}));

jest.mock('../../../hooks/useEnvCategories', () => ({
    useEnvCategories: jest.fn(() => ({ data: [], isLoading: false })),
}));

import { ApiGeneralPage } from './ApiGeneralPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { updateApiGeneral, startApi, stopApi, deleteApi } from '../../../services/apis';

const mockUseEnvironment = useEnvironment as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUpdateApiGeneral = updateApiGeneral as jest.Mock;
const mockStartApi = startApi as jest.Mock;
const mockStopApi = stopApi as jest.Mock;
const mockDeleteApi = deleteApi as jest.Mock;

// lifecycleState: 'CREATED' so the delete button is not blocked by cannotDelete
const STUB_API = {
    id: 'api-1',
    name: 'My Test API',
    apiVersion: 'v1.0',
    description: 'A test API',
    labels: ['alpha'],
    categories: ['Ops'],
    allowedInApiProducts: false,
    lifecycleState: 'CREATED',
    visibility: 'PRIVATE',
    state: 'STOPPED',
    primaryOwner: { displayName: 'Admin User', email: 'admin@example.com' },
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-06-01T00:00:00Z',
};

function makeClient() {
    return new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
}

function renderPage(apiId = 'api-1') {
    const client = makeClient();
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={[`/apis/${apiId}/general`]}>
                <Routes>
                    <Route path="apis">
                        <Route index element={<div data-testid="apis-list" />} />
                        <Route path=":apiId">
                            <Route path="general" element={<ApiGeneralPage />} />
                        </Route>
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ApiGeneralPage', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' });
        mockUseApiDetailContext.mockReturnValue({ api: STUB_API, isLoading: false, permissionsReady: true });
        mockUseHasPermission.mockReturnValue(true);
        mockUpdateApiGeneral.mockResolvedValue({ ...STUB_API, name: 'Updated API' });
        mockStartApi.mockResolvedValue(undefined);
        mockStopApi.mockResolvedValue(undefined);
        mockDeleteApi.mockResolvedValue(undefined);
    });

    afterEach(() => jest.clearAllMocks());

    // ── Loading & initial render ─────────────────────────────────────────────

    it('shows loading skeleton when context is loading', () => {
        mockUseApiDetailContext.mockReturnValue({ api: null, isLoading: true, permissionsReady: false });
        renderPage();
        expect(screen.getAllByTestId('skeleton').length).toBeGreaterThan(0);
    });

    it('renders form fields seeded from API data', () => {
        renderPage();
        expect((screen.getByRole('textbox', { name: /name/i }) as HTMLInputElement).value).toBe('My Test API');
        expect((screen.getByRole('textbox', { name: /version/i }) as HTMLInputElement).value).toBe('v1.0');
        expect((screen.getByRole('textbox', { name: /description/i }) as HTMLTextAreaElement).value).toBe('A test API');
    });

    // ── Dirty tracking & save ────────────────────────────────────────────────

    it('does not show Save button initially when form is clean', () => {
        renderPage();
        expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
    });

    it('shows Save and Discard buttons after name is edited', () => {
        renderPage();
        const nameInput = screen.getByRole('textbox', { name: /name/i });
        fireEvent.change(nameInput, { target: { value: 'Renamed API' } });
        expect(screen.getByRole('button', { name: /save changes/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();
    });

    it('clears dirty state when Discard is clicked', () => {
        renderPage();
        const nameInput = screen.getByRole('textbox', { name: /name/i });
        fireEvent.change(nameInput, { target: { value: 'Renamed API' } });
        fireEvent.click(screen.getByRole('button', { name: /discard/i }));
        expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
        expect((screen.getByRole('textbox', { name: /name/i }) as HTMLInputElement).value).toBe('My Test API');
    });

    it('calls updateApiGeneral with edited values on Save', async () => {
        renderPage();
        fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Renamed API' } });
        fireEvent.click(screen.getByRole('button', { name: /save changes/i }));
        await waitFor(() => expect(mockUpdateApiGeneral).toHaveBeenCalledTimes(1));
        // Signature is now (envId, apiId, current, patch)
        expect(mockUpdateApiGeneral).toHaveBeenCalledWith(
            'DEFAULT',
            'api-1',
            expect.objectContaining({ id: 'api-1' }),
            expect.objectContaining({ name: 'Renamed API' }),
        );
    });

    // ── Start / Stop ─────────────────────────────────────────────────────────

    it('shows Start button when API is stopped', () => {
        renderPage();
        expect(screen.getByRole('button', { name: /start/i })).toBeInTheDocument();
    });

    it('shows Stop button when API is started', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, state: 'STARTED' },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        expect(screen.getByRole('button', { name: /stop/i })).toBeInTheDocument();
    });

    it('calls startApi when Start button is clicked', async () => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /start/i }));
        await waitFor(() => expect(mockStartApi).toHaveBeenCalledWith('DEFAULT', 'api-1'));
    });

    it('calls stopApi when Stop button is clicked', async () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, state: 'STARTED' },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /stop/i }));
        await waitFor(() => expect(mockStopApi).toHaveBeenCalledWith('DEFAULT', 'api-1'));
    });

    // ── Delete ───────────────────────────────────────────────────────────────

    it('opens delete dialog when Delete button is clicked', () => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /delete this api/i }));
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText(/delete api permanently/i)).toBeInTheDocument();
    });

    it('keeps delete confirm button disabled until exact API name is typed', () => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /delete this api/i }));
        const confirmBtn = screen.getByRole('button', { name: /delete permanently/i });
        expect(confirmBtn).toBeDisabled();
        fireEvent.change(screen.getByPlaceholderText('My Test API'), { target: { value: 'My Test API' } });
        expect(confirmBtn).not.toBeDisabled();
    });

    it('calls deleteApi and closes dialog on confirmed delete', async () => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /delete this api/i }));
        fireEvent.change(screen.getByPlaceholderText('My Test API'), { target: { value: 'My Test API' } });
        fireEvent.click(screen.getByRole('button', { name: /delete permanently/i }));
        await waitFor(() => expect(mockDeleteApi).toHaveBeenCalledWith('DEFAULT', 'api-1'));
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    });

    it('disables Delete button when API is running', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, state: 'STARTED', lifecycleState: 'CREATED' },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        expect(screen.getByRole('button', { name: /delete this api/i })).toBeDisabled();
    });

    it('disables Delete button when API is published', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, state: 'STOPPED', lifecycleState: 'PUBLISHED' },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        expect(screen.getByRole('button', { name: /delete this api/i })).toBeDisabled();
    });

    // ── Actions strip ────────────────────────────────────────────────────────

    it('renders Export, Import, Duplicate, and Promote action buttons', () => {
        renderPage();
        expect(screen.getByRole('button', { name: /export/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /import/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /duplicate/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /promote/i })).toBeInTheDocument();
    });

    it('opens Export dialog when Export is clicked', () => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /export/i }));
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText(/export api definition/i)).toBeInTheDocument();
    });

    it('opens Duplicate dialog when Duplicate is clicked', () => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /duplicate/i }));
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText(/duplicate api/i)).toBeInTheDocument();
    });

    // ── Permission-gated rendering ────────────────────────────────────────────

    it('hides Export button when user lacks api-definition-r permission', () => {
        // api-definition-r is checked for Export; return false only for 'api-definition-r'
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-r'));
        renderPage();
        expect(screen.queryByRole('button', { name: /export/i })).toBeNull();
    });

    it('hides Import and Duplicate buttons when user lacks api-definition-c permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-c'));
        renderPage();
        expect(screen.queryByRole('button', { name: /import/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /duplicate/i })).toBeNull();
    });

    it('hides Promote button and API Events section when user lacks api-definition-u permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-u'));
        renderPage();
        expect(screen.queryByRole('button', { name: /promote/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /start/i })).toBeNull();
    });

    it('hides Delete section when user lacks api-definition-d permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-d'));
        renderPage();
        expect(screen.queryByRole('button', { name: /^delete$/i })).toBeNull();
    });

    it('disables form inputs when user lacks api-definition-u permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-u'));
        renderPage();
        expect((screen.getByRole('textbox', { name: /name/i }) as HTMLInputElement).disabled).toBe(true);
        expect((screen.getByRole('textbox', { name: /version/i }) as HTMLInputElement).disabled).toBe(true);
        expect((screen.getByRole('textbox', { name: /description/i }) as HTMLTextAreaElement).disabled).toBe(true);
    });

    it('shows Kubernetes banner and makes form read-only when API is managed by Kubernetes operator', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, definitionContext: { origin: 'KUBERNETES' } },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        expect(screen.getByText(/managed by the kubernetes operator/i)).toBeInTheDocument();
        expect((screen.getByRole('textbox', { name: /name/i }) as HTMLInputElement).disabled).toBe(true);
    });

    it('does not show Save button when form is edited but API is Kubernetes-managed', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, definitionContext: { origin: 'KUBERNETES' } },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Changed' } });
        // Input is disabled so value won't actually change, but even if it did, save must not appear
        expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
    });
});
