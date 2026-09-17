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

function renderTable(overrides: Partial<{ canEdit: boolean; canDelete: boolean; rules: QualityRule[] }> = {}) {
    const onEdit = jest.fn();
    const onDelete = jest.fn();
    render(
        <QualityRulesTable
            rules={overrides.rules ?? RULES}
            canEdit={overrides.canEdit ?? true}
            canDelete={overrides.canDelete ?? true}
            onEdit={onEdit}
            onDelete={onDelete}
        />,
    );
    return { onEdit, onDelete };
}

describe('QualityRulesTable', () => {
    it('renders a row per rule with its name and description', () => {
        renderTable();
        expect(screen.getByText('OpenAPI specification is complete')).toBeInTheDocument();
        expect(screen.getByText('A team owns the API.')).toBeInTheDocument();
        expect(screen.getByRole('columnheader', { name: /rule name/i })).toBeInTheDocument();
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
