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
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PolicyStatementCard } from '../PolicyStatementCard';
import type { PolicyStatement } from '../statement-to-gapl';

const baseStatement: PolicyStatement = {
    id: 'stmt-1',
    effect: 'permit',
    principals: [],
    actions: [],
    resources: [],
    condition: '',
};

const principalOptions = [
    { id: 'p1', label: 'alice', group: 'User' },
    { id: 'p2', label: 'ops-team', group: 'Group' },
];

const actionOptions = [
    { id: 'a1', label: 'can_read', group: 'Action' },
    { id: 'a2', label: 'can_write', group: 'Action' },
];

const resourceOptions = [{ id: 'r1', label: 'get_flight', group: 'MCPTool' }];

describe('PolicyStatementCard', () => {
    it('renders the statement index', () => {
        render(
            <PolicyStatementCard
                index={2}
                statement={baseStatement}
                principalOptions={principalOptions}
                actionOptions={actionOptions}
                resourceOptions={resourceOptions}
                resourceGroups={[{ key: 'MCPTool', label: 'Tools' }]}
                onChange={vi.fn()}
                onDuplicate={vi.fn()}
                onDelete={vi.fn()}
            />,
        );

        expect(screen.getByText(/#3/)).toBeInTheDocument();
    });

    it('switches effect to forbid', async () => {
        const onChange = vi.fn();

        render(
            <PolicyStatementCard
                index={0}
                statement={baseStatement}
                principalOptions={principalOptions}
                actionOptions={actionOptions}
                resourceOptions={resourceOptions}
                resourceGroups={[{ key: 'MCPTool', label: 'Tools' }]}
                onChange={onChange}
                onDuplicate={vi.fn()}
                onDelete={vi.fn()}
            />,
        );

        await userEvent.click(screen.getByRole('radio', { name: /forbid/ }));

        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ effect: 'forbid' }));
    });

    it('calls onDelete when delete button clicked', async () => {
        const onDelete = vi.fn();

        render(
            <PolicyStatementCard
                index={0}
                statement={baseStatement}
                principalOptions={principalOptions}
                actionOptions={actionOptions}
                resourceOptions={resourceOptions}
                resourceGroups={[{ key: 'MCPTool', label: 'Tools' }]}
                onChange={vi.fn()}
                onDuplicate={vi.fn()}
                onDelete={onDelete}
            />,
        );

        await userEvent.click(screen.getByRole('button', { name: /delete statement/i }));

        expect(onDelete).toHaveBeenCalled();
    });

    it('calls onDuplicate when duplicate button clicked', async () => {
        const onDuplicate = vi.fn();

        render(
            <PolicyStatementCard
                index={0}
                statement={baseStatement}
                principalOptions={principalOptions}
                actionOptions={actionOptions}
                resourceOptions={resourceOptions}
                resourceGroups={[{ key: 'MCPTool', label: 'Tools' }]}
                onChange={vi.fn()}
                onDuplicate={onDuplicate}
                onDelete={vi.fn()}
            />,
        );

        await userEvent.click(screen.getByRole('button', { name: /duplicate statement/i }));

        expect(onDuplicate).toHaveBeenCalled();
    });

    it('shows move-up button when canMoveUp is true', () => {
        render(
            <PolicyStatementCard
                index={1}
                statement={baseStatement}
                principalOptions={principalOptions}
                actionOptions={actionOptions}
                resourceOptions={resourceOptions}
                resourceGroups={[]}
                onChange={vi.fn()}
                onDuplicate={vi.fn()}
                onDelete={vi.fn()}
                canMoveUp={true}
                onMoveUp={vi.fn()}
            />,
        );

        expect(screen.getByRole('button', { name: /move statement up/i })).toBeInTheDocument();
    });

    it('renders all three chip pickers with empty-state hints supplied', () => {
        render(
            <PolicyStatementCard
                index={0}
                statement={baseStatement}
                principalOptions={[]}
                actionOptions={[]}
                resourceOptions={[]}
                resourceGroups={[]}
                onChange={vi.fn()}
                onDuplicate={vi.fn()}
                onDelete={vi.fn()}
                emptyActionsHint="Define actions in your schema."
                emptyPrincipalsHint="Add some principals first."
            />,
        );

        expect(screen.getByRole('combobox', { name: /add principal/i })).toBeInTheDocument();
        expect(screen.getByRole('combobox', { name: /add action/i })).toBeInTheDocument();
        expect(screen.getByRole('combobox', { name: /add resource/i })).toBeInTheDocument();
    });

    it('shows existing condition text when statement has a condition', () => {
        const stmt: PolicyStatement = { ...baseStatement, condition: 'context.time.hour >= 9' };

        render(
            <PolicyStatementCard
                index={0}
                statement={stmt}
                principalOptions={[]}
                actionOptions={[]}
                resourceOptions={[]}
                resourceGroups={[]}
                onChange={vi.fn()}
                onDuplicate={vi.fn()}
                onDelete={vi.fn()}
            />,
        );

        expect(screen.getByDisplayValue('context.time.hour >= 9')).toBeInTheDocument();
    });
});
