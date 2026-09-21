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
import { render, screen } from '@testing-library/react';
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
    Switch: ({ checked, disabled }: { checked?: boolean; disabled?: boolean }) => (
        <input type="checkbox" checked={checked} disabled={disabled} readOnly />
    ),
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
import type { PlanContext } from '../../../types/plan';

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
