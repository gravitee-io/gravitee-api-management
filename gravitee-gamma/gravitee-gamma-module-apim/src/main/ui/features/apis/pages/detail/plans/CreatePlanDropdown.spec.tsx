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
import { MemoryRouter } from 'react-router-dom';

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
}));
jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

import { CreatePlanDropdown } from './CreatePlanDropdown';
import type { PlanContext } from '../../../types/plan';

const CTX: PlanContext = { type: 'api', entityId: 'api-1' };

function renderDropdown(restrictToKeyless?: boolean) {
    return render(
        <MemoryRouter>
            <CreatePlanDropdown ctx={CTX} restrictToKeyless={restrictToKeyless} />
        </MemoryRouter>,
    );
}

describe('CreatePlanDropdown', () => {
    it('lists every plan security type for a regular (non-TCP) API', () => {
        renderDropdown(false);

        expect(screen.getByRole('menuitem', { name: /api key/i })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: /oauth2/i })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: /keyless/i })).toBeInTheDocument();
    });

    it('restricts the menu to Keyless only when restrictToKeyless is set (TCP Proxy parity)', () => {
        renderDropdown(true);

        expect(screen.getAllByRole('menuitem')).toHaveLength(1);
        expect(screen.getByRole('menuitem', { name: /keyless/i })).toBeInTheDocument();
    });
});
