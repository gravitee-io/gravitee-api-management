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
    Alert: ({ children }: { children?: ReactNode }) => <div role="alert">{children}</div>,
    AlertTitle: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    AlertDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
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
    Select: ({ value, onValueChange, children }: { value?: string; onValueChange?: (v: string) => void; children: ReactNode }) => (
        <select aria-label="Environment" value={value} onChange={e => onValueChange?.(e.target.value)}>
            {children}
        </select>
    ),
    SelectContent: ({ children }: { children?: ReactNode }) => <>{children}</>,
    SelectItem: ({ value, disabled, children }: { value: string; disabled?: boolean; children: ReactNode }) => (
        <option value={value} disabled={disabled}>
            {children}
        </option>
    ),
    SelectTrigger: ({ children }: { children?: ReactNode }) => <>{children}</>,
    SelectValue: () => null,
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
    apiPromotionKeys: {
        all: ['api-promotion'],
        targets: (envId: string) => ['api-promotion', 'targets', envId],
        pending: (apiId: string) => ['api-promotion', 'pending', apiId],
    },
}));

const mockUseApiReviewEnabled = jest.fn(() => ({ enabled: false, isFetched: true }));
jest.mock('../../../hooks/useApiReviewEnabled', () => ({
    useApiReviewEnabled: () => mockUseApiReviewEnabled(),
}));

