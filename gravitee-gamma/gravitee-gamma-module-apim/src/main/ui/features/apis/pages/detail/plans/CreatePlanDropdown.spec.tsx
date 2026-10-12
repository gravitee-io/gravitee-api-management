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
import { render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

jest.mock('@gravitee/graphene-core', () => ({
    Button: ({ children, disabled, ...props }: { children?: ReactNode; disabled?: boolean }) => (
        <button disabled={disabled} {...props}>
            {children}
        </button>
    ),
    DropdownMenu: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DropdownMenuTrigger: ({ children }: { children?: ReactNode }) => <>{children}</>,
    DropdownMenuContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DropdownMenuItem: ({ children, onClick }: { children?: ReactNode; onClick?: () => void }) => (
        <button role="menuitem" onClick={onClick}>
            {children}
        </button>
    ),
}));
jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../hooks/usePlanSecuritySettings', () => ({
    usePlanSecuritySettings: jest.fn(() => ({
        planSecuritySettings: { API_KEY: true, JWT: true, OAUTH2: true, MTLS: true, KEY_LESS: true },
        isLoading: false,
    })),
}));

import { CreatePlanDropdown } from './CreatePlanDropdown';
import { usePlanSecuritySettings } from '../../../hooks/usePlanSecuritySettings';
import type { PlanContext } from '../../../types/plan';

const CTX: PlanContext = { type: 'api', entityId: 'api-1' };
const mockUsePlanSecuritySettings = usePlanSecuritySettings as jest.Mock;

const ALL_ENABLED = { API_KEY: true, JWT: true, OAUTH2: true, MTLS: true, KEY_LESS: true };

function renderDropdown(restrictToKeyless?: boolean) {
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <MemoryRouter>
                <CreatePlanDropdown ctx={CTX} restrictToKeyless={restrictToKeyless} />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('CreatePlanDropdown', () => {
    beforeEach(() => {
        mockUsePlanSecuritySettings.mockReturnValue({ planSecuritySettings: ALL_ENABLED, isLoading: false });
    });

    it('lists every plan security type for a regular (non-TCP) API', () => {
        renderDropdown(false);

        expect(screen.getByRole('menuitem', { name: /api key/i })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: /oauth2/i })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: /keyless/i })).toBeInTheDocument();
    });

    it('restricts the menu to Keyless only when restrictToKeyless is set', () => {
        renderDropdown(true);

        expect(screen.getAllByRole('menuitem')).toHaveLength(1);
        expect(screen.getByRole('menuitem', { name: /keyless/i })).toBeInTheDocument();
    });

    it('hides Keyless when restrictToKeyless is set but Keyless is disabled in portal settings', () => {
        mockUsePlanSecuritySettings.mockReturnValue({
            planSecuritySettings: { ...ALL_ENABLED, KEY_LESS: false },
            isLoading: false,
        });
        renderDropdown(true);

        expect(screen.queryByRole('menuitem', { name: /keyless/i })).toBeNull();
        expect(screen.queryAllByRole('menuitem')).toHaveLength(0);
    });

    it('hides a plan type when its security setting is disabled', async () => {
        mockUsePlanSecuritySettings.mockReturnValue({
            planSecuritySettings: { ...ALL_ENABLED, API_KEY: false },
            isLoading: false,
        });
        renderDropdown(false);

        await waitFor(() => expect(screen.queryByRole('menuitem', { name: /api key/i })).toBeNull());
        expect(screen.getByRole('menuitem', { name: /oauth2/i })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: /keyless/i })).toBeInTheDocument();
    });

    it('disables Create plan while portal security settings are loading', () => {
        mockUsePlanSecuritySettings.mockReturnValue({ planSecuritySettings: ALL_ENABLED, isLoading: true });
        renderDropdown(false);

        expect(screen.getByRole('button', { name: /create plan/i })).toBeDisabled();
    });

    it('disables Create plan when every security type is disabled in portal settings', () => {
        mockUsePlanSecuritySettings.mockReturnValue({
            planSecuritySettings: { API_KEY: false, JWT: false, OAUTH2: false, MTLS: false, KEY_LESS: false },
            isLoading: false,
        });
        renderDropdown(false);

        expect(screen.getByRole('button', { name: /create plan/i })).toBeDisabled();
        expect(screen.queryAllByRole('menuitem')).toHaveLength(0);
    });
});
