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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core', () => ({
    Button: ({ children, ...props }: { children?: ReactNode }) => <button {...props}>{children}</button>,
    DropdownMenu: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DropdownMenuTrigger: ({ children }: { children?: ReactNode }) => <>{children}</>,
    DropdownMenuContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DropdownMenuItem: ({ children, onClick }: { children?: ReactNode; onClick?: () => void }) => (
        <button role="menuitem" onClick={onClick}>
            {children}
        </button>
    ),
    Dialog: ({ children, open }: { children?: ReactNode; open?: boolean }) => (open ? <div role="dialog">{children}</div> : null),
    DialogClose: ({ children }: { children?: ReactNode }) => <>{children}</>,
    DialogContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    DialogFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
    Label: ({ children, htmlFor }: { children?: ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
    Skeleton: () => <div data-testid="skeleton" />,
    Switch: ({
        id,
        checked,
        disabled,
        onCheckedChange,
    }: {
        id?: string;
        checked?: boolean;
        disabled?: boolean;
        onCheckedChange?: (checked: boolean) => void;
    }) => <input type="checkbox" id={id} checked={checked} disabled={disabled} onChange={e => onCheckedChange?.(e.target.checked)} />,
}));

jest.mock('../../../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));

jest.mock('../../../services/apis', () => ({
    ...jest.requireActual<object>('../../../services/apis'),
    updateAllowMultiJwtOauth2Subscriptions: jest.fn(),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('./PlansListPage', () => ({
    PlansListPage: () => <div data-testid="plans-list" />,
}));

jest.mock('./PlansLearningPage', () => ({
    PlansLearningPage: () => <div data-testid="plans-learning" />,
}));

jest.mock('../../../hooks/useApiDetail', () => ({
    useApiDetail: jest.fn(() => ({ data: undefined })),
}));

jest.mock('../../../hooks/usePlans', () => ({
    usePlanStatusCounts: jest.fn(() => ({ staging: 0, published: 1, deprecated: 0, closed: 0, total: 1, isLoading: false })),
}));

import { PlansPage } from './PlansPage';
import { notify } from '../../../../../shared/notify';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateAllowMultiJwtOauth2Subscriptions } from '../../../services/apis';
import type { PlanContext } from '../../../types/plan';

const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUpdateAllowMultiJwtOauth2Subscriptions = updateAllowMultiJwtOauth2Subscriptions as jest.Mock;
const mockNotifySuccess = notify.success as jest.Mock;
const mockNotifyError = notify.error as jest.Mock;

const API_CTX: PlanContext = { type: 'api', entityId: 'api-1' };
const API_PRODUCT_CTX: PlanContext = { type: 'api-product', entityId: 'product-1' };

function renderPlansPage(ctx: PlanContext, overrides: { canCreate?: boolean; canRead?: boolean } = {}) {
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
            <MemoryRouter>
                <PlansPage ctx={ctx} canRead={overrides.canRead ?? true} canCreate={overrides.canCreate ?? true} canUpdate canDelete />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('PlansPage plan creation control', () => {
    it('keeps offering plan creation to a caller that states no API type, as the API Products plans page does', () => {
        renderPlansPage(API_PRODUCT_CTX);

        expect(screen.getByRole('button', { name: /create plan/i })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: /api key/i })).toBeInTheDocument();
    });

    it('still withholds plan creation from a user without the plan creation permission', () => {
        renderPlansPage(API_CTX, { canCreate: false });

        expect(screen.queryByRole('button', { name: /create plan/i })).toBeNull();
        expect(screen.queryAllByRole('menuitem')).toHaveLength(0);
    });
});

describe('PlansPage without the plan read permission', () => {
    it('explains the missing permission instead of rendering any plan content', () => {
        renderPlansPage(API_CTX, { canRead: false });

        expect(screen.getByText(/don't have permission to view plans/i)).toBeInTheDocument();
        expect(screen.queryByTestId('plans-list')).toBeNull();
        expect(screen.queryByRole('button', { name: /create plan/i })).toBeNull();
    });
});

describe('PlansPage allow multi JWT/OAuth2 subscriptions toggle', () => {
    function givenMultiSubscriptionsAllowed(allowed: boolean) {
        mockUseApiDetail.mockReturnValue({ data: { id: 'api-1', allowMultiJwtOauth2Subscriptions: allowed } });
    }

    function multiSubscriptionsSwitch() {
        return screen.getByRole('checkbox', { name: /allow multi jwt\/oauth2 subscriptions per application/i });
    }

    beforeEach(() => {
        jest.clearAllMocks();
    });

    afterEach(() => {
        mockUseApiDetail.mockReturnValue({ data: undefined });
    });

    it('asks for confirmation before turning the setting on, then saves it as allowed', async () => {
        givenMultiSubscriptionsAllowed(false);
        mockUpdateAllowMultiJwtOauth2Subscriptions.mockResolvedValue({ id: 'api-1', allowMultiJwtOauth2Subscriptions: true });
        renderPlansPage(API_CTX);

        await userEvent.click(multiSubscriptionsSwitch());

        const dialog = screen.getByRole('dialog');
        expect(mockUpdateAllowMultiJwtOauth2Subscriptions).not.toHaveBeenCalled();

        await userEvent.click(within(dialog).getByRole('button', { name: 'Enable' }));

        await waitFor(() => expect(mockNotifySuccess).toHaveBeenCalledTimes(1));
        expect(mockUpdateAllowMultiJwtOauth2Subscriptions).toHaveBeenCalledTimes(1);
        expect(mockUpdateAllowMultiJwtOauth2Subscriptions).toHaveBeenCalledWith('DEFAULT', 'api-1', true);
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it('turns the setting off straight away without asking for confirmation', async () => {
        givenMultiSubscriptionsAllowed(true);
        mockUpdateAllowMultiJwtOauth2Subscriptions.mockResolvedValue({ id: 'api-1', allowMultiJwtOauth2Subscriptions: false });
        renderPlansPage(API_CTX);

        await userEvent.click(multiSubscriptionsSwitch());

        await waitFor(() => expect(mockNotifySuccess).toHaveBeenCalledTimes(1));
        expect(mockUpdateAllowMultiJwtOauth2Subscriptions).toHaveBeenCalledTimes(1);
        expect(mockUpdateAllowMultiJwtOauth2Subscriptions).toHaveBeenCalledWith('DEFAULT', 'api-1', false);
        expect(screen.queryByRole('dialog')).toBeNull();
    });

    it('reports the failure instead of a success when saving the setting fails', async () => {
        const failure = new Error('update rejected');
        givenMultiSubscriptionsAllowed(true);
        mockUpdateAllowMultiJwtOauth2Subscriptions.mockRejectedValue(failure);
        renderPlansPage(API_CTX);

        await userEvent.click(multiSubscriptionsSwitch());

        await waitFor(() => expect(mockNotifyError).toHaveBeenCalledTimes(1));
        expect(mockNotifyError.mock.calls[0][0]).toBe(failure);
        expect(mockNotifySuccess).not.toHaveBeenCalled();
    });
});
