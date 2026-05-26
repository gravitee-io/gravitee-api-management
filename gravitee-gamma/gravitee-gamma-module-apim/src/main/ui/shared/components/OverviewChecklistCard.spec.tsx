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
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { OverviewChecklistCard, type OverviewChecklistItem } from './OverviewChecklistCard';

const TestIcon = () => null;

const BASE_ITEM: OverviewChecklistItem = {
    id: 'item-1',
    label: 'Add APIs',
    tooltip: 'Attach APIs to this product.',
    to: '../apis',
    icon: TestIcon,
    actionLabel: 'Open APIs',
    done: false,
};

function wrap(ui: React.ReactElement) {
    return render(<MemoryRouter>{ui}</MemoryRouter>);
}

describe('OverviewChecklistCard', () => {
    describe('count display', () => {
        it('shows completed/total based on done flags', () => {
            wrap(
                <OverviewChecklistCard
                    description="desc"
                    items={[
                        { ...BASE_ITEM, done: true },
                        { ...BASE_ITEM, id: 'item-2', done: false },
                    ]}
                />,
            );
            expect(screen.getByText('1/2')).toBeInTheDocument();
        });

        it('shows … while isReady is false', () => {
            wrap(<OverviewChecklistCard description="desc" items={[]} isReady={false} totalCountHint={6} />);
            expect(screen.getByText('…/6')).toBeInTheDocument();
        });
    });

    describe('collapse / expand', () => {
        it('hides item labels after collapsing', () => {
            wrap(<OverviewChecklistCard description="desc" items={[BASE_ITEM]} />);
            expect(screen.getByText('Add APIs')).toBeInTheDocument();
            fireEvent.click(screen.getByRole('button', { name: 'Collapse checklist' }));
            expect(screen.queryByText('Add APIs')).toBeNull();
        });

        it('shows item labels again after expanding', () => {
            wrap(<OverviewChecklistCard description="desc" items={[BASE_ITEM]} />);
            fireEvent.click(screen.getByRole('button', { name: 'Collapse checklist' }));
            fireEvent.click(screen.getByRole('button', { name: 'Expand checklist' }));
            expect(screen.getByText('Add APIs')).toBeInTheDocument();
        });
    });

    describe('item states', () => {
        it('renders an action link for a normal item', () => {
            wrap(<OverviewChecklistCard description="desc" items={[BASE_ITEM]} />);
            expect(screen.getByRole('link', { name: 'Open APIs' })).toBeInTheDocument();
        });

        it('renders "Coming soon" text instead of a link for comingSoon items', () => {
            wrap(<OverviewChecklistCard description="desc" items={[{ ...BASE_ITEM, to: undefined, comingSoon: true }]} />);
            expect(screen.getByText('Coming soon')).toBeInTheDocument();
            expect(screen.queryByRole('link', { name: 'Open APIs' })).toBeNull();
        });
    });

    describe('manual toggle', () => {
        it('calls onToggle with newDone=true when an unchecked toggleable row is clicked', () => {
            const onToggle = jest.fn();
            wrap(<OverviewChecklistCard description="desc" items={[BASE_ITEM]} onToggle={onToggle} />);
            fireEvent.click(screen.getByRole('checkbox', { name: 'Add APIs' }));
            expect(onToggle).toHaveBeenCalledWith('item-1', true);
        });

        it('calls onToggle with newDone=false when a checked non-locked row is clicked', () => {
            const onToggle = jest.fn();
            wrap(<OverviewChecklistCard description="desc" items={[{ ...BASE_ITEM, done: true }]} onToggle={onToggle} />);
            fireEvent.click(screen.getByRole('checkbox', { name: 'Add APIs' }));
            expect(onToggle).toHaveBeenCalledWith('item-1', false);
        });

        it('calls onToggle with newDone=false when an auto-done row is clicked (all rows are toggleable)', () => {
            const onToggle = jest.fn();
            wrap(<OverviewChecklistCard description="desc" items={[{ ...BASE_ITEM, done: true }]} onToggle={onToggle} />);
            fireEvent.click(screen.getByRole('checkbox', { name: 'Add APIs' }));
            expect(onToggle).toHaveBeenCalledWith('item-1', false);
        });

        it('does not call onToggle when a comingSoon row is clicked', () => {
            const onToggle = jest.fn();
            wrap(
                <OverviewChecklistCard
                    description="desc"
                    items={[{ ...BASE_ITEM, to: undefined, comingSoon: true }]}
                    onToggle={onToggle}
                />,
            );
            expect(screen.queryByRole('checkbox', { name: 'Add APIs' })).toBeNull();
        });

        it('does not render rows as checkboxes when onToggle is not provided', () => {
            wrap(<OverviewChecklistCard description="desc" items={[BASE_ITEM]} />);
            expect(screen.queryByRole('checkbox')).toBeNull();
        });

        it('calls onToggle via keyboard Enter on a toggleable row', () => {
            const onToggle = jest.fn();
            wrap(<OverviewChecklistCard description="desc" items={[BASE_ITEM]} onToggle={onToggle} />);
            fireEvent.keyDown(screen.getByRole('checkbox', { name: 'Add APIs' }), { key: 'Enter' });
            expect(onToggle).toHaveBeenCalledWith('item-1', true);
        });

        it('clicking the info tooltip button does not trigger the row toggle', () => {
            const onToggle = jest.fn();
            wrap(<OverviewChecklistCard description="desc" items={[BASE_ITEM]} onToggle={onToggle} />);
            fireEvent.click(screen.getByRole('button', { name: 'Info: Add APIs' }));
            expect(onToggle).not.toHaveBeenCalled();
        });

        it('clicking the action link does not trigger the row toggle', () => {
            const onToggle = jest.fn();
            wrap(<OverviewChecklistCard description="desc" items={[BASE_ITEM]} onToggle={onToggle} />);
            fireEvent.click(screen.getByRole('link', { name: 'Open APIs' }));
            expect(onToggle).not.toHaveBeenCalled();
        });
    });
});