const mockAskReviewMutate = jest.fn();
jest.mock('../../../hooks/useApiReviewMutations', () => ({
    useAskApiReview: () => ({ mutate: mockAskReviewMutate, isPending: false }),
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

import { ApiGeneralPage } from './ApiGeneralPage';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useEnvCategories } from '../../../hooks/useEnvCategories';
import * as apiServices from '../../../services/apis';

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment -- graphene-core is mocked above
const { toast } = jest.requireMock<{ toast: { success: jest.Mock } }>('@gravitee/graphene-core');

const mockUseEnvironment = useEnvironment as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseEnvCategories = useEnvCategories as jest.Mock;

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

// `Ops` matches the fixture's already-selected category; `payments` is the one a test can newly select.
const ENV_CATEGORIES = [
    { id: 'cat-ops', key: 'Ops', name: 'Ops' },
    { id: 'cat-payments', key: 'payments', name: 'Payments' },
];

function makeClient() {
    return new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
}

function describeControl(el: Element): string {
    const tag = el.tagName.toLowerCase();
    const id = el.getAttribute('id');
    if (id) return `${tag}#${id}`;
    const ariaLabel = el.getAttribute('aria-label');
    if (ariaLabel) return `${tag}[${ariaLabel}]`;
    const placeholder = el.getAttribute('placeholder');
    if (placeholder) return `${tag}[${placeholder}]`;
    if (el instanceof HTMLInputElement) return `input[type=${el.type}]`;
    // An action card nests its own name in a leading <p> ahead of a longer description <p>
    const heading = el.querySelector('p');
    return `${tag}:${(heading ?? el).textContent?.trim()}`;
}

function interactiveControls(container: HTMLElement): string[] {
    return [...container.querySelectorAll('input, textarea, button, [role="button"]')].map(describeControl).sort();
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
        mockUseEnvCategories.mockReturnValue({ data: [], isLoading: false });

        jest.spyOn(apiServices, 'updateApiGeneral').mockResolvedValue({ ...STUB_API, name: 'Updated API' });
        jest.spyOn(apiServices, 'startApi').mockResolvedValue(undefined);
        jest.spyOn(apiServices, 'stopApi').mockResolvedValue(undefined);
        jest.spyOn(apiServices, 'deleteApi').mockResolvedValue(undefined);
        jest.spyOn(apiServices, 'getPromotionTargets').mockResolvedValue([]);
        jest.spyOn(apiServices, 'getPendingPromotions').mockResolvedValue([]);
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

    it('hides the Start/Stop control for a federated API when the user lacks api-definition-u, keeping Delete in the API Events card', () => {
        mockUseApiDetailContext.mockReturnValue({ api: FEDERATED_API, isLoading: false, permissionsReady: true });
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-u'));
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

    it('hides the API Events card for a natively-managed API when the user holds neither update nor delete permission', () => {
        mockUseHasPermission.mockImplementation(
            ({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-u') && !anyOf.includes('api-definition-d'),
        );
        renderPage();

        expect(screen.queryByText('API Events')).toBeNull();
        expect(screen.queryByText(/alter the runtime state of your API/i)).toBeNull();
        expect(screen.queryByRole('button', { name: /start api/i })).toBeNull();
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

    // The published guard is shared with the natively-managed path and carries no API-type term, so a
    // federated API keeps the control it is entitled to and only loses the ability to press it.
    it('keeps Delete present but disabled for a published federated API', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...FEDERATED_API, lifecycleState: 'PUBLISHED' },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage('federated-api-1');

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

    it('enables both Import and Promote for an eligible API', () => {
        renderPage();
        expect(screen.getByRole('button', { name: /import/i })).not.toBeDisabled();
        expect(screen.getByRole('button', { name: /promote/i })).not.toBeDisabled();
    });

    it('disables Promote for a DEPRECATED API', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, lifecycleState: 'DEPRECATED' },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        expect(screen.getByRole('button', { name: /promote/i })).toBeDisabled();
    });

    it('disables Promote for a Kubernetes-managed API', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, definitionContext: { origin: 'KUBERNETES' } },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        expect(screen.getByRole('button', { name: /promote/i })).toBeDisabled();
    });

    it('promotes the API to the selected target and shows a success toast', async () => {
        jest.spyOn(apiServices, 'getPromotionTargets').mockResolvedValue([
            { id: 'env#1', name: 'Production' },
            { id: 'env#2', name: 'Staging' },
        ]);
        const promoteSpy = jest.spyOn(apiServices, 'promoteApi').mockResolvedValue(undefined);
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /^promote$/i }));
        const dialog = await screen.findByRole('dialog');
        const select = await within(dialog).findByLabelText('Environment');
        await waitFor(() => expect(within(select).getByRole('option', { name: 'Production' }).selected).toBe(true));

        fireEvent.change(select, { target: { value: within(select).getByRole('option', { name: 'Staging' }).getAttribute('value') } });
        fireEvent.click(within(dialog).getByRole('button', { name: /^promote$/i }));

        await waitFor(() =>
            expect(promoteSpy).toHaveBeenCalledWith('DEFAULT', 'api-1', { targetEnvCockpitId: 'env#2', targetEnvName: 'Staging' }),
        );
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(toast.success).toHaveBeenCalledWith('Promotion requested', expect.anything());
    });

    it('shows a "connect to Gravitee Cloud" state when the installation is not accepted', async () => {
        jest.spyOn(apiServices, 'getPromotionTargets').mockRejectedValue(
            new ApimApiError(412, 'Installation not accepted', {
                technicalCode: 'installation.notAccepted',
                parameters: { cockpitURL: 'https://cockpit.gravitee.io/link' },
            }),
        );
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /^promote$/i }));
        const dialog = await screen.findByRole('dialog');

        expect(await within(dialog).findByText(/meet gravitee cloud/i)).toBeInTheDocument();
        expect(within(dialog).getByRole('link', { name: /gravitee cloud/i })).toHaveAttribute('href', 'https://cockpit.gravitee.io/link');
    });

    it('shows an empty-state and disables Promote when there are no eligible destination environments', async () => {
        jest.spyOn(apiServices, 'getPromotionTargets').mockResolvedValue([]);
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /^promote$/i }));
        const dialog = await screen.findByRole('dialog');

        expect(await within(dialog).findByText(/no environment is available/i)).toBeInTheDocument();
        expect(within(dialog).getByRole('button', { name: /^promote$/i })).toBeDisabled();
    });

    it('shows a distinct error state (not "no environment available") when loading targets fails for a reason other than a missing Cockpit connection', async () => {
        jest.spyOn(apiServices, 'getPromotionTargets').mockRejectedValue(new Error('Network error'));
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /^promote$/i }));
        const dialog = await screen.findByRole('dialog');

        expect(await within(dialog).findByText(/could not load promotion targets/i)).toBeInTheDocument();
        expect(within(dialog).getByText('Network error')).toBeInTheDocument();
        expect(within(dialog).queryByText(/no environment is available/i)).toBeNull();
        expect(within(dialog).queryByLabelText('Environment')).toBeNull();
    });

    it('treats a pending-promotions load failure as an error too, not as zero pending promotions', async () => {
        jest.spyOn(apiServices, 'getPromotionTargets').mockResolvedValue([{ id: 'env#1', name: 'Production' }]);
        jest.spyOn(apiServices, 'getPendingPromotions').mockRejectedValue(new Error('Search failed'));
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /^promote$/i }));
        const dialog = await screen.findByRole('dialog');

        expect(await within(dialog).findByText(/could not load promotion targets/i)).toBeInTheDocument();
        expect(within(dialog).queryByLabelText('Environment')).toBeNull();
    });

    it('falls back to a default Cockpit URL when the error response omits one', async () => {
        jest.spyOn(apiServices, 'getPromotionTargets').mockRejectedValue(
            new ApimApiError(412, 'Installation not accepted', { technicalCode: 'installation.notAccepted' }),
        );
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /^promote$/i }));
        const dialog = await screen.findByRole('dialog');

        expect(await within(dialog).findByText(/meet gravitee cloud/i)).toBeInTheDocument();
        expect(within(dialog).getByRole('link', { name: /gravitee cloud/i })).toHaveAttribute('href', 'https://cockpit.gravitee.io');
    });

    it('clears a stale promotion error before the next attempt', async () => {
        jest.spyOn(apiServices, 'getPromotionTargets').mockResolvedValue([{ id: 'env#1', name: 'Production' }]);
        jest.spyOn(apiServices, 'promoteApi').mockRejectedValue(new Error('Target conflict'));
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /^promote$/i }));
        const dialog = await screen.findByRole('dialog');
        await within(dialog).findByLabelText('Environment');
        fireEvent.click(within(dialog).getByRole('button', { name: /^promote$/i }));
        expect(await within(dialog).findByText('Target conflict')).toBeInTheDocument();

        // The trigger resets the mutation before reopening, so a fresh attempt never shows the old error.
        fireEvent.click(screen.getAllByRole('button', { name: /^promote$/i })[0]);
        expect(within(dialog).queryByText('Target conflict')).toBeNull();
    });

    it('disables a destination that already has a pending promotion for this API', async () => {
        jest.spyOn(apiServices, 'getPromotionTargets').mockResolvedValue([
            { id: 'env#1', name: 'Production' },
            { id: 'env#2', name: 'Staging' },
        ]);
        jest.spyOn(apiServices, 'getPendingPromotions').mockResolvedValue([{ status: 'CREATED', targetEnvCockpitId: 'env#1' }]);
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /^promote$/i }));
        const dialog = await screen.findByRole('dialog');

        const select = await within(dialog).findByLabelText('Environment');
        const pendingOption = within(select).getByRole('option', { name: /production \(pending\)/i });
        expect(pendingOption).toBeDisabled();
        // Non-pending target is selected by default, not the pending one.
        await waitFor(() => expect(within(select).getByRole('option', { name: 'Staging' }).selected).toBe(true));
    });

    it('drops only Duplicate from the action strip for a native API', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, type: 'NATIVE' },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();

        expect(screen.queryByRole('button', { name: /duplicate/i })).toBeNull();
        expect(screen.getByRole('button', { name: /export/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /import/i })).toBeInTheDocument();
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

    it('exports the CRD definition as a -crd.yml download from the CRD tab of the export dialog', async () => {
        const crdSpy = jest
            .spyOn(apiServices, 'exportApiCrd')
            .mockResolvedValue(new Blob(['kind: ApiV4Definition'], { type: 'text/yaml' }));
        const definitionSpy = jest.spyOn(apiServices, 'exportApiDefinition');
        const downloadedFileNames: string[] = [];
        jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
            downloadedFileNames.push(this.download);
        });
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /export/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.click(within(dialog).getByRole('tab', { name: /CRD API Definition/i }));
        fireEvent.click(within(dialog).getByRole('button', { name: /^export$/i }));

        await waitFor(() => expect(downloadedFileNames).toEqual(['My-Test-API-v1-0-crd.yml']));
        expect(crdSpy).toHaveBeenCalledWith('DEFAULT', 'api-1');
        expect(definitionSpy).not.toHaveBeenCalled();
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

    it('hides the Allow in API Products toggle, switch included, when user lacks api-definition-r permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-r'));
        const { container } = renderPage();

        expect(screen.queryByText('Allow in API Products')).toBeNull();
        // The Switch is the page's only checkbox outside the export/duplicate sheets, which are closed here
        expect(container.querySelectorAll('input[type="checkbox"]')).toHaveLength(0);
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

    it('disables Import and Duplicate when API is managed by Kubernetes operator', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { ...STUB_API, definitionContext: { origin: 'KUBERNETES' } },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();

        expect(screen.getByRole('button', { name: /import/i })).toBeDisabled();
        expect(screen.getByRole('button', { name: /duplicate/i })).toBeDisabled();
    });

    // ── Federated fields, control inventory, and images ──────────────────────

    describe('for a federated API', () => {
        beforeEach(() => {
            mockUseApiDetailContext.mockReturnValue({ api: FEDERATED_API, isLoading: false, permissionsReady: true });
        });

        const permissionGrants: [string, () => void][] = [
            ['every api-definition permission', () => mockUseHasPermission.mockReturnValue(true)],
            [
                'api-definition-u alone',
                () => mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => anyOf.includes('api-definition-u')),
            ],
        ];

        it.each(permissionGrants)(
            'renders name, version, description and categories as enabled controls under %s',
            (_grant, grantPermissions) => {
                grantPermissions();
                mockUseEnvCategories.mockReturnValue({ data: ENV_CATEGORIES, isLoading: false });
                const { container } = renderPage('federated-api-1');

                expect((screen.getByRole('textbox', { name: /name/i }) as HTMLInputElement).disabled).toBe(false);
                expect((screen.getByRole('textbox', { name: /version/i }) as HTMLInputElement).disabled).toBe(false);
                expect((screen.getByRole('textbox', { name: /description/i }) as HTMLTextAreaElement).disabled).toBe(false);
                expect(container.querySelector('button#api-categories')).not.toBeDisabled();
            },
        );

        // The fifth supported field's enablement is only observable here: `ChipInput` owns its draft state,
        // so a typed value lands in the input whether or not the page accepts it — only a committed chip,
        // which comes back through `form.labels`, proves the page's own read-only guard at
        // ApiGeneralPage.tsx:361 let the edit through for a federated API.
        it.each(permissionGrants)('commits a typed label as a chip under %s', (_grant, grantPermissions) => {
            grantPermissions();
            renderPage('federated-api-1');

            const labelsInput = screen.getByRole('textbox', { name: /labels/i });
            fireEvent.change(labelsInput, { target: { value: 'beta' } });
            fireEvent.keyDown(labelsInput, { key: 'Enter' });

            expect(screen.getByRole('button', { name: 'Remove beta' })).toBeInTheDocument();
        });

        const supportedFieldEdits: { field: string; edit: () => void; patch: Record<string, unknown> }[] = [
            {
                field: 'name',
                edit: () =>
                    fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Renamed Federated API' } }),
                patch: { name: 'Renamed Federated API' },
            },
            {
                field: 'version',
                edit: () => fireEvent.change(screen.getByRole('textbox', { name: /version/i }), { target: { value: 'v2.0' } }),
                patch: { apiVersion: 'v2.0' },
            },
            {
                field: 'description',
                edit: () =>
                    fireEvent.change(screen.getByRole('textbox', { name: /description/i }), { target: { value: 'A federated API' } }),
                patch: { description: 'A federated API' },
            },
            {
                field: 'labels',
                edit: () => {
                    const labelsInput = screen.getByRole('textbox', { name: /labels/i });
                    fireEvent.change(labelsInput, { target: { value: 'beta' } });
                    fireEvent.keyDown(labelsInput, { key: 'Enter' });
                },
                patch: { labels: ['alpha', 'beta'] },
            },
            {
                field: 'categories',
                edit: () => fireEvent.click(screen.getByLabelText('Payments')),
                patch: { categories: ['Ops', 'payments'] },
            },
        ];

        it.each(supportedFieldEdits)('sends an edited $field through the FEDERATED update path on Save', async ({ edit, patch }) => {
            mockUseEnvCategories.mockReturnValue({ data: ENV_CATEGORIES, isLoading: false });
            renderPage('federated-api-1');

            edit();
            fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

            await waitFor(() => expect(apiServices.updateApiGeneral).toHaveBeenCalledTimes(1));
            expect(apiServices.updateApiGeneral).toHaveBeenCalledWith(
                'DEFAULT',
                'federated-api-1',
                expect.objectContaining({ definitionVersion: 'FEDERATED' }),
                expect.objectContaining(patch),
            );
        });

        it('sends an edit through the update path when api-definition-u is the only permission granted', async () => {
            mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => anyOf.includes('api-definition-u'));
            renderPage('federated-api-1');

            fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Renamed Federated API' } });
            fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

            await waitFor(() => expect(apiServices.updateApiGeneral).toHaveBeenCalledTimes(1));
            expect(apiServices.updateApiGeneral).toHaveBeenCalledWith(
                'DEFAULT',
                'federated-api-1',
                expect.objectContaining({ definitionVersion: 'FEDERATED' }),
                expect.objectContaining({ name: 'Renamed Federated API' }),
            );
        });

        // The counterpart of the enabled cases above: `api-definition-u` is what unlocks the federated form,
        // not federation itself. `ChipInput` takes no `disabled` prop — its read-only guard sits in the page's
        // own onChange at ApiGeneralPage.tsx:361 — so only a committed chip, never the typed draft, tells the
        // two states apart.
        it('keeps all five supported fields read-only for a federated API when api-definition-u is withheld', () => {
            mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-u'));
            mockUseEnvCategories.mockReturnValue({ data: ENV_CATEGORIES, isLoading: false });
            const { container } = renderPage('federated-api-1');

            expect((screen.getByRole('textbox', { name: /name/i }) as HTMLInputElement).disabled).toBe(true);
            expect((screen.getByRole('textbox', { name: /version/i }) as HTMLInputElement).disabled).toBe(true);
            expect((screen.getByRole('textbox', { name: /description/i }) as HTMLTextAreaElement).disabled).toBe(true);
            expect(container.querySelector('button#api-categories')).toBeDisabled();

            const labelsInput = screen.getByRole('textbox', { name: /labels/i });
            fireEvent.change(labelsInput, { target: { value: 'beta' } });
            fireEvent.keyDown(labelsInput, { key: 'Enter' });

            expect(screen.queryByRole('button', { name: 'Remove beta' })).toBeNull();
            expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
        });

        // `labels` is here alongside a plain text field because the dirty check compares by JSON.stringify,
        // so a rebaseline that missed the array-valued fields would leave only these two cases dirty.
        const rebaselinedFields: { field: string; edit: () => void; expectKept: () => void }[] = [
            {
                field: 'name',
                edit: () =>
                    fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Renamed Federated API' } }),
                expectKept: () =>
                    expect((screen.getByRole('textbox', { name: /name/i }) as HTMLInputElement).value).toBe('Renamed Federated API'),
            },
            {
                field: 'labels',
                edit: () => {
                    const labelsInput = screen.getByRole('textbox', { name: /labels/i });
                    fireEvent.change(labelsInput, { target: { value: 'beta' } });
                    fireEvent.keyDown(labelsInput, { key: 'Enter' });
                },
                expectKept: () => expect(screen.getByRole('button', { name: 'Remove beta' })).toBeInTheDocument(),
            },
        ];

        it.each(rebaselinedFields)(
            "keeps an edited $field as the form's new clean baseline once the save resolves",
            async ({ edit, expectKept }) => {
                renderPage('federated-api-1');

                edit();
                fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

                await waitFor(() => expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull());
                expect(screen.queryByRole('button', { name: /discard/i })).toBeNull();
                expectKept();
            },
        );

        // A rebaseline moved out of the save's onSuccess would satisfy the cases above just as well, and
        // would silently present a refused edit as saved — only a refused save tells the two apart.
        it('keeps the edited value and the form dirty when the save is refused', async () => {
            jest.spyOn(apiServices, 'updateApiGeneral').mockRejectedValue(new Error('Save refused'));
            renderPage('federated-api-1');

            fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Renamed Federated API' } });
            fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

            await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Save refused', expect.anything()));
            expect((screen.getByRole('textbox', { name: /name/i }) as HTMLInputElement).value).toBe('Renamed Federated API');
            expect(screen.getByRole('button', { name: /save changes/i })).toBeInTheDocument();
            expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();
        });

        // No page to reload in a component test; the checkable equivalent is that the detail query is
        // evicted, so the next read of the API comes from the server rather than from the cached value.
        it('invalidates the API detail query once the save resolves', async () => {
            const client = makeClient();
            const invalidateQueries = jest.spyOn(client, 'invalidateQueries');
            renderPage('federated-api-1', client);

            fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Renamed Federated API' } });
            fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

            await waitFor(() => expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['api-detail', 'DEFAULT', 'federated-api-1'] }));
        });

        // An eviction moved onto the Save click or onto onSettled would look identical to the case above,
        // which only ever sees a save that resolved.
        it('leaves the API detail cache alone when the save is refused', async () => {
            jest.spyOn(apiServices, 'updateApiGeneral').mockRejectedValue(new Error('Save refused'));
            const client = makeClient();
            const invalidateQueries = jest.spyOn(client, 'invalidateQueries');
            renderPage('federated-api-1', client);

            fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Renamed Federated API' } });
            fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

            await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Save refused', expect.anything()));
            expect(invalidateQueries).not.toHaveBeenCalledWith({ queryKey: ['api-detail', 'DEFAULT', 'federated-api-1'] });
        });

        // ── Complete interactive-control inventory ───────────────────────────

        // The last three are markup rather than fields: the Popover mock ignores its `open` prop so the
        // category filter input is always mounted, and each ImagePicker hides an sr-only file input
        // behind its div[role=button]. The fixture's single label produces the one chip-removal button;
        // it carries no `_links`, which is why neither ImagePicker renders a Remove button.
        const federatedInteractiveControls = [
            'input#api-name',
            'input#api-version',
            'textarea#api-description',
            'input#api-labels',
            'button[Remove alpha]',
            'button#api-categories',
            'div[Upload Picture]',
            'div[Upload Background]',
            'button:Delete this API',
            'input[Filter categories…]',
            'input[type=file]',
            'input[type=file]',
        ];

        it('renders no interactive control beyond the supported fields, the image pickers, and Delete', () => {
            const { container } = renderPage('federated-api-1');

            expect(interactiveControls(container)).toEqual([...federatedInteractiveControls].sort());
        });

        it('adds only Save and Discard to that set once the form is dirty', () => {
            const { container } = renderPage('federated-api-1');

            fireEvent.change(screen.getByRole('textbox', { name: /name/i }), { target: { value: 'Renamed Federated API' } });

            expect(interactiveControls(container)).toEqual(
                [...federatedInteractiveControls, 'button:Discard', 'button:Save changes'].sort(),
            );
        });

        // ── Image uploads ────────────────────────────────────────────────────

        // Indexing the hidden file inputs is what stops a mis-wired Background picker from passing on the
        // Picture picker's behaviour — the two sit in the same row and differ only by props.
        const imagePickers: [string, number][] = [
            ['Picture', 0],
            ['Background', 1],
        ];

        // `ImagePicker` exposes no `disabled` or `aria-disabled` attribute on its div[role=button], so
        // opening the hidden file chooser is the only observable form of "enabled".
        it.each(imagePickers)('opens the %s file chooser for a user holding only api-definition-u', (label, index) => {
            mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => anyOf.includes('api-definition-u'));
            const { container } = renderPage('federated-api-1');
            const fileInput = container.querySelectorAll('input[type="file"]')[index] as HTMLInputElement;
            const openChooser = jest.spyOn(fileInput, 'click');

            const uploadControl = screen.getByRole('button', { name: new RegExp(`upload ${label}`, 'i') });
            expect(uploadControl).toBeInTheDocument();
            fireEvent.click(uploadControl);

            expect(openChooser).toHaveBeenCalledTimes(1);
        });

        // Negative control: a picker hard-wired to `disabled={false}` would satisfy the positive case alone.
        it.each(imagePickers)('leaves the %s upload control inert for a user denied api-definition-u', (label, index) => {
            mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-u'));
            const { container } = renderPage('federated-api-1');
            const fileInput = container.querySelectorAll('input[type="file"]')[index] as HTMLInputElement;
            const openChooser = jest.spyOn(fileInput, 'click');

            fireEvent.click(screen.getByRole('button', { name: new RegExp(`upload ${label}`, 'i') }));

            expect(openChooser).not.toHaveBeenCalled();
        });

        it('persists a selected Picture immediately and evicts the API detail query, with no Save click and no dirty form', async () => {
            const pictureSpy = jest.spyOn(apiServices, 'updateApiPicture').mockResolvedValue(undefined);
            const client = makeClient();
            const invalidateQueries = jest.spyOn(client, 'invalidateQueries');
            const { container } = renderPage('federated-api-1', client);

            const pictureInput = container.querySelectorAll('input[type="file"]')[0] as HTMLInputElement;
            await act(async () => {
                fireEvent.change(pictureInput, { target: { files: [new File(['png'], 'picture.png', { type: 'image/png' })] } });
            });

            await waitFor(() =>
                expect(pictureSpy).toHaveBeenCalledWith('DEFAULT', 'federated-api-1', expect.stringMatching(/^data:image\/png;base64,/)),
            );
            await waitFor(() => expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['api-detail', 'DEFAULT', 'federated-api-1'] }));
            expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
            expect(screen.queryByRole('button', { name: /discard/i })).toBeNull();
        });

        // Both pickers sit in the same row and differ only by props, so a mis-wired onSelect would still
        // fire "some" image mutation — the Picture spy is what pins the Background picker to its own call.
        it('persists a selected Background immediately and evicts the API detail query, leaving the Picture request unsent', async () => {
            const backgroundSpy = jest.spyOn(apiServices, 'updateApiBackground').mockResolvedValue(undefined);
            const pictureSpy = jest.spyOn(apiServices, 'updateApiPicture').mockResolvedValue(undefined);
            const client = makeClient();
            const invalidateQueries = jest.spyOn(client, 'invalidateQueries');
            const { container } = renderPage('federated-api-1', client);

            const backgroundInput = container.querySelectorAll('input[type="file"]')[1] as HTMLInputElement;
            await act(async () => {
                fireEvent.change(backgroundInput, { target: { files: [new File(['png'], 'background.png', { type: 'image/png' })] } });
            });

            await waitFor(() =>
                expect(backgroundSpy).toHaveBeenCalledWith('DEFAULT', 'federated-api-1', expect.stringMatching(/^data:image\/png;base64,/)),
            );
            await waitFor(() => expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['api-detail', 'DEFAULT', 'federated-api-1'] }));
            expect(pictureSpy).not.toHaveBeenCalled();
            expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
            expect(screen.queryByRole('button', { name: /discard/i })).toBeNull();
        });
    });
});

