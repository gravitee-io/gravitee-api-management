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

import { QualityRulesTable } from './QualityRulesTable';
import type { QualityRule } from '../types/qualityRule';

const RULES: QualityRule[] = [
    { id: 'rule-openapi', name: 'OpenAPI specification is complete', description: 'Every operation is documented.', weight: 0 },
    { id: 'rule-owner', name: 'Primary owner is a group', description: 'A team owns the API.', weight: 0 },
];

function buildRules(count: number): QualityRule[] {
    return Array.from({ length: count }, (_, index) => ({
        id: `rule-${index}`,
        name: `Rule ${index}`,
        description: `Description ${index}`,
        weight: 0,
    }));
}

function renderTable(overrides: Partial<{ canEdit: boolean; canDelete: boolean; rules: QualityRule[] }> = {}) {
    const onEdit = jest.fn();
    const onDelete = jest.fn();
    const { rerender } = render(
        <QualityRulesTable
            rules={overrides.rules ?? RULES}
            canEdit={overrides.canEdit ?? true}
            canDelete={overrides.canDelete ?? true}
            onEdit={onEdit}
            onDelete={onDelete}
        />,
    );
    const rerenderWith = (rules: QualityRule[]) =>
        rerender(
            <QualityRulesTable
                rules={rules}
                canEdit={overrides.canEdit ?? true}
                canDelete={overrides.canDelete ?? true}
                onEdit={onEdit}
                onDelete={onDelete}
            />,
        );
    return { onEdit, onDelete, rerenderWith };
}

describe('QualityRulesTable', () => {
    it('renders a row per rule with its name and description', () => {
        renderTable();
        expect(screen.getByText('OpenAPI specification is complete')).toBeInTheDocument();
        expect(screen.getByText('A team owns the API.')).toBeInTheDocument();
        expect(screen.getByRole('columnheader', { name: /rule name/i })).toBeInTheDocument();
    });

    it('paginates beyond the first page', async () => {
        renderTable({ rules: buildRules(12) });

        expect(screen.getByText('Rule 0')).toBeInTheDocument();
        expect(screen.queryByText('Rule 10')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: /next page/i }));

        expect(screen.getByText('Rule 10')).toBeInTheDocument();
        expect(screen.queryByText('Rule 0')).not.toBeInTheDocument();
    });

    it('falls back to the last page with rules when deletions empty the current one', async () => {
        const { rerenderWith } = renderTable({ rules: buildRules(12) });
        await userEvent.click(screen.getByRole('button', { name: /next page/i }));
        expect(screen.getByText('Rule 10')).toBeInTheDocument();

        // The reviewer deletes the rules that filled page two.
        rerenderWith(buildRules(4));

        expect(screen.getByText('Rule 0')).toBeInTheDocument();
        expect(screen.getByText('Rule 3')).toBeInTheDocument();
        expect(screen.queryByText('No manual rules to display.')).not.toBeInTheDocument();
    });

    it('shows the empty message when there are no rules', () => {
        renderTable({ rules: [] });
        expect(screen.getByText('No manual rules to display.')).toBeInTheDocument();
    });

    it('offers Edit and Delete from the row menu and reports the chosen rule', async () => {
        const user = userEvent.setup();
        const { onEdit, onDelete } = renderTable();
        await user.click(screen.getByRole('button', { name: 'Actions for Primary owner is a group' }));
        await user.click(screen.getByRole('menuitem', { name: /edit/i }));
        expect(onEdit).toHaveBeenCalledWith(RULES[1]);

        await user.click(screen.getByRole('button', { name: 'Actions for Primary owner is a group' }));
        await user.click(screen.getByRole('menuitem', { name: /delete/i }));
        expect(onDelete).toHaveBeenCalledWith(RULES[1]);
    });

    it('hides the row menu entirely when the user can neither edit nor delete', () => {
        renderTable({ canEdit: false, canDelete: false });
        expect(screen.queryByRole('button', { name: /actions for/i })).not.toBeInTheDocument();
    });

    it('only offers the action the user is allowed to', async () => {
        const user = userEvent.setup();
        renderTable({ canEdit: false });
        await user.click(screen.getByRole('button', { name: 'Actions for Primary owner is a group' }));
        expect(screen.queryByRole('menuitem', { name: /edit/i })).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: /delete/i })).toBeInTheDocument();
    });
});
