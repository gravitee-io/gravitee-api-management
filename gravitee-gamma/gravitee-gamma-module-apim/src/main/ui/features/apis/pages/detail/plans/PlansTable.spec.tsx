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
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { PlansTable } from './PlansTable';
import { notify } from '../../../../../shared/notify';
import type { ManagedPlan, PlanContext, PlanStatus } from '../../../types/plan';

const mockReorderMutate = jest.fn();

jest.mock('../../../hooks/usePlans', () => ({
    usePlanTransition: () => ({ mutate: jest.fn(), isPending: false }),
    useReorderPlan: () => ({ mutate: mockReorderMutate, isPending: false }),
}));
jest.mock('../../../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));

const CTX: PlanContext = { type: 'api', entityId: 'api-1', apiType: 'PROXY' } as PlanContext;
const ROW_HEIGHT = 48;

function plan(id: string, name: string, order: number, status: PlanStatus = 'PUBLISHED'): ManagedPlan {
    return { id, name, order, status, security: { type: 'KEY_LESS' }, validation: 'AUTO' } as ManagedPlan;
}

const gold = plan('gold', 'Gold', 1);
const silver = plan('silver', 'Silver', 2);
const bronze = plan('bronze', 'Bronze', 3);

function renderTable(overrides: Partial<Parameters<typeof PlansTable>[0]> = {}) {
    return render(
        <MemoryRouter>
            <PlansTable
                ctx={CTX}
                status="PUBLISHED"
                plans={[gold, silver, bronze]}
                totalCount={3}
                page={1}
                perPage={10}
                isLoading={false}
                canUpdate
                onPage={jest.fn()}
                onPerPage={jest.fn()}
                {...overrides}
            />
        </MemoryRouter>,
    );
}

async function moveDown(user: ReturnType<typeof userEvent.setup>, planName: string, rows: number) {
    screen.getByRole('button', { name: `Reorder ${planName}` }).focus();
    await user.keyboard('[Space]');
    for (let i = 0; i < rows; i++) {
        await user.keyboard('[ArrowDown]');
    }
    await user.keyboard('[Space]');
}

describe('PlansTable reordering', () => {
    beforeEach(() => {
        mockReorderMutate.mockReset();
        jest.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
            const row = this.closest('tbody tr');
            const index = row?.parentElement ? Array.from(row.parentElement.children).indexOf(row) : 0;
            const top = index * ROW_HEIGHT;
            return { x: 0, y: top, top, left: 0, right: 600, bottom: top + ROW_HEIGHT, width: 600, height: ROW_HEIGHT, toJSON: () => ({}) };
        });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('offers a drag handle per published plan instead of up and down arrows', () => {
        renderTable();

        expect(screen.getByRole('button', { name: 'Reorder Gold' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Move up' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Move down' })).not.toBeInTheDocument();
    });

    it.each<PlanStatus>(['STAGING', 'DEPRECATED', 'CLOSED'])(
        'does not offer reordering on the %s list, where order has no meaning',
        status => {
            renderTable({ status, plans: [plan('a', 'Alpha', 0, status), plan('b', 'Beta', 0, status)] });

            expect(screen.queryByRole('button', { name: /^Reorder / })).not.toBeInTheDocument();
            expect(within(screen.getByRole('table')).getByText('Alpha')).toBeInTheDocument();
        },
    );

    it('does not offer reordering to a user without the plan update permission', () => {
        renderTable({ canUpdate: false });

        expect(screen.queryByRole('button', { name: /^Reorder / })).not.toBeInTheDocument();
    });

    it('moves the plan to its position among all published plans, not within the visible page', async () => {
        const user = userEvent.setup();
        renderTable({ page: 2, perPage: 10, totalCount: 13 });

        await moveDown(user, 'Gold', 1);

        expect(mockReorderMutate).toHaveBeenCalledWith(
            { planId: 'gold', fullPlan: gold, newOrder: 12 },
            expect.objectContaining({ onError: expect.any(Function) }),
        );
    });

    it('reports a refused move and puts the plans back', async () => {
        const user = userEvent.setup();
        mockReorderMutate.mockImplementation((_variables, options) => options.onError(new Error('boom')));
        renderTable();

        await moveDown(user, 'Gold', 1);

        expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Failed to reorder plan.');
        const names = within(screen.getByRole('table'))
            .getAllByRole('row')
            .slice(1)
            .map(row => within(row).getAllByRole('cell')[1].textContent);
        expect(names).toEqual(['Gold', 'Silver', 'Bronze']);
    });
});