describe('ApiGeneralPage — API review', () => {
    function withApi(overrides: Record<string, unknown>) {
        mockUseApiDetailContext.mockReturnValue({ api: { ...STUB_API, ...overrides }, isLoading: false, permissionsReady: true });
    }

    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' });
        mockUseHasPermission.mockReturnValue(true);
        mockUseApiReviewEnabled.mockReturnValue({ enabled: true, isFetched: true });
        jest.spyOn(apiServices, 'getPromotionTargets').mockResolvedValue([]);
        jest.spyOn(apiServices, 'getPendingPromotions').mockResolvedValue([]);
    });

    afterEach(() => {
        jest.clearAllMocks();
        jest.restoreAllMocks();
    });

    it('offers no review action while review is disabled', () => {
        mockUseApiReviewEnabled.mockReturnValue({ enabled: false, isFetched: true });
        withApi({ workflowState: 'DRAFT' });
        renderPage();
        expect(screen.queryByRole('button', { name: /ask for a review/i })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: /start api/i })).toBeInTheDocument();
    });

    it('replaces Start with Ask for a review on a draft and asks after confirmation', () => {
        withApi({ workflowState: 'DRAFT' });
        renderPage();

        expect(screen.queryByRole('button', { name: /start api/i })).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: /ask for a review/i }));
        expect(screen.getByRole('dialog')).toHaveTextContent('Are you sure you want to ask for a review of the API?');
        fireEvent.click(screen.getByRole('button', { name: 'Ask for review' }));
        expect(mockAskReviewMutate).toHaveBeenCalledWith(undefined, expect.objectContaining({ onSuccess: expect.any(Function) }));

        const options = mockAskReviewMutate.mock.calls[0]?.[1] as { onSuccess: () => void };
        act(() => options.onSuccess());
        expect(toast.success).toHaveBeenCalledWith('Review has been asked.', expect.anything());
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('offers neither Start nor Ask while the review is in progress', () => {
        withApi({ workflowState: 'IN_REVIEW' });
        renderPage();
        expect(screen.queryByRole('button', { name: /start api/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /ask for a review/i })).not.toBeInTheDocument();
    });

    it('lets the author ask again after changes were requested', () => {
        withApi({ workflowState: 'REQUEST_FOR_CHANGES' });
        renderPage();
        expect(screen.getByRole('button', { name: /ask for a review/i })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /start api/i })).not.toBeInTheDocument();
    });

    it('restores Start once the review is accepted', () => {
        withApi({ workflowState: 'REVIEW_OK' });
        renderPage();
        expect(screen.getByRole('button', { name: /start api/i })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /ask for a review/i })).not.toBeInTheDocument();
    });

    it('keeps Start for an API created before review was enabled, and still lets it be reviewed', () => {
        withApi({ workflowState: undefined });
        renderPage();
        expect(screen.getByRole('button', { name: /start api/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /ask for a review/i })).toBeInTheDocument();
    });

    it('hides Ask for a review without api-definition-u', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('api-definition-u'));
        withApi({ workflowState: 'DRAFT' });
        renderPage();
        expect(screen.queryByRole('button', { name: /ask for a review/i })).not.toBeInTheDocument();
    });
});
