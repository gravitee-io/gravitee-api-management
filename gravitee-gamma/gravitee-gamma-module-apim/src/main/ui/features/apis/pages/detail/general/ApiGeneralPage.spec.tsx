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
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
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
        id,
    }: {
        checked?: boolean;
        onCheckedChange?: (v: boolean) => void;
        disabled?: boolean;
        id?: string;
    }) => (
        <input
            type="checkbox"
            id={id}
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
    Sheet: ({ children, open }: { children?: ReactNode; open?: boolean }) => (open ? <div role="dialog">{children}</div> : null),
    SheetContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    SheetFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
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
    Separator: ({ className }: { className?: string }) => <hr className={className} />,
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
    toast: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../../utils/queryKeys', () => ({
    apiDetailKeys: {
        all: ['api-detail'],
        detail: (envId: string, apiId: string) => ['api-detail', envId, apiId],
    },
    apiListKeys: {
        all: ['api-list'],
    },
    envCategoryKeys: {
        all: ['env-categories'],
        list: (envId: string) => ['env-categories', envId],
    },
}));

jest.mock('../../../hooks/useEnvCategories', () => ({
    useEnvCategories: jest.fn(() => ({ data: [], isLoading: false })),
}));

jest.mock('../../../services/apiProxy', () => ({
    verifyContextPath: jest.fn(() => Promise.resolve({ ok: true })),
    verifyApiHosts: jest.fn(() => Promise.resolve({ ok: true })),
}));

jest.mock('../../../services/policyStudioService', () => ({
    listPolicies: jest.fn(() => Promise.resolve([])),
}));

import { toast } from '@gravitee/graphene-core';

import { ApiGeneralPage } from './ApiGeneralPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import * as apiServices from '../../../services/apis';

const mockUseEnvironment = useEnvironment as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseHasPermission = useHasPermission as jest.Mock;

