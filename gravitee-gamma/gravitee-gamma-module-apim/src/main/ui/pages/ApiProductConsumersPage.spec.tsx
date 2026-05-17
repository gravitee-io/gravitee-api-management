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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiProductConsumersPage } from './ApiProductConsumersPage';
import { useApiProductResourcePermissions } from '../features/api-products/hooks/useApiProductPermissions';
import { useCreateSubscription } from '../features/apis/hooks/useSubscriptionActions';
import { useApiPlans, useSubscriptionList } from '../features/apis/hooks/useSubscriptions';
import type { SubscriptionPage } from '../features/apis/types/subscription';

// ─── SDK / icon mocks ─────────────────────────────────────────────────────────

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// ─── Hook mocks ───────────────────────────────────────────────────────────────

jest.mock('../features/api-products/hooks/useApiProductPermissions', () => ({
    useApiProductResourcePermissions: jest.fn(),
}));

jest.mock('../features/apis/hooks/useSubscriptionActions', () => ({
    useCreateSubscription: jest.fn(),
}));

jest.mock('../features/apis/hooks/useSubscriptions', () => ({
    useSubscriptionList: jest.fn(),
    useApiPlans: jest.fn(),
    useSubscriptionCount: jest.fn(() => ({ data: 0, isLoading: false })),
    useApplicationSearch: jest.fn(() => ({ data: [], isLoading: false })),
    isSubscriptionFiltersDirty: jest.fn(() => false),
}));

// ─── Graphene UI mock ─────────────────────────────────────────────────────────

