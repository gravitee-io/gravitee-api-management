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
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiProductConsumerDetailPage } from './ApiProductConsumerDetailPage';
import { useSubscriptionDetail } from '../../../../apis/hooks/useSubscriptions';
import type { Subscription } from '../../../../apis/types/subscription';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';

// ─── SDK / icon mocks ─────────────────────────────────────────────────────────

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// ─── Hook mocks ───────────────────────────────────────────────────────────────

jest.mock('../../../hooks/useApiProductPermissions', () => ({
    useApiProductResourcePermissions: jest.fn(),
}));

jest.mock('../../../../apis/hooks/useSubscriptions', () => ({
    useSubscriptionDetail: jest.fn(),
    useApiPlans: jest.fn(() => ({ data: [], isLoading: false })),
}));

jest.mock('./api-consumers/subscription-detail/SubscriptionActionsBar', () => ({
    SubscriptionActionsBar: ({ canUpdate, canDelete }: { canUpdate: boolean; canDelete: boolean }) =>
        canUpdate || canDelete ? <div data-testid="actions-bar" /> : null,
}));

jest.mock('./api-consumers/subscription-detail/SubscriptionInfoCard', () => ({
    SubscriptionInfoCard: () => <div data-testid="info-card" />,
}));

jest.mock('./api-consumers/subscription-detail/SubscriptionApiKeysCard', () => ({
    SubscriptionApiKeysCard: () => <div data-testid="api-keys-card" />,
}));

// ─── Graphene UI mock ─────────────────────────────────────────────────────────

jest.mock('@gravitee/graphene-core', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires, @typescript-eslint/no-require-imports
    const React = require('react');

    return {
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
        Skeleton: () => <div aria-busy="true" />,
    };
});

// ─── Test data ────────────────────────────────────────────────────────────────

const SUBSCRIPTION: Subscription = {
    id: 'sub-1',
    status: 'ACCEPTED',
    plan: { id: 'plan-1', name: 'Default Plan', security: { type: 'API_KEY' } },
    application: { id: 'app-1', name: 'My App', primaryOwner: { displayName: 'Alice' } },
    createdAt: '2024-01-01T00:00:00Z',
};

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockUseApiProductResourcePermissions = useApiProductResourcePermissions as jest.Mock;
const mockUseSubscriptionDetail = useSubscriptionDetail as jest.Mock;

function setupDefaults() {
    mockUseApiProductResourcePermissions.mockReturnValue({
        canRead: true,
        canCreate: true,
        canUpdate: true,
        canDelete: true,
        isLoading: false,
    });
    mockUseSubscriptionDetail.mockReturnValue({ data: SUBSCRIPTION, isLoading: false, isError: false });
}

function renderPage() {
    render(
        <MemoryRouter initialEntries={['/api-products/product-1/consumers/sub-1']}>
            <Routes>
                <Route path="api-products/:productId/consumers/:subscriptionId" element={<ApiProductConsumerDetailPage />} />
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
    mockUseSubscriptionDetail.mockReturnValue({ data: undefined, isLoading: false, isError: false });
    renderPage();
    expect(screen.getAllByRole('generic').some(el => el.getAttribute('aria-busy') === 'true')).toBe(true);
});

// ─── 2. Subscription details ──────────────────────────────────────────────────

it('renders subscription application name when loaded', () => {
    setupDefaults();
    renderPage();
    expect(screen.getByText('My App')).not.toBeNull();
});

it('renders subscription plan name when loaded', () => {
    setupDefaults();
    renderPage();
    expect(screen.getByText(/Default Plan/i)).not.toBeNull();
});

it('renders back navigation button', () => {
    setupDefaults();
    renderPage();
    expect(screen.getByRole('button', { name: /go back to your subscriptions/i })).not.toBeNull();
});

// ─── 3. Loading state ─────────────────────────────────────────────────────────

it('renders skeleton while subscription is loading', () => {
    setupDefaults();
    mockUseSubscriptionDetail.mockReturnValue({ data: undefined, isLoading: true, isError: false });
    renderPage();
    expect(screen.getAllByRole('generic').some(el => el.getAttribute('aria-busy') === 'true')).toBe(true);
});

// ─── 4. Error state ───────────────────────────────────────────────────────────

it('renders error message when subscription fails to load', () => {
    setupDefaults();
    mockUseSubscriptionDetail.mockReturnValue({ data: undefined, isLoading: false, isError: true });
    renderPage();
    expect(screen.getByText(/failed to load subscription/i)).not.toBeNull();
});

// ─── 5. Context isolation — ctx.type is api-product ──────────────────────────

it('calls useSubscriptionDetail with api-product context and resolved subscriptionId', () => {
    setupDefaults();
    renderPage();
    const [ctx, subscriptionId] = mockUseSubscriptionDetail.mock.calls[0];
    expect(ctx).toEqual({ type: 'api-product', entityId: 'product-1' });
    expect(subscriptionId).toBe('sub-1');
});

it('calls useApiProductResourcePermissions with productId and subscription resource', () => {
    setupDefaults();
    renderPage();
    expect(mockUseApiProductResourcePermissions).toHaveBeenCalledWith('product-1', 'subscription');
});

// ─── 6. Actions bar visibility ────────────────────────────────────────────────

it('shows actions bar when canUpdate is true', () => {
    setupDefaults();
    renderPage();
    expect(screen.getByTestId('actions-bar')).not.toBeNull();
});

it('hides actions bar when canUpdate and canDelete are both false', () => {
    setupDefaults();
    mockUseApiProductResourcePermissions.mockReturnValue({
        canRead: true,
        canCreate: false,
        canUpdate: false,
        canDelete: false,
        isLoading: false,
    });
    renderPage();
    expect(screen.queryByTestId('actions-bar')).toBeNull();
});