// lifecycleState: 'CREATED' so the delete button is not blocked by cannotDelete
const STUB_API = {
    id: 'api-1',
    name: 'My Test API',
    apiVersion: 'v1.0',
    listeners: [{ type: 'HTTP', paths: [{ path: '/testVisibility/', host: '' }] }],
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

// A federated API carries no runtime `state`, so only `lifecycleState` can block its delete.
const FEDERATED_API = {
    ...STUB_API,
    id: 'federated-api-1',
    name: 'Federated Orders API',
    definitionVersion: 'FEDERATED',
    state: undefined,
    listeners: undefined,
    lifecycleState: 'CREATED',
};

function makeClient() {
    return new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
}

function renderPage(apiId = 'api-1', client = makeClient()) {
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

        jest.spyOn(apiServices, 'updateApiGeneral').mockResolvedValue({ ...STUB_API, name: 'Updated API' });
        jest.spyOn(apiServices, 'startApi').mockResolvedValue(undefined);
        jest.spyOn(apiServices, 'stopApi').mockResolvedValue(undefined);
        jest.spyOn(apiServices, 'deleteApi').mockResolvedValue(undefined);
    });

    beforeAll(() => {
        Object.defineProperty(URL, 'createObjectURL', { value: jest.fn(() => 'blob:test'), writable: true });
        Object.defineProperty(URL, 'revokeObjectURL', { value: jest.fn(), writable: true });
    });

    afterEach(() => {
        jest.clearAllMocks();
        jest.restoreAllMocks();
    });

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

    it('hides the Allow in API Products toggle, switch included, for a federated API', () => {
        mockUseApiDetailContext.mockReturnValue({ api: FEDERATED_API, isLoading: false, permissionsReady: true });
        const { container } = renderPage('federated-api-1');

        expect(screen.queryByText('Allow in API Products')).toBeNull();
        // The Switch is the page's only checkbox outside the export/duplicate sheets, which are closed here
        expect(container.querySelectorAll('input[type="checkbox"]')).toHaveLength(0);
    });

    it('keeps the Allow in API Products switch bound to the form and dirty-tracking for a natively-managed API', () => {
        const { container } = renderPage();

        expect(screen.getByText('Allow in API Products')).toBeInTheDocument();
        const allowSwitch = container.querySelector('input[type="checkbox"]') as HTMLInputElement;
        expect(allowSwitch).not.toBeChecked();

        fireEvent.click(allowSwitch);

        expect(allowSwitch).toBeChecked();
        expect(screen.getByRole('button', { name: /save changes/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();
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
        await waitFor(() => expect(apiServices.updateApiGeneral).toHaveBeenCalledTimes(1));
        // Signature is now (envId, apiId, current, patch)
        expect(apiServices.updateApiGeneral).toHaveBeenCalledWith(
            'DEFAULT',
            'api-1',
            expect.objectContaining({ id: 'api-1' }),
            expect.objectContaining({ name: 'Renamed API' }),
        );
    });

    it('sends the toggled Allow in API Products value on Save for a natively-managed API', async () => {
        const { container } = renderPage();
        fireEvent.click(container.querySelector('input[type="checkbox"]') as HTMLInputElement);
        fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

        await waitFor(() =>
            expect(apiServices.updateApiGeneral).toHaveBeenCalledWith(
                'DEFAULT',
                'api-1',
                expect.objectContaining({ id: 'api-1' }),
                expect.objectContaining({ allowedInApiProducts: true }),
            ),
        );
    });

    // The federated page hides the Allow in API Products toggle but keeps its form field, so the API's own
    // value has to round-trip through the save payload rather than be dropped or reset to the `false` default.
    it('sends a federated API its own unchanged allowedInApiProducts value on Save', async () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...FEDERATED_API, allowedInApiProducts: true },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage('federated-api-1');
        fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Renamed Federated API' } });
        fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

        await waitFor(() => expect(apiServices.updateApiGeneral).toHaveBeenCalledTimes(1));
        expect(apiServices.updateApiGeneral).toHaveBeenCalledWith(
            'DEFAULT',
            'federated-api-1',
            expect.objectContaining({ id: 'federated-api-1' }),
            {
                name: 'Renamed Federated API',
                apiVersion: 'v1.0',
                description: 'A test API',
                labels: ['alpha'],
                categories: ['Ops'],
                allowedInApiProducts: true,
            },
        );
    });

    it('shows an error toast when the save request is refused', async () => {
        jest.spyOn(apiServices, 'updateApiGeneral').mockRejectedValue(new Error('Save refused'));
        renderPage();
        fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Renamed API' } });
        fireEvent.click(screen.getByRole('button', { name: /save changes/i }));
        await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Save refused', expect.anything()));
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
        await waitFor(() => expect(apiServices.startApi).toHaveBeenCalledWith('DEFAULT', 'api-1'));
    });

    it('calls stopApi when Stop button is clicked', async () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, state: 'STARTED' },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /stop/i }));
        await waitFor(() => expect(apiServices.stopApi).toHaveBeenCalledWith('DEFAULT', 'api-1'));
    });

    it('shows an error toast when the start request is refused', async () => {
        jest.spyOn(apiServices, 'startApi').mockRejectedValue(new Error('Start refused'));
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /start/i }));
        await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Start refused', expect.anything()));
    });

    it('shows an error toast when the stop request is refused', async () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, state: 'STARTED' },
            isLoading: false,
            permissionsReady: true,
        });
        jest.spyOn(apiServices, 'stopApi').mockRejectedValue(new Error('Stop refused'));
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /stop/i }));
        await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Stop refused', expect.anything()));
    });

    // 'STARTED' is not a shape a federated API can really carry; it is here to prove the control is
    // gated on the API type rather than on `apiStarted`, which an absent state already degrades to false.
    it.each([
        ['no runtime state', undefined],
        ['a runtime state of STARTED', 'STARTED'],
    ])('hides the Start/Stop control for a federated API with %s, keeping the API Events card', (_shape, state) => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...FEDERATED_API, state },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage('federated-api-1');

        expect(screen.queryByRole('button', { name: /start api/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /stop api/i })).toBeNull();
        expect(screen.getByText('API Events')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /delete this api/i })).toBeInTheDocument();
    });

    it('hides the API Events card for a federated API when the user holds update but not delete permission', () => {
        mockUseApiDetailContext.mockReturnValue({ api: FEDERATED_API, isLoading: false, permissionsReady: true });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-d'));
        renderPage('federated-api-1');

        expect(screen.queryByText('API Events')).toBeNull();
        expect(screen.queryByText(/alter the runtime state of your API/i)).toBeNull();
    });

    it('keeps the API Events card and its Start control for a natively-managed API when the user holds update but not delete permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-d'));
        renderPage();

        expect(screen.getByText('API Events')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /start api/i })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /delete this api/i })).toBeNull();
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
        await waitFor(() => expect(apiServices.deleteApi).toHaveBeenCalledWith('DEFAULT', 'api-1'));
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    });

    it('deletes a federated API and invalidates the API Proxies list cache', async () => {
        mockUseApiDetailContext.mockReturnValue({ api: FEDERATED_API, isLoading: false, permissionsReady: true });
        const client = makeClient();
        const invalidateQueries = jest.spyOn(client, 'invalidateQueries');
        renderPage('federated-api-1', client);

        const deleteButton = screen.getByRole('button', { name: /delete this api/i });
        expect(deleteButton).not.toBeDisabled();

        fireEvent.click(deleteButton);
        fireEvent.change(screen.getByPlaceholderText('Federated Orders API'), { target: { value: 'Federated Orders API' } });
        fireEvent.click(screen.getByRole('button', { name: /delete permanently/i }));

        await waitFor(() => expect(apiServices.deleteApi).toHaveBeenCalledWith('DEFAULT', 'federated-api-1'));
        await waitFor(() => expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['api-list'] }));
    });

    // The refused API is still in the list, so evicting the list here would cost a refetch that changes
    // nothing — and an invalidation moved out of onSuccess onto the click or onSettled would look
    // identical to the case above, which only ever sees a delete that resolved.
    it('leaves the API Proxies list cache alone when the delete request is refused', async () => {
        mockUseApiDetailContext.mockReturnValue({ api: FEDERATED_API, isLoading: false, permissionsReady: true });
        jest.spyOn(apiServices, 'deleteApi').mockRejectedValue(new Error('Delete refused'));
        const client = makeClient();
        const invalidateQueries = jest.spyOn(client, 'invalidateQueries');
        renderPage('federated-api-1', client);

        fireEvent.click(screen.getByRole('button', { name: /delete this api/i }));
        fireEvent.change(screen.getByPlaceholderText('Federated Orders API'), { target: { value: 'Federated Orders API' } });
        fireEvent.click(screen.getByRole('button', { name: /delete permanently/i }));

        await waitFor(() => expect(toast.error).toHaveBeenCalled());
        expect(invalidateQueries).not.toHaveBeenCalledWith({ queryKey: ['api-list'] });
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

    it('enables Import but keeps Promote disabled until fully implemented', () => {
        renderPage();
        expect(screen.getByRole('button', { name: /import/i })).not.toBeDisabled();
        expect(screen.getByRole('button', { name: /promote/i })).toBeDisabled();
    });

    it('hides the whole action strip, divider included, for a federated API', () => {
        mockUseApiDetailContext.mockReturnValue({ api: FEDERATED_API, isLoading: false, permissionsReady: true });
        const { container } = renderPage('federated-api-1');

        expect(screen.queryByRole('button', { name: /export/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /import/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /duplicate/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /promote/i })).toBeNull();
        // `my-5` is the action strip's own separator; the sidebar separator carries no class
        expect(container.querySelector('hr.my-5')).toBeNull();
        expect(container.querySelectorAll('hr')).toHaveLength(1);
    });

    it('calls exportApiDefinition with unchecked exclude options from the export dialog', async () => {
        const exportSpy = jest.spyOn(apiServices, 'exportApiDefinition').mockResolvedValue(new Blob(['{}'], { type: 'application/json' }));
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /export/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.click(within(dialog).getByLabelText('Members'));
        fireEvent.click(within(dialog).getByRole('button', { name: /^export$/i }));

        await waitFor(() => expect(exportSpy).toHaveBeenCalledWith('DEFAULT', 'api-1', ['members']));
        exportSpy.mockRestore();
    });

    it('shows an inline error in the export sheet when the export request is refused', async () => {
        const exportSpy = jest.spyOn(apiServices, 'exportApiDefinition').mockRejectedValue(new Error('Export refused'));
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /export/i }));
        const dialog = screen.getByRole('dialog');
        await act(async () => {
            fireEvent.click(within(dialog).getByRole('button', { name: /^export$/i }));
        });

        expect(await screen.findByText('Export refused')).toBeInTheDocument();
        exportSpy.mockRestore();
    });

    it('calls duplicateApi with context path, version, and filtered fields from the duplicate dialog', async () => {
        const duplicateSpy = jest.spyOn(apiServices, 'duplicateApi').mockResolvedValue({ id: 'api-2', name: 'My Test API copy' });
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /duplicate/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByPlaceholderText('/testVisibility/'), { target: { value: '/duplicate' } });
        fireEvent.change(within(dialog).getByPlaceholderText('v1.0'), { target: { value: 'v2' } });
        fireEvent.click(within(dialog).getByLabelText('Members'));

        const duplicateBtn = within(dialog).getByRole('button', { name: /^duplicate$/i });
        await waitFor(() => expect(duplicateBtn).not.toBeDisabled());
        await act(async () => {
            fireEvent.click(duplicateBtn);
        });

        await waitFor(() =>
            expect(duplicateSpy).toHaveBeenCalledWith('DEFAULT', 'api-1', {
                version: 'v2',
                contextPath: '/duplicate',
                filteredFields: ['MEMBERS'],
            }),
        );
        duplicateSpy.mockRestore();
    });

    it('shows an inline error in the duplicate sheet when the duplicate request is refused', async () => {
        const duplicateSpy = jest.spyOn(apiServices, 'duplicateApi').mockRejectedValue(new Error('Duplicate refused'));
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /duplicate/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByPlaceholderText('/testVisibility/'), { target: { value: '/duplicate' } });
        fireEvent.change(within(dialog).getByPlaceholderText('v1.0'), { target: { value: 'v2' } });

        const duplicateBtn = within(dialog).getByRole('button', { name: /^duplicate$/i });
        await waitFor(() => expect(duplicateBtn).not.toBeDisabled());
        await act(async () => {
            fireEvent.click(duplicateBtn);
        });

        expect(await screen.findByText('Duplicate refused')).toBeInTheDocument();
        duplicateSpy.mockRestore();
    });

    it('calls updateApiFromDefinition when importing a local Gravitee definition file', async () => {
        const importSpy = jest.spyOn(apiServices, 'updateApiFromDefinition').mockResolvedValue({ id: 'api-1', name: 'My Test API' });
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
        const dialog = screen.getByRole('dialog');

        const definition = { api: { name: 'My Test API' } };
        const file = new File([JSON.stringify(definition)], 'api.json', { type: 'application/json' });
        Object.defineProperty(file, 'text', { value: () => Promise.resolve(JSON.stringify(definition)) });
        const fileInput = dialog.querySelector('input[type="file"]') as HTMLInputElement;
        await act(async () => {
            fireEvent.change(fileInput, { target: { files: [file] } });
        });

        const importBtn = within(dialog).getByRole('button', { name: /^import$/i });
        await waitFor(() => expect(importBtn).not.toBeDisabled());
        fireEvent.click(importBtn);

        await waitFor(() => expect(importSpy).toHaveBeenCalledWith('DEFAULT', 'api-1', definition));
        importSpy.mockRestore();
    });

    it('shows an inline error in the import sheet when the import request is refused', async () => {
        const importSpy = jest.spyOn(apiServices, 'updateApiFromDefinition').mockRejectedValue(new Error('Import refused'));
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
        const dialog = screen.getByRole('dialog');

        const definition = { api: { name: 'My Test API' } };
        const file = new File([JSON.stringify(definition)], 'api.json', { type: 'application/json' });
        Object.defineProperty(file, 'text', { value: () => Promise.resolve(JSON.stringify(definition)) });
        const fileInput = dialog.querySelector('input[type="file"]') as HTMLInputElement;
        await act(async () => {
            fireEvent.change(fileInput, { target: { files: [file] } });
        });

        const importBtn = within(dialog).getByRole('button', { name: /^import$/i });
        await waitFor(() => expect(importBtn).not.toBeDisabled());
        fireEvent.click(importBtn);

        expect(await screen.findByText('Import refused')).toBeInTheDocument();
        importSpy.mockRestore();
    });

    it('calls updateApiFromSwagger when importing an OpenAPI spec file', async () => {
        const importSpy = jest.spyOn(apiServices, 'updateApiFromSwagger').mockResolvedValue({ id: 'api-1', name: 'My Test API' });
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.click(within(dialog).getByRole('tab', { name: 'OpenAPI specification' }));

        const yaml = 'openapi: 3.0.0\ninfo:\n  title: My API';
        const file = new File([yaml], 'api.yaml', { type: 'application/x-yaml' });
        Object.defineProperty(file, 'text', { value: () => Promise.resolve(yaml) });
        const fileInput = dialog.querySelector('input[type="file"]') as HTMLInputElement;
        await act(async () => {
            fireEvent.change(fileInput, { target: { files: [file] } });
        });

        const importBtn = within(dialog).getByRole('button', { name: /^import$/i });
        await waitFor(() => expect(importBtn).not.toBeDisabled());
        fireEvent.click(importBtn);

        await waitFor(() => expect(importSpy).toHaveBeenCalledWith('DEFAULT', 'api-1', { payload: yaml, withDocumentation: true }));
        importSpy.mockRestore();
    });

    it('calls updateApiFromWsdl when importing a WSDL file', async () => {
        const importSpy = jest.spyOn(apiServices, 'updateApiFromWsdl').mockResolvedValue({ id: 'api-1', name: 'My Test API' });
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.click(within(dialog).getByRole('tab', { name: 'WSDL' }));

        const wsdl = '<?xml version="1.0"?><definitions></definitions>';
        const file = new File([wsdl], 'service.wsdl', { type: 'application/xml' });
        Object.defineProperty(file, 'text', { value: () => Promise.resolve(wsdl) });
        const fileInput = dialog.querySelector('input[type="file"]') as HTMLInputElement;
        await act(async () => {
            fireEvent.change(fileInput, { target: { files: [file] } });
        });

        const importBtn = within(dialog).getByRole('button', { name: /^import$/i });
        await waitFor(() => expect(importBtn).not.toBeDisabled());
        fireEvent.click(importBtn);

        await waitFor(() =>
            expect(importSpy).toHaveBeenCalledWith('DEFAULT', 'api-1', {
                payload: wsdl,
                type: 'INLINE',
                withDocumentation: false,
                withPolicies: [],
            }),
        );
        importSpy.mockRestore();
    });

    // ── Images ───────────────────────────────────────────────────────────────

    it('shows an error toast when removing the picture is refused', async () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, _links: { pictureUrl: 'https://example.com/picture.png' } },
            isLoading: false,
            permissionsReady: true,
        });
        jest.spyOn(apiServices, 'deleteApiPicture').mockRejectedValue(new Error('Remove picture refused'));
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /^remove$/i }));
        await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Remove picture refused', expect.anything()));
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