jest.mock('@gravitee/graphene-core', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires, @typescript-eslint/no-require-imports
    const React = require('react');

    return {
        Badge: ({ children }: { children: ReactNode }) => <span>{children}</span>,
        Button: ({
            children,
            disabled,
            onClick,
            type,
        }: {
            children: ReactNode;
            disabled?: boolean;
            onClick?: () => void;
            type?: React.ButtonHTMLAttributes<HTMLButtonElement>['type'];
        }) => (
            <button type={type ?? 'button'} disabled={disabled} onClick={onClick}>
                {children}
            </button>
        ),
        Card: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        CardContent: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        CardHeader: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        CardTitle: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        DataTablePagination: () => null,
        Dialog: ({ open, children }: { open?: boolean; children: ReactNode }) => (open ? <div role="dialog">{children}</div> : null),
        DialogContent: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        DialogFooter: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        DialogHeader: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        DialogTitle: ({ children }: { children: ReactNode }) => <h2>{children}</h2>,
        Input: ({
            id,
            value,
            onChange,
            placeholder,
            type,
        }: {
            id?: string;
            value?: string;
            onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
            placeholder?: string;
            type?: string;
        }) => <input id={id} value={value} onChange={onChange} placeholder={placeholder} type={type ?? 'text'} />,
        Label: ({ children, htmlFor }: { children: ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
        Select: ({ value, onValueChange, children }: { value?: string; onValueChange?: (v: string) => void; children: ReactNode }) => (
            <select value={value} onChange={e => onValueChange?.(e.target.value)}>
                {children}
            </select>
        ),
        SelectContent: ({ children }: { children: ReactNode }) => <>{children}</>,
        SelectItem: ({ value, children }: { value: string; children: ReactNode }) => <option value={value}>{children}</option>,
        SelectTrigger: ({ children }: { children: ReactNode }) => <>{children}</>,
        SelectValue: () => null,
        Separator: () => <hr />,
        Skeleton: () => <div aria-busy="true" />,
        Table: ({ children }: { children: ReactNode }) => <table>{children}</table>,
        TableBody: ({ children }: { children: ReactNode }) => <tbody>{children}</tbody>,
        TableCell: ({ children }: { children: ReactNode }) => <td>{children}</td>,
        TableHead: ({ children }: { children: ReactNode }) => <th>{children}</th>,
        TableHeader: ({ children }: { children: ReactNode }) => <thead>{children}</thead>,
        TableRow: ({ children, onClick }: { children: ReactNode; onClick?: () => void }) => <tr onClick={onClick}>{children}</tr>,
    };
});

// ─── Test data ────────────────────────────────────────────────────────────────

const EMPTY_PAGE: SubscriptionPage = {
    data: [],
    pagination: { totalCount: 0, pageIndex: 1, pageSize: 10 },
};

const SUBSCRIPTION_PAGE: SubscriptionPage = {
    data: [
        {
            id: 'sub-1',
            status: 'ACCEPTED',
            plan: { id: 'plan-1', name: 'Default Plan', security: { type: 'API_KEY' } },
            application: { id: 'app-1', name: 'My App', primaryOwner: { displayName: 'Alice' } },
            createdAt: '2024-01-01T00:00:00Z',
        },
    ],
    pagination: { totalCount: 1, pageIndex: 1, pageSize: 10 },
};

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockUseApiProductResourcePermissions = useApiProductResourcePermissions as jest.Mock;
const mockUseSubscriptionList = useSubscriptionList as jest.Mock;
const mockUseApiPlans = useApiPlans as jest.Mock;
const mockUseCreateSubscription = useCreateSubscription as jest.Mock;

function setupDefaults() {
    mockUseApiProductResourcePermissions.mockReturnValue({
        canRead: true,
        canCreate: true,
        canUpdate: true,
        canDelete: true,
        isLoading: false,
    });
    mockUseSubscriptionList.mockReturnValue({ data: EMPTY_PAGE, isLoading: false });
    mockUseApiPlans.mockReturnValue({ data: [], isLoading: false });
    mockUseCreateSubscription.mockReturnValue({ mutate: jest.fn(), isPending: false, error: null, reset: jest.fn() });
}

function renderPage() {
    render(
        <MemoryRouter initialEntries={['/api-products/product-1/consumers']}>
            <Routes>
                <Route path="api-products/:productId/consumers" element={<ApiProductConsumersPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

// ─── Tests ────────────────────────────────────────────────────────────────────

beforeEach(() => {
    jest.clearAllMocks();
});

// ─── 1. Permission loading skeleton ──────────────────────────────────────────

it('renders skeleton while permissions are loading', () => {
    mockUseApiProductResourcePermissions.mockReturnValue({
        canRead: false,
        canCreate: false,
        canUpdate: false,
        canDelete: false,
        isLoading: true,
    });
    mockUseSubscriptionList.mockReturnValue({ data: EMPTY_PAGE, isLoading: false });
    mockUseApiPlans.mockReturnValue({ data: [], isLoading: false });
    mockUseCreateSubscription.mockReturnValue({ mutate: jest.fn(), isPending: false, error: null, reset: jest.fn() });
    renderPage();
    expect(screen.getAllByRole('generic').some(el => el.getAttribute('aria-busy') === 'true')).toBe(true);
});

// ─── 2. Permission gating ─────────────────────────────────────────────────────

it('shows permission denied message when user lacks canRead', () => {
    setupDefaults();
    mockUseApiProductResourcePermissions.mockReturnValue({
        canRead: false,
        canCreate: false,
        canUpdate: false,
        canDelete: false,
        isLoading: false,
    });
    renderPage();
    expect(screen.getByText(/you don't have permission to view subscriptions/i)).not.toBeNull();
});

it('hides Create subscription button when user lacks canCreate', () => {
    setupDefaults();
    mockUseApiProductResourcePermissions.mockReturnValue({
        canRead: true,
        canCreate: false,
        canUpdate: false,
        canDelete: false,
        isLoading: false,
    });
    renderPage();
    expect(screen.queryByRole('button', { name: /create subscription/i })).toBeNull();
});

it('shows Create subscription button when user has canCreate', () => {
    setupDefaults();
    renderPage();
    expect(screen.getByRole('button', { name: /create subscription/i })).not.toBeNull();
});

// ─── 3. Empty state ──────────────────────────────────────────────────────────

it('renders empty state when there are no subscriptions', () => {
    setupDefaults();
    renderPage();
    expect(screen.queryByRole('table')).toBeNull();
});

// ─── 4. Subscription list ─────────────────────────────────────────────────────

it('renders subscriptions table when data is present', () => {
    setupDefaults();
    mockUseSubscriptionList.mockReturnValue({ data: SUBSCRIPTION_PAGE, isLoading: false });
    renderPage();
    expect(screen.getByText('My App')).not.toBeNull();
    expect(screen.getByText('Default Plan')).not.toBeNull();
});

// ─── 5. Loading state ─────────────────────────────────────────────────────────

it('renders skeletons while loading subscription list', () => {
    setupDefaults();
    mockUseSubscriptionList.mockReturnValue({ data: undefined, isLoading: true });
    renderPage();
    expect(screen.getAllByRole('cell').some(el => el.querySelector('[aria-busy="true"]') !== null)).toBe(true);
});

// ─── 6. Context isolation — ctx.type is api-product ──────────────────────────

it('calls useSubscriptionList with api-product context for the resolved productId', () => {
    setupDefaults();
    renderPage();
    const [ctx] = mockUseSubscriptionList.mock.calls[0];
    expect(ctx).toEqual({ type: 'api-product', entityId: 'product-1' });
});

it('calls useApiPlans with api-product context', () => {
    setupDefaults();
    renderPage();
    const [ctx] = mockUseApiPlans.mock.calls[0];
    expect(ctx).toEqual({ type: 'api-product', entityId: 'product-1' });
});

it('calls useCreateSubscription with api-product context', () => {
    setupDefaults();
    renderPage();
    const [ctx] = mockUseCreateSubscription.mock.calls[0];
    expect(ctx).toEqual({ type: 'api-product', entityId: 'product-1' });
});

it('calls useApiProductResourcePermissions with productId and subscription resource', () => {
    setupDefaults();
    renderPage();
    expect(mockUseApiProductResourcePermissions).toHaveBeenCalledWith('product-1', 'subscription');
});

// ─── 7. Create dialog opens ───────────────────────────────────────────────────

it('opens the create subscription dialog when the button is clicked', async () => {
    setupDefaults();
    const user = userEvent.setup();
    renderPage();
    await user.click(screen.getByRole('button', { name: /create subscription/i }));
    await waitFor(() => expect(screen.getByRole('dialog')).not.toBeNull());
});
