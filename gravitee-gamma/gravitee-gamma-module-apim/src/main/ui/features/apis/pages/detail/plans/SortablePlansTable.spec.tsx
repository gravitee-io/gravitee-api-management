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
import type { ColumnDef } from '@tanstack/react-table';
import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { SortablePlansTable } from './SortablePlansTable';

interface TestPlan {
    id: string;
    name: string;
}

const columns: ColumnDef<TestPlan, unknown>[] = [{ id: 'name', header: 'Name', cell: ({ row }) => row.original.name }];

const plans: TestPlan[] = [
    { id: 'gold', name: 'Gold' },
    { id: 'silver', name: 'Silver' },
    { id: 'bronze', name: 'Bronze' },
];

const ROW_HEIGHT = 48;

type Props = Parameters<typeof SortablePlansTable<TestPlan>>[0];

function tableProps(overrides: Partial<Props> = {}): Props {
    return {
        columns,
        plans,
        totalCount: plans.length,
        page: 1,
        perPage: 10,
        emptyMessage: 'No published plans',
        onPage: jest.fn(),
        onPerPage: jest.fn(),
        disabled: false,
        onReorder: jest.fn(),
        ...overrides,
    };
}

function renderTable(overrides: Partial<Props> = {}) {
    const props = tableProps(overrides);
    const view = render(<SortablePlansTable<TestPlan> {...props} />);
    return { onReorder: props.onReorder as jest.Mock, ...view };
}

function rowNames() {
    const [, body] = screen.getAllByRole('rowgroup');
    return within(body)
        .getAllByRole('row')
        .map(row => within(row).getAllByRole('cell')[1].textContent);
}

/** Space picks the row up, each arrow moves it one row down, Space drops it: dnd-kit's keyboard path. */
async function moveDown(user: ReturnType<typeof userEvent.setup>, planName: string, rows: number) {
    screen.getByRole('button', { name: `Reorder ${planName}` }).focus();
    await user.keyboard('[Space]');
    for (let i = 0; i < rows; i++) {
        await user.keyboard('[ArrowDown]');
    }
    await user.keyboard('[Space]');
}

describe('SortablePlansTable', () => {
    // jsdom lays nothing out, so dnd-kit's keyboard sensor finds no row below the dragged one.
    // Stacking the body rows by index gives it the geometry a browser would.
    beforeEach(() => {
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

    it('gives each plan a labelled drag handle, in the order it was given', () => {
        renderTable();

        expect(rowNames()).toEqual(['Gold', 'Silver', 'Bronze']);
        for (const name of ['Gold', 'Silver', 'Bronze']) {
            expect(screen.getByRole('button', { name: `Reorder ${name}` })).toBeEnabled();
        }
    });

    it('moves a plan by keyboard and asks for its new 1-based position', async () => {
        const user = userEvent.setup();
        const { onReorder } = renderTable();

        await moveDown(user, 'Gold', 2);

        expect(onReorder).toHaveBeenCalledTimes(1);
        expect(onReorder).toHaveBeenCalledWith(plans[0], 3, expect.objectContaining({ onError: expect.any(Function) }));
        expect(rowNames()).toEqual(['Silver', 'Bronze', 'Gold']);
    });

    it('counts the rows on earlier pages into the order, which is a position among every published plan', async () => {
        const user = userEvent.setup();
        const { onReorder } = renderTable({ page: 2, perPage: 10, totalCount: 13 });

        await moveDown(user, 'Gold', 1);

        expect(onReorder).toHaveBeenCalledWith(plans[0], 12, expect.anything());
    });

    it('puts the plans back where the server has them when the move is refused', async () => {
        const user = userEvent.setup();
        const { onReorder } = renderTable();

        await moveDown(user, 'Gold', 1);
        expect(rowNames()).toEqual(['Silver', 'Gold', 'Bronze']);

        const [, , { onError }] = onReorder.mock.calls[0] as [TestPlan, number, { onError: () => void }];
        act(() => onError());

        expect(rowNames()).toEqual(['Gold', 'Silver', 'Bronze']);
    });

    it('shows the server order once the refreshed plans arrive', async () => {
        const user = userEvent.setup();
        const { rerender } = renderTable();

        await moveDown(user, 'Gold', 1);

        rerender(<SortablePlansTable<TestPlan> {...tableProps({ plans: [plans[2], plans[0], plans[1]] })} />);

        expect(rowNames()).toEqual(['Bronze', 'Gold', 'Silver']);
    });

    it('locks every handle while a move is being written', async () => {
        const user = userEvent.setup();
        const { onReorder } = renderTable({ disabled: true });

        for (const name of ['Gold', 'Silver', 'Bronze']) {
            expect(screen.getByRole('button', { name: `Reorder ${name}` })).toBeDisabled();
        }
        await moveDown(user, 'Gold', 1);

        expect(onReorder).not.toHaveBeenCalled();
    });

    it('does not reorder a plan dropped where it started', async () => {
        const user = userEvent.setup();
        const { onReorder } = renderTable();

        await moveDown(user, 'Gold', 0);

        expect(onReorder).not.toHaveBeenCalled();
    });

    it('shows the empty message when there are no plans', () => {
        renderTable({ plans: [], totalCount: 0 });

        expect(screen.getByText('No published plans')).toBeInTheDocument();
    });
});
